package com.github.dumann089.theatricalextralights.client.blockentities;

import com.github.dumann089.theatricalextralights.blockentities.Flow2JetBlockEntity;
import com.github.dumann089.theatricalextralights.util.FixtureMountTransform;
import com.github.dumann089.theatricalextralights.fixtures.Flow2JetFixture;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.imabad.theatrical.TheatricalExpectPlatform;
import dev.imabad.theatrical.blocks.HangableBlock;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

/** Machine entière pivotée (pan + tilt), pas seulement la buse. */
public class Flow2JetRenderer extends ExtraLightsRenderer<Flow2JetBlockEntity> {
    private BakedModel wholeModel;

    public Flow2JetRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    /**
     * Sur truss : tilt rig (+180°). Sans support : flip corps (debug stick / truss cassé).
     */
    @Override
    public void render(
            Flow2JetBlockEntity blockEntity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource multiBufferSource,
            int packedLight,
            int packedOverlay
    ) {
        poseStack.pushPose();
        VertexConsumer vertexConsumer = multiBufferSource.getBuffer(RenderType.cutout());
        BlockState blockState = blockEntity.getBlockState();
        boolean isFlipped = blockEntity.isUpsideDown();
        boolean isRigged = blockState.getValue(HangableBlock.HANGING);
        Direction facing = blockState.getValue(HangableBlock.FACING);
        renderModel(blockEntity, poseStack, vertexConsumer, facing, partialTick, isFlipped, blockState, isRigged, packedLight, packedOverlay);
        beforeRenderBeam(blockEntity, poseStack, vertexConsumer, multiBufferSource, facing, partialTick, isFlipped, blockState, isRigged, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public void renderModel(
            Flow2JetBlockEntity blockEntity,
            PoseStack poseStack,
            VertexConsumer vertexConsumer,
            Direction facing,
            float partialTicks,
            boolean isFlipped,
            BlockState blockState,
            boolean isRigged,
            int packedLight,
            int packedOverlay
    ) {
        if (wholeModel == null) {
            wholeModel = TheatricalExpectPlatform.getBakedModel(blockEntity.getFixture().getPanModel());
        }

        boolean isMounted = ((HangableBlock) blockState.getBlock()).isHanging(blockEntity.getLevel(), blockEntity.getBlockPos());
        boolean bodyFlip = Flow2JetFixture.shouldApplyBodyFlip(isFlipped, isMounted);
        applyFixturePose(poseStack, blockEntity, facing, blockState, bodyFlip, isRigged, isMounted, partialTicks);
        minecraftRenderModel(poseStack, vertexConsumer, blockState, wholeModel, packedLight, packedOverlay);
    }

    private static void applyFixturePose(
            PoseStack poseStack,
            Flow2JetBlockEntity blockEntity,
            Direction facing,
            BlockState blockState,
            boolean isFlipped,
            boolean isRigged,
            boolean isMounted,
            float partialTicks
    ) {
        poseStack.translate(0.5F, 0, 0.5F);
        if (isMounted) {
            Direction hangDirection = blockState.getValue(HangableBlock.HANG_DIRECTION);
            poseStack.translate(0, 0.5, 0F);
            if (hangDirection.getAxis() != Direction.Axis.Y) {
                if (hangDirection.getAxis() == Direction.Axis.Z) {
                    if (hangDirection == Direction.SOUTH) {
                        poseStack.mulPose(Axis.XP.rotationDegrees(90));
                    } else {
                        poseStack.mulPose(Axis.XN.rotationDegrees(90));
                    }
                } else {
                    if (hangDirection == Direction.EAST) {
                        poseStack.mulPose(Axis.ZN.rotationDegrees(90));
                    } else {
                        poseStack.mulPose(Axis.ZN.rotationDegrees(-90));
                    }
                }
            }
            poseStack.translate(0, -0.5, 0F);
        }

        if (facing.getAxis() == Direction.Axis.X) {
            poseStack.mulPose(Axis.YP.rotationDegrees(facing.toYRot()));
        } else {
            poseStack.mulPose(Axis.YP.rotationDegrees(facing.getOpposite().toYRot()));
        }
        poseStack.translate(-0.5F, 0, -0.5F);

        if (isMounted) {
            Optional<BlockState> optionalSupport = blockEntity.getSupportingStructure();
            if (optionalSupport.isPresent()) {
                float[] transforms = blockEntity.getFixture().getTransforms(blockState, optionalSupport.get());
                poseStack.translate(transforms[0], transforms[1], transforms[2]);
            } else if (isRigged) {
                poseStack.translate(0, 0.19, 0);
            }
        }

        if (isFlipped) {
            poseStack.translate(0.5F, 0.5F, 0.5F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(180));
            poseStack.translate(-0.5F, -0.5F, -0.5F);
        }

        float[] headPivot = blockEntity.getFixture().getPanRotationPosition();
        float pan = blockEntity.getInterpolatedPan(partialTicks);
        float userTilt = blockEntity.getInterpolatedTilt(partialTicks);
        float effectiveTilt = Flow2JetFixture.effectiveTilt(userTilt, isRigged, isFlipped);

        poseStack.translate(headPivot[0], headPivot[1], headPivot[2]);
        poseStack.mulPose(Axis.YN.rotationDegrees(pan));
        poseStack.mulPose(Axis.XP.rotationDegrees(effectiveTilt));
        poseStack.translate(-headPivot[0], -headPivot[1], -headPivot[2]);
    }

    @Override
    public void preparePoseStack(
            Flow2JetBlockEntity blockEntity,
            PoseStack poseStack,
            Direction facing,
            float partialTicks,
            boolean isFlipped,
            BlockState blockState,
            boolean isRigged
    ) {
        FixtureMountTransform.apply(poseStack, blockEntity);
        boolean isMounted = ((HangableBlock) blockState.getBlock()).isHanging(blockEntity.getLevel(), blockEntity.getBlockPos());
        boolean bodyFlip = Flow2JetFixture.shouldApplyBodyFlip(isFlipped, isMounted);
        applyFixturePose(poseStack, blockEntity, facing, blockState, bodyFlip, isRigged, isMounted, partialTicks);
    }
}
