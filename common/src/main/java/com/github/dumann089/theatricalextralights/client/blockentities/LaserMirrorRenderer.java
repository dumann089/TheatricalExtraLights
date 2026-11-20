package com.github.dumann089.theatricalextralights.client.blockentities;

import com.github.dumann089.theatricalextralights.blockentities.LaserMirrorBlockEntity;
import com.github.dumann089.theatricalextralights.client.ExtraLightsRenderTypes;
import com.github.dumann089.theatricalextralights.client.render.laser.LaserFigure;
import com.github.dumann089.theatricalextralights.client.render.laser.LaserRaymarchRenderer;
import com.github.dumann089.theatricalextralights.config.TheatricalExtraLightsConfig;
import com.github.dumann089.theatricalextralights.util.FixtureMountTransform;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.imabad.theatrical.blocks.HangableBlock;
import dev.imabad.theatrical.client.LazyRenderers;
import dev.imabad.theatrical.TheatricalExpectPlatform;
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
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

/**
 * Realistic DMX laser mirror. The five-channel personality remains unchanged:
 * intensity, red, green, blue and focus. Focus controls the 1..9 ray fan.
 */
public class LaserMirrorRenderer extends ExtraLightsRenderer<LaserMirrorBlockEntity> {

    private static final float MAX_AIR_LENGTH = 96.0f;
    private static final float RAYCAST_SKIP_RADIUS = 2.5f;
    private static final long RAYCAST_TTL_NANOS = 180_000_000L;
    private static final float RAYCAST_QUANT = 8192.0f;
    private static final float FAN_HALF_ANGLE = 15.0f;

    private BakedModel cachedPanModel;
    private BakedModel cachedTiltModel;
    private BakedModel cachedStaticModel;

    private final WeakHashMap<LaserMirrorBlockEntity, RaycastCache> raycastCaches = new WeakHashMap<>();

    public LaserMirrorRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void renderModel(LaserMirrorBlockEntity blockEntity, PoseStack poseStack, VertexConsumer vertexConsumer,
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

        applyBasePose(blockEntity, poseStack, facing, isFlipped, blockState, isHanging);
        minecraftRenderModel(poseStack, vertexConsumer, blockState, cachedStaticModel, packedLight, packedOverlay);

        float[] panPivot = blockEntity.getFixture().getPanRotationPosition();
        poseStack.translate(panPivot[0], panPivot[1], panPivot[2]);
        poseStack.mulPose(Axis.YP.rotationDegrees(interpolatedPan(blockEntity, partialTicks)));
        poseStack.translate(-panPivot[0], -panPivot[1], -panPivot[2]);
        minecraftRenderModel(poseStack, vertexConsumer, blockState, cachedPanModel, packedLight, packedOverlay);

        float[] tiltPivot = blockEntity.getFixture().getTiltRotationPosition();
        poseStack.translate(tiltPivot[0], tiltPivot[1], tiltPivot[2]);
        poseStack.mulPose(Axis.XP.rotationDegrees(isFlipped ? -180.0f : 180.0f));
        poseStack.mulPose(Axis.XP.rotationDegrees(interpolatedTilt(blockEntity, partialTicks)));
        poseStack.translate(-tiltPivot[0], -tiltPivot[1], -tiltPivot[2]);
        minecraftRenderModel(poseStack, vertexConsumer, blockState, cachedTiltModel, packedLight, packedOverlay);
    }

    @Override
    public void beforeRenderBeam(LaserMirrorBlockEntity blockEntity, PoseStack poseStack, VertexConsumer vertexConsumer,
                                 MultiBufferSource multiBufferSource, Direction facing, float partialTicks,
                                 boolean isFlipped, BlockState blockState, boolean isHanging,
                                 int packedLight, int packedOverlay) {
        FrameData frame = buildFrame(blockEntity, facing, partialTicks, isFlipped, blockState, isHanging);
        if (frame == null || frame.intensity <= 0.001f) {
            return;
        }

        if (LaserRaymarchRenderer.isAvailable()) {
            submitRealistic(blockEntity, frame);
            return;
        }

        LazyRenderers.addLazyRender(new LazyRenderers.LazyRenderer() {
            @Override
            public void render(MultiBufferSource.BufferSource bufferSource, PoseStack renderPose,
                               Camera camera, float partialTick) {
                renderLegacy(blockEntity, frame, bufferSource, renderPose, camera,
                        facing, partialTick, isFlipped, blockState, isHanging);
            }

            @Override
            public Vec3 getPos(float partialTick) {
                return blockEntity.getBlockPos().getCenter();
            }
        });
    }

    /** Builds the fan once; both the raymarch and fallback renderers use this exact geometry. */
    private FrameData buildFrame(LaserMirrorBlockEntity be, Direction facing, float partialTicks,
                                 boolean isFlipped, BlockState blockState, boolean isHanging) {
        if (be.getLevel() == null) {
            return null;
        }

        float intensity = Mth.clamp(be.getDisplayIntensity(partialTicks) / 255.0f, 0.0f, 1.0f);
        if (intensity <= 0.001f) {
            return null;
        }

        int focus = Mth.clamp(be.getFocus(), 0, 255);
        int count = Mth.clamp((int) Math.ceil(focus / 255.0f * 9.0f), 1, 9);
        int color = be.getDisplayColour(partialTicks) & 0xFFFFFF;
        float maxLength = Math.min(TheatricalExtraLightsConfig.getLaserBeamLength(), MAX_AIR_LENGTH);
        if (maxLength <= 0.001f) {
            return null;
        }

        PoseStack localPose = new PoseStack();
        preparePoseStack(be, localPose, facing, partialTicks, isFlipped, blockState, isHanging);
        Matrix4f pose = localPose.last().pose();

        float[] source = be.getFixture().getBeamStartPosition();
        Vector4f sourcePoint = new Vector4f(source[0], source[1], source[2], 1.0f);
        pose.transform(sourcePoint);
        Vec3 localOrigin = new Vec3(source[0], source[1], source[2]);
        Vec3 worldOrigin = new Vec3(
                be.getBlockPos().getX() + sourcePoint.x,
                be.getBlockPos().getY() + sourcePoint.y,
                be.getBlockPos().getZ() + sourcePoint.z
        );

        Vec3[] localDirections = new Vec3[count];
        Vec3[] worldDirections = new Vec3[count];
        float[] lengths = new float[count];
        boolean[] hits = new boolean[count];
        Vector4f transformedDirection = new Vector4f();

        RaycastCache cache = raycastCaches.computeIfAbsent(be, key -> new RaycastCache());
        long now = System.nanoTime();
        cache.prepare(worldOrigin, now);
        boolean[] hitOut = new boolean[1];

        for (int i = 0; i < count; i++) {
            float angle = count == 1 ? 0.0f : -FAN_HALF_ANGLE + (2.0f * FAN_HALF_ANGLE * i / (count - 1));
            Vec3 localDirection = localDirection(angle);
            transformedDirection.set((float) localDirection.x, (float) localDirection.y,
                    (float) localDirection.z, 0.0f);
            pose.transform(transformedDirection);
            Vec3 worldDirection = new Vec3(transformedDirection.x, transformedDirection.y, transformedDirection.z);
            if (worldDirection.lengthSqr() < 1.0e-10) {
                worldDirection = new Vec3(0.0, 0.0, -1.0);
            } else {
                worldDirection = worldDirection.normalize();
            }

            localDirections[i] = localDirection;
            worldDirections[i] = worldDirection;
            lengths[i] = cache.length(be, worldOrigin, worldDirection, maxLength, now, hitOut);
            hits[i] = hitOut[0];
        }

        return new FrameData(localOrigin, worldOrigin, localDirections, worldDirections,
                lengths, hits, count, color, intensity);
    }

    /** Sends the same fan to the volumetric raymarch renderer used by LaserProjectorRenderer. */
    private void submitRealistic(LaserMirrorBlockEntity be, FrameData frame) {
        LaserFigure figure = LaserRaymarchRenderer.begin();
        if (figure == null) {
            return;
        }

        int n = frame.count;
        float rayShare = (float) Math.pow(Math.max(1, n), -0.75);
        float rayWeight = rayShare * 0.72f;

        // Each DMX ray is an independent volumetric beam. Do not add FLAG_SHEET
        // segments between neighboring rays: those create a continuous fan surface.
        for (int i = 0; i < n; i++) {
            int flags = LaserFigure.FLAG_BEAM0 | (frame.hits[i] ? LaserFigure.FLAG_HIT0 : 0);
            figure.add(frame.worldDirections[i], frame.worldDirections[i], frame.lengths[i],
                    frame.lengths[i], 0.0f, frame.color, 0.0f, rayWeight, 0.0f,
                    0.0f, 0.0f, flags);
        }

        figure.fixturePos = be.getBlockPos();
        figure.origin = frame.worldOrigin;
        figure.intensity = frame.intensity;
        figure.totalSpan = 1.0f;
        figure.divergence = 0.0012f;
        figure.striation = 0.0f;
        figure.scanHead = 0.0f;
        figure.scanTrail = 0.0f;
    }

    private static float sum(float[] values, int endExclusive) {
        float result = 0.0f;
        for (int i = 0; i < endExclusive && i < values.length; i++) {
            result += values[i];
        }
        return result;
    }

    /** Fallback for Iris/shaderpacks or when the realistic laser shader is disabled. */
    private void renderLegacy(LaserMirrorBlockEntity be, FrameData frame,
                              MultiBufferSource.BufferSource bufferSource, PoseStack poseStack,
                              Camera camera, Direction facing, float partialTicks,
                              boolean isFlipped, BlockState blockState, boolean isHanging) {
        poseStack.pushPose();
        Vec3 offset = Vec3.atLowerCornerOf(be.getBlockPos()).subtract(camera.getPosition());
        poseStack.translate(offset.x, offset.y, offset.z);
        preparePoseStack(be, poseStack, facing, partialTicks, isFlipped, blockState, isHanging);

        Vec3 cameraLocal = cameraInLocal(poseStack);
        VertexConsumer beam = bufferSource.getBuffer(ExtraLightsRenderTypes.BEAM);
        int core = hotCore(frame.color);
        float haze = Math.max(0.08f, TheatricalExtraLightsConfig.getLaserDacHazeRadius());
        float rayShare = (float) Math.pow(Math.max(1, frame.count), -0.75);

        for (int i = 0; i < frame.count; i++) {
            Vec3 end = frame.localOrigin.add(frame.localDirections[i].scale(frame.lengths[i]));
            renderBillboardShaft(beam, poseStack, cameraLocal, frame.localOrigin, end,
                    core, frame.color, frame.intensity * rayShare, haze);
            if (frame.hits[i]) {
                renderImpact(beam, poseStack, cameraLocal, end, core, frame.color,
                        frame.intensity * rayShare);
            }
        }

        poseStack.popPose();
    }

    private void renderBillboardShaft(VertexConsumer beam, PoseStack pose, Vec3 camera,
                                      Vec3 start, Vec3 end, int core, int color,
                                      float intensity, float hazeRadius) {
        Vec3 direction = end.subtract(start);
        if (direction.lengthSqr() < 1.0e-8 || intensity <= 0.002f) {
            return;
        }
        direction = direction.normalize();
        Vec3 midpoint = start.add(end).scale(0.5);
        Vec3 toCamera = camera.subtract(midpoint);
        if (toCamera.lengthSqr() < 1.0e-10) {
            return;
        }
        Vec3 toCameraNormal = toCamera.normalize();
        Vec3 right = direction.cross(toCamera);
        if (right.lengthSqr() < 1.0e-10) {
            Vec3 up = Math.abs(direction.y) > 0.92 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
            right = direction.cross(up);
        }
        right = right.normalize();

        float along = (float) Math.abs(direction.dot(toCameraNormal));
        float mie = 1.0f + 2.4f * along * along;
        billboardLayer(beam, pose, start, end, right, 0.007f, 0xFFFFFF, intensity * 0.62f * mie);
        billboardLayer(beam, pose, start, end, right, 0.018f, core, intensity * 0.95f);
        billboardLayer(beam, pose, start, end, right, 0.048f, color, intensity * 0.42f);
        billboardLayer(beam, pose, start, end, right, hazeRadius * 0.95f, color, intensity * 0.16f);
        billboardLayer(beam, pose, start, end, right, hazeRadius * 2.15f, color, intensity * 0.055f);
    }

    private void billboardLayer(VertexConsumer beam, PoseStack pose, Vec3 start, Vec3 end,
                                Vec3 right, float halfWidth, int color, float alpha) {
        if (alpha <= 0.002f || halfWidth <= 0.0f) {
            return;
        }
        Vec3 middle = start.add(end.subtract(start).scale(0.82));
        int full = packAlpha(alpha);
        Vec3 aLeft = start.subtract(right.scale(halfWidth));
        Vec3 aRight = start.add(right.scale(halfWidth));
        Vec3 mLeft = middle.subtract(right.scale(halfWidth));
        Vec3 mRight = middle.add(right.scale(halfWidth));
        Vec3 bLeft = end.subtract(right.scale(halfWidth * 1.25f));
        Vec3 bRight = end.add(right.scale(halfWidth * 1.25f));
        fadedQuad(beam, pose, aLeft, start, middle, mLeft, color, 0, full, full, 0);
        fadedQuad(beam, pose, start, aRight, mRight, middle, color, full, 0, 0, full);
        fadedQuad(beam, pose, mLeft, middle, end, bLeft, color, 0, full, 0, 0);
        fadedQuad(beam, pose, middle, mRight, bRight, end, color, full, 0, 0, 0);
    }

    private void renderImpact(VertexConsumer beam, PoseStack pose, Vec3 camera,
                              Vec3 point, int core, int color, float intensity) {
        Vec3 toCamera = camera.subtract(point);
        if (toCamera.lengthSqr() < 1.0e-8) {
            return;
        }
        toCamera = toCamera.normalize();
        Vec3 up = Math.abs(toCamera.y) > 0.92 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 right = toCamera.cross(up).normalize();
        Vec3 vertical = right.cross(toCamera).normalize();
        renderSparkBillboard(beam, pose, point, right, vertical, 0.22f, color, intensity * 0.28f);
        renderSparkBillboard(beam, pose, point, right, vertical, 0.08f, core, intensity * 0.72f);
        renderSparkBillboard(beam, pose, point, right, vertical, 0.028f, 0xFFFFFF, intensity * 0.90f);
    }

    private void renderSparkBillboard(VertexConsumer beam, PoseStack pose, Vec3 point,
                                      Vec3 right, Vec3 vertical, float size, int color, float alpha) {
        int full = packAlpha(alpha);
        Vec3 r = right.scale(size);
        Vec3 v = vertical.scale(size);
        Vec3 c00 = point.subtract(r).subtract(v);
        Vec3 c10 = point.add(r).subtract(v);
        Vec3 c11 = point.add(r).add(v);
        Vec3 c01 = point.subtract(r).add(v);
        fadedQuad(beam, pose, point, c10, c11, point, color, full, 0, 0, full);
        fadedQuad(beam, pose, point, c11, c01, point, color, full, 0, 0, full);
        fadedQuad(beam, pose, point, c01, c00, point, color, full, 0, 0, full);
        fadedQuad(beam, pose, point, c00, c10, point, color, full, 0, 0, full);
    }

    private void fadedQuad(VertexConsumer beam, PoseStack pose,
                           Vec3 a, Vec3 b, Vec3 c, Vec3 d,
                           int color, int alphaA, int alphaB, int alphaC, int alphaD) {
        if (alphaA + alphaB + alphaC + alphaD <= 0) {
            return;
        }
        Matrix4f matrix = pose.last().pose();
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;
        addVertex(beam, matrix, red, green, blue, alphaA, a);
        addVertex(beam, matrix, red, green, blue, alphaB, b);
        addVertex(beam, matrix, red, green, blue, alphaC, c);
        addVertex(beam, matrix, red, green, blue, alphaD, d);
    }

    private static void addVertex(VertexConsumer builder, Matrix4f matrix,
                                  int red, int green, int blue, int alpha, Vec3 point) {
        builder.vertex(matrix, (float) point.x, (float) point.y, (float) point.z)
                .color(red, green, blue, alpha)
                .endVertex();
    }

    private void applyBasePose(LaserMirrorBlockEntity blockEntity, PoseStack poseStack, Direction facing,
                               boolean isFlipped, BlockState blockState, boolean isHanging) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        if (isHanging) {
            Direction hangDirection = blockState.getValue(HangableBlock.HANG_DIRECTION);
            poseStack.translate(0.0, 0.5, 0.0);
            if (hangDirection.getAxis() != Direction.Axis.Y) {
                if (hangDirection.getAxis() == Direction.Axis.Z) {
                    poseStack.mulPose(Axis.ZP.rotationDegrees(90.0f));
                    poseStack.mulPose(Axis.XP.rotationDegrees(hangDirection == Direction.SOUTH ? -90.0f : 90.0f));
                } else {
                    poseStack.mulPose(Axis.ZN.rotationDegrees(hangDirection == Direction.EAST ? -90.0f : 90.0f));
                }
            }
            poseStack.translate(0.0, -0.5, 0.0);
        }

        poseStack.mulPose(Axis.YP.rotationDegrees(facing.toYRot()));
        poseStack.translate(-0.5F, 0.0F, -0.5F);

        if (isHanging) {
            Optional<BlockState> support = blockEntity.getSupportingStructure();
            if (support.isPresent()) {
                float[] transforms = blockEntity.getFixture().getTransforms(blockState, support.get());
                poseStack.translate(transforms[0], transforms[1], transforms[2]);
            } else {
                poseStack.translate(0.0, 0.19, 0.0);
            }
            poseStack.translate(0.0, -0.08, 0.0);
        }

        if (isFlipped) {
            poseStack.translate(0.5F, 0.5F, 0.5F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(180.0f));
            poseStack.translate(-0.5F, -0.5F, -0.5F);
        }
    }

    /** Full local-to-world head transform used by both the model and all emitted rays. */
    public void preparePoseStack(LaserMirrorBlockEntity blockEntity, PoseStack poseStack, Direction facing,
                                 float partialTicks, boolean isFlipped, BlockState blockState, boolean isHanging) {
        FixtureMountTransform.apply(poseStack, blockEntity);
        applyBasePose(blockEntity, poseStack, facing, isFlipped, blockState, isHanging);

        float[] panPivot = blockEntity.getFixture().getPanRotationPosition();
        poseStack.translate(panPivot[0], panPivot[1], panPivot[2]);
        poseStack.mulPose(Axis.YP.rotationDegrees(interpolatedPan(blockEntity, partialTicks)));
        poseStack.translate(-panPivot[0], -panPivot[1], -panPivot[2]);

        float[] tiltPivot = blockEntity.getFixture().getTiltRotationPosition();
        poseStack.translate(tiltPivot[0], tiltPivot[1], tiltPivot[2]);
        poseStack.mulPose(Axis.XP.rotationDegrees(isFlipped ? -180.0f : 180.0f));
        poseStack.mulPose(Axis.XP.rotationDegrees(interpolatedTilt(blockEntity, partialTicks)));
        poseStack.translate(-tiltPivot[0], -tiltPivot[1], -tiltPivot[2]);
    }

    private static Vec3 cameraInLocal(PoseStack pose) {
        Matrix4f inverse = new Matrix4f(pose.last().pose());
        if (Math.abs(inverse.determinant()) < 1.0e-10f) {
            return Vec3.ZERO;
        }
        inverse.invert();
        Vector4f camera = new Vector4f(0.0f, 0.0f, 0.0f, 1.0f);
        camera.mul(inverse);
        return new Vec3(camera.x, camera.y, camera.z);
    }

    private static Vec3 localDirection(float yawDegrees) {
        double yaw = Math.toRadians(yawDegrees);
        return new Vec3(Math.sin(yaw), 0.0, Math.cos(yaw));
    }

    private static int hotCore(int color) {
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;
        red = Math.min(255, red + ((255 - red) * 7) / 10);
        green = Math.min(255, green + ((255 - green) * 7) / 10);
        blue = Math.min(255, blue + ((255 - blue) * 7) / 10);
        return (red << 16) | (green << 8) | blue;
    }

    private static int packAlpha(float alpha) {
        return Mth.clamp(Math.round(alpha * 255.0f), 0, 255);
    }

    private static float interpolatedPan(LaserMirrorBlockEntity be, float partialTicks) {
        return be.getPrevPan() + (be.getPan() - be.getPrevPan()) * partialTicks;
    }

    private static float interpolatedTilt(LaserMirrorBlockEntity be, float partialTicks) {
        return be.getPrevTilt() + (be.getTilt() - be.getPrevTilt()) * partialTicks;
    }

    private static float raycastLength(LaserMirrorBlockEntity be, Vec3 origin, Vec3 direction,
                                       float maxLength, boolean[] hitOut) {
        hitOut[0] = false;
        if (be.getLevel() == null || maxLength <= 0.001f || direction.lengthSqr() < 1.0e-8) {
            return maxLength;
        }

        Vec3 end = origin.add(direction.scale(maxLength));
        Vec3 start = origin;
        int safety = 24;
        while (safety-- > 0) {
            BlockHitResult hit = be.getLevel().clip(new ClipContext(
                    start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null));
            if (hit.getType() == HitResult.Type.MISS) {
                return maxLength;
            }

            BlockState hitState = be.getLevel().getBlockState(hit.getBlockPos());
            ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(hitState.getBlock());
            boolean theatricalFixture = blockId != null
                    && (blockId.getNamespace().equals("theatrical")
                    || blockId.getNamespace().equals("theatricalextralights"));
            boolean passThrough = blockId != null
                    && TheatricalExtraLightsConfig.isLaserPassThrough(blockId.toString());
            float distance = (float) hit.getLocation().distanceTo(origin);

            if (!theatricalFixture && !passThrough && distance >= RAYCAST_SKIP_RADIUS) {
                hitOut[0] = true;
                return Math.min(maxLength, distance);
            }

            start = hit.getLocation().add(direction.scale(0.01));
            if (start.distanceToSqr(end) < 1.0e-4) {
                return maxLength;
            }
        }
        return maxLength;
    }

    private static final class RaycastCache {
        private final Map<Long, Entry> entries = new HashMap<>();
        private Vec3 lastOrigin;
        private long now;

        void prepare(Vec3 origin, long nowNanos) {
            now = nowNanos;
            if (lastOrigin == null || lastOrigin.distanceToSqr(origin) > 0.0025 || entries.size() > 6000) {
                entries.clear();
            }
            lastOrigin = origin;
        }

        float length(LaserMirrorBlockEntity be, Vec3 origin, Vec3 direction,
                     float maxLength, long nowNanos, boolean[] hitOut) {
            int qx = Math.round((float) direction.x * RAYCAST_QUANT);
            int qy = Math.round((float) direction.y * RAYCAST_QUANT);
            int qz = Math.round((float) direction.z * RAYCAST_QUANT);
            long key = ((long) (qx & 0x7FFF) << 30)
                    | ((long) (qy & 0x7FFF) << 15)
                    | (long) (qz & 0x7FFF);

            Entry entry = entries.get(key);
            if (entry != null && nowNanos - entry.stamp <= RAYCAST_TTL_NANOS
                    && Float.compare(entry.maxLength, maxLength) == 0) {
                hitOut[0] = entry.hit;
                return entry.length;
            }

            float length = raycastLength(be, origin, direction, maxLength, hitOut);
            if (entry == null) {
                entry = new Entry();
                entries.put(key, entry);
            }
            entry.length = length;
            entry.maxLength = maxLength;
            entry.hit = hitOut[0];
            entry.stamp = nowNanos;
            return length;
        }

        private static final class Entry {
            float length;
            float maxLength;
            boolean hit;
            long stamp;
        }
    }

    private static final class FrameData {
        final Vec3 localOrigin;
        final Vec3 worldOrigin;
        final Vec3[] localDirections;
        final Vec3[] worldDirections;
        final float[] lengths;
        final boolean[] hits;
        final int count;
        final int color;
        final float intensity;

        FrameData(Vec3 localOrigin, Vec3 worldOrigin, Vec3[] localDirections, Vec3[] worldDirections,
                  float[] lengths, boolean[] hits, int count, int color, float intensity) {
            this.localOrigin = localOrigin;
            this.worldOrigin = worldOrigin;
            this.localDirections = localDirections;
            this.worldDirections = worldDirections;
            this.lengths = lengths;
            this.hits = hits;
            this.count = count;
            this.color = color;
            this.intensity = intensity;
        }
    }
}