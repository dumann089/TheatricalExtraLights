package com.github.dumann089.theatricalextralights.client.blockentities;

import com.github.dumann089.theatricalextralights.blockentities.Blinder1x2BlockEntity;
import com.github.dumann089.theatricalextralights.util.FixtureMountTransform;
import com.github.dumann089.theatricalextralights.client.BlinderRenderHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.imabad.theatrical.TheatricalExpectPlatform;
import dev.imabad.theatrical.blocks.HangableBlock;
import dev.imabad.theatrical.client.LazyRenderers;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/** Blinder 1x2 : le placement du 1x1, deux faces allumees (une par lampe). */
public class Blinder1x2Renderer extends ExtraLightsRenderer<Blinder1x2BlockEntity> {
    private BakedModel cachedPanModel, cachedTiltModel, cachedStaticModel;

    public Blinder1x2Renderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void renderModel(Blinder1x2BlockEntity blockEntity, PoseStack poseStack, VertexConsumer vertexConsumer, Direction facing, float partialTicks, boolean isFlipped, BlockState blockState, boolean isHanging, int packedLight, int packedOverlay) {
        if (cachedStaticModel == null) {
            cachedStaticModel = TheatricalExpectPlatform.getBakedModel(blockEntity.getFixture().getStaticModel());
        }
        if (cachedPanModel == null) {
            cachedPanModel = TheatricalExpectPlatform.getBakedModel(blockEntity.getFixture().getPanModel());
        }
        if (cachedTiltModel == null) {
            cachedTiltModel = TheatricalExpectPlatform.getBakedModel(blockEntity.getFixture().getTiltModel());
        }
        placeHead(blockEntity, poseStack, facing, blockState, isHanging);
        minecraftRenderModel(poseStack, vertexConsumer, blockState, cachedStaticModel, packedLight, packedOverlay);
        float[] pans = blockEntity.getFixture().getPanRotationPosition();
        poseStack.translate(pans[0], pans[1], pans[2]);
        int prevPan = blockEntity.getPrevPan();
        int pan = blockEntity.getPan();
        poseStack.mulPose(Axis.YN.rotationDegrees((prevPan + (pan - prevPan) * partialTicks)));
        poseStack.translate(-pans[0], -pans[1], -pans[2]);
        minecraftRenderModel(poseStack, vertexConsumer, blockState, cachedPanModel, packedLight, packedOverlay);
        float[] tilts = blockEntity.getFixture().getTiltRotationPosition();
        poseStack.translate(tilts[0], tilts[1], tilts[2]);
        int prevTilt = blockEntity.getPrevTilt();
        int tilt = blockEntity.getTilt();
        poseStack.mulPose(Axis.XP.rotationDegrees((prevTilt + (tilt - prevTilt) * partialTicks)));
        poseStack.translate(-tilts[0], -tilts[1], -tilts[2]);
        minecraftRenderModel(poseStack, vertexConsumer, blockState, cachedTiltModel, packedLight, packedOverlay);
    }

    @Override
    public void beforeRenderBeam(
            Blinder1x2BlockEntity blockEntity,
            PoseStack poseStack,
            VertexConsumer vertexConsumer,
            MultiBufferSource multiBufferSource,
            Direction facing,
            float partialTicks,
            boolean isFlipped,
            BlockState blockstate,
            boolean isHanging,
            int packedLight,
            int packedOverlay
    ) {
        if (blockEntity.getIntensity() > 0) {
            LazyRenderers.addLazyRender(new LazyRenderers.LazyRenderer() {
                @Override
                public void render(MultiBufferSource.BufferSource bufferSource, PoseStack poseStack, Camera camera, float partialTick) {
                    Vec3 offset = Vec3.atLowerCornerOf(blockEntity.getBlockPos()).subtract(camera.getPosition());
                    BlinderRenderHelper.renderLamps(blockEntity, bufferSource, poseStack, camera, partialTick,
                            pose -> {
                                pose.translate(offset.x, offset.y, offset.z);
                                preparePoseStack(blockEntity, pose, facing, partialTick, isFlipped, blockstate, isHanging);
                            }, BlinderRenderHelper.Lamps.DUAL_1X2);
                }

                @Override
                public Vec3 getPos(float partialTick) {
                    return BlinderRenderHelper.lazyRenderPos(blockEntity);
                }
            });
        }
    }

    @Override
    public void preparePoseStack(Blinder1x2BlockEntity blockEntity, PoseStack poseStack, Direction facing, float partialTicks, boolean isFlipped, BlockState blockState, boolean isHanging) {
        FixtureMountTransform.apply(poseStack, blockEntity);
        placeHead(blockEntity, poseStack, facing, blockState, isHanging);
        float[] pans = blockEntity.getFixture().getPanRotationPosition();
        poseStack.translate(pans[0], pans[1], pans[2]);
        int prevPan = blockEntity.getPrevPan();
        int pan = blockEntity.getPan();
        poseStack.mulPose(Axis.YN.rotationDegrees((prevPan + (pan - prevPan) * partialTicks)));
        poseStack.translate(-pans[0], -pans[1], -pans[2]);
        float[] tilts = blockEntity.getFixture().getTiltRotationPosition();
        poseStack.translate(tilts[0], tilts[1], tilts[2]);
        int prevTilt = blockEntity.getPrevTilt();
        int tilt = blockEntity.getTilt();
        poseStack.mulPose(Axis.XP.rotationDegrees((prevTilt + (tilt - prevTilt) * partialTicks)));
        poseStack.translate(-tilts[0], -tilts[1], -tilts[2]);
    }

    /** Accroche, orientation et decalage de support : identiques au Blinder 1x1. */
    private static void placeHead(Blinder1x2BlockEntity blockEntity, PoseStack poseStack, Direction facing, BlockState blockState, boolean isHanging) {
        poseStack.translate(0.5F, 0, .5F);
        if (isHanging) {
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
            poseStack.translate(0, -0.66, 0F);
        }
        if (facing.getAxis() == Direction.Axis.X) {
            poseStack.mulPose(Axis.YP.rotationDegrees(facing.toYRot()));
        } else {
            poseStack.mulPose(Axis.YP.rotationDegrees(facing.getOpposite().toYRot()));
        }
        poseStack.translate(-0.5F, 0, -.5F);
        if (isHanging) {
            Optional<BlockState> optionalSupport = blockEntity.getSupportingStructure();
            if (optionalSupport.isPresent()) {
                float[] transforms = blockEntity.getFixture().getTransforms(blockState, optionalSupport.get());
                poseStack.translate(transforms[0], transforms[1], transforms[2]);
            } else {
                poseStack.translate(0, 0.19, 0);
            }
        }
    }
}
