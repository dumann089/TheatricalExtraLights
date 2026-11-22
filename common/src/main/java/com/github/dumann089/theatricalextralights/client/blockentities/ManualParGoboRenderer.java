package com.github.dumann089.theatricalextralights.client.blockentities;

import com.github.dumann089.theatricalextralights.blockentities.ManualParGoboBlockEntity;
import com.github.dumann089.theatricalextralights.client.Beam2DRenderTypes;
import com.github.dumann089.theatricalextralights.client.gobo.GoboLibrary;
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
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

public class ManualParGoboRenderer<T extends ManualParGoboBlockEntity> extends ExtraLightsFixtureRenderer<T> {

    protected static final Vec3 LENS_OFFSET = new Vec3(0.5F, 0.56F, 0.143F);
    private static final float PROJECTOR_MIN_ANGLE = 1.0F;
    private static final float PROJECTOR_MAX_ANGLE = 45.0F;
    private static final float BASE_RADIUS = 0.30F;

    private final Map<T, GoboGPUProjector> goboProjectors = new WeakHashMap<>();
    private final Double beamOpacity = TheatricalConfig.INSTANCE.CLIENT.beamOpacity;

    private BakedModel cachedPanModel;
    private BakedModel cachedTiltModel;
    private BakedModel cachedStaticModel;

    public ManualParGoboRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void renderModel(
            T blockEntity,
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
        if (cachedStaticModel == null) {
            cachedStaticModel = TheatricalExpectPlatform.getBakedModel(blockEntity.getFixture().getStaticModel());
        }
        if (cachedPanModel == null) {
            cachedPanModel = TheatricalExpectPlatform.getBakedModel(blockEntity.getFixture().getPanModel());
        }
        if (cachedTiltModel == null) {
            cachedTiltModel = TheatricalExpectPlatform.getBakedModel(blockEntity.getFixture().getTiltModel());
        }

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);

        if (isHanging) {
            Direction hangDirection = blockState.getValue(HangableBlock.HANG_DIRECTION);
            poseStack.translate(0.0F, 0.5F, 0.0F);
            if (hangDirection.getAxis() != Direction.Axis.Y) {
                if (hangDirection.getAxis() == Direction.Axis.Z) {
                    poseStack.mulPose(Axis.XP.rotationDegrees(hangDirection == Direction.SOUTH ? -90.0F : 90.0F));
                    poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
                } else {
                    poseStack.mulPose(Axis.ZN.rotationDegrees(hangDirection == Direction.EAST ? -90.0F : 90.0F));
                }
            }
            poseStack.translate(0.0F, -0.5F, 0.0F);
        }

        poseStack.mulPose(Axis.YP.rotationDegrees(facing.toYRot()));
        poseStack.translate(-0.5F, 0.0F, -0.5F);

        if (isHanging) {
            Optional<BlockState> support = blockEntity.getSupportingStructure();
            if (support.isPresent()) {
                float[] transforms = blockEntity.getFixture().getTransforms(blockState, support.get());
                poseStack.translate(transforms[0], transforms[1], transforms[2]);
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

        minecraftRenderModel(poseStack, vertexConsumer, blockState, cachedStaticModel, packedLight, packedOverlay);

        float[] panPivot = blockEntity.getFixture().getPanRotationPosition();
        poseStack.translate(panPivot[0], panPivot[1], panPivot[2]);
        poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(
                partialTicks,
                blockEntity.getPrevPan(),
                blockEntity.getPan()
        )));
        poseStack.translate(-panPivot[0], -panPivot[1], -panPivot[2]);
        minecraftRenderModel(poseStack, vertexConsumer, blockState, cachedPanModel, packedLight, packedOverlay);

        float[] tiltPivot = blockEntity.getFixture().getTiltRotationPosition();
        poseStack.translate(tiltPivot[0], tiltPivot[1], tiltPivot[2]);
        poseStack.mulPose(Axis.XP.rotationDegrees(isFlipped ? -180.0F : 180.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(
                partialTicks,
                blockEntity.getPrevTilt(),
                blockEntity.getTilt()
        )));
        poseStack.translate(-tiltPivot[0], -tiltPivot[1], -tiltPivot[2]);
        minecraftRenderModel(poseStack, vertexConsumer, blockState, cachedTiltModel, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public void beforeRenderBeam(
            T blockEntity,
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
        if (blockEntity.getDisplayIntensity(partialTicks) <= 0.0F) {
            return;
        }

        submitGoboProjection(
                blockEntity,
                multiBufferSource,
                facing,
                partialTicks,
                isFlipped,
                blockState,
                isHanging
        );
        submitVolumetricGoboBeam(
                blockEntity,
                facing,
                partialTicks,
                isFlipped,
                blockState,
                isHanging
        );

        LazyRenderers.addLazyRender(new LazyRenderers.LazyRenderer() {
            @Override
            public void render(
                    MultiBufferSource.BufferSource bufferSource,
                    PoseStack renderPose,
                    Camera camera,
                    float partialTick
            ) {
                renderPose.pushPose();
                Vec3 offset = Vec3.atLowerCornerOf(blockEntity.getBlockPos()).subtract(camera.getPosition());
                renderPose.translate(offset.x, offset.y, offset.z);
                preparePoseStack(blockEntity, renderPose, facing, partialTick, isFlipped, blockState, isHanging);

                float intensity = blockEntity.getDisplayIntensity(partialTick);
                int color = blockEntity.getDisplayColour(partialTick);
                float alpha = intensity / 255.0F * beamOpacity.floatValue();
                VertexConsumer beamBuilder = bufferSource.getBuffer(Beam2DRenderTypes.getBeam());

                renderFallbackBeam(
                        blockEntity,
                        beamBuilder,
                        renderPose,
                        camera,
                        partialTick,
                        alpha,
                        color
                );

                renderPose.pushPose();
                renderPose.translate(LENS_OFFSET.x, LENS_OFFSET.y, LENS_OFFSET.z);
                renderLensGlow(beamBuilder, renderPose, color, 0.18F, intensity);
                renderPose.popPose();

                renderLens(
                        bufferSource,
                        renderPose,
                        alpha,
                        color,
                        0.88F,
                        (float) LENS_OFFSET.x,
                        (float) LENS_OFFSET.y,
                        (float) LENS_OFFSET.z
                );
                renderPose.popPose();
            }

            @Override
            public Vec3 getPos(float partialTick) {
                return blockEntity.getBlockPos().getCenter();
            }
        });
    }

    protected final void submitGoboProjection(
            T blockEntity,
            MultiBufferSource multiBufferSource,
            Direction facing,
            float partialTicks,
            boolean isFlipped,
            BlockState blockState,
            boolean isHanging
    ) {
        submitGoboProjection(blockEntity, multiBufferSource, facing, partialTicks, isFlipped,
                blockState, isHanging, blockEntity.getDisplayColour(partialTicks));
    }

    protected final void submitGoboProjection(
            T blockEntity,
            MultiBufferSource multiBufferSource,
            Direction facing,
            float partialTicks,
            boolean isFlipped,
            BlockState blockState,
            boolean isHanging,
            int colorOverride
    ) {
        float[] structuralTransform = new float[]{0.0F, 0.19F, 0.0F};
        Optional<BlockState> support = blockEntity.getSupportingStructure();
        if (support.isPresent()) {
            structuralTransform = blockEntity.getFixture().getTransforms(blockState, support.get());
        }

        float[] panPivot = blockEntity.getFixture().getPanRotationPosition();
        float[] tiltPivot = blockEntity.getFixture().getTiltRotationPosition();
        float smoothPan = Mth.lerp(partialTicks, blockEntity.getPrevPan(), blockEntity.getPan());
        float smoothTilt = Mth.lerp(partialTicks, blockEntity.getPrevTilt(), blockEntity.getTilt());

        goboProjectors.computeIfAbsent(blockEntity, ignored -> new GoboGPUProjector()).render(
                blockEntity,
                multiBufferSource,
                facing,
                partialTicks,
                isFlipped,
                blockState,
                isHanging,
                LENS_OFFSET,
                panPivot,
                tiltPivot,
                structuralTransform,
                PROJECTOR_MIN_ANGLE,
                PROJECTOR_MAX_ANGLE,
                smoothPan,
                smoothTilt,
                BASE_RADIUS,
                colorOverride
        );
    }

    protected final void submitVolumetricGoboBeam(
            T blockEntity,
            Direction facing,
            float partialTicks,
            boolean isFlipped,
            BlockState blockState,
            boolean isHanging
    ) {
        submitVolumetricGoboBeam(blockEntity, facing, partialTicks, isFlipped,
                blockState, isHanging, blockEntity.getDisplayColour(partialTicks));
    }

    protected final void submitVolumetricGoboBeam(
            T blockEntity,
            Direction facing,
            float partialTicks,
            boolean isFlipped,
            BlockState blockState,
            boolean isHanging,
            int colorOverride
    ) {
        PoseStack beamPose = new PoseStack();
        preparePoseStack(blockEntity, beamPose, facing, partialTicks, isFlipped, blockState, isHanging);
        beamPose.translate(LENS_OFFSET.x, LENS_OFFSET.y, LENS_OFFSET.z);

        float halfAngle = blockEntity.getGoboFieldAngle() * 0.5F;
        float intensity = blockEntity.getDisplayIntensity(partialTicks) / 255.0F;
        int color = colorOverride;

        submitVolumetricBeam(
                blockEntity,
                beamPose,
                partialTicks,
                halfAngle,
                halfAngle,
                blockEntity.getGoboLibrary(),
                blockEntity.getGobo(),
                0.0F,
                1.0F,
                1.0F,
                0,
                color,
                intensity,
                BASE_RADIUS
        );
    }

    protected final void renderFallbackBeam(
            T blockEntity,
            VertexConsumer builder,
            PoseStack renderPose,
            Camera camera,
            float partialTick,
            float alpha,
            int color
    ) {
        if (TheatricalExtraLightsConfig.isVolumetricBeamEnabled()) {
            return;
        }

        renderPose.pushPose();
        renderPose.translate(LENS_OFFSET.x, LENS_OFFSET.y, LENS_OFFSET.z);
        if (TheatricalExtraLightsConfig.shouldRender2DBeam()) {
            renderLightBeam2D(
                    builder,
                    renderPose,
                    blockEntity,
                    camera,
                    alpha,
                    0.0F,
                    (float) blockEntity.getDistance(),
                    color,
                    0.002F
            );
        } else {
            renderLightBeam4D(
                    builder,
                    renderPose,
                    blockEntity,
                    partialTick,
                    alpha,
                    0.0F,
                    (float) blockEntity.getDistance(),
                    color,
                    0.002F
            );
        }
        renderPose.popPose();
    }

    @Override
    public void preparePoseStack(
            T blockEntity,
            PoseStack poseStack,
            Direction facing,
            float partialTicks,
            boolean isFlipped,
            BlockState blockState,
            boolean isHanging
    ) {
        FixtureMountTransform.apply(poseStack, blockEntity);
        poseStack.translate(0.5F, 0.0F, 0.5F);

        if (isHanging) {
            Direction hangDirection = blockState.getValue(HangableBlock.HANG_DIRECTION);
            poseStack.translate(0.0F, 0.5F, 0.0F);
            if (hangDirection.getAxis() != Direction.Axis.Y) {
                if (hangDirection.getAxis() == Direction.Axis.Z) {
                    poseStack.mulPose(Axis.XP.rotationDegrees(hangDirection == Direction.SOUTH ? -90.0F : 90.0F));
                    poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
                } else {
                    poseStack.mulPose(Axis.ZN.rotationDegrees(hangDirection == Direction.EAST ? -90.0F : 90.0F));
                }
            }
            poseStack.translate(0.0F, -0.5F, 0.0F);
        }

        poseStack.mulPose(Axis.YP.rotationDegrees(facing.toYRot()));
        poseStack.translate(-0.5F, 0.0F, -0.5F);

        if (isHanging) {
            Optional<BlockState> support = blockEntity.getSupportingStructure();
            if (support.isPresent()) {
                float[] transforms = blockEntity.getFixture().getTransforms(blockState, support.get());
                poseStack.translate(transforms[0], transforms[1], transforms[2]);
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

        float[] panPivot = blockEntity.getFixture().getPanRotationPosition();
        poseStack.translate(panPivot[0], panPivot[1], panPivot[2]);
        poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(
                partialTicks,
                blockEntity.getPrevPan(),
                blockEntity.getPan()
        )));
        poseStack.translate(-panPivot[0], -panPivot[1], -panPivot[2]);

        float[] tiltPivot = blockEntity.getFixture().getTiltRotationPosition();
        poseStack.translate(tiltPivot[0], tiltPivot[1], tiltPivot[2]);
        poseStack.mulPose(Axis.XP.rotationDegrees(isFlipped ? -180.0F : 180.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(
                partialTicks,
                blockEntity.getPrevTilt(),
                blockEntity.getTilt()
        )));
        poseStack.translate(-tiltPivot[0], -tiltPivot[1], -tiltPivot[2]);
    }
}
