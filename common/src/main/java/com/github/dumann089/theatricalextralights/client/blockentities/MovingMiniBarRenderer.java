package com.github.dumann089.theatricalextralights.client.blockentities;

import com.github.dumann089.theatricalextralights.blockentities.MovingMiniBarBlockEntity;
import com.github.dumann089.theatricalextralights.util.FixtureMountTransform;
import com.github.dumann089.theatricalextralights.client.Beam2DRenderTypes;
import com.github.dumann089.theatricalextralights.config.TheatricalExtraLightsConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.imabad.theatrical.TheatricalExpectPlatform;
import dev.imabad.theatrical.blocks.HangableBlock;
import dev.imabad.theatrical.client.LazyRenderers;
import dev.imabad.theatrical.client.TheatricalRenderTypes;
import com.github.dumann089.theatricalextralights.client.blockentities.ExtraLightsRenderer;
import dev.imabad.theatrical.config.TheatricalConfig;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.Optional;

public class MovingMiniBarRenderer extends ExtraLightsRenderer<MovingMiniBarBlockEntity> {

    private static final float[] EMITTER_X_OFFSETS = new float[]{
            -0.42857143F, -0.2857143F, -0.14285715F, 0F, 0.14285715F, 0.2857143F, 0.42857143F
    };
    private static final float BEAM_ORIGIN_Y = 0.203125F;
    private static final float BEAM_ORIGIN_Z = 0.34375F;
    private static final float BEAM_WIDTH = 0.0525F;
    private static final float BEAM_HEIGHT = 0.045F;

    private BakedModel cachedTiltModel;
    private BakedModel cachedStaticModel;

    public MovingMiniBarRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void renderModel(MovingMiniBarBlockEntity blockEntity, PoseStack poseStack, VertexConsumer vertexConsumer, Direction facing, float partialTicks, boolean isFlipped, BlockState blockState, boolean isHanging, int packedLight, int packedOverlay) {
        if (cachedStaticModel == null) {
            cachedStaticModel = TheatricalExpectPlatform.getBakedModel(blockEntity.getFixture().getStaticModel());
        }
        if (cachedTiltModel == null) {
            cachedTiltModel = TheatricalExpectPlatform.getBakedModel(blockEntity.getFixture().getTiltModel());
        }

        applyBaseTransforms(blockEntity, poseStack, facing, isFlipped, blockState, isHanging);
        minecraftRenderModel(poseStack, vertexConsumer, blockState, cachedStaticModel, packedLight, packedOverlay);

        for (int i = 0; i < MovingMiniBarBlockEntity.BEAM_COUNT; i++) {
            poseStack.pushPose();
            poseStack.translate(EMITTER_X_OFFSETS[i], 0, 0);
            applyModuleTilt(blockEntity, poseStack, partialTicks, isFlipped, i);
            minecraftRenderModel(poseStack, vertexConsumer, blockState, cachedTiltModel, packedLight, packedOverlay);
            poseStack.popPose();
        }
    }

    @Override
    public void beforeRenderBeam(MovingMiniBarBlockEntity blockEntity, PoseStack poseStack, VertexConsumer vertexConsumer, MultiBufferSource multiBufferSource, Direction facing, float partialTicks, boolean isFlipped, BlockState blockState, boolean isHanging, int packedLight, int packedOverlay) {
        if (!hasVisibleBeam(blockEntity)) {
            return;
        }

        LazyRenderers.addLazyRender(new LazyRenderers.LazyRenderer() {
            @Override
            public void render(MultiBufferSource.BufferSource bufferSource, PoseStack poseStack, Camera camera, float partialTick) {
                poseStack.pushPose();
                Vec3 offset = Vec3.atLowerCornerOf(blockEntity.getBlockPos()).subtract(camera.getPosition());
                poseStack.translate(offset.x, offset.y, offset.z);
                applyBaseTransforms(blockEntity, poseStack, facing, isFlipped, blockState, isHanging);

                VertexConsumer builder = multiBufferSource.getBuffer(Beam2DRenderTypes.getBeam());
                float beamLength = TheatricalExtraLightsConfig.getRgbBarBeamLength();

                for (int i = 0; i < MovingMiniBarBlockEntity.BEAM_COUNT; i++) {
                    float intensity = blockEntity.getPartialBeamIntensity(i, partialTick);
                    if (intensity <= 0.0F) {
                        continue;
                    }

                    float alpha = intensity / 255f;
                    int color = interpolateBeamColor(blockEntity, i, partialTick);
                    int r = (color >> 16) & 0xFF;
                    int g = (color >> 8) & 0xFF;
                    int b = color & 0xFF;
                    int a = (int) (alpha * 255);

                    poseStack.pushPose();
                    poseStack.translate(EMITTER_X_OFFSETS[i], 0, 0);
                    applyModuleTilt(blockEntity, poseStack, partialTick, isFlipped, i);
                    poseStack.translate(0.5F, BEAM_ORIGIN_Y, BEAM_ORIGIN_Z);

                    Matrix4f matrix = poseStack.last().pose();
                    Matrix3f normal = poseStack.last().normal();
                    addVertex(builder, matrix, normal, r, g, b, a, -BEAM_WIDTH, BEAM_HEIGHT, 0f);
                    addVertex(builder, matrix, normal, r, g, b, a, BEAM_WIDTH, BEAM_HEIGHT, 0f);
                    addVertex(builder, matrix, normal, r, g, b, a, BEAM_WIDTH, -BEAM_HEIGHT, 0f);
                    addVertex(builder, matrix, normal, r, g, b, a, -BEAM_WIDTH, -BEAM_HEIGHT, 0f);

                    renderLightBeam(builder, poseStack, alpha, BEAM_WIDTH, BEAM_HEIGHT, beamLength, color);
                    poseStack.popPose();
                }

                poseStack.popPose();
            }

            @Override
            public Vec3 getPos(float partialTick) {
                return blockEntity.getBlockPos().getCenter();
            }
        });
    }

    @Override
    public void preparePoseStack(MovingMiniBarBlockEntity blockEntity, PoseStack poseStack, Direction facing, float partialTicks, boolean isFlipped, BlockState blockState, boolean isHanging) {
        FixtureMountTransform.apply(poseStack, blockEntity);
        applyBaseTransforms(blockEntity, poseStack, facing, isFlipped, blockState, isHanging);
        poseStack.translate(EMITTER_X_OFFSETS[MovingMiniBarBlockEntity.BEAM_COUNT / 2], 0, 0);
        applyModuleTilt(blockEntity, poseStack, partialTicks, isFlipped, MovingMiniBarBlockEntity.BEAM_COUNT / 2);
    }

    private void applyBaseTransforms(MovingMiniBarBlockEntity blockEntity, PoseStack poseStack, Direction facing, boolean isFlipped, BlockState blockState, boolean isHanging) {
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
    }

    private void applyModuleTilt(MovingMiniBarBlockEntity blockEntity, PoseStack poseStack, float partialTicks, boolean isFlipped, int moduleIndex) {
        float[] tiltOrigin = blockEntity.getFixture().getTiltRotationPosition();
        poseStack.translate(tiltOrigin[0], tiltOrigin[1], tiltOrigin[2]);
        if (isFlipped) {
            poseStack.mulPose(Axis.XP.rotationDegrees(-180));
        } else {
            poseStack.mulPose(Axis.XP.rotationDegrees(180));
        }
        poseStack.mulPose(Axis.XP.rotationDegrees(blockEntity.getPartialBeamTilt(moduleIndex, partialTicks)));
        poseStack.translate(-tiltOrigin[0], -tiltOrigin[1], -tiltOrigin[2]);
    }

    private boolean hasVisibleBeam(MovingMiniBarBlockEntity blockEntity) {
        for (int i = 0; i < MovingMiniBarBlockEntity.BEAM_COUNT; i++) {
            if (blockEntity.getBeamIntensity(i) > 0 || blockEntity.getPrevBeamIntensity(i) > 0) {
                return true;
            }
        }
        return false;
    }

    private int interpolateBeamColor(MovingMiniBarBlockEntity blockEntity, int index, float partialTicks) {
        int r = interpolate(blockEntity.getPrevBeamRed(index), blockEntity.getBeamRed(index), partialTicks);
        int g = interpolate(blockEntity.getPrevBeamGreen(index), blockEntity.getBeamGreen(index), partialTicks);
        int b = interpolate(blockEntity.getPrevBeamBlue(index), blockEntity.getBeamBlue(index), partialTicks);
        return (r << 16) | (g << 8) | b;
    }

    private int interpolate(int previous, int current, float partialTicks) {
        return (int) (previous + (current - previous) * partialTicks);
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

    protected void renderLightBeam(VertexConsumer builder, PoseStack stack, float alpha, float beamWidth, float beamHeight, float length, int color) {
        alpha *= (float) TheatricalConfig.INSTANCE.CLIENT.beamOpacity;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int a = (int) (alpha * 255);
        Matrix4f matrix = stack.last().pose();
        Matrix3f normal = stack.last().normal();

        addVertex(builder, matrix, normal, r, g, b, 0, beamWidth, beamHeight, -length);
        addVertex(builder, matrix, normal, r, g, b, a, beamWidth, beamHeight, 0);
        addVertex(builder, matrix, normal, r, g, b, a, beamWidth, -beamHeight, 0);
        addVertex(builder, matrix, normal, r, g, b, 0, beamWidth, -beamHeight, -length);

        addVertex(builder, matrix, normal, r, g, b, 0, -beamWidth, -beamHeight, -length);
        addVertex(builder, matrix, normal, r, g, b, a, -beamWidth, -beamHeight, 0);
        addVertex(builder, matrix, normal, r, g, b, a, -beamWidth, beamHeight, 0);
        addVertex(builder, matrix, normal, r, g, b, 0, -beamWidth, beamHeight, -length);

        addVertex(builder, matrix, normal, r, g, b, 0, -beamWidth, beamHeight, -length);
        addVertex(builder, matrix, normal, r, g, b, a, -beamWidth, beamHeight, 0);
        addVertex(builder, matrix, normal, r, g, b, a, beamWidth, beamHeight, 0);
        addVertex(builder, matrix, normal, r, g, b, 0, beamWidth, beamHeight, -length);

        addVertex(builder, matrix, normal, r, g, b, 0, beamWidth, -beamHeight, -length);
        addVertex(builder, matrix, normal, r, g, b, a, beamWidth, -beamHeight, 0);
        addVertex(builder, matrix, normal, r, g, b, a, -beamWidth, -beamHeight, 0);
        addVertex(builder, matrix, normal, r, g, b, 0, -beamWidth, -beamHeight, -length);
    }
}