package com.github.dumann089.theatricalextralights.client.blockentities;

import com.github.dumann089.theatricalextralights.blockentities.ParScrollerBlockEntity;
import com.github.dumann089.theatricalextralights.client.Beam2DRenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.imabad.theatrical.client.LazyRenderers;
import dev.imabad.theatrical.config.TheatricalConfig;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class ParScrollerRenderer extends ManualParGoboRenderer<ParScrollerBlockEntity> {

    private static final float LENS_X = 0.5F;
    private static final float LENS_Y = 0.56F;
    private static final float LENS_Z = 0.04F;
    private static final int COLOR_WHEEL_NEUTRAL = 0xFFFFFF;

    private final Double beamOpacity = TheatricalConfig.INSTANCE.CLIENT.beamOpacity;

    public ParScrollerRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void beforeRenderBeam(
            ParScrollerBlockEntity blockEntity,
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
        float intensity = blockEntity.getDisplayIntensity(partialTicks);
        if (intensity <= 0.0F) {
            return;
        }

        // The projected texture keeps its native RGB; the volumetric shaft follows the wheel.
        int wheelColor = blockEntity.getAnimatedColorWheelColor();
        submitGoboProjection(
                blockEntity, multiBufferSource, facing, partialTicks, isFlipped,
                blockState, isHanging, COLOR_WHEEL_NEUTRAL
        );
        submitVolumetricGoboBeam(
                blockEntity, facing, partialTicks, isFlipped, blockState, isHanging,
                wheelColor
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

                float currentIntensity = blockEntity.getDisplayIntensity(partialTick);
                float alpha = currentIntensity / 255.0F * beamOpacity.floatValue();
                VertexConsumer beamBuilder = bufferSource.getBuffer(Beam2DRenderTypes.getBeam());

                renderFallbackBeam(blockEntity, beamBuilder, renderPose, camera, partialTick, alpha, wheelColor);

                renderPose.pushPose();
                renderPose.translate(LENS_X, LENS_Y, LENS_Z);
                renderLensGlow(beamBuilder, renderPose, wheelColor, 0.15F, currentIntensity);
                renderPose.popPose();

                renderLens(bufferSource, renderPose, alpha, wheelColor, 0.88F, LENS_X, LENS_Y, -0.0125F);
                renderPose.popPose();
            }

            @Override
            public Vec3 getPos(float partialTick) {
                return blockEntity.getBlockPos().getCenter();
            }
        });
    }
}
