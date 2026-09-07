package com.github.dumann089.theatricalextralights.client.blockentities;

import com.github.dumann089.theatricalextralights.blockentities.RGBBarBlockEntity;
import com.github.dumann089.theatricalextralights.util.FixtureMountTransform;
import com.github.dumann089.theatricalextralights.client.Beam2DRenderTypes;
import com.github.dumann089.theatricalextralights.config.TheatricalExtraLightsConfig;
import dev.imabad.theatrical.config.TheatricalConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.imabad.theatrical.TheatricalExpectPlatform;
import dev.imabad.theatrical.blocks.HangableBlock;
import dev.imabad.theatrical.client.LazyRenderers;
import dev.imabad.theatrical.client.TheatricalRenderTypes;
import com.github.dumann089.theatricalextralights.client.blockentities.ExtraLightsRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.renderer.LightTexture;

import java.util.Optional;

import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class RGBbarRenderer extends ExtraLightsRenderer<RGBBarBlockEntity> {
    private BakedModel cachedPanModel, cachedTiltModel, cachedStaticModel;

    public RGBbarRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void renderModel(RGBBarBlockEntity blockEntity, PoseStack poseStack, VertexConsumer vertexConsumer, Direction facing, float partialTicks, boolean isFlipped, BlockState blockState, boolean isHanging, int packedLight, int packedOverlay) {
        if(cachedStaticModel == null){
            cachedStaticModel = TheatricalExpectPlatform.getBakedModel(blockEntity.getFixture().getStaticModel());
        }
        if (cachedPanModel == null){
            cachedPanModel = TheatricalExpectPlatform.getBakedModel(blockEntity.getFixture().getPanModel());
        }
        if (cachedTiltModel == null){
            cachedTiltModel = TheatricalExpectPlatform.getBakedModel(blockEntity.getFixture().getTiltModel());
        }
        //#region Fixture Hanging
        poseStack.translate(0.5F, 0, .5F);
        if(isHanging){
            Direction hangDirection = blockState.getValue(HangableBlock.HANG_DIRECTION);
            poseStack.translate(0, 0.5, 0F);
            if(hangDirection.getAxis() != Direction.Axis.Y){
                if(hangDirection.getAxis() == Direction.Axis.Z){
                    if(hangDirection == Direction.SOUTH) {
                        poseStack.mulPose(Axis.XP.rotationDegrees(-90));
                    } else {
                        poseStack.mulPose(Axis.XP.rotationDegrees(90));
                    }
                    poseStack.mulPose(Axis.YP.rotationDegrees(180));
                } else {
                    if(hangDirection == Direction.EAST) {
                        poseStack.mulPose(Axis.ZN.rotationDegrees(-90));
                    } else {
                        poseStack.mulPose(Axis.ZN.rotationDegrees(90));
                    }
                }
            } else {
                //TODO: Handle hanging up
            }
            poseStack.translate(0, -0.5, 0F);
        }
        //#endregion
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
        // Static Model Render
        minecraftRenderModel(poseStack, vertexConsumer, blockState, cachedStaticModel, packedLight, packedOverlay);
        //#region Model Pan
        float[] pans = blockEntity.getFixture().getPanRotationPosition();
        poseStack.translate(pans[0], pans[1], pans[2]);
        int prevPan = blockEntity.getPrevPan();
        int pan = blockEntity.getPan();
        poseStack.mulPose(Axis.YP.rotationDegrees((prevPan + (pan - prevPan) * partialTicks)));
        poseStack.translate(-pans[0], -pans[1], -pans[2]);
        minecraftRenderModel(poseStack, vertexConsumer, blockState, cachedPanModel, packedLight, packedOverlay);
        //#endregion
        //#region Model Tilt
        float[] tilts = blockEntity.getFixture().getTiltRotationPosition();
        poseStack.translate(tilts[0], tilts[1], tilts[2]);
        int prevTilt = blockEntity.getPrevTilt();
        int tilt = blockEntity.getTilt();
        if (isFlipped) {
            poseStack.mulPose(Axis.XP.rotationDegrees(-180));
        } else {
            poseStack.mulPose(Axis.XP.rotationDegrees(180));
        }
        poseStack.mulPose(Axis.XP.rotationDegrees((prevTilt + (tilt - prevTilt) * partialTicks)));
        poseStack.translate(-tilts[0], -tilts[1], -tilts[2]);
        minecraftRenderModel(poseStack, vertexConsumer, blockState, cachedTiltModel, packedLight, packedOverlay);
        //#endregion
    }
    @Override
    public void beforeRenderBeam(RGBBarBlockEntity blockEntity, PoseStack poseStack, VertexConsumer vertexConsumer, MultiBufferSource multiBufferSource, Direction facing, float partialTicks, boolean isFlipped, BlockState blockstate, boolean isHanging, int packedLight, int packedOverlay) {
        if(blockEntity.getIntensity() > 0){
            LazyRenderers.addLazyRender(new LazyRenderers.LazyRenderer() {
                @Override
                public void render(MultiBufferSource.BufferSource bufferSource, PoseStack poseStack, Camera camera, float partialTick) {
                    poseStack.pushPose();
                    Vec3 offset = Vec3.atLowerCornerOf(blockEntity.getBlockPos()).subtract(camera.getPosition());
                    poseStack.translate(offset.x, offset.y, offset.z);
                    preparePoseStack(blockEntity, poseStack, facing, partialTick, isFlipped, blockstate, isHanging);

                    VertexConsumer builder = multiBufferSource.getBuffer(Beam2DRenderTypes.getBeam());

                    float intensity = (blockEntity.getPrevIntensity() + ((blockEntity.getIntensity()) - blockEntity.getPrevIntensity()) * partialTicks);
                    float alpha = intensity / 255f;
                    int color = blockEntity.getColour();
                    int r = (color >> 16) & 0xFF;
                    int g = (color >> 8) & 0xFF;
                    int b = color & 0xFF;
                    int a = (int) (alpha * 255);

                    poseStack.translate(0.5, 0.5f, 0.38f);

                    Matrix4f m = poseStack.last().pose();
                    Matrix3f normal = poseStack.last().normal();

                    addVertex(builder, m, normal, r, g, b, a, -1.375f, 0.125f, 0f);
                    addVertex(builder, m, normal, r, g, b, a, 1.375f, 0.125f, 0f);
                    addVertex(builder, m, normal, r, g, b, a, 1.375f, -0.125f, 0f);
                    addVertex(builder, m, normal, r, g, b, a, -1.375f, -0.125f, 0f);

                    float beamLength = TheatricalExtraLightsConfig.getRgbBarBeamLength();

                    renderLightBeam(builder, poseStack, blockEntity, partialTicks, alpha, 1.375f, 0.125f, beamLength, color);
                    poseStack.popPose();
                }

                @Override
                public Vec3 getPos(float partialTick) {
                    return blockEntity.getBlockPos().getCenter();
                }
            });
        }
    }

    @Override
    protected void addVertex(VertexConsumer builder, Matrix4f m, Matrix3f nm,
                             int r, int g, int b, int a,
                             float x, float y, float z) {
        if (Beam2DRenderTypes.isShadersActive()) {
            builder.vertex(m, x, y, z)
                    .color(r, g, b, a)
                    .uv(0f, 0f)
                    .uv2(LightTexture.FULL_BRIGHT)
                    .endVertex();
        } else {
            super.addVertex(builder, m, nm, r, g, b, a, x, y, z);
        }
    }

    protected void renderLightBeam(VertexConsumer builder, PoseStack stack, RGBBarBlockEntity tileEntityFixture, float partialTicks, float alpha, float beamWidth, float beamHeight, float length, int color) {
        alpha *= (float) TheatricalConfig.INSTANCE.CLIENT.beamOpacity;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int a = (int) (alpha * 255);
        Matrix4f m = stack.last().pose();
        Matrix3f normal = stack.last().normal();
        float focus = 1.0f;
        float endWidth = beamWidth * focus;
        float endHeight = beamHeight * focus;

        // Right Face
        addVertex(builder, m, normal, r, g, b, 0, endWidth, endHeight, -length);
        addVertex(builder, m, normal, r, g, b, a, beamWidth, beamHeight, 0);
        addVertex(builder, m, normal, r, g, b, a, beamWidth, -beamHeight, 0);
        addVertex(builder, m, normal, r, g, b, 0, endWidth, -endHeight, -length);

        // Left Face
        addVertex(builder, m, normal, r, g, b, 0, -endWidth, -endHeight, -length);
        addVertex(builder, m, normal, r, g, b, a, -beamWidth, -beamHeight, 0);
        addVertex(builder, m, normal, r, g, b, a, -beamWidth, beamHeight, 0);
        addVertex(builder, m, normal, r, g, b, 0, -endWidth, endHeight, -length);

        // UP Face
        addVertex(builder, m, normal, r, g, b, 0, -endWidth, endHeight, -length);
        addVertex(builder, m, normal, r, g, b, a, -beamWidth, beamHeight, 0);
        addVertex(builder, m, normal, r, g, b, a, beamWidth, beamHeight, 0);
        addVertex(builder, m, normal, r, g, b, 0, endWidth, endHeight, -length);

        // Down Face
        addVertex(builder, m, normal, r, g, b, 0, endWidth, -endHeight, -length);
        addVertex(builder, m, normal, r, g, b, a, beamWidth, -beamHeight, 0);
        addVertex(builder, m, normal, r, g, b, a, -beamWidth, -beamHeight, 0);
        addVertex(builder, m, normal, r, g, b, 0, -endWidth, -endHeight, -length);
    }

    @Override
    public void preparePoseStack(RGBBarBlockEntity blockEntity, PoseStack poseStack, Direction facing, float partialTicks, boolean isFlipped, BlockState blockState, boolean isHanging) {
        FixtureMountTransform.apply(poseStack, blockEntity);
        poseStack.translate(0.5F, 0, .5F);
        if(isHanging){
            Direction hangDirection = blockState.getValue(HangableBlock.HANG_DIRECTION);
            poseStack.translate(0, 0.5, 0F);
            if(hangDirection.getAxis() != Direction.Axis.Y){
                if(hangDirection.getAxis() == Direction.Axis.Z){
                    if(hangDirection == Direction.SOUTH) {
                        poseStack.mulPose(Axis.XP.rotationDegrees(-90));
                    } else {
                        poseStack.mulPose(Axis.XP.rotationDegrees(90));
                    }
                    poseStack.mulPose(Axis.YP.rotationDegrees(180));
                } else {
                    if(hangDirection == Direction.EAST) {
                        poseStack.mulPose(Axis.ZN.rotationDegrees(-90));
                    } else {
                        poseStack.mulPose(Axis.ZN.rotationDegrees(90));
                    }
                }
            } else {
                //TODO: Handle hanging up
            }
            poseStack.translate(0, -0.5, 0F);
        }
        //#endregion
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
        //#endregion
        //#region Model Tilt
        float[] tilts = blockEntity.getFixture().getTiltRotationPosition();
        poseStack.translate(tilts[0], tilts[1], tilts[2]);
        int prevTilt = blockEntity.getPrevTilt();
        int tilt = blockEntity.getTilt();
        if (isFlipped) {
            poseStack.mulPose(Axis.XP.rotationDegrees(-180));
        } else {
            poseStack.mulPose(Axis.XP.rotationDegrees(180));
        }
        poseStack.mulPose(Axis.XP.rotationDegrees((prevTilt + (tilt - prevTilt) * partialTicks)));
        poseStack.translate(-tilts[0], -tilts[1], -tilts[2]);
        //#endregion
    }
}