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

    // ══════════════════════════════════════════════════════════════════════════
//  Background rebuild infrastructure
// ══════════════════════════════════════════════════════════════════════════

    // Double-buffer: the render thread reads from the "front" buffer while
// the worker thread writes into the "back" buffer. No locking needed on
// the hot path — we only swap atomically once the worker is done.
    // Replaces frontVerts and frontQuadCount
    private volatile VboState frontState = new VboState(new float[2048 * 12], 0);

    private float[] backVerts = new float[2048 * 12];
    private int     backQuadCount = 0;

    // True while a background rebuild is in flight. Prevents queuing a second
// rebuild before the first one finishes.
    private final java.util.concurrent.atomic.AtomicBoolean rebuildInFlight =
            new java.util.concurrent.atomic.AtomicBoolean(false);

    // Shared executor — one daemon thread is enough since rebuilds are
// per-fixture and short-lived (< 5 ms at typical ray counts).
    // Reemplazar tu REBUILD_EXECUTOR actual por este:
    private static final java.util.concurrent.ExecutorService REBUILD_EXECUTOR =
            java.util.concurrent.Executors.newFixedThreadPool(
                    Math.max(1, Math.min(4, Runtime.getRuntime().availableProcessors() / 2)),
                    r -> {
                        Thread t = new Thread(r, "gobo-geometry-rebuild");
                        t.setDaemon(true);
                        t.setPriority(Thread.NORM_PRIORITY - 1); // Prioridad ligeramente baja para no afectar los ticks del server/render
                        return t;
                    });

    // ── GLOBAL rebuild throttle (shared across every GoboGPUProjector instance) ──
    // THIS IS THE MAIN FIX FOR THE LAG: rebuildInFlight above only stops a
    // SINGLE fixture from queueing twice, but does nothing to stop N fixtures
    // from all queueing AT THE SAME TIME when they all move at once (e.g. a
    // synced light show). Each rebuild walks up to 8192 DDA rays, so if many
    // fixtures move simultaneously the worker pool (only 1-4 threads) gets
    // flooded with queued jobs. This submission storm + resulting queue
    // backlog is what causes the FPS drop when moving gobos fast — work piles
    // up and starts contending with everything else on those CPU cores.
    // Capping global concurrent rebuilds keeps total background CPU usage
    // bounded no matter how many fixtures move at once.
    private static final int MAX_CONCURRENT_REBUILDS = 3;
    private static final java.util.concurrent.atomic.AtomicInteger GLOBAL_INFLIGHT_REBUILDS =
            new java.util.concurrent.atomic.AtomicInteger(0);

    // ── Reused Vector4f slots for the lazy-render closure ──────────────────────
    private final Vector4f tmpLightPos = new Vector4f();
    private final Vector4f tmpLightDir = new Vector4f();
    private final Vector4f tmpAxisU    = new Vector4f();
    private final Vector4f tmpAxisV    = new Vector4f();

    private static final Direction[] DIRS = Direction.values();

    // ── Variables para limitar la caída de FPS al mover suavemente ──────────────
    private long lastEntityCheckTime = 0;
    private long lastRebuildTime = 0;
    private float cachedMaxLen = 100.0f;

    // ── Control de movimiento para rebuild adaptativo ──────────────────────────
    // Guarda el pan/tilt del frame anterior para detectar si el fixture sigue moviendose
    private float lastKnownPan  = Float.NaN;
    private float lastKnownTilt = Float.NaN;
    // Cuantos frames consecutivos el fixture estuvo quieto
    private int   stillFrameCount = 0;
    // Threshold: diferencia de pan/tilt en grados que se considera "en movimiento"
    private static final float MOTION_THRESHOLD_DEG = 0.15f;
    // Frames quietos necesarios antes de permitir un rebuild
    private static final int   STILL_FRAMES_REQUIRED = 4;
    // Cooldown durante movimiento (ms) — rebuilds muy espaciados
    private static final long  REBUILD_COOLDOWN_MOVING_MS = 200L;
    // Cooldown en reposo (ms) — rebuild rapido cuando el fixture se detiene
    private static final long  REBUILD_COOLDOWN_STILL_MS  = 60L;

    // ── Geometry cache ─────────────────────────────────────────────────────────
    private int   cachedGeoHash   = Integer.MIN_VALUE;

    // Reusable MutableBlockPos for DDA — eliminates one allocation per isOccludedFast call

    // ── Tunables ───────────────────────────────────────────────────────────────
    /**
     * Hard cap on the scan radius (blocks). Keeps the triple-loop bounded even
     * at wide zoom angles.
     */
    private static final float MAX_SCAN_RADIUS = 6.0f;
    private static final float Z_BIAS = 0.02f;

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

        // ── Entity hit → shorten beam (OPTIMIZADO CON CACHÉ) ───────────────────
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastEntityCheckTime > 50) {
            float tempLen = TheatricalExtraLightsConfig.getLaserBeamLength() * 1.5f;
            Vec3  centerEnd = origin.add(beamDir.scale(tempLen));
            AABB  beamVolume = new AABB(origin, centerEnd).inflate(3.0);

            List<LivingEntity> entities = be.getLevel().getEntitiesOfClass(
                    LivingEntity.class, beamVolume, e -> !e.isSpectator());
            for (LivingEntity entity : entities) {
                Optional<Vec3> hit = entity.getBoundingBox().clip(origin, centerEnd);
                if (hit.isPresent()) {
                    float d = (float) origin.distanceToSqr(hit.get());
                    if (d < tempLen * tempLen) tempLen = (float) Math.sqrt(d);
                }
            }
            cachedMaxLen = tempLen;
            lastEntityCheckTime = currentTime;
        }
        float finalLen = cachedMaxLen;

        // ── Beam axes (with optional gobo rotation) ───────────────────────────
        Vec3 axisU, axisV;
        {
            Vector4f uVec = new Vector4f(1f, 0f, 0f, 0f).mul(m);
            Vector4f vVec = new Vector4f(0f, 1f, 0f, 0f).mul(m);

            axisU = new Vec3(uVec.x, uVec.y, uVec.z).normalize();
            axisV = new Vec3(vVec.x, vVec.y, vVec.z).normalize();

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
        final float  finalZoomNorm  = zoomNorm;

        LazyRenderers.addLazyRender(new LazyRenderers.LazyRenderer() {
            @Override
            public void render(MultiBufferSource.BufferSource bufferSource,
                               PoseStack poseStack, Camera camera, float partialTick) {

                Vec3 camPos = camera.getPosition();

                poseStack.pushPose();
                poseStack.translate(-camPos.x, -camPos.y, -camPos.z);
                Matrix4f matrix = poseStack.last().pose();

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

                ResourceLocation texture    = be.getGoboLibrary().getTexture(finalGoboSlot);
                RenderType       renderType = ModShaders.getGoboRenderType(texture);
                VertexConsumer   vc         = bufferSource.getBuffer(renderType);

                int   r          = (finalColour >> 16) & 0xFF;
                int   g          = (finalColour >> 8)  & 0xFF;
                int   b          = finalColour          & 0xFF;
                int   finalAlpha = (int)(Math.min(1f, finalIntensity * 1.5f) * 255f);

                float scanLenAtMin  = TheatricalExtraLightsConfig.getMaxGoboDistance();
                float zoomT         = finalZoomNorm * finalZoomNorm;
                float scanLen       = scanLenAtMin + zoomT * (SCAN_LEN_ZOOM_MAX - scanLenAtMin);
                scanLen = Math.min(scanLen, finalMaxLen);

                // Cuantización gruesa: solo cambia cuando el cono apunta a bloques distintos.
                // * 8 en vez de 16 → resolución ~7° por click (suficiente para detectar
                //   cambio de bloque objetivo sin disparar rebuild en cada frame de lerp).
                // * scanLen redondeada a 2 bloques para estabilidad adicional.
                int qDirX = Math.round((float) finalBeamDir.x * 8f);
                int qDirY = Math.round((float) finalBeamDir.y * 8f);
                int qDirZ = Math.round((float) finalBeamDir.z * 8f);
                int qTan  = Math.round(tanHalfAngle * 10f);
                int qScan = Math.round(scanLen / 2f);

                int geoHash = java.util.Objects.hash(
                        bePos, qDirX, qDirY, qDirZ, qTan, qScan);

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

                // ── Rebuild adaptativo basado en detección de movimiento ────────────
                // Detectamos si el fixture está en movimiento comparando pan/tilt actuales
                // con los del frame anterior. smoothPan/smoothTilt llegan como parámetros
                // del render() y son capturados en las finals de abajo.
                long renderTime = System.currentTimeMillis();

                boolean isMoving = false;
                if (!Float.isNaN(lastKnownPan) && !Float.isNaN(lastKnownTilt)) {
                    float dpan  = Math.abs(smoothPan  - lastKnownPan);
                    float dtilt = Math.abs(smoothTilt - lastKnownTilt);
                    isMoving = (dpan > MOTION_THRESHOLD_DEG || dtilt > MOTION_THRESHOLD_DEG);
                }
                lastKnownPan  = smoothPan;
                lastKnownTilt = smoothTilt;

                if (isMoving) {
                    stillFrameCount = 0;
                } else {
                    if (stillFrameCount < STILL_FRAMES_REQUIRED) stillFrameCount++;
                }

                boolean fixtureIsStill = (stillFrameCount >= STILL_FRAMES_REQUIRED);
                long cooldown = fixtureIsStill ? REBUILD_COOLDOWN_STILL_MS : REBUILD_COOLDOWN_MOVING_MS;

                if (geoHash != cachedGeoHash && (renderTime - lastRebuildTime > cooldown)) {
                    // Pass fixtureIsStill so the worker can use a much cheaper
                    // ray budget while the fixture is actively sweeping, and
                    // only spend full quality once it settles. The user can't
                    // tell the difference in geometry detail while the gobo
                    // is moving fast anyway, but the CPU savings are what
                    // stop the FPS drop.
                    scheduleRebuild(level, bePos, finalOrigin, finalBeamDir,
                            finalAxisU, finalAxisV,
                            scanLen, tanHalfAngle, geoHash, fixtureIsStill);
                    lastRebuildTime = renderTime;
                }

                VboState state = frontState;
                int snapCount = state.quadCount;
                float[] verts = state.verts;

                double ox = finalOrigin.x, oy = finalOrigin.y, oz = finalOrigin.z;
                double bx = finalBeamDir.x, by = finalBeamDir.y, bz = finalBeamDir.z;
                double ux = finalAxisU.x, uy = finalAxisU.y, uz = finalAxisU.z;
                double vxA = finalAxisV.x, vyA = finalAxisV.y, vzA = finalAxisV.z;

                for (int i = 0; i < snapCount; i++) {
                    int vIdx = i * 12;

                    for (int j = 0; j < 4; j++) {
                        float vx = verts[vIdx + (j * 3)];
                        float vy = verts[vIdx + (j * 3) + 1];
                        float vz = verts[vIdx + (j * 3) + 2];

                        double vecX = vx - ox;
                        double vecY = vy - oy;
                        double vecZ = vz - oz;

                        double zDist = vecX * bx + vecY * by + vecZ * bz;
                        double rZ = Math.max(zDist * tanHalfAngle, 0.0001);

                        double uDist = vecX * ux + vecY * uy + vecZ * uz;
                        double vDist = vecX * vxA + vecY * vyA + vecZ * vzA;

                        float u = (float) ((uDist / rZ) * 0.5 + 0.5);
                        float v = (float) (1.0 - ((vDist / rZ) * 0.5 + 0.5));
                        vc.vertex(matrix, vx, vy, vz).color(r, g, b, finalAlpha).uv(u, v).endVertex();
                    }
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
 * Vogel-disk ray-march geometry builder. Dispatched to a background thread
 * by {@link #scheduleRebuild}. Results are written into the back buffer and
 * then swapped atomically to the front buffer when done.
 */

    // ══════════════════════════════════════════════════════════════════════════
    //  DDA occlusion test
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Walks a ray from (sx,sy,sz) in direction (dx,dy,dz) using DDA traversal
     * and writes the first solid, non-passthrough block hit into {@code outHit}.
     *
     * @return {@code true} if a hit was found within {@code maxLen} blocks.
     */


    // ══════════════════════════════════════════════════════════════════════════
    //  VBO helpers
    // ══════════════════════════════════════════════════════════════════════════

    // ══════════════════════════════════════════════════════════════════════════
//  Async rebuild scheduling
// ══════════════════════════════════════════════════════════════════════════

    private static class VboState {
        final float[] verts;
        final int quadCount;

        VboState(float[] verts, int quadCount) {
            this.verts = verts;
            this.quadCount = quadCount;
        }
    }

    /**
     * Schedules a geometry rebuild on the background thread.
     * Sets cachedGeoHash immediately so the render thread stops re-queuing
     * for this exact geometry while the worker is running.
     * If a rebuild is already in flight, skips — it will finish soon and
     * the next frame will queue a new one if still dirty.
     */
    private void scheduleRebuild(Level level, BlockPos bePos, Vec3 finalOrigin,
                                 Vec3 finalBeamDir, Vec3 finalAxisU, Vec3 finalAxisV,
                                 float scanLen, float tanHalfAngle, int newGeoHash,
                                 boolean fixtureIsStill) {

        if (!rebuildInFlight.compareAndSet(false, true)) {
            return;
        }

        // ── Global concurrency gate ────────────────────────────────────────
        // Even though THIS fixture is free to rebuild, only let it actually
        // start if we're under the global cap. Otherwise skip this frame —
        // cachedGeoHash is NOT updated, so the fixture will simply try again
        // next frame (or as soon as the cooldown allows). This is what
        // prevents 10+ fixtures moving together from flooding the worker
        // pool and stalling the game.
        if (GLOBAL_INFLIGHT_REBUILDS.get() >= MAX_CONCURRENT_REBUILDS) {
            rebuildInFlight.set(false);
            return;
        }
        GLOBAL_INFLIGHT_REBUILDS.incrementAndGet();

        // Solo actualizamos el hash si hemos adquirido el "lock" y vamos a procesarlo
        cachedGeoHash = newGeoHash;

        // Capture immutable snapshots — nothing mutable is shared with the worker
        final double ox = finalOrigin.x, oy = finalOrigin.y, oz = finalOrigin.z;
        final double bx = finalBeamDir.x, by = finalBeamDir.y, bz = finalBeamDir.z;
        final double ux = finalAxisU.x,   uy = finalAxisU.y,   uz = finalAxisU.z;
        final double vxA = finalAxisV.x,  vyA = finalAxisV.y,  vzA = finalAxisV.z;
        final BlockPos bePosSnap = bePos.immutable();

        REBUILD_EXECUTOR.submit(() -> {
            try {
                rebuildGeometryCache(level, bePosSnap,
                        ox, oy, oz, bx, by, bz, ux, uy, uz, vxA, vyA, vzA,
                        scanLen, tanHalfAngle, fixtureIsStill);
            } finally {
                // Always clear the flags so the next frame can queue a new rebuild
                rebuildInFlight.set(false);
                GLOBAL_INFLIGHT_REBUILDS.decrementAndGet();
            }
        });
    }

// ══════════════════════════════════════════════════════════════════════════
//  Worker-thread geometry rebuild
// ══════════════════════════════════════════════════════════════════════════

    /**
     * Runs on the background thread. Writes results into the back buffer,
     * then atomically swaps it to the front buffer so the render thread
     * picks it up on the next frame with zero stall.
     * Uses its own local MutableBlockPos instances — never touches render-thread fields.
     */
    private void rebuildGeometryCache(Level level, BlockPos bePos,
                                      double ox, double oy, double oz,
                                      double bx, double by, double bz,
                                      double ux, double uy, double uz,
                                      double vxA, double vyA, double vzA,
                                      float scanLen, float tanHalfAngle,
                                      boolean fixtureIsStill) {

        // Worker-local collections — never shared with the render thread
        LongOpenHashSet localBlocks = new LongOpenHashSet(512);
        BlockPos.MutableBlockPos hitPos   = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos ddaLocal = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos nbPos    = new BlockPos.MutableBlockPos();

        float projectedRadius = scanLen * tanHalfAngle;
        float projectedArea   = (float)(Math.PI * projectedRadius * projectedRadius);

        // ── Adaptive ray budget ─────────────────────────────────────────────
        // While the fixture is actively sweeping, the player only sees the
        // gobo for a fraction of a second at any given angle — full geometry
        // precision is wasted work. Cut the ray cap hard during movement
        // (2048 instead of 8192) and skip neighbour-expansion entirely; both
        // are restored automatically once the fixture settles (fixtureIsStill)
        // and the next rebuild fires. This is the second half of the lag fix:
        // it shrinks the cost of EACH rebuild, while the global cap above
        // shrinks how many can run AT ONCE.
        int maxRays = fixtureIsStill ? 8192 : 2048;
        int rayCount = Math.max(256, Math.min(maxRays, (int)(projectedArea * 16f) + 256));

        for (int i = 0; i < rayCount; i++) {
            double progress = (i + 0.5d) / rayCount;
            double r        = Math.sqrt(progress) * tanHalfAngle;
            double theta    = i * GOLDEN_ANGLE;
            double cu       = r * Math.cos(theta);
            double cv       = r * Math.sin(theta);

            double dx = bx + cu * ux + cv * vxA;
            double dy = by + cu * uy + cv * vyA;
            double dz = bz + cu * uz + cv * vzA;
            double dlen = Math.sqrt(dx*dx + dy*dy + dz*dz);
            if (dlen < 1e-6) continue;
            dx /= dlen; dy /= dlen; dz /= dlen;

            if (!ddaFirstHitLocal(level, ox, oy, oz, dx, dy, dz, scanLen, bePos,
                    hitPos, ddaLocal)) continue;

            int relX = hitPos.getX() - bePos.getX();
            int relY = hitPos.getY() - bePos.getY();
            int relZ = hitPos.getZ() - bePos.getZ();
            long blockKey = (((long)(relX & 0xFFFF)) << 32)
                    | (((long)(relY & 0xFFFF)) << 16)
                    | ((long)(relZ & 0xFFFF));
            localBlocks.add(blockKey);
        }

        // Neighbour expansion — fills gaps where rays passed between blocks.
        // Skipped entirely while moving: it's pure visual-quality polish
        // (filling small gaps between adjacent lit blocks) and costs real
        // CPU time per round. Not worth paying for geometry that will be
        // replaced again within ~200ms anyway.
        if (fixtureIsStill) {
            LongOpenHashSet expansionSeed = localBlocks;
            for (int round = 0; round < 2; round++) {
                LongOpenHashSet expansion = new LongOpenHashSet(expansionSeed.size() * 2);
                LongIterator hitIter = expansionSeed.iterator();
                while (hitIter.hasNext()) {
                    long bk    = hitIter.nextLong();
                    int hRelX = (short)(bk >>> 32);
                    int hRelY = (short)(bk >>> 16);
                    int hRelZ = (short)(bk & 0xFFFF);
                    for (Direction d : DIRS) {
                        double dotBeam = d.getStepX() * bx + d.getStepY() * by + d.getStepZ() * bz;
                        if (Math.abs(dotBeam) > 0.5) continue;

                        int nRelX = hRelX + d.getStepX();
                        int nRelY = hRelY + d.getStepY();
                        int nRelZ = hRelZ + d.getStepZ();
                        long expKey = (((long)(nRelX & 0xFFFF)) << 32)
                                | (((long)(nRelY & 0xFFFF)) << 16)
                                | ((long)(nRelZ & 0xFFFF));
                        if (localBlocks.contains(expKey)) continue;

                        int nAbsX = bePos.getX() + nRelX;
                        int nAbsY = bePos.getY() + nRelY;
                        int nAbsZ = bePos.getZ() + nRelZ;

                        double vxN = nAbsX + 0.5 - ox;
                        double vyN = nAbsY + 0.5 - oy;
                        double vzN = nAbsZ + 0.5 - oz;
                        double tN  = vxN * bx + vyN * by + vzN * bz;
                        if (tN < 0 || tN > scanLen) continue;
                        double radSqN = (vxN * vxN + vyN * vyN + vzN * vzN) - tN * tN;
                        double maxRN  = tN * tanHalfAngle + 0.5;
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
                localBlocks.addAll(expansion);
                expansionSeed = expansion;
            }
        }

        // Build VBO data into the back buffer
        backQuadCount = 0;
        int needed = localBlocks.size() * 6 * 12;
        if (backVerts.length < needed) {
            backVerts = new float[needed + 256 * 12];
        }

        LongIterator iter = localBlocks.iterator();
        while (iter.hasNext()) {
            long bk  = iter.nextLong();
            int  pdx = (short)(bk >>> 32);
            int  pdy = (short)(bk >>> 16);
            int  pdz = (short)(bk & 0xFFFF);

            BlockPos pos = bePos.offset(pdx, pdy, pdz);
            BlockState s = level.getBlockState(pos);
            if (s.isAir()) continue;

            float cvx, cvy, cvz;
            double dirX  = pos.getX() + 0.5 - ox;
            double dirY  = pos.getY() + 0.5 - oy;
            double dirZ  = pos.getZ() + 0.5 - oz;
            double dirLen = Math.sqrt(dirX*dirX + dirY*dirY + dirZ*dirZ);
            if (dirLen > 0.001) {
                cvx = (float)(dirX / dirLen);
                cvy = (float)(dirY / dirLen);
                cvz = (float)(dirZ / dirLen);
            } else {
                cvx = (float) bx; cvy = (float) by; cvz = (float) bz;
            }

            if (s.isCollisionShapeFullBlock(level, pos)) {
                AABB box = new AABB(pos.getX(), pos.getY(), pos.getZ(),
                        pos.getX() + 1.0, pos.getY() + 1.0, pos.getZ() + 1.0);
                for (int f = 0; f < 6; f++) {
                    Direction face = DIRS[f];
                    if ((face.getStepX() * cvx + face.getStepY() * cvy + face.getStepZ() * cvz) > 0.01f) continue;
                    BlockPos adj = pos.relative(face);
                    if (level.getBlockState(adj).isSolidRender(level, adj)) continue;
                    addQuadToBack(box, f);
                }
            } else {
                for (AABB box : s.getShape(level, pos).toAabbs()) {
                    AABB wBox = box.move(pos);
                    for (int f = 0; f < 6; f++) {
                        Direction face = DIRS[f];
                        if ((face.getStepX() * cvx + face.getStepY() * cvy + face.getStepZ() * cvz) > 0.01f) continue;
                        BlockPos adj = pos.relative(face);
                        if (level.getBlockState(adj).isSolidRender(level, adj)) continue;
                        addQuadToBack(wBox, f);
                    }
                }
            }
        }
        // Atomic front-buffer swap using immutable container
        float[] oldFrontVerts = frontState.verts;
        frontState = new VboState(backVerts, backQuadCount);
        backVerts = oldFrontVerts;

    }

// ══════════════════════════════════════════════════════════════════════════
//  DDA — worker-thread variant (uses caller-supplied MutableBlockPos)
// ══════════════════════════════════════════════════════════════════════════

    /**
     * Identical to ddaFirstHit but accepts an external ddaCheck MutableBlockPos
     * so the worker thread never touches the render-thread-owned ddaCheckPos field.
     */
    private boolean ddaFirstHitLocal(Level level,
                                     double sx, double sy, double sz,
                                     double dx, double dy, double dz,
                                     double maxLen, BlockPos bePos,
                                     BlockPos.MutableBlockPos outHit,
                                     BlockPos.MutableBlockPos ddaCheck) {
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
        while (t < maxLen) {
            if (tMX < tMY && tMX < tMZ) { t = tMX; vx += stepX; tMX += tDX; }
            else if (tMY < tMZ)          { t = tMY; vy += stepY; tMY += tDY; }
            else                          { t = tMZ; vz += stepZ; tMZ += tDZ; }

            ddaCheck.set(vx, vy, vz);
            if (ddaCheck.equals(bePos)) continue;

            BlockState state = level.getBlockState(ddaCheck);
            if (state.isAir() || state.getShape(level, ddaCheck).isEmpty()) continue;

            ResourceLocation key = BuiltInRegistries.BLOCK.getKey(state.getBlock());
            if (key != null) {
                String ns = key.getNamespace();
                if (ns.equals("theatrical") || ns.equals("theatricalextralights")) continue;
                if (TheatricalExtraLightsConfig.isLaserPassThrough(key.toString())) continue;
            }

            outHit.set(vx, vy, vz);
            return true;
        }
        return false;
    }

// ══════════════════════════════════════════════════════════════════════════
//  Back-buffer VBO helpers (worker thread only)
// ══════════════════════════════════════════════════════════════════════════

    /**
     * Writes a quad into the back buffer. Mirrors addQuadToVBO but targets
     * backVerts/backQuadCount instead of the front buffer.
     */
    private void addQuadToBack(AABB box, int faceIdx) {
        if (backQuadCount * 12 >= backVerts.length) {
            backVerts = Arrays.copyOf(backVerts, backVerts.length * 2);
        }
        int   vIdx = backQuadCount * 12;
        float mx = (float) box.minX, my = (float) box.minY, mz = (float) box.minZ;
        float Mx = (float) box.maxX, My = (float) box.maxY, Mz = (float) box.maxZ;
        switch (DIRS[faceIdx]) {
            case DOWN:  writeQuadTo(backVerts, vIdx, mx,my-Z_BIAS,Mz, mx,my-Z_BIAS,mz, Mx,my-Z_BIAS,mz, Mx,my-Z_BIAS,Mz); break;
            case UP:    writeQuadTo(backVerts, vIdx, mx,My+Z_BIAS,mz, mx,My+Z_BIAS,Mz, Mx,My+Z_BIAS,Mz, Mx,My+Z_BIAS,mz); break;
            case NORTH: writeQuadTo(backVerts, vIdx, Mx,my,mz-Z_BIAS, mx,my,mz-Z_BIAS, mx,My,mz-Z_BIAS, Mx,My,mz-Z_BIAS); break;
            case SOUTH: writeQuadTo(backVerts, vIdx, mx,my,Mz+Z_BIAS, Mx,my,Mz+Z_BIAS, Mx,My,Mz+Z_BIAS, mx,My,Mz+Z_BIAS); break;
            case WEST:  writeQuadTo(backVerts, vIdx, mx-Z_BIAS,my,mz, mx-Z_BIAS,my,Mz, mx-Z_BIAS,My,Mz, mx-Z_BIAS,My,mz); break;
            case EAST:  writeQuadTo(backVerts, vIdx, Mx+Z_BIAS,my,Mz, Mx+Z_BIAS,my,mz, Mx+Z_BIAS,My,mz, Mx+Z_BIAS,My,Mz); break;
        }
        backQuadCount++;
    }

    /**
     * Writes 12 floats (4 corners × xyz) into an arbitrary float array at offset i.
     * Replaces the old writeQuad() which always targeted cachedVerts.
     */
    private static void writeQuadTo(float[] buf, int i,
                                    float x1, float y1, float z1,
                                    float x2, float y2, float z2,
                                    float x3, float y3, float z3,
                                    float x4, float y4, float z4) {
        buf[i++]=x1; buf[i++]=y1; buf[i++]=z1;
        buf[i++]=x2; buf[i++]=y2; buf[i++]=z2;
        buf[i++]=x3; buf[i++]=y3; buf[i++]=z3;
        buf[i++]=x4; buf[i++]=y4; buf[i]   =z4;
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