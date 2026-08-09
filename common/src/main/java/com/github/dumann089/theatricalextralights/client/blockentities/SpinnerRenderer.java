package com.github.dumann089.theatricalextralights.client.blockentities;

import com.github.dumann089.theatricalextralights.blockentities.SpinnerBlockEntity;
import com.github.dumann089.theatricalextralights.util.FixtureMountTransform;
import com.github.dumann089.theatricalextralights.blockentities.SpinnerBlockEntity;
import com.github.dumann089.theatricalextralights.client.particle.JetVariant;
import com.github.dumann089.theatricalextralights.client.particle.WaterJetParticleOptions;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.imabad.theatrical.TheatricalExpectPlatform;
import dev.imabad.theatrical.blocks.HangableBlock;
import dev.imabad.theatrical.client.LazyRenderers;
import com.github.dumann089.theatricalextralights.client.blockentities.ExtraLightsRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

public class SpinnerRenderer extends ExtraLightsRenderer<SpinnerBlockEntity> {
    private BakedModel cachedPanModel, cachedTiltModel, cachedStaticModel;

    public SpinnerRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    private static class SmoothingState {
        float smoothPan = 0f;
        float smoothTilt = 0f;
        long lastUpdateTime = -1;
    }
    private final Map<SpinnerBlockEntity, SpinnerRenderer.SmoothingState> smoothingStates = new WeakHashMap<>();
    private static final float SMOOTH_SPEED = 5f;
    
    @Override
    public void renderModel(SpinnerBlockEntity blockEntity, PoseStack poseStack, VertexConsumer vertexConsumer,
                            Direction facing, float partialTicks, boolean isFlipped, BlockState blockState,
                            boolean isHanging, int packedLight, int packedOverlay) {
        if(cachedStaticModel == null){
            cachedStaticModel = TheatricalExpectPlatform.getBakedModel(blockEntity.getFixture().getStaticModel());
        }
        if (cachedPanModel == null){
            cachedPanModel = TheatricalExpectPlatform.getBakedModel(blockEntity.getFixture().getPanModel());
        }
        if (cachedTiltModel == null){
            cachedTiltModel = TheatricalExpectPlatform.getBakedModel(blockEntity.getFixture().getTiltModel());
        }

        poseStack.translate(0.5F, 0, .5F);
        if(isHanging){
            Direction hangDirection = blockState.getValue(HangableBlock.HANG_DIRECTION);
            poseStack.translate(0, 0.5, 0F);
            if(hangDirection.getAxis() != Direction.Axis.Y){
                if(hangDirection.getAxis() == Direction.Axis.Z){
                    if(hangDirection == Direction.SOUTH) {
                        poseStack.mulPose(Axis.XP.rotationDegrees(90));
                    } else {
                        poseStack.mulPose(Axis.XN.rotationDegrees(90));
                    }
                } else {
                    if(hangDirection == Direction.EAST) {
                        poseStack.mulPose(Axis.ZN.rotationDegrees(90));
                    } else {
                        poseStack.mulPose(Axis.ZN.rotationDegrees(-90));
                    }
                }
            } else {

            }
            poseStack.translate(0, -0.5, 0F);
        }

        if(facing.getAxis() == Direction.Axis.X){
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
        poseStack.pushPose();

        minecraftRenderModel(poseStack, vertexConsumer, blockState, cachedStaticModel, packedLight, packedOverlay);
        SpinnerRenderer.SmoothingState state = smoothingStates.computeIfAbsent(blockEntity, k -> new SpinnerRenderer.SmoothingState());

        long now = System.nanoTime();
        if (state.lastUpdateTime < 0) state.lastUpdateTime = now;
        float deltaTime = (now - state.lastUpdateTime) / 1_000_000_000f;
        state.lastUpdateTime = now;
        deltaTime = Math.min(deltaTime, 0.1f);

        float targetPan = blockEntity.getPrevPan() + (blockEntity.getPan() - blockEntity.getPrevPan()) * partialTicks;
        float targetTilt = blockEntity.getPrevTilt() + (blockEntity.getTilt() - blockEntity.getPrevTilt()) * partialTicks;

        float alpha = 1f - (float) Math.exp(-SMOOTH_SPEED * deltaTime);
        state.smoothPan = state.smoothPan + (targetPan - state.smoothPan) * alpha;
        state.smoothTilt = state.smoothTilt + (targetTilt - state.smoothTilt) * alpha;

        float prevSpin = blockEntity.getPrevSpinAngle();
        float currentSpin = blockEntity.getSpinAngle();
        float interpolatedSpin = prevSpin + (currentSpin - prevSpin) * partialTicks;

        float[] pans = blockEntity.getFixture().getPanRotationPosition();
        poseStack.translate(pans[0], pans[1], pans[2]);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.smoothPan));
        poseStack.mulPose(Axis.YN.rotationDegrees(interpolatedSpin));
        poseStack.translate(-pans[0], -pans[1], -pans[2]);
        minecraftRenderModel(poseStack, vertexConsumer, blockState, cachedPanModel, packedLight, packedOverlay);

        float[] tilts = blockEntity.getFixture().getTiltRotationPosition();
        poseStack.translate(tilts[0], tilts[1], tilts[2]);
        if (isFlipped) {
            poseStack.mulPose(Axis.XP.rotationDegrees(-180));
        } else {
            poseStack.mulPose(Axis.XP.rotationDegrees(180));
        }
        poseStack.mulPose(Axis.XP.rotationDegrees(state.smoothTilt));
        poseStack.translate(-tilts[0], -tilts[1], -tilts[2]);
        minecraftRenderModel(poseStack, vertexConsumer, blockState, cachedTiltModel, packedLight, packedOverlay);
        poseStack.popPose();
    }

        @Override
    public void preparePoseStack(SpinnerBlockEntity blockEntity, PoseStack poseStack, Direction facing,
                                 float partialTicks, boolean isFlipped, BlockState blockState, boolean isHanging) {
        FixtureMountTransform.apply(poseStack, blockEntity);
        poseStack.translate(0.5F, 0, .5F);
        if(isHanging){
            Direction hangDirection = blockState.getValue(HangableBlock.HANG_DIRECTION);
            poseStack.translate(0, 0.5, 0F);
            if(hangDirection.getAxis() != Direction.Axis.Y){
                if(hangDirection.getAxis() == Direction.Axis.Z){
                    if(hangDirection == Direction.SOUTH) {
                        poseStack.mulPose(Axis.XP.rotationDegrees(90));
                    } else {
                        poseStack.mulPose(Axis.XN.rotationDegrees(90));
                    }
                } else {
                    if(hangDirection == Direction.EAST) {
                        poseStack.mulPose(Axis.ZN.rotationDegrees(90));
                    } else {
                        poseStack.mulPose(Axis.ZN.rotationDegrees(-90));
                    }
                }
            } else {

            }
            poseStack.translate(0, -0.5, 0F);
        }

        if(facing.getAxis() == Direction.Axis.X){
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
            // Dentro de preparePoseStack()

            SpinnerRenderer.SmoothingState state = smoothingStates.computeIfAbsent(blockEntity, k -> new SpinnerRenderer.SmoothingState());

            float prevSpin = blockEntity.getPrevSpinAngle();
            float currentSpin = blockEntity.getSpinAngle();
            float interpolatedSpin = prevSpin + (currentSpin - prevSpin) * partialTicks;

            float[] pans = blockEntity.getFixture().getPanRotationPosition();
            poseStack.translate(pans[0], pans[1], pans[2]);
            poseStack.mulPose(Axis.YP.rotationDegrees(state.smoothPan));
            poseStack.mulPose(Axis.YN.rotationDegrees(interpolatedSpin));
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
            poseStack.mulPose(Axis.XP.rotationDegrees(state.smoothTilt));
            poseStack.translate(-tilts[0], -tilts[1], -tilts[2]);
    }
}