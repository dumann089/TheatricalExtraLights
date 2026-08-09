package com.github.dumann089.theatricalextralights.client.blockentities;

import com.github.dumann089.theatricalextralights.blockentities.FireworkLauncherBlockEntity;
import com.github.dumann089.theatricalextralights.util.FixtureMountTransform;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.imabad.theatrical.TheatricalExpectPlatform;
import dev.imabad.theatrical.blocks.HangableBlock;
import com.github.dumann089.theatricalextralights.client.blockentities.ExtraLightsRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

public class FireworkLauncherRenderer extends ExtraLightsRenderer<FireworkLauncherBlockEntity> {
    private BakedModel cachedTiltModel;

    public FireworkLauncherRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void renderModel(FireworkLauncherBlockEntity blockEntity, PoseStack poseStack, VertexConsumer vertexConsumer, Direction facing, float partialTicks, boolean isFlipped, BlockState blockState, boolean isHanging, int packedLight, int packedOverlay) {
        if (cachedTiltModel == null) {
            cachedTiltModel = TheatricalExpectPlatform.getBakedModel(blockEntity.getFixture().getTiltModel());
        }

        poseStack.translate(0.5f, 0, 0.5f);
        if (isHanging) {
            Direction hangDirection = blockState.getValue(HangableBlock.HANG_DIRECTION);
            poseStack.translate(0, 0.5, 0);
            if (hangDirection.getAxis() != Direction.Axis.Y) {
                if (hangDirection.getAxis() == Direction.Axis.Z) {
                    poseStack.mulPose(Axis.XP.rotationDegrees(hangDirection == Direction.SOUTH ? -90 : 90));
                    poseStack.mulPose(Axis.YP.rotationDegrees(180));
                } else {
                    poseStack.mulPose(Axis.ZN.rotationDegrees(hangDirection == Direction.EAST ? -90 : 90));
                }
            }
            poseStack.translate(0, -0.5, 0);
        }

        poseStack.mulPose(Axis.YP.rotationDegrees(facing.toYRot()));
        poseStack.translate(-0.5f, 0, -0.5f);
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

        float[] pivot = blockEntity.getFixture().getTiltRotationPosition();
        float tiltDegrees = blockEntity.getPitchDegrees() - 90.0f;
        poseStack.translate(pivot[0], pivot[1], pivot[2]);
        poseStack.mulPose(Axis.ZP.rotationDegrees(tiltDegrees));
        poseStack.translate(-pivot[0], -pivot[1], -pivot[2]);
        minecraftRenderModel(poseStack, vertexConsumer, blockState, cachedTiltModel, packedLight, packedOverlay);
    }

    @Override
    public void beforeRenderBeam(FireworkLauncherBlockEntity blockEntity, PoseStack poseStack, VertexConsumer vertexConsumer, MultiBufferSource multiBufferSource, Direction facing, float partialTicks, boolean isFlipped, BlockState blockstate, boolean isHanging, int packedLight, int packedOverlay) {
    }

    @Override
    public void preparePoseStack(FireworkLauncherBlockEntity blockEntity, PoseStack poseStack, Direction facing, float partialTicks, boolean isFlipped, BlockState blockState, boolean isHanging) {
        FixtureMountTransform.apply(poseStack, blockEntity);
    }
}
