package com.github.dumann089.theatricalextralights.client.blockentities;

import com.github.dumann089.theatricalextralights.blockentities.LaserBlockEntity;
import com.github.dumann089.theatricalextralights.client.render.laser.LaserFigure;
import com.github.dumann089.theatricalextralights.client.render.laser.LaserRaymarchRenderer;
import com.github.dumann089.theatricalextralights.config.TheatricalExtraLightsConfig;
import com.github.dumann089.theatricalextralights.laser.LaserBeam;
import com.github.dumann089.theatricalextralights.laser.LaserPattern;
import com.github.dumann089.theatricalextralights.util.FixtureMountTransform;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.imabad.theatrical.TheatricalExpectPlatform;
import dev.imabad.theatrical.blocks.HangableBlock;
import dev.imabad.theatrical.client.LazyRenderers;
import com.github.dumann089.theatricalextralights.client.ExtraLightsRenderTypes;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

/**
 * Laser DMX. Deux chemins de rendu :
 * <ul>
 *   <li>realiste : la figure balayee est confiee a {@link LaserRaymarchRenderer}, qui integre
 *   faisceaux, nappes et impacts dans la brume avec le depth buffer de la scene ;</li>
 *   <li>legacy : rubans et rideaux plats dans le render type BEAM de Theatrical, quand le
 *   shader n'est pas disponible (Iris actif, moteur desactive dans les reglages).</li>
 * </ul>
 */
public class LaserRenderer extends ExtraLightsRenderer<LaserBlockEntity> {
    private BakedModel cachedPanModel, cachedTiltModel, cachedStaticModel;

    /** Hits within this distance of the laser are treated as the laser's own
     *  mounting structure (truss, frame, the laser block itself) and skipped.
     *  Real walls beyond this distance still block beams. */
    private static final float RAYCAST_SKIP_RADIUS = 2.5f;
    private static final long RAYCAST_TTL_NANOS = 150_000_000L;
    private static final long RAYCAST_SWEEP_NANOS = 2_000_000_000L;

    /** Tours de figure par seconde de la tete de balayage visible a persistance basse. */
    private static final float SCAN_HEAD_HZ = 6.5f;
    private static final double CORNER_DWELL_START = Math.toRadians(10.0);
    private static final double CORNER_DWELL_FULL = Math.toRadians(60.0);

    private final WeakHashMap<LaserBlockEntity, RaycastCache> raycastCaches = new WeakHashMap<>();

    public LaserRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void renderModel(LaserBlockEntity blockEntity, PoseStack poseStack, VertexConsumer vertexConsumer, Direction facing, float partialTicks, boolean isFlipped, BlockState blockState, boolean isHanging, int packedLight, int packedOverlay) {
        if (cachedStaticModel == null) {
            cachedStaticModel = TheatricalExpectPlatform.getBakedModel(blockEntity.getFixture().getStaticModel());
        }
        if (cachedPanModel == null) {
            cachedPanModel = TheatricalExpectPlatform.getBakedModel(blockEntity.getFixture().getPanModel());
        }
        if (cachedTiltModel == null) {
            cachedTiltModel = TheatricalExpectPlatform.getBakedModel(blockEntity.getFixture().getTiltModel());
        }
        poseStack.translate(0.5F, 0, .5F);
        if (isHanging) {
            Direction hangDirection = blockState.getValue(HangableBlock.HANG_DIRECTION);
            poseStack.translate(0, 0.5, 0F);
            if (hangDirection.getAxis() != Direction.Axis.Y) {
                if (hangDirection.getAxis() == Direction.Axis.Z) {
                    if (hangDirection == Direction.SOUTH) {
                        poseStack.mulPose(Axis.ZP.rotationDegrees(90));
                        poseStack.mulPose(Axis.XP.rotationDegrees(-90));
                    } else {
                        poseStack.mulPose(Axis.ZP.rotationDegrees(90));
                        poseStack.mulPose(Axis.XP.rotationDegrees(90));
                    }
                } else {
                    if (hangDirection == Direction.EAST) {
                        poseStack.mulPose(Axis.ZN.rotationDegrees(-90));
                    } else {
                        poseStack.mulPose(Axis.ZN.rotationDegrees(90));
                    }
                }
            }
            poseStack.translate(0, -0.5, 0F);
        }
        poseStack.mulPose(Axis.YP.rotationDegrees(facing.toYRot()));
        poseStack.translate(-0.5F, 0, -.5F);
        if (isHanging) {
            Optional<BlockState> optionalSupport = blockEntity.getSupportingStructure();
            if (optionalSupport.isPresent()) {
                float[] transforms = blockEntity.getFixture().getTransforms(blockState, optionalSupport.get());
                poseStack.translate(transforms[0], transforms[1], transforms[2]);
            } else {
                poseStack.translate(0, 0.19, 0);
            }
            poseStack.translate(0, -0.08, 0);
        }
        if (isFlipped) {
            poseStack.translate(0.5F, 0.5, .5F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(180));
            poseStack.translate(-0.5F, -0.5, -.5F);
        }
        minecraftRenderModel(poseStack, vertexConsumer, blockState, cachedStaticModel, packedLight, packedOverlay);
        float[] pans = blockEntity.getFixture().getPanRotationPosition();
        poseStack.translate(pans[0], pans[1], pans[2]);
        int prevPan = blockEntity.getPrevPan();
        int pan = blockEntity.getPan();
        poseStack.mulPose(Axis.YP.rotationDegrees((prevPan + (pan - prevPan) * partialTicks)));
        poseStack.translate(-pans[0], -pans[1], -pans[2]);
        minecraftRenderModel(poseStack, vertexConsumer, blockState, cachedPanModel, packedLight, packedOverlay);
        float[] tilts = blockEntity.getFixture().getTiltRotationPosition();
        poseStack.translate(tilts[0], tilts[1], tilts[2]);
        int prevTilt = blockEntity.getPrevTilt();
        int tilt = blockEntity.getTilt();
        if (isFlipped) {
            poseStack.mulPose(Axis.XP.rotationDegrees(-180));
        } else {
            poseStack.mulPose(Axis.XP.rotationDegrees(180));
        }
        poseStack.mulPose(Axis.XP.rotationDegrees((prevTilt + (tilt - prevTilt) * partialTicks)));
        poseStack.translate(-tilts[0], -tilts[1], -tilts[2]);
        minecraftRenderModel(poseStack, vertexConsumer, blockState, cachedTiltModel, packedLight, packedOverlay);
    }

    @Override
    public void beforeRenderBeam(LaserBlockEntity blockEntity, PoseStack poseStack, VertexConsumer vertexConsumer,
                                 MultiBufferSource multiBufferSource, Direction facing, float partialTicks, boolean isFlipped,
                                 BlockState blockstate, boolean isHanging, int packedLight, int packedOverlay) {
        if (blockEntity.getDisplayIntensity(partialTicks) <= 0 || blockEntity.isEmergencyStop()) {
            // Vaciar el rastro cuando el fixture está apagado para que no se quede congelado
            blockEntity.getTrailBuffer().clear();
            return;
        }

        if (LaserRaymarchRenderer.isAvailable()) {
            submitRealistic(blockEntity, facing, partialTicks, isFlipped, blockstate, isHanging);
            return;
        }

        LazyRenderers.addLazyRender(new LazyRenderers.LazyRenderer() {
            @Override
            public void render(MultiBufferSource.BufferSource bufferSource, PoseStack poseStack, Camera camera, float partialTick) {
                renderLegacy(blockEntity, bufferSource, poseStack, camera, facing, partialTick, isFlipped, blockstate, isHanging);
            }

            @Override
            public Vec3 getPos(float partialTick) {
                return blockEntity.getBlockPos().getCenter();
            }
        });
    }

    // ── Chemin realiste ──────────────────────────────────────────────────────

    /**
     * Construit la figure balayee en repere monde et la soumet au renderer laser. Les
     * directions sortent de la meme chaine de transformations que le modele (montage, accroche,
     * pan, tilt interpoles), donc faisceau et tete coincident quel que soit le montage.
     */
    private void submitRealistic(LaserBlockEntity be, Direction facing, float partialTicks,
                                 boolean isFlipped, BlockState blockstate, boolean isHanging) {
        if (be.getLevel() == null) {
            return;
        }
        float intensity = be.getDisplayIntensity(partialTicks);
        float intensity01 = Mth.clamp(intensity / 255f, 0f, 1f);
        if (intensity01 <= 0.001f) {
            return;
        }

        int c1 = be.getDisplayColour(partialTicks);
        int c2 = be.getColour2();
        int c3 = be.getColour3();
        if (c2 == 0) c2 = c1;
        if (c3 == 0) c3 = c2;

        double animTimeSec = (be.getLevel().getGameTime() + partialTicks) / 20.0;
        LaserPattern pattern = be.getPattern();
        List<LaserBeam> beams = pattern.generate(
                be.getSizeRaw(), be.getAmplitudeRaw(), be.getSpeedRaw(), be.getRotationRaw(),
                animTimeSec, c1, c2, c3);
        int n = Math.min(beams.size(), LaserFigure.MAX_SEGMENTS);
        if (n == 0) {
            return;
        }

        LaserFigure fig = LaserRaymarchRenderer.begin();
        if (fig == null) {
            return;
        }

        // Transformation locale -> bloc, translations comprises, identique au rendu.
        PoseStack local = new PoseStack();
        preparePoseStack(be, local, facing, partialTicks, isFlipped, blockstate, isHanging);
        Matrix4f m = local.last().pose();

        Vec3 baseOrigin = new Vec3(0.5F, 0.5F, 0.0F);
        if (isHanging) {
            baseOrigin = new Vec3(baseOrigin.x, 1.0 - baseOrigin.y, baseOrigin.z);
        }
        Vector4f o = new Vector4f((float) baseOrigin.x, (float) baseOrigin.y, (float) baseOrigin.z, 1f);
        m.transform(o);
        Vec3 originW = new Vec3(be.getBlockPos().getX() + o.x, be.getBlockPos().getY() + o.y, be.getBlockPos().getZ() + o.z);

        RaycastCache cache = raycastCaches.computeIfAbsent(be, k -> new RaycastCache());
        long now = System.nanoTime();
        cache.sweep(now);

        float baseLength = TheatricalExtraLightsConfig.getLaserBeamLength();
        Vec3[] dirs = new Vec3[n];
        float[] lens = new float[n];
        boolean[] hits = new boolean[n];
        Vector4f d = new Vector4f();
        boolean[] hitOut = new boolean[1];
        for (int i = 0; i < n; i++) {
            LaserBeam beam = beams.get(i);
            double y = Math.toRadians(beam.yawDeg);
            double p = Math.toRadians(beam.pitchDeg);
            double cp = Math.cos(p);
            d.set((float) (Math.sin(y) * cp), (float) -Math.sin(p), (float) (Math.cos(y) * cp), 0f);
            m.transform(d);
            Vec3 dir = new Vec3(d.x, d.y, d.z);
            dir = dir.lengthSqr() > 1.0e-10 ? dir.normalize() : new Vec3(0, 0, 1);
            dirs[i] = dir;
            float maxLen = baseLength * (beam.length / 32f);
            lens[i] = cache.length(be, originW, dir, maxLen, now, hitOut);
            hits[i] = hitOut[0];
        }

        // Liaisons entre points consecutifs d'un meme trace (et fermeture des traces clos).
        boolean closed = pattern.strokesClosed();
        int[] next = new int[n];
        int[] prev = new int[n];
        boolean[] hasIn = new boolean[n];
        java.util.Arrays.fill(prev, -1);
        int strokeStart = 0;
        for (int i = 0; i < n; i++) {
            int stroke = beams.get(i).stroke;
            if (i > 0 && beams.get(i - 1).stroke != stroke) {
                strokeStart = i;
            }
            boolean last = i + 1 >= n || beams.get(i + 1).stroke != stroke;
            if (!last) {
                next[i] = i + 1;
            } else if (closed && i - strokeStart >= 2) {
                next[i] = strokeStart;
            } else {
                next[i] = -1;
            }
            if (next[i] >= 0) {
                hasIn[next[i]] = true;
                prev[next[i]] = i;
            }
        }

        float persistence01 = Mth.clamp(be.getPersistenceRaw() / 255f, 0f, 1f);
        float[] spans = new float[n];
        double totalSpan = 0.0;
        for (int i = 0; i < n; i++) {
            if (next[i] < 0) {
                continue;
            }
            double dot = Mth.clamp(dirs[i].dot(dirs[next[i]]), -1.0, 1.0);
            spans[i] = (float) Math.acos(dot);
            if (spans[i] > 1.0e-4f) {
                totalSpan += spans[i];
            }
        }
        float[] path = new float[n];
        if (totalSpan > 0.0) {
            double cum = 0.0;
            for (int i = 0; i < n; i++) {
                path[i] = (float) (cum / totalSpan);
                if (next[i] >= 0) {
                    cum += spans[i];
                }
            }
        }

        // Temps de sejour du scanner sur chaque point : reparti sur tous les points quand la
        // persistance est basse, concentre sur les angles vifs et les bouts de trace sinon.
        // La puissance par point suit n^-0.75 plutot que 1/n : un motif de 72 faisceaux reste
        // lisible, comme le ferait l'exposition automatique d'une camera.
        float[] weight = new float[n];
        float share = (float) Math.pow(n, -0.75);
        // En (1 - p)^1.5 : a persistance moyenne la nappe porte deja le dessin, les faisceaux
        // de chaque sommet ne doivent pas la rayer de traits.
        float base = (float) Math.pow(1f - persistence01, 1.5) * share;
        for (int i = 0; i < n; i++) {
            boolean isolated = next[i] < 0 && !hasIn[i];
            if (isolated) {
                weight[i] = share;
                continue;
            }
            float dwell = 0f;
            if (prev[i] >= 0 && next[i] >= 0) {
                Vec3 in = dirs[i].subtract(dirs[prev[i]]);
                Vec3 out = dirs[next[i]].subtract(dirs[i]);
                if (in.lengthSqr() > 1.0e-10 && out.lengthSqr() > 1.0e-10) {
                    double turn = Math.acos(Mth.clamp(in.normalize().dot(out.normalize()), -1.0, 1.0));
                    dwell += 0.02f * smoothstep(CORNER_DWELL_START, CORNER_DWELL_FULL, turn);
                }
            } else {
                dwell += 0.012f;
            }
            weight[i] = base + persistence01 * dwell;
        }

        for (int i = 0; i < n; i++) {
            int j = next[i];
            int color = beams.get(i).color;
            if (j < 0 && !hasIn[i]) {
                int flags = LaserFigure.FLAG_BEAM0 | (hits[i] ? LaserFigure.FLAG_HIT0 : 0);
                fig.add(dirs[i], dirs[i], lens[i], lens[i], 0f, color, 0f, weight[i], 0f, path[i], path[i], flags);
                continue;
            }
            if (j < 0) {
                // Fin de trace ouvert deja portee par le segment precedent (faisceau 1).
                continue;
            }
            float sheetW = spans[i] > 1.0e-4f ? persistence01 : 0f;
            float beamW1 = next[j] < 0 ? weight[j] : 0f;
            int flags = 0;
            if (hits[i]) flags |= LaserFigure.FLAG_HIT0;
            if (hits[j]) flags |= LaserFigure.FLAG_HIT1;
            if (sheetW > 0f) flags |= LaserFigure.FLAG_SHEET;
            if (weight[i] > 0f) flags |= LaserFigure.FLAG_BEAM0;
            if (beamW1 > 0f) flags |= LaserFigure.FLAG_BEAM1;
            // Segment de fermeture (j revient au debut du trace) : le chemin continue au-dela
            // du dernier point au lieu de repartir de zero.
            float path1 = j < i && totalSpan > 0.0 ? path[i] + (float) (spans[i] / totalSpan) : path[j];
            fig.add(dirs[i], dirs[j], lens[i], lens[j], spans[i], color, sheetW, weight[i], beamW1, path[i],
                    path1, flags);
        }

        fig.striation = (1f - persistence01) * 0.6f;
        fig.fixturePos = be.getBlockPos();
        fig.origin = originW;
        fig.intensity = intensity01;
        fig.totalSpan = totalSpan > 0.0 ? (float) totalSpan : 1f;
        fig.divergence = 0.0006f + Mth.clamp(be.getFocus() / 255f, 0f, 1f) * 0.012f;
        if (TheatricalExtraLightsConfig.isLaserScanFlickerEnabled() && totalSpan > 0.0 && persistence01 < 0.995f) {
            fig.scanHead = (float) Mth.frac(animTimeSec * SCAN_HEAD_HZ);
            fig.scanTrail = (1f - persistence01) * 2.0f;
        } else {
            fig.scanHead = 0f;
            fig.scanTrail = 0f;
        }
    }

    private static float smoothstep(double e0, double e1, double x) {
        double t = Mth.clamp((x - e0) / (e1 - e0), 0.0, 1.0);
        return (float) (t * t * (3.0 - 2.0 * t));
    }

    // ── Raycast ──────────────────────────────────────────────────────────────

    /**
     * Cast a ray in world space along the beam direction, returning the effective length
     * until the first solid block hit (or {@code maxLen} if no hit). Skips the laser's own
     * rig: hits within {@link #RAYCAST_SKIP_RADIUS}, Theatrical blocks, and configured
     * pass-through blocks.
     *
     * @param hitOut single-element array; set to {@code true} if the ray hit a real surface
     */
    private static float raycastLength(LaserBlockEntity be, Vec3 origin, Vec3 dir, float maxLen, boolean[] hitOut) {
        hitOut[0] = false;
        if (be == null || be.getLevel() == null || maxLen <= 0.001f) return maxLen;
        if (dir.lengthSqr() < 1e-8) return maxLen;
        Vec3 endWorld = origin.add(dir.scale(maxLen));

        Vec3 rayStart = origin;
        int safety = 24;
        while (safety-- > 0) {
            BlockHitResult hit = be.getLevel().clip(new ClipContext(rayStart, endWorld,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null));
            if (hit.getType() == HitResult.Type.MISS) {
                return maxLen;
            }
            BlockState hitState = be.getLevel().getBlockState(hit.getBlockPos());
            ResourceLocation key = BuiltInRegistries.BLOCK.getKey(hitState.getBlock());
            boolean isModBlock = key != null
                    && (key.getNamespace().equals("theatrical")
                        || key.getNamespace().equals("theatricalextralights"));
            boolean isPassThrough = key != null
                    && TheatricalExtraLightsConfig.isLaserPassThrough(key.toString());
            float dist = (float) hit.getLocation().distanceTo(origin);

            if (!isModBlock && !isPassThrough && dist >= RAYCAST_SKIP_RADIUS) {
                hitOut[0] = true;
                return Math.min(maxLen, dist);
            }
            rayStart = hit.getLocation().add(dir.scale(0.01));
            if (rayStart.distanceToSqr(endWorld) < 1e-4) {
                return maxLen;
            }
        }
        return maxLen;
    }

    /**
     * Memoire courte des raycasts par direction quantifiee : un motif fixe ne relance pas une
     * centaine de clips par image, un motif qui tourne ne paie que les directions nouvelles.
     */
    private static final class RaycastCache {
        private static final class Entry {
            float length;
            boolean hit;
            long stamp;
        }

        private final Map<Long, Entry> entries = new HashMap<>();
        private long lastSweep;

        float length(LaserBlockEntity be, Vec3 origin, Vec3 dir, float maxLen, long now, boolean[] hitOut) {
            long key = key(dir, maxLen);
            Entry e = entries.get(key);
            if (e != null && now - e.stamp < RAYCAST_TTL_NANOS) {
                hitOut[0] = e.hit;
                return e.length;
            }
            float len = raycastLength(be, origin, dir, maxLen, hitOut);
            if (e == null) {
                e = new Entry();
                entries.put(key, e);
            }
            e.length = len;
            e.hit = hitOut[0];
            e.stamp = now;
            return len;
        }

        void sweep(long now) {
            if (now - lastSweep < RAYCAST_SWEEP_NANOS) {
                return;
            }
            lastSweep = now;
            Iterator<Map.Entry<Long, Entry>> it = entries.entrySet().iterator();
            while (it.hasNext()) {
                if (now - it.next().getValue().stamp > RAYCAST_SWEEP_NANOS) {
                    it.remove();
                }
            }
        }

        private static long key(Vec3 dir, float maxLen) {
            long ix = Math.round(dir.x * 1000.0) + 1024;
            long iy = Math.round(dir.y * 1000.0) + 1024;
            long iz = Math.round(dir.z * 1000.0) + 1024;
            long il = Math.round(maxLen) & 0xFFFFF;
            return (il << 33) | (ix << 22) | (iy << 11) | iz;
        }
    }

    // ── Chemin legacy (rubans plats) ─────────────────────────────────────────

    private void renderLegacy(LaserBlockEntity blockEntity, MultiBufferSource.BufferSource bufferSource,
                              PoseStack poseStack, Camera camera, Direction facing, float partialTick,
                              boolean isFlipped, BlockState blockstate, boolean isHanging) {
        poseStack.pushPose();
        Vec3 offset = Vec3.atLowerCornerOf(blockEntity.getBlockPos()).subtract(camera.getPosition());
        poseStack.translate(offset.x, offset.y, offset.z);

        preparePoseStack(blockEntity, poseStack, facing, partialTick, isFlipped, blockstate, isHanging);

        VertexConsumer beamConsumer = bufferSource.getBuffer(ExtraLightsRenderTypes.BEAM);

        float intensity = blockEntity.getDisplayIntensity(partialTick);
        float intensity01 = intensity / 255f;

        int c1 = blockEntity.getDisplayColour(partialTick);
        int c2 = blockEntity.getColour2();
        int c3 = blockEntity.getColour3();
        if (c2 == 0) c2 = c1;
        if (c3 == 0) c3 = c2;

        double animTimeSec = (blockEntity.getLevel().getGameTime() + partialTick) / 20.0;
        LaserPattern pattern = blockEntity.getPattern();
        List<LaserBeam> beams = pattern.generate(
                blockEntity.getSizeRaw(),
                blockEntity.getAmplitudeRaw(),
                blockEntity.getSpeedRaw(),
                blockEntity.getRotationRaw(),
                animTimeSec,
                c1, c2, c3
        );

        int focus = blockEntity.getFocus();
        float beamWidth = 0.04f + (focus / 255f) * 0.10f;
        float baseLength = TheatricalExtraLightsConfig.getLaserBeamLength();

        Vec3 baseOrigin = new Vec3(0.5F, 0.5F, 0.0F);
        if (isHanging) {
            baseOrigin = new Vec3(baseOrigin.x, 1.0 - baseOrigin.y, baseOrigin.z);
        }

        Vec3 worldOrigin = blockEntity.getBlockPos().getCenter();
        float[] effLengths = new float[beams.size()];
        boolean[] hit = new boolean[1];
        for (int i = 0; i < beams.size(); i++) {
            LaserBeam beam = beams.get(i);
            float maxLen = baseLength * (beam.length / 32f);
            effLengths[i] = raycastLength(blockEntity, worldOrigin, getBeamWorldDir(blockEntity, beam.yawDeg, beam.pitchDeg), maxLen, hit);
        }

        Vec3[] currentEndpoints = new Vec3[beams.size()];
        for (int i = 0; i < beams.size(); i++) {
            LaserBeam beam = beams.get(i);
            currentEndpoints[i] = computeEndpoint(baseOrigin, beam.yawDeg, beam.pitchDeg, effLengths[i]);
        }

        for (int idx = 0; idx < beams.size(); idx++) {
            LaserBeam beam = beams.get(idx);
            renderOneBeam(beamConsumer, poseStack, baseOrigin,
                    beam.yawDeg, beam.pitchDeg, effLengths[idx], beamWidth,
                    beam.color, intensity01);
        }

        int persistenceRaw = blockEntity.getPersistenceRaw();
        if (persistenceRaw > 0 && beams.size() > 1) {
            float persistAlpha = (persistenceRaw / 255f) * intensity01 * 0.8f;
            int loopLimit = pattern.isClosed() ? beams.size() : beams.size() - 1;
            for (int i = 0; i < loopLimit; i++) {
                int nextIdx = (i + 1) % beams.size();
                Vec3 pA = currentEndpoints[i];
                Vec3 pB = currentEndpoints[nextIdx];
                int color = beams.get(i).color;
                renderCurtain(beamConsumer, poseStack, baseOrigin, pA, pB, color, persistAlpha * 0.35f);
                renderRibbon(beamConsumer, poseStack, pA, pB, beamWidth * 0.5f, color, persistAlpha);
            }
        }

        poseStack.popPose();
    }

    private void renderOneBeam(VertexConsumer beamConsumer, PoseStack poseStack, Vec3 baseOrigin,
                               float yawDeg, float pitchDeg, float length, float beamWidth,
                               int color, float alpha) {
        poseStack.pushPose();
        poseStack.translate(baseOrigin.x, baseOrigin.y, baseOrigin.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(yawDeg));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitchDeg));
        renderLightBeam(beamConsumer, poseStack, alpha, beamWidth, length, color);
        poseStack.popPose();
    }

    /**
     * Compute the world-space direction of a beam by replicating EXACTLY the
     * pose-stack rotation chain used by {@link #preparePoseStack} and
     * {@link #renderOneBeam}, applied to the local +Z forward vector.
     */
    private static Vec3 getBeamWorldDir(LaserBlockEntity be, float beamYawDeg, float beamPitchDeg) {
        BlockState state = be.getBlockState();
        Direction facing = state.getValue(dev.imabad.theatrical.blocks.light.BaseLightBlock.FACING);
        Direction hangDir = Direction.UP;
        boolean isHanging = false;
        try {
            hangDir = state.getValue(dev.imabad.theatrical.blocks.HangableBlock.HANG_DIRECTION);
            isHanging = state.getValue(dev.imabad.theatrical.blocks.HangableBlock.HANGING);
        } catch (Exception ignored) {}
        boolean isFlipped = be.isUpsideDown();
        float pan = be.getPan();
        float tilt = be.getTilt();

        PoseStack temp = new PoseStack();
        if (isHanging && hangDir.getAxis() != Direction.Axis.Y) {
            if (hangDir.getAxis() == Direction.Axis.Z) {
                temp.mulPose(Axis.ZP.rotationDegrees(90));
                if (hangDir == Direction.SOUTH) {
                    temp.mulPose(Axis.XP.rotationDegrees(-90));
                } else {
                    temp.mulPose(Axis.XP.rotationDegrees(90));
                }
            } else {
                if (hangDir == Direction.EAST) {
                    temp.mulPose(Axis.ZN.rotationDegrees(-90));
                } else {
                    temp.mulPose(Axis.ZN.rotationDegrees(90));
                }
            }
        }
        temp.mulPose(Axis.YP.rotationDegrees(facing.toYRot()));
        if (isFlipped) {
            temp.mulPose(Axis.ZP.rotationDegrees(180));
        }
        temp.mulPose(Axis.YP.rotationDegrees(pan));
        if (isFlipped) {
            temp.mulPose(Axis.XP.rotationDegrees(-180));
        } else {
            temp.mulPose(Axis.XP.rotationDegrees(180));
        }
        temp.mulPose(Axis.XP.rotationDegrees(tilt));
        temp.mulPose(Axis.YP.rotationDegrees(beamYawDeg));
        temp.mulPose(Axis.XP.rotationDegrees(beamPitchDeg));

        Matrix4f m = temp.last().pose();
        Vector4f v = new Vector4f(0f, 0f, 1f, 0f);
        v.mul(m);
        Vec3 dir = new Vec3(v.x, v.y, v.z);
        if (dir.lengthSqr() < 1e-8) return Vec3.ZERO;
        return dir.normalize();
    }

    /**
     * Compute the 3D endpoint of a beam in fixture-local space, given (yaw, pitch, length).
     */
    private Vec3 computeEndpoint(Vec3 baseOrigin, float yawDeg, float pitchDeg, float length) {
        double y = Math.toRadians(yawDeg);
        double p = Math.toRadians(pitchDeg);
        double cy = Math.cos(y), sy = Math.sin(y);
        double cp = Math.cos(p), sp = Math.sin(p);
        double x = length * sy * cp;
        double yc = -length * sp;
        double z = length * cy * cp;
        return new Vec3(baseOrigin.x + x, baseOrigin.y + yc, baseOrigin.z + z);
    }

    /** Thin 4-sided ribbon (tube) connecting two 3D points in fixture-local space. */
    private void renderRibbon(VertexConsumer builder, PoseStack stack,
                              Vec3 a, Vec3 b, float halfWidth, int color, float alpha) {
        Vec3 dir = b.subtract(a);
        if (dir.lengthSqr() < 1e-8) return;
        dir = dir.normalize();
        Vec3 up = Math.abs(dir.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 right = dir.cross(up).normalize().scale(halfWidth);
        Vec3 perp = right.cross(dir).normalize().scale(halfWidth);

        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int bC = color & 0xFF;
        int aV = (int) (Math.max(0f, Math.min(1f, alpha)) * 255);

        Matrix4f m = stack.last().pose();
        Matrix3f normal = stack.last().normal();

        float ax1 = (float) (a.x + right.x), ay1 = (float) (a.y + right.y), az1 = (float) (a.z + right.z);
        float ax2 = (float) (a.x - right.x), ay2 = (float) (a.y - right.y), az2 = (float) (a.z - right.z);
        float ax3 = (float) (a.x + perp.x),  ay3 = (float) (a.y + perp.y),  az3 = (float) (a.z + perp.z);
        float ax4 = (float) (a.x - perp.x),  ay4 = (float) (a.y - perp.y),  az4 = (float) (a.z - perp.z);
        float bx1 = (float) (b.x + right.x), by1 = (float) (b.y + right.y), bz1 = (float) (b.z + right.z);
        float bx2 = (float) (b.x - right.x), by2 = (float) (b.y - right.y), bz2 = (float) (b.z - right.z);
        float bx3 = (float) (b.x + perp.x),  by3 = (float) (b.y + perp.y),  bz3 = (float) (b.z + perp.z);
        float bx4 = (float) (b.x - perp.x),  by4 = (float) (b.y - perp.y),  bz4 = (float) (b.z - perp.z);

        addVertex(builder, m, normal, r, g, bC, aV, ax1, ay1, az1);
        addVertex(builder, m, normal, r, g, bC, aV, ax3, ay3, az3);
        addVertex(builder, m, normal, r, g, bC, aV, bx3, by3, bz3);
        addVertex(builder, m, normal, r, g, bC, aV, bx1, by1, bz1);

        addVertex(builder, m, normal, r, g, bC, aV, ax3, ay3, az3);
        addVertex(builder, m, normal, r, g, bC, aV, ax2, ay2, az2);
        addVertex(builder, m, normal, r, g, bC, aV, bx2, by2, bz2);
        addVertex(builder, m, normal, r, g, bC, aV, bx3, by3, bz3);

        addVertex(builder, m, normal, r, g, bC, aV, ax2, ay2, az2);
        addVertex(builder, m, normal, r, g, bC, aV, ax4, ay4, az4);
        addVertex(builder, m, normal, r, g, bC, aV, bx4, by4, bz4);
        addVertex(builder, m, normal, r, g, bC, aV, bx2, by2, bz2);

        addVertex(builder, m, normal, r, g, bC, aV, ax4, ay4, az4);
        addVertex(builder, m, normal, r, g, bC, aV, ax1, ay1, az1);
        addVertex(builder, m, normal, r, g, bC, aV, bx1, by1, bz1);
        addVertex(builder, m, normal, r, g, bC, aV, bx4, by4, bz4);
    }

    private void renderCurtain(VertexConsumer builder, PoseStack stack, Vec3 origin, Vec3 a, Vec3 b, int color, float alpha) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int bC = color & 0xFF;
        int aV = (int) (Math.max(0f, Math.min(1f, alpha)) * 255);

        if (aV <= 0) return;

        Matrix4f m = stack.last().pose();
        Matrix3f normal = stack.last().normal();

        float ox = (float) origin.x, oy = (float) origin.y, oz = (float) origin.z;
        float ax = (float) a.x, ay = (float) a.y, az = (float) a.z;
        float bx = (float) b.x, by = (float) b.y, bz = (float) b.z;

        addVertex(builder, m, normal, r, g, bC, aV, ox, oy, oz);
        addVertex(builder, m, normal, r, g, bC, aV, ax, ay, az);
        addVertex(builder, m, normal, r, g, bC, aV, bx, by, bz);
        addVertex(builder, m, normal, r, g, bC, aV, ox, oy, oz);

        addVertex(builder, m, normal, r, g, bC, aV, ox, oy, oz);
        addVertex(builder, m, normal, r, g, bC, aV, bx, by, bz);
        addVertex(builder, m, normal, r, g, bC, aV, ax, ay, az);
        addVertex(builder, m, normal, r, g, bC, aV, ox, oy, oz);
    }

    private void renderLightBeam(VertexConsumer builder, PoseStack stack, float alpha, float beamSize, float length, int color) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int a = (int) (Math.max(0f, Math.min(1f, alpha)) * 255);
        Matrix4f m = stack.last().pose();
        Matrix3f normal = stack.last().normal();

        float zMid = length * 0.8f;
        float s = beamSize;

        addVertex(builder, m, normal, r, g, b, a,  s,  s, zMid);
        addVertex(builder, m, normal, r, g, b, a,  s,  s, 0);
        addVertex(builder, m, normal, r, g, b, a,  s, -s, 0);
        addVertex(builder, m, normal, r, g, b, a,  s, -s, zMid);
        addVertex(builder, m, normal, r, g, b, a, -s, -s, zMid);
        addVertex(builder, m, normal, r, g, b, a, -s, -s, 0);
        addVertex(builder, m, normal, r, g, b, a, -s,  s, 0);
        addVertex(builder, m, normal, r, g, b, a, -s,  s, zMid);
        addVertex(builder, m, normal, r, g, b, a, -s,  s, zMid);
        addVertex(builder, m, normal, r, g, b, a, -s,  s, 0);
        addVertex(builder, m, normal, r, g, b, a,  s,  s, 0);
        addVertex(builder, m, normal, r, g, b, a,  s,  s, zMid);
        addVertex(builder, m, normal, r, g, b, a,  s, -s, zMid);
        addVertex(builder, m, normal, r, g, b, a,  s, -s, 0);
        addVertex(builder, m, normal, r, g, b, a, -s, -s, 0);
        addVertex(builder, m, normal, r, g, b, a, -s, -s, zMid);

        int aEnd = 0;
        addVertex(builder, m, normal, r, g, b, aEnd,  s,  s, length);
        addVertex(builder, m, normal, r, g, b, a,    s,  s, zMid);
        addVertex(builder, m, normal, r, g, b, a,    s, -s, zMid);
        addVertex(builder, m, normal, r, g, b, aEnd,  s, -s, length);
        addVertex(builder, m, normal, r, g, b, aEnd, -s, -s, length);
        addVertex(builder, m, normal, r, g, b, a,   -s, -s, zMid);
        addVertex(builder, m, normal, r, g, b, a,   -s,  s, zMid);
        addVertex(builder, m, normal, r, g, b, aEnd, -s,  s, length);
        addVertex(builder, m, normal, r, g, b, aEnd, -s,  s, length);
        addVertex(builder, m, normal, r, g, b, a,   -s,  s, zMid);
        addVertex(builder, m, normal, r, g, b, a,    s,  s, zMid);
        addVertex(builder, m, normal, r, g, b, aEnd,  s,  s, length);
        addVertex(builder, m, normal, r, g, b, aEnd,  s, -s, length);
        addVertex(builder, m, normal, r, g, b, a,    s, -s, zMid);
        addVertex(builder, m, normal, r, g, b, a,   -s, -s, zMid);
        addVertex(builder, m, normal, r, g, b, aEnd, -s, -s, length);
    }

    @Override
    public void preparePoseStack(LaserBlockEntity blockEntity, PoseStack poseStack, Direction facing, float partialTicks, boolean isFlipped, BlockState blockState, boolean isHanging) {
        FixtureMountTransform.apply(poseStack, blockEntity);
        poseStack.translate(0.5F, 0, .5F);
        if (isHanging) {
            Direction hangDirection = blockState.getValue(HangableBlock.HANG_DIRECTION);
            poseStack.translate(0, 0.5, 0F);
            if (hangDirection.getAxis() != Direction.Axis.Y) {
                if (hangDirection.getAxis() == Direction.Axis.Z) {
                    if (hangDirection == Direction.SOUTH) {
                        poseStack.mulPose(Axis.ZP.rotationDegrees(90));
                        poseStack.mulPose(Axis.XP.rotationDegrees(-90));
                    } else {
                        poseStack.mulPose(Axis.ZP.rotationDegrees(90));
                        poseStack.mulPose(Axis.XP.rotationDegrees(90));
                    }
                } else {
                    if (hangDirection == Direction.EAST) {
                        poseStack.mulPose(Axis.ZN.rotationDegrees(-90));
                    } else {
                        poseStack.mulPose(Axis.ZN.rotationDegrees(90));
                    }
                }
            }
            poseStack.translate(0, -0.5, 0F);
        }
        poseStack.mulPose(Axis.YP.rotationDegrees(facing.toYRot()));
        poseStack.translate(-0.5F, 0, -.5F);
        if (isHanging) {
            Optional<BlockState> optionalSupport = blockEntity.getSupportingStructure();
            if (optionalSupport.isPresent()) {
                float[] transforms = blockEntity.getFixture().getTransforms(blockState, optionalSupport.get());
                poseStack.translate(transforms[0], transforms[1], transforms[2]);
            } else {
                poseStack.translate(0, 0.19, 0);
            }
            poseStack.translate(0, -0.08, 0);
        }
        if (isFlipped) {
            poseStack.translate(0.5F, 0.5, .5F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(180));
            poseStack.translate(-0.5F, -0.5, -.5F);
        }
        float[] pans = blockEntity.getFixture().getPanRotationPosition();
        poseStack.translate(pans[0], pans[1], pans[2]);
        int prevPan = blockEntity.getPrevPan();
        int pan = blockEntity.getPan();
        poseStack.mulPose(Axis.YP.rotationDegrees((prevPan + (pan - prevPan) * partialTicks)));
        poseStack.translate(-pans[0], -pans[1], -pans[2]);
        float[] tilts = blockEntity.getFixture().getTiltRotationPosition();
        poseStack.translate(tilts[0], tilts[1], tilts[2]);
        int prevTilt = blockEntity.getPrevTilt();
        int tilt = blockEntity.getTilt();
        if (isFlipped) {
            poseStack.mulPose(Axis.XP.rotationDegrees(-180));
        } else {
            poseStack.mulPose(Axis.XP.rotationDegrees(180));
        }
        poseStack.mulPose(Axis.XP.rotationDegrees((prevTilt + (tilt - prevTilt) * partialTicks)));
        poseStack.translate(-tilts[0], -tilts[1], -tilts[2]);
    }
}
