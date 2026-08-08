package com.github.dumann089.theatricalextralights.client.blockentities;

import com.github.dumann089.theatricalextralights.blockentities.interfaces.HasGobo;
import com.github.dumann089.theatricalextralights.config.TheatricalExtraLightsConfig;
import com.github.dumann089.theatricalextralights.client.ModShaders;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.imabad.theatrical.blocks.HangableBlock;
import dev.imabad.theatrical.client.LazyRenderers;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class GoboGPUProjector {

    // ── Reused per-instance temporaries (never allocate in hot paths) ──────────
    private final Vector4f raycastOriginVec = new Vector4f();
    private final Vector4f raycastDirVec    = new Vector4f();
    private final PoseStack projectorPoseStack = new PoseStack();

    // ── Reused Vector4f slots for the lazy-render closure ──────────────────────
    private final Vector4f tmpLightPos = new Vector4f();
    private final Vector4f tmpLightDir = new Vector4f();
    private final Vector4f tmpAxisU    = new Vector4f();
    private final Vector4f tmpAxisV    = new Vector4f();

    private static final Direction[] DIRS = Direction.values();

    // ── Geometry cache ─────────────────────────────────────────────────────────
    private int   cachedGeoHash   = Integer.MIN_VALUE;
    private long  lastRebuildNanos = 0L;
    private final LongOpenHashSet uniqueBlocks = new LongOpenHashSet(512);

    // ── Control de movimiento para rebuild adaptativo (SIN threads) ────────────
    // Mismo propósito que antes: si el fixture está moviendo pan/tilt activamente,
    // usamos un ray budget más barato porque el jugador solo ve ese ángulo una
    // fracción de segundo. Todo esto corre síncrono en el render thread, no hay
    // ningún Thread/Executor involucrado — es solo aritmética de comparación.
    private float lastKnownPan  = Float.NaN;
    private float lastKnownTilt = Float.NaN;
    private int   stillFrameCount = 0;
    private static final float MOTION_THRESHOLD_DEG   = 0.15f;
    private static final int   STILL_FRAMES_REQUIRED  = 4;
    // Cooldowns reemplazan el MIN_REBUILD_INTERVAL_NS único de arriba con dos
    // valores: más espaciado mientras se mueve, más rápido apenas se frena.
    private static final long  REBUILD_COOLDOWN_MOVING_NS = 200_000_000L; // 200 ms
    private static final long  REBUILD_COOLDOWN_STILL_NS  = 60_000_000L;  // 60 ms

    // Flat VBO: 3 floats per corner × 4 corners = 12 floats per quad
    private float[] cachedVerts    = new float[2048 * 12];
    private int     cachedQuadCount = 0;

    // Reusable MutableBlockPos for DDA — eliminates one allocation per isOccludedFast call
    private final BlockPos.MutableBlockPos ddaCheckPos = new BlockPos.MutableBlockPos();

    // ── Tunables ───────────────────────────────────────────────────────────────
    /**
     * Hard cap on the scan radius (blocks). Keeps the triple-loop bounded even
     * at wide zoom angles.
     */
    private static final float MAX_SCAN_RADIUS = 6.0f;

    /**
     * Scan length at maximum zoom (widest cone, zoomNorm = 1.0).
     * The gobo stops being projected past this distance when fully open.
     */
    private static final float SCAN_LEN_ZOOM_MAX = 8.0f;

    // ══════════════════════════════════════════════════════════════════════════
    //  Public entry point
    // ══════════════════════════════════════════════════════════════════════════

    public <T extends BlockEntity & HasGobo> void render(
            T be, MultiBufferSource multiBufferSource, Direction facing, float partialTicks,
            boolean isFlipped, BlockState blockState, boolean isHanging, Vec3 localLensOffset,
            float[] panPivot, float[] tiltPivot, float[] structuralTransform,
            float minAngle, float maxAngle, float smoothPan, float smoothTilt) {

        float intensity01 = be.getPartialIntensity(partialTicks) / 255f;
        if (intensity01 <= 0f || be.getLevel() == null) return;

        int   colour   = be.getColour() == 0 ? 0xFFFFFF : be.getColour();
        // --- USAMOS LOS VALORES SUAVES EN LUGAR DE LOS DEL BLOCK ENTITY ---
        float panDeg   = smoothPan;
        float tiltDeg  = smoothTilt;
        float zoomNorm = be.getPartialZoom(partialTicks) / 255f;
        float coneHalfAngle = minAngle + zoomNorm * (maxAngle - minAngle);

        projectorPoseStack.setIdentity();
        applyFixtureOrientation(projectorPoseStack, facing, isFlipped, blockState, isHanging,
                panDeg, tiltDeg, panPivot, tiltPivot, structuralTransform);
        projectorPoseStack.translate(localLensOffset.x, localLensOffset.y, localLensOffset.z);

        Matrix4f m = projectorPoseStack.last().pose();
        raycastOriginVec.set(0f, 0f, 0f, 1f).mul(m);
        raycastDirVec   .set(0f, 0f, -1f, 0f).mul(m);

        Vec3 origin  = Vec3.atLowerCornerOf(be.getBlockPos())
                .add(raycastOriginVec.x, raycastOriginVec.y, raycastOriginVec.z);
        Vec3 beamDir = new Vec3(raycastDirVec.x, raycastDirVec.y, raycastDirVec.z).normalize();

        // ── Entity hit → shorten beam ──────────────────────────────────────────
        float finalLen = TheatricalExtraLightsConfig.getLaserBeamLength() * 1.5f;
        Vec3  centerEnd = origin.add(beamDir.scale(finalLen));
        AABB  beamVolume = new AABB(origin, centerEnd).inflate(3.0);

        List<LivingEntity> entities = be.getLevel().getEntitiesOfClass(
                LivingEntity.class, beamVolume, e -> !e.isSpectator());
        for (LivingEntity entity : entities) {
            Optional<Vec3> hit = entity.getBoundingBox().clip(origin, centerEnd);
            if (hit.isPresent()) {
                float d = (float) origin.distanceToSqr(hit.get());
                if (d < finalLen * finalLen) finalLen = (float) Math.sqrt(d);
            }
        }

        // ── Beam axes (with optional gobo rotation) ───────────────────────────
        Vec3 axisU, axisV;
        {
            Vec3 up = Math.abs(beamDir.y) > 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
            axisU = beamDir.cross(up).normalize();
            axisV = beamDir.cross(axisU).normalize();

            float goboRot = be.getGoboRotation();
            if (Math.abs(goboRot) > 0.001f) {
                double rad = Math.toRadians(goboRot);
                double cos = Math.cos(rad), sin = Math.sin(rad);
                Vec3 oldU = axisU;
                axisU = oldU.scale(cos).add(axisV.scale(sin)).normalize();
                axisV = oldU.scale(-sin).add(axisV.scale(cos)).normalize();
            }
        }

        float tanHalfAngle = (float) Math.tan(Math.toRadians(coneHalfAngle));

        // ── Snapshot values for the lazy closure (primitives / immutable refs) ─
        final int    finalColour    = colour;
        final float  finalIntensity = intensity01;
        final int    finalGoboSlot  = be.getGobo();
        final BlockPos bePos        = be.getBlockPos();
        final Level  level          = be.getLevel();
        final Vec3   finalOrigin    = origin;
        final Vec3   finalBeamDir   = beamDir;
        final Vec3   finalAxisU     = axisU;
        final Vec3   finalAxisV     = axisV;
        final float  finalMaxLen    = finalLen;
        final float  finalZoomNorm  = zoomNorm;   // 0 = narrow, 1 = wide

        LazyRenderers.addLazyRender(new LazyRenderers.LazyRenderer() {

            @Override
            public void render(MultiBufferSource.BufferSource bufferSource,
                               PoseStack poseStack, Camera camera, float partialTick) {

                Vec3 camPos = camera.getPosition();

                // ── Transform light vectors into view space ────────────────────
                poseStack.pushPose();
                poseStack.translate(-camPos.x, -camPos.y, -camPos.z);
                Matrix4f matrix = poseStack.last().pose();

                // Reuse pre-allocated Vector4f slots — zero allocations here
                tmpLightPos.set((float) finalOrigin.x, (float) finalOrigin.y,
                        (float) finalOrigin.z, 1.0f);
                matrix.transform(tmpLightPos);

                tmpLightDir.set((float) finalBeamDir.x, (float) finalBeamDir.y,
                        (float) finalBeamDir.z, 0.0f);
                matrix.transform(tmpLightDir);
                tmpLightDir.normalize();

                tmpAxisU.set((float) finalAxisU.x, (float) finalAxisU.y,
                        (float) finalAxisU.z, 0.0f);
                matrix.transform(tmpAxisU);
                tmpAxisU.normalize();

                tmpAxisV.set((float) finalAxisV.x, (float) finalAxisV.y,
                        (float) finalAxisV.z, 0.0f);
                matrix.transform(tmpAxisV);
                tmpAxisV.normalize();

                // ── Vertex buffer setup ───────────────────────────────────────
                ResourceLocation texture    = be.getGoboLibrary().getTexture(finalGoboSlot);
                RenderType       renderType = ModShaders.getGoboRenderType(texture);
                VertexConsumer   vc         = bufferSource.getBuffer(renderType);

                int   r          = (finalColour >> 16) & 0xFF;
                int   g          = (finalColour >> 8)  & 0xFF;
                int   b          = finalColour          & 0xFF;
                int   finalAlpha = (int)(Math.min(1f, finalIntensity * 1.5f) * 255f);

                // ── Zoom-driven scan length ───────────────────────────────────
                // zoomNorm=0 → narrow beam → projects far (up to MaxGoboDistance)
                // zoomNorm=1 → wide  beam → cuts off early (SCAN_LEN_ZOOM_MAX)
                //
                // Quadratic ease-in so the cut-off feels gradual at low zoom
                // and aggressive at high zoom, matching how a real gobo frosts out.
                // Scan length at min zoom = full configured gobo distance.
                // The ray-march rebuild handles long projections cheaply now,
                // so no internal cap is needed — let the config decide.
                float scanLenAtMin  = TheatricalExtraLightsConfig.getMaxGoboDistance();
                float zoomT         = finalZoomNorm * finalZoomNorm; // ease-in^2
                float scanLen       = scanLenAtMin + zoomT * (SCAN_LEN_ZOOM_MAX - scanLenAtMin);
                // Also respect the actual beam stop (entity hit, etc.)
                scanLen = Math.min(scanLen, finalMaxLen);

                // Coarser hash quantisation so partial-tick interpolation jitter
                // (sub-degree pan/tilt drift, sub-block scan changes) does not
                // invalidate the cache every frame. Trade some precision in
                // when-to-rebuild for a stable cache hit during steady state.
                int qDirX = Math.round((float) finalBeamDir.x * 16f);
                int qDirY = Math.round((float) finalBeamDir.y * 16f);
                int qDirZ = Math.round((float) finalBeamDir.z * 16f);
                int qTan  = Math.round(tanHalfAngle * 20f);
                int qScan = Math.round(scanLen / 4f);   // bucket by 4 blocks

                int geoHash = java.util.Objects.hash(
                        bePos, qDirX, qDirY, qDirZ, qTan, qScan);

                // ── Shader uniforms (after scanLen is computed) ───────────────
                // MaxLen is set to scanLen so the shader's lengthFade matches
                // the zoom-driven cutoff exactly — no geometry/shader mismatch.
                ShaderInstance shader = ModShaders.goboProjectorShader;
                if (shader != null) {
                    shader.safeGetUniform("LightPos").set(tmpLightPos.x(), tmpLightPos.y(), tmpLightPos.z());
                    shader.safeGetUniform("LightDir").set(tmpLightDir.x(), tmpLightDir.y(), tmpLightDir.z());
                    shader.safeGetUniform("AxisU")   .set(tmpAxisU.x(),    tmpAxisU.y(),    tmpAxisU.z());
                    shader.safeGetUniform("AxisV")   .set(tmpAxisV.x(),    tmpAxisV.y(),    tmpAxisV.z());
                    shader.safeGetUniform("TanHalfAngle").set(tanHalfAngle);
                    shader.safeGetUniform("MaxLen")  .set(scanLen);
                    shader.safeGetUniform("MaxGoboDist")
                            .set(TheatricalExtraLightsConfig.getMaxGoboDistance());
                }

                // ── Rebuild adaptativo basado en detección de movimiento ───────
                // Comparamos el smoothPan/smoothTilt actual contra el del frame
                // anterior para saber si el fixture sigue en movimiento. Todo
                // esto es aritmética simple en el render thread — no hay
                // ningún Thread/Executor corriendo en paralelo.
                long nowNanos = System.nanoTime();

                boolean isMoving = false;
                if (!Float.isNaN(lastKnownPan) && !Float.isNaN(lastKnownTilt)) {
                    float dpan  = Math.abs(panDeg  - lastKnownPan);
                    float dtilt = Math.abs(tiltDeg - lastKnownTilt);
                    isMoving = (dpan > MOTION_THRESHOLD_DEG || dtilt > MOTION_THRESHOLD_DEG);
                }
                lastKnownPan  = panDeg;
                lastKnownTilt = tiltDeg;

                if (isMoving) {
                    stillFrameCount = 0;
                } else if (stillFrameCount < STILL_FRAMES_REQUIRED) {
                    stillFrameCount++;
                }

                boolean fixtureIsStill = (stillFrameCount >= STILL_FRAMES_REQUIRED);
                long cooldownNanos = fixtureIsStill ? REBUILD_COOLDOWN_STILL_NS : REBUILD_COOLDOWN_MOVING_NS;

                if (geoHash != cachedGeoHash) {
                    // Mismo throttle de antes (no rebuildear más seguido que el
                    // cooldown), pero el cooldown ahora depende de si el
                    // fixture está quieto o en movimiento.
                    if (cachedQuadCount == 0
                            || nowNanos - lastRebuildNanos >= cooldownNanos) {
                        rebuildGeometryCache(level, bePos, finalOrigin, finalBeamDir,
                                finalAxisU, finalAxisV,
                                scanLen, tanHalfAngle, geoHash, fixtureIsStill);
                        lastRebuildNanos = nowNanos;
                    }
                }

                // ── Emit cached quads every frame (no block lookups) ──────────
                for (int i = 0; i < cachedQuadCount; i++) {
                    int vIdx = i * 12;
                    vc.vertex(matrix, cachedVerts[vIdx],   cachedVerts[vIdx+1], cachedVerts[vIdx+2])
                            .color(r, g, b, finalAlpha).uv(0f, 0f).endVertex();
                    vc.vertex(matrix, cachedVerts[vIdx+3], cachedVerts[vIdx+4], cachedVerts[vIdx+5])
                            .color(r, g, b, finalAlpha).uv(0f, 0f).endVertex();
                    vc.vertex(matrix, cachedVerts[vIdx+6], cachedVerts[vIdx+7], cachedVerts[vIdx+8])
                            .color(r, g, b, finalAlpha).uv(0f, 0f).endVertex();
                    vc.vertex(matrix, cachedVerts[vIdx+9], cachedVerts[vIdx+10],cachedVerts[vIdx+11])
                            .color(r, g, b, finalAlpha).uv(0f, 0f).endVertex();
                }

                poseStack.popPose();
                bufferSource.endBatch(renderType);
            }

            @Override
            public Vec3 getPos(float partialTick) { return bePos.getCenter(); }
        });
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Geometry rebuild — called only when geoHash changes
    // ══════════════════════════════════════════════════════════════════════════

    /** Vogel-disk golden angle, gives uniform 2D disk sampling. */
    private static final double GOLDEN_ANGLE = 2.39996322972865332;

    /**
     * Rebuilds {@link #cachedVerts} and {@link #cachedQuadCount}.
     *
     * <p>Strategy: ray-march from the origin through the cone using a Vogel-disk
     * sample pattern. Each ray finds the FIRST solid block it hits within
     * {@code scanLen}. Hit blocks are deduplicated. This replaces the previous
     * volumetric scan (which iterated every block in the AABB and ran a per-face
     * DDA occlusion test for each) and is physically more correct — a gobo only
     * lights the first surface it strikes.
     *
     * <p>Cost is O(rayCount × averageRayLength) instead of
     * O(coneVolume × 6 × scanLen). For a 30-block projection with a typical
     * cone, this is roughly 10× faster while producing the same visual result.
     */
    private void rebuildGeometryCache(Level level, BlockPos bePos, Vec3 finalOrigin,
                                      Vec3 finalBeamDir, Vec3 finalAxisU, Vec3 finalAxisV,
                                      float scanLen, float tanHalfAngle, int newGeoHash,
                                      boolean fixtureIsStill) {

        cachedGeoHash  = newGeoHash;
        cachedQuadCount = 0;
        uniqueBlocks.clear();

        // Adaptive ray count: full precision (hasta 8192) cuando el fixture
        // está quieto; budget recortado (hasta 2048) mientras se mueve, porque
        // el jugador solo ve ese ángulo exacto una fracción de segundo. Esto
        // es la misma idea de antes, solo que ahora corre en el render thread
        // en vez de un worker — el costo por rebuild se reduce en vez de
        // paralelizarse.
        float projectedRadius = scanLen * tanHalfAngle;
        float projectedArea   = (float) (Math.PI * projectedRadius * projectedRadius);
        int   maxRays         = fixtureIsStill ? 8192 : 2048;
        int   rayCount        = Math.max(256, Math.min(maxRays, (int) (projectedArea * 16f) + 256));

        final double ox = finalOrigin.x;
        final double oy = finalOrigin.y;
        final double oz = finalOrigin.z;
        final double bx = finalBeamDir.x, by = finalBeamDir.y, bz = finalBeamDir.z;
        final double ux = finalAxisU.x,   uy = finalAxisU.y,   uz = finalAxisU.z;
        final double vxA = finalAxisV.x,  vyA = finalAxisV.y,  vzA = finalAxisV.z;

        BlockPos.MutableBlockPos hitPos = new BlockPos.MutableBlockPos();

        for (int i = 0; i < rayCount; i++) {
            // Vogel disk: uniform sampling of the cone's perpendicular disk.
            double progress = (i + 0.5d) / rayCount;
            double r        = Math.sqrt(progress) * tanHalfAngle;
            double theta    = i * GOLDEN_ANGLE;
            double cu       = r * Math.cos(theta);
            double cv       = r * Math.sin(theta);

            // Ray dir = beam + cu*U + cv*V, normalised.
            double dx = bx + cu * ux + cv * vxA;
            double dy = by + cu * uy + cv * vyA;
            double dz = bz + cu * uz + cv * vzA;
            double dlen = Math.sqrt(dx*dx + dy*dy + dz*dz);
            if (dlen < 1e-6) continue;
            dx /= dlen; dy /= dlen; dz /= dlen;

            // Walk the ray, find the first non-passthrough solid block.
            if (!ddaFirstHit(level, ox, oy, oz, dx, dy, dz, scanLen, bePos, hitPos)) continue;

            int relX = hitPos.getX() - bePos.getX();
            int relY = hitPos.getY() - bePos.getY();
            int relZ = hitPos.getZ() - bePos.getZ();
            long blockKey = (((long)(relX & 0xFFFF)) << 32)
                    | (((long)(relY & 0xFFFF)) << 16)
                    | ((long)(relZ & 0xFFFF));
            uniqueBlocks.add(blockKey);
        }

        // ── Neighbour expansion: fill gaps where rays passed between blocks ──
        // Solo corre cuando el fixture está quieto. Es pulido visual (rellena
        // huecos entre bloques adyacentes) y cuesta tiempo real de CPU en el
        // render thread — no vale la pena pagarlo para geometría que se va a
        // reemplazar en ~200ms mientras el fixture sigue moviendo.
        if (fixtureIsStill) {
            BlockPos.MutableBlockPos nbPos = new BlockPos.MutableBlockPos();
            LongOpenHashSet expansionSeed = uniqueBlocks;
            for (int round = 0; round < 2; round++) {
                LongOpenHashSet expansion = new LongOpenHashSet(expansionSeed.size() * 2);
                LongIterator hitIter = expansionSeed.iterator();
                while (hitIter.hasNext()) {
                    long bk = hitIter.nextLong();
                    int hRelX = (short)(bk >>> 32);
                    int hRelY = (short)(bk >>> 16);
                    int hRelZ = (short)(bk & 0xFFFF);
                    for (Direction d : DIRS) {
                        // Skip neighbours along the beam axis — they sit above/below
                        // the hit surface, not in the projection plane.
                        double dotBeam = d.getStepX() * bx + d.getStepY() * by + d.getStepZ() * bz;
                        if (Math.abs(dotBeam) > 0.5) continue;

                        int nRelX = hRelX + d.getStepX();
                        int nRelY = hRelY + d.getStepY();
                        int nRelZ = hRelZ + d.getStepZ();
                        long expKey = (((long)(nRelX & 0xFFFF)) << 32)
                                | (((long)(nRelY & 0xFFFF)) << 16)
                                | ((long)(nRelZ & 0xFFFF));
                        if (uniqueBlocks.contains(expKey)) continue; // already covered

                        int nAbsX = bePos.getX() + nRelX;
                        int nAbsY = bePos.getY() + nRelY;
                        int nAbsZ = bePos.getZ() + nRelZ;

                        // Cone inclusion with 0.5-block slack. Fragments truly
                        // outside the cone are still clipped by the shader.
                        double vxN = nAbsX + 0.5 - ox;
                        double vyN = nAbsY + 0.5 - oy;
                        double vzN = nAbsZ + 0.5 - oz;
                        double tN = vxN * bx + vyN * by + vzN * bz;
                        if (tN < 0 || tN > scanLen) continue;
                        double radSqN = (vxN * vxN + vyN * vyN + vzN * vzN) - tN * tN;
                        double maxRN = tN * tanHalfAngle + 0.5;
                        if (radSqN > maxRN * maxRN) continue;

                        nbPos.set(nAbsX, nAbsY, nAbsZ);
                        if (nbPos.equals(bePos)) continue;
                        BlockState ns = level.getBlockState(nbPos);
                        if (ns.isAir() || ns.getShape(level, nbPos).isEmpty()) continue;

                        ResourceLocation nkey = BuiltInRegistries.BLOCK.getKey(ns.getBlock());
                        if (nkey != null) {
                            String namespace = nkey.getNamespace();
                            if (namespace.equals("theatrical") || namespace.equals("theatricalextralights")) continue;
                            if (TheatricalExtraLightsConfig.isLaserPassThrough(nkey.toString())) continue;
                        }

                        expansion.add(expKey);
                    }
                }
                if (expansion.isEmpty()) break;
                uniqueBlocks.addAll(expansion);
                expansionSeed = expansion; // round 2 only iterates the new blocks
            }
        }

        // Build VBO data from accepted blocks
        final float zBias = 0.0002f;

        LongIterator iter = uniqueBlocks.iterator();
        while (iter.hasNext()) {
            long bk  = iter.nextLong();
            int  pdx = (short)(bk >>> 32);
            int  pdy = (short)(bk >>> 16);
            int  pdz = (short)(bk & 0xFFFF);

            BlockPos pos = bePos.offset(pdx, pdy, pdz);
            BlockState s = level.getBlockState(pos);
            if (s.isAir()) continue;

            // Per-block radial direction for back-face culling
            float cvx, cvy, cvz;
            double dirX = pos.getX() + 0.5 - finalOrigin.x;
            double dirY = pos.getY() + 0.5 - finalOrigin.y;
            double dirZ = pos.getZ() + 0.5 - finalOrigin.z;
            double dirLen = Math.sqrt(dirX*dirX + dirY*dirY + dirZ*dirZ);
            if (dirLen > 0.001) {
                cvx = (float)(dirX / dirLen);
                cvy = (float)(dirY / dirLen);
                cvz = (float)(dirZ / dirLen);
            } else {
                cvx = (float) finalBeamDir.x;
                cvy = (float) finalBeamDir.y;
                cvz = (float) finalBeamDir.z;
            }

            if (s.isCollisionShapeFullBlock(level, pos)) {
                AABB box = new AABB(pos.getX(), pos.getY(), pos.getZ(),
                        pos.getX() + 1.0, pos.getY() + 1.0, pos.getZ() + 1.0)
                        .inflate(zBias);
                for (int f = 0; f < 6; f++) {
                    Direction face = DIRS[f];
                    if ((face.getStepX() * cvx + face.getStepY() * cvy + face.getStepZ() * cvz) > 0.01f) continue;
                    BlockPos adj = pos.relative(face);
                    if (level.getBlockState(adj).isSolidRender(level, adj)) continue;
                    addQuadToVBO(box, f);
                }
            } else {
                for (AABB box : s.getShape(level, pos).toAabbs()) {
                    AABB wBox = box.move(pos).inflate(zBias);
                    for (int f = 0; f < 6; f++) {
                        Direction face = DIRS[f];
                        if ((face.getStepX() * cvx + face.getStepY() * cvy + face.getStepZ() * cvz) > 0.01f) continue;
                        BlockPos adj = pos.relative(face);
                        if (level.getBlockState(adj).isSolidRender(level, adj)) continue;
                        addQuadToVBO(wBox, f);
                    }
                }
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  DDA occlusion test
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Walks a ray from (sx,sy,sz) in direction (dx,dy,dz) using DDA traversal
     * and writes the first solid, non-passthrough block hit into {@code outHit}.
     *
     * @return {@code true} if a hit was found within {@code maxLen} blocks.
     */
    private boolean ddaFirstHit(Level level,
                                double sx, double sy, double sz,
                                double dx, double dy, double dz,
                                double maxLen, BlockPos bePos,
                                BlockPos.MutableBlockPos outHit) {
        int stepX = dx > 0 ? 1 : (dx < 0 ? -1 : 0);
        int stepY = dy > 0 ? 1 : (dy < 0 ? -1 : 0);
        int stepZ = dz > 0 ? 1 : (dz < 0 ? -1 : 0);

        double tDX = stepX != 0 ? Math.abs(1.0 / dx) : Double.MAX_VALUE;
        double tDY = stepY != 0 ? Math.abs(1.0 / dy) : Double.MAX_VALUE;
        double tDZ = stepZ != 0 ? Math.abs(1.0 / dz) : Double.MAX_VALUE;

        int vx = (int) Math.floor(sx);
        int vy = (int) Math.floor(sy);
        int vz = (int) Math.floor(sz);

        double tMX = stepX > 0 ? (vx + 1.0 - sx) * tDX : (stepX < 0 ? (sx - vx) * tDX : Double.MAX_VALUE);
        double tMY = stepY > 0 ? (vy + 1.0 - sy) * tDY : (stepY < 0 ? (sy - vy) * tDY : Double.MAX_VALUE);
        double tMZ = stepZ > 0 ? (vz + 1.0 - sz) * tDZ : (stepZ < 0 ? (sz - vz) * tDZ : Double.MAX_VALUE);

        double t = 0;
        BlockPos.MutableBlockPos check = ddaCheckPos;

        while (t < maxLen) {
            if (tMX < tMY && tMX < tMZ) { t = tMX; vx += stepX; tMX += tDX; }
            else if (tMY < tMZ)          { t = tMY; vy += stepY; tMY += tDY; }
            else                          { t = tMZ; vz += stepZ; tMZ += tDZ; }

            check.set(vx, vy, vz);
            if (check.equals(bePos)) continue;

            BlockState state = level.getBlockState(check);
            if (state.isAir() || state.getShape(level, check).isEmpty()) continue;

            // Skip mod/passthrough blocks — let the ray continue past them.
            ResourceLocation key = BuiltInRegistries.BLOCK.getKey(state.getBlock());
            if (key != null) {
                String ns = key.getNamespace();
                if (ns.equals("theatrical") || ns.equals("theatricalextralights")) continue;
                if (TheatricalExtraLightsConfig.isLaserPassThrough(key.toString())) continue;
            }

            // First real hit — record and stop.
            outHit.set(vx, vy, vz);
            return true;
        }
        return false;
    }


    // ══════════════════════════════════════════════════════════════════════════
    //  VBO helpers
    // ══════════════════════════════════════════════════════════════════════════

    private void addQuadToVBO(AABB box, int faceIdx) {
        if (cachedQuadCount * 12 >= cachedVerts.length) {
            cachedVerts = Arrays.copyOf(cachedVerts, cachedVerts.length * 2);
        }

        int   vIdx = cachedQuadCount * 12;
        float mx = (float) box.minX, my = (float) box.minY, mz = (float) box.minZ;
        float Mx = (float) box.maxX, My = (float) box.maxY, Mz = (float) box.maxZ;

        switch (DIRS[faceIdx]) {
            case DOWN:  writeQuad(vIdx, mx,my,Mz, mx,my,mz, Mx,my,mz, Mx,my,Mz); break;
            case UP:    writeQuad(vIdx, mx,My,mz, mx,My,Mz, Mx,My,Mz, Mx,My,mz); break;
            case NORTH: writeQuad(vIdx, Mx,my,mz, mx,my,mz, mx,My,mz, Mx,My,mz); break;
            case SOUTH: writeQuad(vIdx, mx,my,Mz, Mx,my,Mz, Mx,My,Mz, mx,My,Mz); break;
            case WEST:  writeQuad(vIdx, mx,my,mz, mx,my,Mz, mx,My,Mz, mx,My,mz); break;
            case EAST:  writeQuad(vIdx, Mx,my,Mz, Mx,my,mz, Mx,My,mz, Mx,My,Mz); break;
        }
        cachedQuadCount++;
    }

    private void writeQuad(int i,
                           float x1, float y1, float z1,
                           float x2, float y2, float z2,
                           float x3, float y3, float z3,
                           float x4, float y4, float z4) {
        cachedVerts[i++] = x1; cachedVerts[i++] = y1; cachedVerts[i++] = z1;
        cachedVerts[i++] = x2; cachedVerts[i++] = y2; cachedVerts[i++] = z2;
        cachedVerts[i++] = x3; cachedVerts[i++] = y3; cachedVerts[i++] = z3;
        cachedVerts[i++] = x4; cachedVerts[i++] = y4; cachedVerts[i]   = z4;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Fixture orientation — unchanged, no allocations removed here because
    //  PoseStack.mulPose already pools internally in Minecraft.
    // ══════════════════════════════════════════════════════════════════════════

    private static void applyFixtureOrientation(PoseStack ps, Direction facing,
                                                boolean isFlipped, BlockState blockState,
                                                boolean isHanging, float panDeg, float tiltDeg,
                                                float[] pans, float[] tilts,
                                                float[] structuralTransform) {
        ps.translate(0.5f, 0f, 0.5f);
        if (isHanging) {
            Direction hangDir = Direction.UP;
            try { hangDir = blockState.getValue(HangableBlock.HANG_DIRECTION); } catch (Exception ignored) {}
            ps.translate(0, 0.5, 0);
            if (hangDir.getAxis() != Direction.Axis.Y) {
                if (hangDir.getAxis() == Direction.Axis.Z) {
                    ps.mulPose(Axis.ZP.rotationDegrees(90));
                    ps.mulPose(hangDir == Direction.SOUTH
                            ? Axis.XP.rotationDegrees(-90)
                            : Axis.XP.rotationDegrees(90));
                } else {
                    ps.mulPose(Axis.ZN.rotationDegrees(-90));
                }
            }
            ps.translate(0, -0.5, 0);
        }
        ps.mulPose(Axis.YP.rotationDegrees(facing.toYRot()));
        ps.translate(-0.5f, 0f, -0.5f);
        if (isHanging) {
            ps.translate(structuralTransform[0], structuralTransform[1], structuralTransform[2]);
            ps.translate(0, -0.08, 0);
        }
        if (isFlipped) {
            ps.translate(0.5f, 0.5f, 0.5f);
            ps.mulPose(Axis.ZP.rotationDegrees(180));
            ps.translate(-0.5f, -0.5f, -0.5f);
        }
        ps.translate(pans[0], pans[1], pans[2]);
        ps.mulPose(Axis.YP.rotationDegrees(panDeg));
        ps.translate(-pans[0], -pans[1], -pans[2]);
        ps.translate(tilts[0], tilts[1], tilts[2]);
        ps.mulPose(isFlipped ? Axis.XP.rotationDegrees(-180) : Axis.XP.rotationDegrees(180));
        ps.mulPose(Axis.XP.rotationDegrees(tiltDeg));
        ps.translate(-tilts[0], -tilts[1], -tilts[2]);
    }
}