package com.github.dumann089.theatricalextralights.client;

import com.github.dumann089.theatricalextralights.blockentities.ConfettiCannonBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.imabad.theatrical.blocks.HangableBlock;
import dev.imabad.theatrical.blocks.light.BaseLightBlock;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Client-only PoseStack transforms for confetti cannon rendering.
 * Server aim uses {@link com.github.dumann089.theatricalextralights.pyro.ConfettiCannonOrientation}.
 */
public final class ConfettiCannonRenderTransforms {
    private ConfettiCannonRenderTransforms() {
    }

    public static void applyRenderingTransforms(PoseStack poseStack, BlockState state, ConfettiCannonBlockEntity blockEntity) {
        Direction facing = state.getValue(HangableBlock.FACING);
        boolean isFlipped = blockEntity.isUpsideDown();
        boolean isHanging = state.getValue(BaseLightBlock.HANGING);

        poseStack.translate(0.5F, 0.0F, 0.5F);
        if (isHanging) {
            Direction hangDirection = state.getValue(HangableBlock.HANG_DIRECTION);
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
            var support = blockEntity.getSupportingStructure();
            if (support.isPresent()) {
                float[] transforms = blockEntity.getFixture().getTransforms(state, support.get());
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

        applyBlockbenchEntityTransform(poseStack);
    }

    public static void applyBlockbenchEntityTransform(PoseStack poseStack) {
        poseStack.translate(0.5D, 1.5D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.scale(-1.0F, -1.0F, 1.0F);
    }
}
