package com.github.dumann089.theatricalextralights.client.blockentities;

import com.github.dumann089.theatricalextralights.blockentities.LaserProjectorBlockEntity;
import com.github.dumann089.theatricalextralights.config.TheatricalExtraLightsConfig;
import com.github.dumann089.theatricalextralights.laser.dac.LaserDacHub;
import com.github.dumann089.theatricalextralights.laser.dac.LaserFrame;
import com.github.dumann089.theatricalextralights.laser.dac.LaserSegment;
import com.github.dumann089.theatricalextralights.util.FixtureMountTransform;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.imabad.theatrical.TheatricalExpectPlatform;
import dev.imabad.theatrical.blocks.HangableBlock;
import dev.imabad.theatrical.blocks.light.BaseLightBlock;
import dev.imabad.theatrical.client.LazyRenderers;
import com.github.dumann089.theatricalextralights.client.ExtraLightsRenderTypes;
import com.github.dumann089.theatricalextralights.client.render.laser.LaserFigure;
import com.github.dumann089.theatricalextralights.client.render.laser.LaserRaymarchRenderer;
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
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

/**
 * Ether Dream scan picture as a show laser. Two render paths, same as the DMX laser:
 * <ul>
 *   <li>realistic: the scanned figure is handed to {@link LaserRaymarchRenderer}, which draws
 *   beams, sheets and impacts in the haze against the scene depth buffer;</li>
 *   <li>legacy: camera-facing billboard shafts, soft persistence fans, the ILDA drawing in
 *   mid-air and impacts, in the depth-safe BEAM render type, when the shader is unavailable
 *   (Iris active, engine disabled in the settings).</li>
 * </ul>
 *
 * <p>Brightness follows {@link LaserSegment#persistence()}.
 */
public class LaserProjectorRenderer extends ExtraLightsFixtureRenderer<LaserProjectorBlockEntity> {

    private static final float RAYCAST_SKIP_RADIUS = 2.5f;
    private static final float ILDA_FULL = 32767f;
    private static final float MAX_AIR_LENGTH = 96.0f;
    private static final float PICTURE_PLANE = 22.0f;
    private static final float SHEET_MIN_DEG = 0.45f;
    private static final float SHEET_MAX_DEG = 16.0f;
    private static final long FRAME_MAX_AGE_NANOS = 250_000_000L;
    private static final long RAYCAST_TTL_NANOS = 180_000_000L;
    private static final float RAYCAST_QUANT_DEG = 0.25f;
    private static final float MIN_SHEET_SPAN_RAD = (float) Math.toRadians(0.25);

    private BakedModel cachedPanModel;
    private BakedModel cachedTiltModel;
    private BakedModel cachedStaticModel;

    private final WeakHashMap<LaserProjectorBlockEntity, RaycastCache> raycastCaches = new WeakHashMap<>();
    private long lastDebugNanos;
    private long lastDirDebugNanos;

    private static String fmt(Vec3 v) {
        return String.format("(%.2f,%.2f,%.2f)", v.x, v.y, v.z);
    }

    private void debug(String fmt, Object... args) {
        long now = System.nanoTime();
        if (now - lastDebugNanos < 2_000_000_000L) {
            return;
        }
        lastDebugNanos = now;
        com.github.dumann089.theatricalextralights.TheatricalExtraLights.LOGGER.debug("[LaserProjector] " + fmt, args);
    }

    public LaserProjectorRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    // ── Model ─────────────────────────────────────────────────────────────

    @Override
    public void renderModel(LaserProjectorBlockEntity blockEntity, PoseStack poseStack, VertexConsumer vertexConsumer,
                            Direction facing, float partialTicks, boolean isFlipped, BlockState blockState,
                            boolean isHanging, int packedLight, int packedOverlay) {
        if (cachedStaticModel == null) {
            cachedStaticModel = TheatricalExpectPlatform.getBakedModel(blockEntity.getFixture().getStaticModel());
        }
        if (cachedPanModel == null) {
            cachedPanModel = TheatricalExpectPlatform.getBakedModel(blockEntity.getFixture().getPanModel());
        }
        if (cachedTiltModel == null) {
            cachedTiltModel = TheatricalExpectPlatform.getBakedModel(blockEntity.getFixture().getTiltModel());
        }
        applyHeadPose(blockEntity, poseStack, facing, partialTicks, isFlipped, blockState, isHanging);
        minecraftRenderModel(poseStack, vertexConsumer, blockState, cachedStaticModel, packedLight, packedOverlay);
        float[] pans = blockEntity.getFixture().getPanRotationPosition();
        poseStack.translate(pans[0], pans[1], pans[2]);
        poseStack.mulPose(Axis.YP.rotationDegrees(interpolatedPan(blockEntity, partialTicks)));
        poseStack.translate(-pans[0], -pans[1], -pans[2]);
        minecraftRenderModel(poseStack, vertexConsumer, blockState, cachedPanModel, packedLight, packedOverlay);
        float[] tilts = blockEntity.getFixture().getTiltRotationPosition();
        poseStack.translate(tilts[0], tilts[1], tilts[2]);
        if (isFlipped) {
            poseStack.mulPose(Axis.XP.rotationDegrees(-180));
        } else {
            poseStack.mulPose(Axis.XP.rotationDegrees(180));
        }
        poseStack.mulPose(Axis.XP.rotationDegrees(interpolatedTilt(blockEntity, partialTicks)));
        poseStack.translate(-tilts[0], -tilts[1], -tilts[2]);
        minecraftRenderModel(poseStack, vertexConsumer, blockState, cachedTiltModel, packedLight, packedOverlay);
    }

    // ── Beam entry point ──────────────────────────────────────────────────

    @Override
    public void beforeRenderBeam(LaserProjectorBlockEntity blockEntity, PoseStack poseStack, VertexConsumer vertexConsumer,
                                 MultiBufferSource multiBufferSource, Direction facing, float partialTicks,
                                 boolean isFlipped, BlockState blockstate, boolean isHanging,
                                 int packedLight, int packedOverlay) {
        LaserFrame frame = LaserDacHub.frame(blockEntity.getDacId());
        if (!frame.isFresh(System.nanoTime(), FRAME_MAX_AGE_NANOS)) {
            debug("{} frame stale/empty: segments={} ageMs={}", blockEntity.getBlockPos(),
                    frame.segments == null ? -1 : frame.segments.length,
                    (System.nanoTime() - frame.publishedAtNanos) / 1_000_000L);
            return;
        }

        ScanGeometry geo = buildGeometry(blockEntity, frame, isHanging);
        if (geo.count == 0) {
            debug("{} frame has {} segments but none visible", blockEntity.getBlockPos(), frame.segments.length);
            return;
        }
        debug("{} segments={} visible={} realistic={} first: len0={} hit0={} bright={} color={} span={}",
                blockEntity.getBlockPos(), frame.segments.length, geo.count, LaserRaymarchRenderer.isAvailable(),
                geo.len0[0], geo.hit0[0], geo.brightness[0], Integer.toHexString(geo.color[0]), geo.spanDeg[0]);

        if (LaserRaymarchRenderer.isAvailable()) {
            submitRealistic(blockEntity, geo, facing, partialTicks, isFlipped, blockstate, isHanging);
            return;
        }

        LazyRenderers.addLazyRender(new LazyRenderers.LazyRenderer() {
            @Override
            public void render(MultiBufferSource.BufferSource bufferSource, PoseStack renderPose, Camera camera, float partialTick) {
                renderShow(blockEntity, bufferSource, renderPose, camera, facing, partialTick,
                        isFlipped, blockstate, isHanging, geo);
            }

            @Override
            public Vec3 getPos(float partialTick) {
                return blockEntity.getBlockPos().getCenter();
            }
        });
    }

    // ── Realistic path ────────────────────────────────────────────────────

    /**
     * Converts the lens-space geometry to world space through the same transform chain as the
     * head model (mount, hang, pan, tilt) and fills a {@link LaserFigure}. Every visible DAC
     * segment becomes a sheet between its two directions; isolated points and the ends of
     * open strokes keep a beam. Received point age already models the scan persistence, so the
     * figure has no scan head of its own.
     */
    private void submitRealistic(LaserProjectorBlockEntity be, ScanGeometry geo, Direction facing,
                                 float partialTicks, boolean isFlipped, BlockState blockstate, boolean isHanging) {
        LaserFigure fig = LaserRaymarchRenderer.begin();
        if (fig == null) {
            debug("figure quota reached");
            return;
        }
        PoseStack local = new PoseStack();
        preparePoseStack(be, local, facing, partialTicks, isFlipped, blockstate, isHanging);
        Matrix4f m = local.last().pose();

        Vector4f o = new Vector4f((float) geo.origin.x, (float) geo.origin.y, (float) geo.origin.z, 1f);
        m.transform(o);
        Vec3 originW = new Vec3(be.getBlockPos().getX() + o.x, be.getBlockPos().getY() + o.y, be.getBlockPos().getZ() + o.z);

        int n = Math.min(geo.count, LaserFigure.MAX_SEGMENTS);
        float share = (float) Math.pow(Math.max(1, n), -0.75);
        double totalSpan = 0.0;
        for (int i = 0; i < n; i++) {
            if (geo.spanDeg[i] > 0.01f) {
                totalSpan += Math.toRadians(geo.spanDeg[i]);
            }
        }
        double cum = 0.0;
        Vector4f d = new Vector4f();
        for (int i = 0; i < n; i++) {
            float bright = geo.brightness[i];
            if (bright <= 0.002f) {
                continue;
            }
            Vec3 d0 = worldDir(m, geo.dir0[i], d);
            Vec3 d1 = worldDir(m, geo.dir1[i], d);
            float spanRad = (float) Math.toRadians(geo.spanDeg[i]);
            float p0 = totalSpan > 0.0 ? (float) (cum / totalSpan) : 0f;
            // Below ~0.25 deg a sheet degenerates into a line; draw it as a beam instead.
            boolean point = geo.segment[i].isPoint() || spanRad <= MIN_SHEET_SPAN_RAD;
            int color = geo.color[i];
            if (point) {
                int flags = LaserFigure.FLAG_BEAM0 | (geo.hit0[i] ? LaserFigure.FLAG_HIT0 : 0);
                fig.add(d0, d0, geo.len0[i], geo.len0[i], 0f, color, 0f, bright * share, 0f, p0, p0, flags);
                continue;
            }
            cum += spanRad;
            float p1 = totalSpan > 0.0 ? (float) (cum / totalSpan) : 0f;
            // Scanned drawing: the sheet carries the picture, a faint beam marks each vertex.
            float beamW = bright * share * 0.35f;
            int flags = LaserFigure.FLAG_SHEET | LaserFigure.FLAG_BEAM0 | LaserFigure.FLAG_BEAM1;
            if (geo.hit0[i]) flags |= LaserFigure.FLAG_HIT0;
            if (geo.hit1[i]) flags |= LaserFigure.FLAG_HIT1;
            fig.add(d0, d1, geo.len0[i], geo.len1[i], spanRad, color, bright, beamW, beamW, p0, p1, flags);
        }

        fig.fixturePos = be.getBlockPos();
        fig.origin = originW;
        if (System.nanoTime() - lastDirDebugNanos > 1_000_000_000L) {
            lastDirDebugNanos = System.nanoTime();
            Vec3 mean = Vec3.ZERO;
            for (int i = 0; i < n; i++) mean = mean.add(worldDir(m, geo.dir0[i], d));
            mean = mean.lengthSqr() > 1e-9 ? mean.normalize() : new Vec3(0, 0, 1);
            double minDot = 1.0; int worst = -1;
            for (int i = 0; i < n; i++) {
                double dot = worldDir(m, geo.dir0[i], d).dot(mean);
                if (dot < minDot) { minDot = dot; worst = i; }
            }
            Vec3 axis = worldDir(m, new Vec3(0, 0, 1), d);
            Vec3 rayAxis = worldDir(be, 0f, 0f);
            com.github.dumann089.theatricalextralights.TheatricalExtraLights.LOGGER.debug(
                    "[LaserProjector] dirs: n={} mean={} minDot={} worstLocal={} lensAxis(pose)={} lensAxis(raycast)={} agree={} pan={} tilt={} hanging={} flipped={} origin={}",
                    n, fmt(mean), String.format("%.3f", minDot), worst >= 0 ? fmt(geo.dir0[worst]) : "-",
                    fmt(axis), fmt(rayAxis), String.format("%.3f", axis.dot(rayAxis)),
                    be.getPan(), be.getTilt(), isHanging, isFlipped, fmt(originW));
        }
        fig.intensity = 1.0f;
        fig.totalSpan = totalSpan > 0.0 ? (float) totalSpan : 1f;
        fig.divergence = 0.0012f;
        fig.striation = 0.15f;
        fig.scanHead = 0f;
        fig.scanTrail = 0f;
    }

    private static Vec3 worldDir(Matrix4f m, Vec3 local, Vector4f tmp) {
        tmp.set((float) local.x, (float) local.y, (float) local.z, 0f);
        m.transform(tmp);
        Vec3 dir = new Vec3(tmp.x, tmp.y, tmp.z);
        return dir.lengthSqr() > 1.0e-10 ? dir.normalize() : new Vec3(0, 0, 1);
    }

    // ── Geometry (computed once per frame, shared by every layer) ─────────

    private ScanGeometry buildGeometry(LaserProjectorBlockEntity be, LaserFrame frame, boolean isHanging) {
        LaserSegment[] segments = frame.segments;
        float halfAngle = be.effectiveScanHalfAngle();
        float maxLen = Math.min(TheatricalExtraLightsConfig.getLaserBeamLength(), MAX_AIR_LENGTH);
        RaycastCache cache = raycastCaches.computeIfAbsent(be, k -> new RaycastCache());
        cache.prepare(be, System.nanoTime());

        ScanGeometry geo = new ScanGeometry(segments.length);
        // Lens exit in head space (the hanging mirror maps 0.5 onto itself).
        geo.origin = new Vec3(0.5, 0.5, 0.0);

        int n = 0;
        float[] hit = new float[1];
        for (LaserSegment segment : segments) {
            if (segment.intensity <= 0.001f) {
                continue;
            }
            float yaw0 = ildaToYaw(segment.x0, halfAngle);
            float pitch0 = ildaToPitch(segment.y0, halfAngle);
            geo.segment[n] = segment;
            geo.dir0[n] = localDir(yaw0, pitch0);
            geo.len0[n] = cache.length(be, yaw0, pitch0, maxLen, hit);
            geo.hit0[n] = hit[0] > 0.5f;
            if (segment.isPoint()) {
                geo.dir1[n] = geo.dir0[n];
                geo.len1[n] = geo.len0[n];
                geo.hit1[n] = geo.hit0[n];
                geo.spanDeg[n] = 0.0f;
            } else {
                float yaw1 = ildaToYaw(segment.x1, halfAngle);
                float pitch1 = ildaToPitch(segment.y1, halfAngle);
                geo.dir1[n] = localDir(yaw1, pitch1);
                geo.len1[n] = cache.length(be, yaw1, pitch1, maxLen, hit);
                geo.hit1[n] = hit[0] > 0.5f;
                double dot = Mth.clamp(geo.dir0[n].dot(geo.dir1[n]), -1.0, 1.0);
                geo.spanDeg[n] = (float) Math.toDegrees(Math.acos(dot));
            }
            geo.color[n] = saturate(segment.color);
            geo.brightness[n] = Mth.clamp(segment.intensity * segment.persistence() * 1.15f, 0.0f, 1.0f);
            n++;
        }
        geo.count = n;
        return geo;
    }

    private void renderShow(LaserProjectorBlockEntity blockEntity, MultiBufferSource.BufferSource bufferSource,
                            PoseStack poseStack, Camera camera, Direction facing, float partialTick,
                            boolean isFlipped, BlockState blockstate, boolean isHanging,
                            ScanGeometry geo) {
        poseStack.pushPose();
        Vec3 offset = Vec3.atLowerCornerOf(blockEntity.getBlockPos()).subtract(camera.getPosition());
        poseStack.translate(offset.x, offset.y, offset.z);
        preparePoseStack(blockEntity, poseStack, facing, partialTick, isFlipped, blockstate, isHanging);

        Vec3 cam = cameraInLocal(poseStack);
        VertexConsumer beam = bufferSource.getBuffer(ExtraLightsRenderTypes.BEAM);
        Vec3 origin = geo.origin;
        float pictureDist = PICTURE_PLANE * blockEntity.getProjectionScale();
        float haze = TheatricalExtraLightsConfig.getLaserDacHazeRadius();

        for (int i = 0; i < geo.count; i++) {
            LaserSegment segment = geo.segment[i];
            int color = geo.color[i];
            int core = hotCore(color);
            float intensity = geo.brightness[i];
            if (intensity <= 0.002f) {
                continue;
            }

            Vec3 endA = origin.add(geo.dir0[i].scale(geo.len0[i]));
            Vec3 endB = origin.add(geo.dir1[i].scale(geo.len1[i]));

            renderBillboardShaft(beam, poseStack, cam, origin, endA, core, color, intensity, haze);
            if (!segment.isPoint()) {
                renderBillboardShaft(beam, poseStack, cam, origin, endB, core, color, intensity * 0.92f, haze);
                renderSoftFan(beam, poseStack, origin, geo.dir0[i], geo.dir1[i],
                        geo.len0[i], geo.len1[i], color, intensity);
            }

            if (geo.hit0[i]) {
                renderImpact(beam, poseStack, cam, endA, core, color, intensity);
            }
            if (!segment.isPoint()) {
                if (geo.hit1[i]) {
                    renderImpact(beam, poseStack, cam, endB, core, color, intensity);
                }
                float picture = Math.min(pictureDist, Math.min(geo.len0[i], geo.len1[i]));
                if (picture > 2.0f) {
                    Vec3 picA = origin.add(geo.dir0[i].scale(picture));
                    Vec3 picB = origin.add(geo.dir1[i].scale(picture));
                    renderBillboardRibbon(beam, poseStack, cam, picA, picB, 0.055f, color, intensity * 0.40f);
                    renderBillboardRibbon(beam, poseStack, cam, picA, picB, 0.016f, core, intensity * 0.85f);
                }
            }
        }

        poseStack.popPose();
    }

    /**
     * One smooth shaft: nested camera-facing quads. Never crossed planes or
     * stacked discs — those read as parallel ribs when you look up the beam.
     */
    private void renderBillboardShaft(VertexConsumer beam, PoseStack pose, Vec3 cam,
                                      Vec3 start, Vec3 end, int core, int color,
                                      float intensity, float hazeRadius) {
        Vec3 dir = end.subtract(start);
        if (dir.lengthSqr() < 1.0e-8 || intensity <= 0.002f) {
            return;
        }
        dir = dir.normalize();
        Vec3 mid = start.add(end).scale(0.5);
        Vec3 toCam = cam.subtract(mid);
        double toCamLen = toCam.length();
        if (toCamLen < 1.0e-6) {
            return;
        }
        Vec3 toCamN = toCam.scale(1.0 / toCamLen);
        Vec3 right = dir.cross(toCam);
        if (right.lengthSqr() < 1.0e-10) {
            Vec3 up = Math.abs(dir.y) > 0.92 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
            right = dir.cross(up);
        }
        right = right.normalize();

        float along = (float) Math.abs(dir.dot(toCamN));
        float mie = 1.0f + 2.4f * along * along;
        float haze = Math.max(0.08f, hazeRadius);

        billboardLayer(beam, pose, start, end, right, 0.007f, 0xFFFFFF, intensity * 0.62f * mie);
        billboardLayer(beam, pose, start, end, right, 0.018f, core, intensity * 0.95f);
        billboardLayer(beam, pose, start, end, right, 0.048f, color, intensity * 0.42f);
        billboardLayer(beam, pose, start, end, right, haze * 0.95f, color, intensity * 0.16f);
        billboardLayer(beam, pose, start, end, right, haze * 2.15f, color, intensity * 0.055f);
    }

    private void billboardLayer(VertexConsumer beam, PoseStack pose, Vec3 start, Vec3 end,
                                Vec3 right, float halfW, int color, float alpha) {
        if (alpha <= 0.002f || halfW <= 0.0f) {
            return;
        }
        Vec3 mid = start.add(end.subtract(start).scale(0.82));
        int full = packAlpha(alpha);
        Vec3 aL = start.subtract(right.scale(halfW));
        Vec3 aR = start.add(right.scale(halfW));
        Vec3 mL = mid.subtract(right.scale(halfW));
        Vec3 mR = mid.add(right.scale(halfW));
        Vec3 bL = end.subtract(right.scale(halfW * 1.25f));
        Vec3 bR = end.add(right.scale(halfW * 1.25f));
        fadedQuad(beam, pose, aL, start, mid, mL, color, 0, full, full, 0);
        fadedQuad(beam, pose, start, aR, mR, mid, color, full, 0, 0, full);
        fadedQuad(beam, pose, mL, mid, end, bL, color, 0, full, 0, 0);
        fadedQuad(beam, pose, mid, mR, bR, end, color, full, 0, 0, 0);
    }

    private void renderBillboardRibbon(VertexConsumer beam, PoseStack pose, Vec3 cam,
                                       Vec3 a, Vec3 b, float halfW, int color, float alpha) {
        Vec3 dir = b.subtract(a);
        if (dir.lengthSqr() < 1.0e-8 || alpha <= 0.002f) {
            return;
        }
        dir = dir.normalize();
        Vec3 toCam = cam.subtract(a.add(b).scale(0.5));
        Vec3 right = dir.cross(toCam);
        if (right.lengthSqr() < 1.0e-10) {
            Vec3 up = Math.abs(dir.y) > 0.92 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
            right = dir.cross(up);
        }
        right = right.normalize();
        int full = packAlpha(alpha);
        Vec3 aL = a.subtract(right.scale(halfW));
        Vec3 aR = a.add(right.scale(halfW));
        Vec3 bL = b.subtract(right.scale(halfW));
        Vec3 bR = b.add(right.scale(halfW));
        fadedQuad(beam, pose, aL, a, b, bL, color, 0, full, full, 0);
        fadedQuad(beam, pose, a, aR, bR, b, color, full, 0, 0, full);
    }

    private void renderSoftFan(VertexConsumer beam, PoseStack pose, Vec3 origin,
                               Vec3 dirA, Vec3 dirB, float lenA, float lenB, int color, float intensity) {
        double dot = Mth.clamp(dirA.dot(dirB), -1.0, 1.0);
        float deg = (float) Math.toDegrees(Math.acos(dot));
        if (deg < SHEET_MIN_DEG || deg > SHEET_MAX_DEG) {
            return;
        }
        int steps = Mth.clamp(Math.round(deg / 2.4f), 3, 8);
        for (int i = 0; i < steps; i++) {
            float t0 = i / (float) steps;
            float t1 = (i + 1) / (float) steps;
            Vec3 d0 = slerp(dirA, dirB, t0);
            Vec3 d1 = slerp(dirA, dirB, t1);
            float l0 = Mth.lerp(t0, lenA, lenB);
            float l1 = Mth.lerp(t1, lenA, lenB);
            float center = Math.abs((t0 + t1) * 0.5f - 0.5f) * 2.0f;
            float strip = intensity * (0.045f + 0.11f * (1.0f - center * 0.65f));
            for (int ring = 0; ring < 4; ring++) {
                float z0 = ring / 4.0f;
                float z1 = (ring + 1) / 4.0f;
                float near = strip * (1.0f - z0 * 0.42f);
                float far = strip * (1.0f - z1 * 0.42f);
                if (ring == 3) {
                    far *= 0.12f;
                }
                Vec3 p00 = origin.add(d0.scale(l0 * z0));
                Vec3 p01 = origin.add(d0.scale(l0 * z1));
                Vec3 p10 = origin.add(d1.scale(l1 * z0));
                Vec3 p11 = origin.add(d1.scale(l1 * z1));
                fadedQuad(beam, pose, p00, p10, p11, p01, color,
                        packAlpha(near), packAlpha(near), packAlpha(far), packAlpha(far));
                fadedQuad(beam, pose, p00, p01, p11, p10, color,
                        packAlpha(near), packAlpha(far), packAlpha(far), packAlpha(near));
            }
        }
    }

    private void renderImpact(VertexConsumer beam, PoseStack pose, Vec3 cam,
                              Vec3 point, int core, int color, float intensity) {
        Vec3 toCam = cam.subtract(point);
        if (toCam.lengthSqr() < 1.0e-8) {
            return;
        }
        toCam = toCam.normalize();
        Vec3 up = Math.abs(toCam.y) > 0.92 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 right = toCam.cross(up).normalize();
        Vec3 vert = right.cross(toCam).normalize();
        renderSparkBillboard(beam, pose, point, right, vert, 0.22f, color, intensity * 0.28f);
        renderSparkBillboard(beam, pose, point, right, vert, 0.08f, core, intensity * 0.72f);
        renderSparkBillboard(beam, pose, point, right, vert, 0.028f, 0xFFFFFF, intensity * 0.90f);
    }

    private void renderSparkBillboard(VertexConsumer beam, PoseStack pose, Vec3 p,
                                      Vec3 right, Vec3 vert, float size, int color, float alpha) {
        int full = packAlpha(alpha);
        Vec3 r = right.scale(size);
        Vec3 v = vert.scale(size);
        Vec3 c00 = p.subtract(r).subtract(v);
        Vec3 c10 = p.add(r).subtract(v);
        Vec3 c11 = p.add(r).add(v);
        Vec3 c01 = p.subtract(r).add(v);
        fadedQuad(beam, pose, p, c10, c11, p, color, full, 0, 0, full);
        fadedQuad(beam, pose, p, c11, c01, p, color, full, 0, 0, full);
        fadedQuad(beam, pose, p, c01, c00, p, color, full, 0, 0, full);
        fadedQuad(beam, pose, p, c00, c10, p, color, full, 0, 0, full);
    }

    private void fadedQuad(VertexConsumer beam, PoseStack pose,
                           Vec3 a, Vec3 b, Vec3 c, Vec3 d,
                           int color, int aA, int aB, int aC, int aD) {
        if (aA + aB + aC + aD <= 0) {
            return;
        }
        Matrix4f m = pose.last().pose();
        Matrix3f n = pose.last().normal();
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int bl = color & 0xFF;
        addVertex(beam, m, n, r, g, bl, aA, (float) a.x, (float) a.y, (float) a.z);
        addVertex(beam, m, n, r, g, bl, aB, (float) b.x, (float) b.y, (float) b.z);
        addVertex(beam, m, n, r, g, bl, aC, (float) c.x, (float) c.y, (float) c.z);
        addVertex(beam, m, n, r, g, bl, aD, (float) d.x, (float) d.y, (float) d.z);
    }

    // ── Pose helpers ──────────────────────────────────────────────────────

    @Override
    public void preparePoseStack(LaserProjectorBlockEntity blockEntity, PoseStack poseStack, Direction facing,
                                 float partialTicks, boolean isFlipped, BlockState blockState, boolean isHanging) {
        applyHeadPose(blockEntity, poseStack, facing, partialTicks, isFlipped, blockState, isHanging);
        float[] pans = blockEntity.getFixture().getPanRotationPosition();
        poseStack.translate(pans[0], pans[1], pans[2]);
        poseStack.mulPose(Axis.YP.rotationDegrees(interpolatedPan(blockEntity, partialTicks)));
        poseStack.translate(-pans[0], -pans[1], -pans[2]);
        float[] tilts = blockEntity.getFixture().getTiltRotationPosition();
        poseStack.translate(tilts[0], tilts[1], tilts[2]);
        if (isFlipped) {
            poseStack.mulPose(Axis.XP.rotationDegrees(-180));
        } else {
            poseStack.mulPose(Axis.XP.rotationDegrees(180));
        }
        poseStack.mulPose(Axis.XP.rotationDegrees(interpolatedTilt(blockEntity, partialTicks)));
        poseStack.translate(-tilts[0], -tilts[1], -tilts[2]);
    }

    private void applyHeadPose(LaserProjectorBlockEntity blockEntity, PoseStack poseStack, Direction facing,
                               float partialTicks, boolean isFlipped, BlockState blockState, boolean isHanging) {
        FixtureMountTransform.apply(poseStack, blockEntity);
        poseStack.translate(0.5F, 0, .5F);
        if (isHanging) {
            Direction hangDirection = blockState.getValue(HangableBlock.HANG_DIRECTION);
            poseStack.translate(0, 0.5, 0F);
            if (hangDirection.getAxis() != Direction.Axis.Y) {
                if (hangDirection.getAxis() == Direction.Axis.Z) {
                    poseStack.mulPose(Axis.ZP.rotationDegrees(90));
                    poseStack.mulPose(Axis.XP.rotationDegrees(hangDirection == Direction.SOUTH ? -90 : 90));
                } else {
                    poseStack.mulPose(Axis.ZN.rotationDegrees(hangDirection == Direction.EAST ? -90 : 90));
                }
            }
            poseStack.translate(0, -0.5, 0F);
        }
        poseStack.mulPose(Axis.YP.rotationDegrees(facing.toYRot()));
        poseStack.translate(-0.5F, 0, -.5F);
        if (isHanging) {
            Optional<BlockState> support = blockEntity.getSupportingStructure();
            if (support.isPresent()) {
                float[] transforms = blockEntity.getFixture().getTransforms(blockState, support.get());
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
    }

    /** Camera position in fixture-local space after the lazy-render pose is applied. */
    private static Vec3 cameraInLocal(PoseStack pose) {
        Matrix4f inv = new Matrix4f(pose.last().pose());
        if (Math.abs(inv.determinant()) < 1.0e-10f) {
            return Vec3.ZERO;
        }
        inv.invert();
        Vector4f cam = new Vector4f(0f, 0f, 0f, 1f);
        cam.mul(inv);
        return new Vec3(cam.x, cam.y, cam.z);
    }

    // ── Raycast (world) ───────────────────────────────────────────────────

    private static float raycastLength(LaserProjectorBlockEntity be, float yawDeg, float pitchDeg, float maxLen, float[] hitOut) {
        hitOut[0] = 0f;
        if (be.getLevel() == null || maxLen <= 0.001f) {
            return maxLen;
        }
        Vec3 origin = be.getBlockPos().getCenter();
        Vec3 dir = worldDir(be, yawDeg, pitchDeg);
        if (dir.lengthSqr() < 1e-8) {
            return maxLen;
        }
        Vec3 end = origin.add(dir.scale(maxLen));
        Vec3 start = origin;
        int safety = 24;
        while (safety-- > 0) {
            BlockHitResult hit = be.getLevel().clip(new ClipContext(
                    start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null));
            if (hit.getType() == HitResult.Type.MISS) {
                return maxLen;
            }
            BlockState hitState = be.getLevel().getBlockState(hit.getBlockPos());
            ResourceLocation key = BuiltInRegistries.BLOCK.getKey(hitState.getBlock());
            boolean modBlock = key != null && (key.getNamespace().equals("theatrical")
                    || key.getNamespace().equals("theatricalextralights"));
            boolean pass = key != null && TheatricalExtraLightsConfig.isLaserPassThrough(key.toString());
            float dist = (float) hit.getLocation().distanceTo(origin);
            if (!modBlock && !pass && dist >= RAYCAST_SKIP_RADIUS) {
                hitOut[0] = 1f;
                return Math.min(maxLen, dist);
            }
            start = hit.getLocation().add(dir.scale(0.01));
            if (start.distanceToSqr(end) < 1e-4) {
                return maxLen;
            }
        }
        return maxLen;
    }

    private static Vec3 worldDir(LaserProjectorBlockEntity be, float beamYaw, float beamPitch) {
        BlockState state = be.getBlockState();
        Direction facing = state.getValue(BaseLightBlock.FACING);
        Direction hangDir = Direction.UP;
        boolean hanging = false;
        try {
            hangDir = state.getValue(HangableBlock.HANG_DIRECTION);
            hanging = state.getValue(HangableBlock.HANGING);
        } catch (Exception ignored) {
        }
        boolean flipped = be.isUpsideDown();
        PoseStack temp = new PoseStack();
        if (hanging && hangDir.getAxis() != Direction.Axis.Y) {
            if (hangDir.getAxis() == Direction.Axis.Z) {
                temp.mulPose(Axis.ZP.rotationDegrees(90));
                temp.mulPose(Axis.XP.rotationDegrees(hangDir == Direction.SOUTH ? -90 : 90));
            } else {
                temp.mulPose(Axis.ZN.rotationDegrees(hangDir == Direction.EAST ? -90 : 90));
            }
        }
        temp.mulPose(Axis.YP.rotationDegrees(facing.toYRot()));
        if (flipped) {
            temp.mulPose(Axis.ZP.rotationDegrees(180));
        }
        temp.mulPose(Axis.YP.rotationDegrees(be.getPan()));
        temp.mulPose(Axis.XP.rotationDegrees(flipped ? -180 : 180));
        temp.mulPose(Axis.XP.rotationDegrees(be.getTilt()));
        temp.mulPose(Axis.YP.rotationDegrees(beamYaw));
        temp.mulPose(Axis.XP.rotationDegrees(beamPitch));
        Matrix4f m = temp.last().pose();
        org.joml.Vector4f v = new org.joml.Vector4f(0f, 0f, 1f, 0f);
        v.mul(m);
        Vec3 dir = new Vec3(v.x, v.y, v.z);
        return dir.lengthSqr() < 1e-8 ? Vec3.ZERO : dir.normalize();
    }

    /**
     * Raycasts are the CPU hot spot: ~200 world clips per frame. Directions are
     * quantised to a quarter degree and remembered for a few frames; the cache is
     * dropped whenever the head moves (pan/tilt) so hits stay exact.
     */
    private static final class RaycastCache {
        private final Map<Integer, Entry> entries = new HashMap<>();
        private int lastPan = Integer.MIN_VALUE;
        private int lastTilt = Integer.MIN_VALUE;
        private long now;

        void prepare(LaserProjectorBlockEntity be, long nowNanos) {
            this.now = nowNanos;
            int pan = be.getPan();
            int tilt = be.getTilt();
            if (pan != lastPan || tilt != lastTilt || entries.size() > 6000) {
                entries.clear();
                lastPan = pan;
                lastTilt = tilt;
            }
        }

        float length(LaserProjectorBlockEntity be, float yawDeg, float pitchDeg, float maxLen, float[] hitOut) {
            int qy = Math.round(yawDeg / RAYCAST_QUANT_DEG);
            int qp = Math.round(pitchDeg / RAYCAST_QUANT_DEG);
            int key = ((qy & 0xFFFF) << 16) | (qp & 0xFFFF);
            Entry e = entries.get(key);
            if (e != null && now - e.stamp <= RAYCAST_TTL_NANOS && e.maxLen == maxLen) {
                hitOut[0] = e.hit ? 1f : 0f;
                return e.len;
            }
            float len = raycastLength(be, yawDeg, pitchDeg, maxLen, hitOut);
            if (e == null) {
                e = new Entry();
                entries.put(key, e);
            }
            e.len = len;
            e.hit = hitOut[0] > 0.5f;
            e.maxLen = maxLen;
            e.stamp = now;
            return len;
        }

        private static final class Entry {
            float len;
            float maxLen;
            boolean hit;
            long stamp;
        }
    }

    /** Per-frame scan geometry in lens space, shared by the haze and core layers. */
    private static final class ScanGeometry {
        final LaserSegment[] segment;
        final Vec3[] dir0;
        final Vec3[] dir1;
        final float[] len0;
        final float[] len1;
        final boolean[] hit0;
        final boolean[] hit1;
        final float[] spanDeg;
        final int[] color;
        final float[] brightness;
        Vec3 origin;
        int count;

        ScanGeometry(int capacity) {
            segment = new LaserSegment[capacity];
            dir0 = new Vec3[capacity];
            dir1 = new Vec3[capacity];
            len0 = new float[capacity];
            len1 = new float[capacity];
            hit0 = new boolean[capacity];
            hit1 = new boolean[capacity];
            spanDeg = new float[capacity];
            color = new int[capacity];
            brightness = new float[capacity];
        }
    }

    // ── Math helpers ──────────────────────────────────────────────────────

    private static Vec3 localDir(float yawDeg, float pitchDeg) {
        double y = Math.toRadians(yawDeg);
        double p = Math.toRadians(pitchDeg);
        double cy = Math.cos(y), sy = Math.sin(y);
        double cp = Math.cos(p), sp = Math.sin(p);
        // Same chain as mulPose(YP yaw) * mulPose(XP pitch) applied to +Z.
        return new Vec3(sy * cp, -sp, cy * cp);
    }

    private static Vec3 slerp(Vec3 a, Vec3 b, float t) {
        double dot = Mth.clamp(a.dot(b), -1.0, 1.0);
        if (dot > 0.9994) {
            return a.add(b.subtract(a).scale(t)).normalize();
        }
        double theta = Math.acos(dot);
        double sin = Math.sin(theta);
        return a.scale(Math.sin((1.0 - t) * theta) / sin).add(b.scale(Math.sin(t * theta) / sin));
    }

    private static int hotCore(int color) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        r = Math.min(255, r + ((255 - r) * 7) / 10);
        g = Math.min(255, g + ((255 - g) * 7) / 10);
        b = Math.min(255, b + ((255 - b) * 7) / 10);
        return (r << 16) | (g << 8) | b;
    }

    private static int saturate(int color) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int max = Math.max(r, Math.max(g, b));
        if (max <= 0) {
            return color;
        }
        float scale = 255f / max;
        r = Math.min(255, Math.round(r * scale));
        g = Math.min(255, Math.round(g * scale));
        b = Math.min(255, Math.round(b * scale));
        return (r << 16) | (g << 8) | b;
    }

    private static int packAlpha(float alpha) {
        return Mth.clamp(Math.round(alpha * 255.0f), 0, 255);
    }

    private static float ildaToYaw(short x, float halfAngle) {
        return (x / ILDA_FULL) * halfAngle;
    }

    private static float ildaToPitch(short y, float halfAngle) {
        return -(y / ILDA_FULL) * halfAngle;
    }

    private static float interpolatedPan(LaserProjectorBlockEntity be, float partialTicks) {
        return be.getPrevPan() + (be.getPan() - be.getPrevPan()) * partialTicks;
    }

    private static float interpolatedTilt(LaserProjectorBlockEntity be, float partialTicks) {
        return be.getPrevTilt() + (be.getTilt() - be.getPrevTilt()) * partialTicks;
    }
}
