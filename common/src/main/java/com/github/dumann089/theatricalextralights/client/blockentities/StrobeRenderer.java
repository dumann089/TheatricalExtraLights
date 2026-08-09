package com.github.dumann089.theatricalextralights.client.blockentities;



import com.github.dumann089.theatricalextralights.blockentities.StrobeBlockEntity;
import com.github.dumann089.theatricalextralights.util.FixtureMountTransform;

import com.github.dumann089.theatricalextralights.client.StrobeRenderHelper;
import com.github.dumann089.theatricalextralights.client.StrobeVisualEffects;

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

import net.minecraft.core.BlockPos;

import net.minecraft.core.Direction;

import net.minecraft.util.Mth;

import net.minecraft.world.level.block.state.BlockState;

import net.minecraft.world.phys.Vec3;

import java.util.Optional;



public class StrobeRenderer extends ExtraLightsFixtureRenderer<StrobeBlockEntity> {

    private BakedModel cachedPanModel;

    private BakedModel cachedTiltModel;

    private BakedModel cachedStaticModel;



    public StrobeRenderer(BlockEntityRendererProvider.Context context) {

        super(context);

    }



    @Override

    public void renderModel(

            StrobeBlockEntity blockEntity,

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



        poseStack.translate(0.5F, 0, .5F);

        if (isHanging) {

            Direction hangDirection = blockState.getValue(HangableBlock.HANG_DIRECTION);

            poseStack.translate(0, 0.5, 0F);

            if (hangDirection.getAxis() != Direction.Axis.Y) {

                if (hangDirection.getAxis() == Direction.Axis.Z) {

                    if (hangDirection == Direction.SOUTH) {

                        poseStack.mulPose(Axis.XP.rotationDegrees(-90));

                    } else {

                        poseStack.mulPose(Axis.XP.rotationDegrees(90));

                    }

                    poseStack.mulPose(Axis.YP.rotationDegrees(180));

                } else {

                    if (hangDirection == Direction.EAST) {

                        poseStack.mulPose(Axis.ZN.rotationDegrees(-90));

                    } else {

                        poseStack.mulPose(Axis.ZN.rotationDegrees(90));

                    }

                }

            }

            poseStack.translate(0, -0.5, 0F);

        }



        poseStack.mulPose(Axis.YP.rotationDegrees(facing.toYRot()));

        poseStack.translate(-0.5F, 0, -.5F);

        if (isHanging) {

            Optional<BlockState> optionalSupport = blockEntity.getSupportingStructure();

            if (optionalSupport.isPresent()) {

                float[] transforms = blockEntity.getFixture().getTransforms(blockState, optionalSupport.get());

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



        minecraftRenderModel(poseStack, vertexConsumer, blockState, cachedStaticModel, packedLight, packedOverlay);



        float[] pans = blockEntity.getFixture().getPanRotationPosition();

        poseStack.translate(pans[0], pans[1], pans[2]);

        int prevPan = blockEntity.getPrevPan();

        int pan = blockEntity.getPan();

        poseStack.mulPose(Axis.YP.rotationDegrees((prevPan + (pan - prevPan) * partialTicks)));

        poseStack.translate(-pans[0], -pans[1], -pans[2]);

        minecraftRenderModel(poseStack, vertexConsumer, blockState, cachedPanModel, packedLight, packedOverlay);



        float[] tilts = blockEntity.getFixture().getTiltRotationPosition();

        poseStack.translate(tilts[0], tilts[1], tilts[2]);

        if (isFlipped) {

            poseStack.mulPose(Axis.XP.rotationDegrees(-180));

        } else {

            poseStack.mulPose(Axis.XP.rotationDegrees(180));

        }

        int prevTilt = blockEntity.getPrevTilt();

        int tilt = blockEntity.getTilt();

        poseStack.mulPose(Axis.XP.rotationDegrees((prevTilt + (tilt - prevTilt) * partialTicks)));

        poseStack.translate(-tilts[0], -tilts[1], -tilts[2]);

        minecraftRenderModel(poseStack, vertexConsumer, blockState, cachedTiltModel, packedLight, packedOverlay);

    }



    @Override

    public void beforeRenderBeam(

            StrobeBlockEntity blockEntity,

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

        if (!StrobeRenderHelper.isVisuallyLit(blockEntity)) {

            return;

        }



        LazyRenderers.addLazyRender(new LazyRenderers.LazyRenderer() {

            @Override

            public void render(MultiBufferSource.BufferSource bufferSource, PoseStack poseStack, Camera camera, float partialTick) {

                float intensity = StrobeRenderHelper.renderedIntensity(blockEntity, partialTick);

                if (intensity <= 0f) {

                    return;

                }



                float focusInterpolated = blockEntity.getPrevFocus()

                        + (blockEntity.getFocus() - blockEntity.getPrevFocus()) * partialTick;

                float focusNorm = (Math.max(1f, focusInterpolated) - 1f) / 254f;

                float visualScale = Mth.lerp(focusNorm, 0.30f, 1.0f);



                int color = blockEntity.getColour();

                int r = (color >> 16) & 0xFF;

                int g = (color >> 8) & 0xFF;

                int b = color & 0xFF;

                int a = (int) ((intensity / 255f) * visualScale * 255f);

                poseStack.pushPose();

                Vec3 blockOffset = Vec3.atLowerCornerOf(blockEntity.getBlockPos()).subtract(camera.getPosition());

                poseStack.translate(blockOffset.x, blockOffset.y, blockOffset.z);

                preparePoseStack(blockEntity, poseStack, facing, partialTick, isFlipped, blockstate, isHanging);



                StrobeVisualEffects.renderFace(bufferSource, poseStack, r, g, b, a);

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

            StrobeBlockEntity blockEntity,

            PoseStack poseStack,

            Direction facing,

            float partialTicks,

            boolean isFlipped,

            BlockState blockState,

            boolean isHanging

    ) {

        FixtureMountTransform.apply(poseStack, blockEntity);
        poseStack.translate(0.5F, 0, .5F);

        if (isHanging) {

            Direction hangDirection = blockState.getValue(HangableBlock.HANG_DIRECTION);

            poseStack.translate(0, 0.5, 0F);

            if (hangDirection.getAxis() != Direction.Axis.Y) {

                if (hangDirection.getAxis() == Direction.Axis.Z) {

                    if (hangDirection == Direction.SOUTH) {

                        poseStack.mulPose(Axis.XP.rotationDegrees(-90));

                    } else {

                        poseStack.mulPose(Axis.XP.rotationDegrees(90));

                    }

                    poseStack.mulPose(Axis.YP.rotationDegrees(180));

                } else {

                    if (hangDirection == Direction.EAST) {

                        poseStack.mulPose(Axis.ZN.rotationDegrees(-90));

                    } else {

                        poseStack.mulPose(Axis.ZN.rotationDegrees(90));

                    }

                }

            }

            poseStack.translate(0, -0.5, 0F);

        }



        poseStack.mulPose(Axis.YP.rotationDegrees(facing.toYRot()));

        poseStack.translate(-0.5F, 0, -.5F);

        if (isHanging) {

            Optional<BlockState> optionalSupport = blockEntity.getSupportingStructure();

            if (optionalSupport.isPresent()) {

                float[] transforms = blockEntity.getFixture().getTransforms(blockState, optionalSupport.get());

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



        float[] pans = blockEntity.getFixture().getPanRotationPosition();

        poseStack.translate(pans[0], pans[1], pans[2]);

        int prevPan = blockEntity.getPrevPan();

        int pan = blockEntity.getPan();

        poseStack.mulPose(Axis.YP.rotationDegrees((prevPan + (pan - prevPan) * partialTicks)));

        poseStack.translate(-pans[0], -pans[1], -pans[2]);



        float[] tilts = blockEntity.getFixture().getTiltRotationPosition();

        poseStack.translate(tilts[0], tilts[1], tilts[2]);

        if (isFlipped) {

            poseStack.mulPose(Axis.XP.rotationDegrees(-180));

        } else {

            poseStack.mulPose(Axis.XP.rotationDegrees(180));

        }

        int prevTilt = blockEntity.getPrevTilt();

        int tilt = blockEntity.getTilt();

        poseStack.mulPose(Axis.XP.rotationDegrees((prevTilt + (tilt - prevTilt) * partialTicks)));

        poseStack.translate(-tilts[0], -tilts[1], -tilts[2]);

    }

}


