package com.github.dumann089.theatricalextralights.client.blockentities;

import com.github.dumann089.theatricalextralights.blockentities.FollowspotBlockEntity;
import com.github.dumann089.theatricalextralights.client.Beam2DRenderTypes;
import com.github.dumann089.theatricalextralights.client.ModShaders;
import com.github.dumann089.theatricalextralights.client.blockentities.GoboGPUProjector;
import com.github.dumann089.theatricalextralights.client.followspot.FollowspotFixtureCameraSession;
import com.github.dumann089.theatricalextralights.client.render.beam.BeamRenderData;
import com.github.dumann089.theatricalextralights.client.render.beam.raymarch.RaymarchBeamRenderer;
import com.github.dumann089.theatricalextralights.config.TheatricalExtraLightsConfig;
import com.github.dumann089.theatricalextralights.util.FixtureMountTransform;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.imabad.theatrical.TheatricalExpectPlatform;
import dev.imabad.theatrical.blocks.HangableBlock;
import dev.imabad.theatrical.client.LazyRenderers;
import dev.imabad.theatrical.config.TheatricalConfig;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.Optional;
import java.util.WeakHashMap;

public class FollowspotRenderer extends ExtraLightsFixtureRenderer<FollowspotBlockEntity> {

    private BakedModel cachedPanModel;
    private BakedModel cachedTiltModel;
    private BakedModel cachedStaticModel;

    private final WeakHashMap<FollowspotBlockEntity, GoboGPUProjector> goboProjectors =
            new WeakHashMap<>();

    private static final WeakHashMap<FollowspotBlockEntity, TrackedAngleInterpolator>
            ANGLE_INTERPOLATORS = new WeakHashMap<>();

    private static final Vec3 LENS_GLOW_OFFSET =
            new Vec3(0.5F, 1.343F, -0.437F);

    private static final float LENS_GLOW_SIZE = 0.14F;

    private static final float MIN_ZOOM_ANGLE = 1.0F;
    private static final float MAX_ZOOM_ANGLE = 25.0F;

    public FollowspotRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void renderModel(
            FollowspotBlockEntity blockEntity,
            PoseStack poseStack,
            VertexConsumer vertexConsumer,
            Direction facing,
            float partialTicks,
            boolean isFlipped,
            BlockState blockState,
            boolean isHanging,
            int packedLight,
            int packedOverlay
    ) {
        if (FollowspotFixtureCameraSession.isControlling(blockEntity.getBlockPos())) {
            return;
        }

        if (cachedStaticModel == null) {
            cachedStaticModel = TheatricalExpectPlatform.getBakedModel(
                    blockEntity.getFixture().getStaticModel()
            );
        }

        if (cachedPanModel == null) {
            cachedPanModel = TheatricalExpectPlatform.getBakedModel(
                    blockEntity.getFixture().getPanModel()
            );
        }

        if (cachedTiltModel == null) {
            cachedTiltModel = TheatricalExpectPlatform.getBakedModel(
                    blockEntity.getFixture().getTiltModel()
            );
        }

        RenderAngles angles = getRenderAngles(blockEntity, partialTicks);

        poseStack.translate(0.5F, 0.0F, 0.5F);

        if (isHanging) {
            Direction hangDirection = blockState.getValue(HangableBlock.HANG_DIRECTION);

            poseStack.translate(0.0F, 0.5F, 0.0F);

            if (hangDirection.getAxis() != Direction.Axis.Y) {
                if (hangDirection.getAxis() == Direction.Axis.Z) {
                    if (hangDirection == Direction.SOUTH) {
                        poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
                        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
                    } else {
                        poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
                        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
                    }
                } else {
                    if (hangDirection == Direction.EAST) {
                        poseStack.mulPose(Axis.ZN.rotationDegrees(-90.0F));
                    } else {
                        poseStack.mulPose(Axis.ZN.rotationDegrees(90.0F));
                    }
                }
            } else if (hangDirection == Direction.UP) {
                poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
            }

            poseStack.translate(0.0F, -0.5F, 0.0F);
        }

        poseStack.mulPose(Axis.YP.rotationDegrees(facing.toYRot()));
        poseStack.translate(-0.5F, 0.0F, -0.5F);

        if (isHanging) {
            Optional<BlockState> support = blockEntity.getSupportingStructure();

            if (support.isPresent()) {
                float[] transforms = blockEntity.getFixture().getTransforms(
                        blockState,
                        support.get()
                );

                poseStack.translate(
                        transforms[0],
                        transforms[1],
                        transforms[2]
                );
            } else {
                poseStack.translate(0.0F, 0.19F, 0.0F);
            }

            poseStack.translate(0.0F, -0.08F, 0.0F);
        }

        if (isFlipped) {
            poseStack.translate(0.5F, 0.5F, 0.5F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
            poseStack.translate(-0.5F, -0.5F, -0.5F);
        }

        minecraftRenderModel(
                poseStack,
                vertexConsumer,
                blockState,
                cachedStaticModel,
                packedLight,
                packedOverlay
        );

        float[] pans = blockEntity.getFixture().getPanRotationPosition();

        poseStack.translate(pans[0], pans[1], pans[2]);
        poseStack.mulPose(Axis.YP.rotationDegrees(angles.pan));
        poseStack.translate(-pans[0], -pans[1], -pans[2]);

        minecraftRenderModel(
                poseStack,
                vertexConsumer,
                blockState,
                cachedPanModel,
                packedLight,
                packedOverlay
        );

        float[] tilts = blockEntity.getFixture().getTiltRotationPosition();

        poseStack.translate(tilts[0], tilts[1], tilts[2]);

        if (isFlipped) {
            poseStack.mulPose(Axis.XP.rotationDegrees(-180.0F));
        } else {
            poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
        }

        poseStack.mulPose(Axis.XP.rotationDegrees(angles.tilt));
        poseStack.translate(-tilts[0], -tilts[1], -tilts[2]);

        minecraftRenderModel(
                poseStack,
                vertexConsumer,
                blockState,
                cachedTiltModel,
                packedLight,
                packedOverlay
        );
    }

    @Override
    public void beforeRenderBeam(
            FollowspotBlockEntity blockEntity,
            PoseStack poseStack,
            VertexConsumer vertexConsumer,
            MultiBufferSource multiBufferSource,
            Direction facing,
            float partialTicks,
            boolean isFlipped,
            BlockState blockState,
            boolean isHanging,
            int packedLight,
            int packedOverlay
    ) {
        if (blockEntity.getLevel() == null
                || blockEntity.getDisplayIntensity(partialTicks) <= 0.0F) {
            return;
        }

        RenderAngles angles = getRenderAngles(blockEntity, partialTicks);

        float currentPan = angles.pan;
        float currentTilt = angles.tilt;

        float[] beamStart = blockEntity.getFixture().getBeamStartPosition();

        Vec3 localLensOffset = new Vec3(
                beamStart[0],
                beamStart[1],
                beamStart[2]
        );

        float[] panPivot = blockEntity.getFixture().getPanRotationPosition();
        float[] tiltPivot = blockEntity.getFixture().getTiltRotationPosition();

        float[] structuralTransform = new float[]{0.0F, 0.19F, 0.0F};

        Optional<BlockState> support = blockEntity.getSupportingStructure();

        if (isHanging && support.isPresent()) {
            structuralTransform = blockEntity.getFixture().getTransforms(
                    blockState,
                    support.get()
            );
        }

        goboProjectors.computeIfAbsent(
                blockEntity,
                key -> new GoboGPUProjector()
        ).render(
                blockEntity,
                multiBufferSource,
                facing,
                partialTicks,
                isFlipped,
                blockState,
                isHanging,
                localLensOffset,
                panPivot,
                tiltPivot,
                structuralTransform,
                MIN_ZOOM_ANGLE,
                MAX_ZOOM_ANGLE,
                currentPan,
                currentTilt,
                0.0F
        );

        submitRaymarchBeam(
                blockEntity,
                facing,
                partialTicks,
                isFlipped,
                blockState,
                isHanging,
                currentPan,
                currentTilt,
                localLensOffset
        );

        queueLensRender(
                blockEntity,
                facing,
                isFlipped,
                blockState,
                isHanging,
                currentPan,
                currentTilt
        );
    }

    private void submitRaymarchBeam(
            FollowspotBlockEntity blockEntity,
            Direction facing,
            float partialTicks,
            boolean isFlipped,
            BlockState blockState,
            boolean isHanging,
            float currentPan,
            float currentTilt,
            Vec3 localLensOffset
    ) {
        if (!TheatricalExtraLightsConfig.isVolumetricBeamEnabled()
                || !TheatricalExtraLightsConfig.isRaymarchEngine()
                || !ModShaders.canUseRaymarch()) {
            return;
        }

        PoseStack raymarchPose = new PoseStack();

        preparePoseStack(
                blockEntity,
                raymarchPose,
                facing,
                partialTicks,
                isFlipped,
                blockState,
                isHanging,
                currentPan,
                currentTilt
        );

        raymarchPose.translate(
                localLensOffset.x,
                localLensOffset.y,
                localLensOffset.z
        );

        Matrix4f headMatrix = raymarchPose.last().pose();

        Vec3 origin = new Vec3(
                headMatrix.m30(),
                headMatrix.m31(),
                headMatrix.m32()
        );

        Vec3 axisU = new Vec3(
                headMatrix.m00(),
                headMatrix.m01(),
                headMatrix.m02()
        ).normalize();

        Vec3 axisV = new Vec3(
                headMatrix.m10(),
                headMatrix.m11(),
                headMatrix.m12()
        ).normalize();

        Vec3 beamDir = new Vec3(
                -headMatrix.m20(),
                -headMatrix.m21(),
                -headMatrix.m22()
        ).normalize();

        float zoomNorm = blockEntity.getPartialZoom(partialTicks) / 255.0F;

        float coneHalfAngle = MIN_ZOOM_ANGLE
                + zoomNorm * (MAX_ZOOM_ANGLE - MIN_ZOOM_ANGLE);

        float tanHalfAngle = (float) Math.tan(
                Math.toRadians(coneHalfAngle)
        );

        float scanLength = (float) blockEntity.getDistance();

        if (scanLength <= 0.01F) {
            scanLength = TheatricalExtraLightsConfig.getVolumetricBeamDistance();
        }

        ResourceLocation goboTexture = blockEntity.getGoboLibrary().getTexture(
                blockEntity.getGobo()
        );

        if (goboTexture == null) {
            goboTexture = new ResourceLocation(
                    "theatricalextralights",
                    "textures/gobos/generic_1/open.png"
            );
        }

        int color = blockEntity.getPartialColour(partialTicks);

        if (color == 0) {
            color = 0xFFFFFF;
        }

        float intensity = blockEntity.getPartialIntensity(partialTicks) / 255.0F;

        BeamRenderData renderData = new BeamRenderData(
                blockEntity.getBlockPos(),
                origin,
                beamDir,
                axisU,
                axisV,
                zoomNorm,
                scanLength,
                tanHalfAngle,
                color,
                intensity,
                goboTexture,
                goboTexture,
                blockEntity.getGoboRotation(),
                0.0F,
                blockEntity.getLevel(),
                1.0F,
                1.0F,
                0.11F
        );

        RaymarchBeamRenderer.submit(renderData);
    }

    private void queueLensRender(
            FollowspotBlockEntity blockEntity,
            Direction facing,
            boolean isFlipped,
            BlockState blockState,
            boolean isHanging,
            float currentPan,
            float currentTilt
    ) {
        LazyRenderers.addLazyRender(new LazyRenderers.LazyRenderer() {
            @Override
            public void render(
                    MultiBufferSource.BufferSource bufferSource,
                    PoseStack poseStack,
                    Camera camera,
                    float partialTick
            ) {
                float intensity = blockEntity.getDisplayIntensity(partialTick);

                if (intensity <= 0.0F) {
                    return;
                }

                int color = blockEntity.getDisplayColour(partialTick);

                if (color == 0) {
                    color = 0xFFFFFF;
                }

                float alpha = (intensity / 255.0F)
                        * (float) TheatricalConfig.INSTANCE.CLIENT.beamOpacity;

                Vec3 offset = Vec3.atLowerCornerOf(
                        blockEntity.getBlockPos()
                ).subtract(camera.getPosition());

                poseStack.pushPose();

                poseStack.translate(
                        offset.x,
                        offset.y,
                        offset.z
                );

                preparePoseStack(
                        blockEntity,
                        poseStack,
                        facing,
                        partialTick,
                        isFlipped,
                        blockState,
                        isHanging,
                        currentPan,
                        currentTilt
                );

                VertexConsumer builder = bufferSource.getBuffer(
                        Beam2DRenderTypes.getBeam()
                );

                poseStack.pushPose();

                poseStack.translate(
                        LENS_GLOW_OFFSET.x,
                        LENS_GLOW_OFFSET.y,
                        LENS_GLOW_OFFSET.z
                );

                renderLensGlow(
                        builder,
                        poseStack,
                        color,
                        LENS_GLOW_SIZE,
                        intensity
                );

                poseStack.popPose();

                renderLens(
                        bufferSource,
                        poseStack,
                        alpha,
                        color,
                        0.13F,
                        0.5F,
                        1.031F,
                        0.0F
                );

                poseStack.popPose();
            }

            @Override
            public Vec3 getPos(float partialTick) {
                return blockEntity.getBlockPos().getCenter();
            }
        });
    }

    @Override
    public void preparePoseStack(
            FollowspotBlockEntity blockEntity,
            PoseStack poseStack,
            Direction facing,
            float partialTicks,
            boolean isFlipped,
            BlockState blockState,
            boolean isHanging
    ) {
        RenderAngles angles = getRenderAngles(blockEntity, partialTicks);

        preparePoseStack(
                blockEntity,
                poseStack,
                facing,
                partialTicks,
                isFlipped,
                blockState,
                isHanging,
                angles.pan,
                angles.tilt
        );
    }

    private void preparePoseStack(
            FollowspotBlockEntity blockEntity,
            PoseStack poseStack,
            Direction facing,
            float partialTicks,
            boolean isFlipped,
            BlockState blockState,
            boolean isHanging,
            float currentPan,
            float currentTilt
    ) {
        FixtureMountTransform.apply(poseStack, blockEntity);

        poseStack.translate(0.5F, 0.0F, 0.5F);

        if (isHanging) {
            Direction hangDirection = blockState.getValue(HangableBlock.HANG_DIRECTION);

            poseStack.translate(0.0F, 0.5F, 0.0F);

            if (hangDirection.getAxis() != Direction.Axis.Y) {
                if (hangDirection.getAxis() == Direction.Axis.Z) {
                    if (hangDirection == Direction.SOUTH) {
                        poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
                        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
                    } else {
                        poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
                        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
                    }
                } else {
                    if (hangDirection == Direction.EAST) {
                        poseStack.mulPose(Axis.ZN.rotationDegrees(-90.0F));
                    } else {
                        poseStack.mulPose(Axis.ZN.rotationDegrees(90.0F));
                    }
                }
            } else if (hangDirection == Direction.UP) {
                poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
            }

            poseStack.translate(0.0F, -0.5F, 0.0F);
        }

        poseStack.mulPose(Axis.YP.rotationDegrees(facing.toYRot()));
        poseStack.translate(-0.5F, 0.0F, -0.5F);

        if (isHanging) {
            Optional<BlockState> support = blockEntity.getSupportingStructure();

            if (support.isPresent()) {
                float[] transforms = blockEntity.getFixture().getTransforms(
                        blockState,
                        support.get()
                );

                poseStack.translate(
                        transforms[0],
                        transforms[1],
                        transforms[2]
                );
            } else {
                poseStack.translate(0.0F, 0.19F, 0.0F);
            }

            poseStack.translate(0.0F, -0.08F, 0.0F);
        }

        if (isFlipped) {
            poseStack.translate(0.5F, 0.5F, 0.5F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
            poseStack.translate(-0.5F, -0.5F, -0.5F);
        }

        float[] pans = blockEntity.getFixture().getPanRotationPosition();

        poseStack.translate(pans[0], pans[1], pans[2]);
        poseStack.mulPose(Axis.YP.rotationDegrees(currentPan));
        poseStack.translate(-pans[0], -pans[1], -pans[2]);

        float[] tilts = blockEntity.getFixture().getTiltRotationPosition();

        poseStack.translate(tilts[0], tilts[1], tilts[2]);

        if (isFlipped) {
            poseStack.mulPose(Axis.XP.rotationDegrees(-180.0F));
        } else {
            poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
        }

        poseStack.mulPose(Axis.XP.rotationDegrees(currentTilt));
        poseStack.translate(-tilts[0], -tilts[1], -tilts[2]);
    }

    private RenderAngles getRenderAngles(
            FollowspotBlockEntity blockEntity,
            float partialTicks
    ) {
        if (FollowspotFixtureCameraSession.isControlling(blockEntity.getBlockPos())) {
            FollowspotFixtureCameraSession session =
                    FollowspotFixtureCameraSession.getActive();

            if (session != null) {
                return new RenderAngles(
                        session.getPanAngle(),
                        session.getTiltAngle()
                );
            }
        }

        if (blockEntity.getTrackedEntity() != null
                && blockEntity.getLevel() != null) {
            double frameTime = blockEntity.getLevel().getGameTime() + partialTicks;

            TrackedAngleInterpolator interpolator =
                    ANGLE_INTERPOLATORS.computeIfAbsent(
                            blockEntity,
                            key -> new TrackedAngleInterpolator()
                    );

            return interpolator.update(
                    blockEntity.getPan(),
                    blockEntity.getTilt(),
                    frameTime
            );
        }

        ANGLE_INTERPOLATORS.remove(blockEntity);

        float pan = Mth.lerp(
                partialTicks,
                blockEntity.getPrevPan(),
                blockEntity.getPan()
        );

        float tilt = Mth.lerp(
                partialTicks,
                blockEntity.getPrevTilt(),
                blockEntity.getTilt()
        );

        return new RenderAngles(pan, tilt);
    }

    private static final class TrackedAngleInterpolator {

        private boolean initialized;

        private float startPan;
        private float startTilt;

        private float targetPan;
        private float targetTilt;

        private double startTime;

        RenderAngles update(
                float newPan,
                float newTilt,
                double frameTime
        ) {
            if (!initialized) {
                initialized = true;

                startPan = newPan;
                startTilt = newTilt;

                targetPan = newPan;
                targetTilt = newTilt;

                startTime = frameTime;

                return new RenderAngles(newPan, newTilt);
            }

            if (newPan != targetPan || newTilt != targetTilt) {
                RenderAngles current = sample(frameTime);

                startPan = current.pan;
                startTilt = current.tilt;

                targetPan = newPan;
                targetTilt = newTilt;

                startTime = frameTime;
            }

            return sample(frameTime);
        }

        private RenderAngles sample(double frameTime) {
            float progress = (float) Mth.clamp(
                    frameTime - startTime,
                    0.0,
                    1.0
            );

            return new RenderAngles(
                    Mth.lerp(progress, startPan, targetPan),
                    Mth.lerp(progress, startTilt, targetTilt)
            );
        }
    }

    private record RenderAngles(float pan, float tilt) {
    }

    public static void resetBeamLengthSmoothing() {
        ANGLE_INTERPOLATORS.clear();
    }
}