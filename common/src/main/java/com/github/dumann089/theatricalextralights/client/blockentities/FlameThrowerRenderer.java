package com.github.dumann089.theatricalextralights.client.blockentities;

import com.github.dumann089.theatricalextralights.blockentities.FlameThrowerBlockEntity;
import com.github.dumann089.theatricalextralights.util.FixtureMountTransform;
import com.github.dumann089.theatricalextralights.blocks.FlameThrowerBlock;
import com.github.dumann089.theatricalextralights.util.DirectionOffset;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.imabad.theatrical.TheatricalExpectPlatform;
import dev.imabad.theatrical.blocks.HangableBlock;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public class FlameThrowerRenderer extends ExtraLightsRenderer<FlameThrowerBlockEntity> {
    private BakedModel bodyModel;
    private BakedModel headModel;

    public FlameThrowerRenderer(net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void renderModel(
            FlameThrowerBlockEntity blockEntity,
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
        if (bodyModel == null) {
            bodyModel = TheatricalExpectPlatform.getBakedModel(blockEntity.getFixture().getStaticModel());
        }
        if (headModel == null) {
            headModel = TheatricalExpectPlatform.getBakedModel(blockEntity.getFixture().getPanModel());
        }

        applyFixturePose(blockEntity, poseStack, facing, isFlipped, blockState, isHanging);
        minecraftRenderModel(poseStack, vertexConsumer, blockState, bodyModel, packedLight, packedOverlay);

        float headAngle = DirectionOffset.panToHeadRenderAngle(blockEntity.getPan());
        float prevHeadAngle = DirectionOffset.panToHeadRenderAngle(blockEntity.getPrevPan());
        headAngle = prevHeadAngle + (headAngle - prevHeadAngle) * partialTicks;

        Vec3 pivot = DirectionOffset.FLAME_HEAD_PIVOT_BLOCK;

        poseStack.pushPose();
        poseStack.translate(pivot.x, pivot.y, pivot.z);
        DirectionOffset.applyPanRotation(poseStack, facing, headAngle);
        poseStack.translate(
                DirectionOffset.FLAME_HEAD_MESH_LIFT.x,
                DirectionOffset.FLAME_HEAD_MESH_LIFT.y,
                DirectionOffset.FLAME_HEAD_MESH_LIFT.z
        );
        poseStack.translate(-pivot.x, -pivot.y, -pivot.z);
        minecraftRenderModel(
                poseStack,
                vertexConsumer,
                blockState.setValue(FlameThrowerBlock.MODEL, 1),
                headModel,
                packedLight,
                packedOverlay
        );
        poseStack.popPose();
    }

    private static void applyFixturePose(
            FlameThrowerBlockEntity blockEntity,
            PoseStack poseStack,
            Direction facing,
            boolean isFlipped,
            BlockState blockState,
            boolean isHanging
    ) {
        poseStack.translate(0.5F, 0, 0.5F);
        if (isHanging) {
            Direction hangDirection = blockState.getValue(HangableBlock.HANG_DIRECTION);
            poseStack.translate(0, 0.5F, 0F);
            if (hangDirection.getAxis() != Direction.Axis.Y) {
                if (hangDirection.getAxis() == Direction.Axis.Z) {
                    poseStack.mulPose(Axis.XP.rotationDegrees(hangDirection == Direction.SOUTH ? -90 : 90));
                    poseStack.mulPose(Axis.YP.rotationDegrees(180));
                } else {
                    poseStack.mulPose(Axis.ZN.rotationDegrees(hangDirection == Direction.EAST ? -90 : 90));
                }
            }
            poseStack.translate(0, -0.5F, 0F);
        }

        if (facing.getAxis() == Direction.Axis.X) {
            poseStack.mulPose(Axis.YP.rotationDegrees(facing.toYRot()));
        } else {
            poseStack.mulPose(Axis.YP.rotationDegrees(facing.getOpposite().toYRot()));
        }
        poseStack.translate(-0.5F, 0, -0.5F);

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
            poseStack.translate(0.5F, 0.5F, 0.5F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(180));
            poseStack.translate(-0.5F, -0.5F, -0.5F);
        }
    }

    @Override
    public void preparePoseStack(
            FlameThrowerBlockEntity blockEntity,
            PoseStack poseStack,
            Direction facing,
            float partialTicks,
            boolean isFlipped,
            BlockState blockState,
            boolean isHanging
    ) {
        FixtureMountTransform.apply(poseStack, blockEntity);
    }
}
