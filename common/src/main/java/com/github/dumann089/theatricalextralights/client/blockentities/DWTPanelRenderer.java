package com.github.dumann089.theatricalextralights.client.blockentities;

import com.github.dumann089.theatricalextralights.blockentities.DWTPanelBlockEntity;
import com.github.dumann089.theatricalextralights.util.FixtureMountTransform;
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
import org.joml.Matrix4f;
import dev.imabad.theatrical.client.LazyRenderers;
import com.github.dumann089.theatricalextralights.client.Beam2DRenderTypes;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;

import java.util.Optional;

public class DWTPanelRenderer extends ExtraLightsRenderer<DWTPanelBlockEntity> {
    private BakedModel cachedModel;

    public DWTPanelRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void renderModel(DWTPanelBlockEntity blockEntity, PoseStack poseStack, VertexConsumer vertexConsumer, Direction facing, float partialTicks, boolean isFlipped, BlockState blockState, boolean isHanging, int packedLight, int packedOverlay) {
        if(cachedModel == null){
            cachedModel = TheatricalExpectPlatform.getBakedModel(blockEntity.getFixture().getPanModel());
        }

        poseStack.translate(0.5F, 0, .5F);
        if (isHanging) {
            Direction hangDirection = blockState.getValue(HangableBlock.HANG_DIRECTION);
            poseStack.translate(0, 0.5, 0F);
            if (hangDirection.getAxis() != Direction.Axis.Y) {
                if (hangDirection.getAxis() == Direction.Axis.Z) {
                    poseStack.mulPose(hangDirection == Direction.SOUTH ? Axis.XP.rotationDegrees(-90) : Axis.XP.rotationDegrees(90));
                    poseStack.mulPose(Axis.YP.rotationDegrees(180));
                } else {
                    poseStack.mulPose(hangDirection == Direction.EAST ? Axis.ZN.rotationDegrees(-90) : Axis.ZN.rotationDegrees(90));
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
        poseStack.mulPose(Axis.YP.rotationDegrees(prevPan + (pan - prevPan) * partialTicks));
        poseStack.translate(-pans[0], -pans[1], -pans[2]);

        minecraftRenderModel(poseStack, vertexConsumer, blockState, cachedModel, packedLight, packedOverlay);

        renderZoneOverlays(blockEntity, poseStack);
    }

    private void renderZoneOverlays(DWTPanelBlockEntity be, PoseStack poseStack) {
        MultiBufferSource buffers = net.minecraft.client.Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer vc = buffers.getBuffer(RenderType.lightning());
        Matrix4f m = poseStack.last().pose();

        final float z = (21.5f / 16f) + 0.002f;

        final float W = 3f; // Width
        final float H = 4; // Height

        // White Center
        final float[][] mainOffsets = {
                { 7f,  2f },
                { 7f,  10f },
                { 7f, 17f },
                { 7f, 25f }
        };

        final float[][] leftWarmOffsets = {
                { 2.625f,  1f },
                { 2.625f,  9f },
                { 2.625f, 17f },
                { 2.625f, 25f }
        };
        final float[][] rightWarmOffsets = {
                { 11.25f,  1f },
                { 11.25f,  9f },
                { 11.25f, 17f },
                { 11.25f, 25f }
        };

        final int warmR = 255;
        final int warmG = 160;
        final int warmB = 60;

        for (int i = 0; i < 4; i++) {
            int intensity = be.getSectionIntensity(i);
            if (intensity > 0) {
                float ox = mainOffsets[i][0];
                float oy = mainOffsets[i][1];
                quad(vc, m, ox/16f, oy/16f, (ox + W)/16f, (oy + H)/16f, z, intensity, intensity, intensity, 255);
            }
        }

        int warmInt = be.getWarmSectionIntensity();
        if (warmInt > 0) {
            int warmAlpha = (int) ((warmInt / 255f) * 255);

            for (int i = 0; i < 4; i++) {
                float lx = leftWarmOffsets[i][0];
                float ly = leftWarmOffsets[i][1];
                quad(vc, m, lx/16f, ly/16f, (lx + W)/16f, (ly + H)/16f, z, warmR, warmG, warmB, warmAlpha);

                float rx = rightWarmOffsets[i][0];
                float ry = rightWarmOffsets[i][1];
                quad(vc, m, rx/16f, ry/16f, (rx + W)/16f, (ry + H)/16f, z, warmR, warmG, warmB, warmAlpha);
            }
        }
    }

    private static void quad(VertexConsumer vc, Matrix4f m, float x0, float y0, float x1, float y1, float z, int r, int g, int b, int a) {
        vc.vertex(m, x0, y0, z).color(r, g, b, a).endVertex();
        vc.vertex(m, x1, y0, z).color(r, g, b, a).endVertex();
        vc.vertex(m, x1, y1, z).color(r, g, b, a).endVertex();
        vc.vertex(m, x0, y1, z).color(r, g, b, a).endVertex();
    }

    @Override
    public void beforeRenderBeam(DWTPanelBlockEntity blockEntity, PoseStack poseStack, VertexConsumer vertexConsumer, MultiBufferSource multiBufferSource, Direction facing, float partialTicks, boolean isFlipped, BlockState blockstate, boolean isHanging, int packedLight, int packedOverlay) {
        if (blockEntity.getWarmSectionIntensity() > 0) {
            LazyRenderers.addLazyRender(new LazyRenderers.LazyRenderer() {
                @Override
                public void render(MultiBufferSource.BufferSource bufferSource, PoseStack poseStack, Camera camera, float partialTick) {
                    Vec3 offset = Vec3.atLowerCornerOf(blockEntity.getBlockPos()).subtract(camera.getPosition());
                    VertexConsumer builder = bufferSource.getBuffer(Beam2DRenderTypes.getBeam());

                    int warmInt = blockEntity.getWarmSectionIntensity();
                    float alpha = warmInt / 255f;
                    final int warmR = 255;
                    final int warmG = 160;
                    final int warmB = 60;

                    final float z = (21.5f / 16f);

                    final float[][] leftWarmOffsets = {
                            { 2.625f,  1f }, { 2.625f,  9f }, { 2.625f, 17f }, { 2.625f, 25f }
                    };
                    final float[][] rightWarmOffsets = {
                            { 11.25f,  1f }, { 11.25f,  9f }, { 11.25f, 17f }, { 11.25f, 25f }
                    };

                    final float quadW = 4f;
                    final float quadH = 4f;

                    final float beamW = 2f;
                    final float beamH = 3f;
                    float beamWidth = (beamW / 16f) / 2f;
                    float beamHeight = (beamH / 16f) / 2f;
                    float beamLength = 10.0f;

                    for (float[] offs : leftWarmOffsets) {
                        float centerX = offs[0] + (quadW / 2f);
                        float centerY = offs[1] + (quadH / 2f);
                        drawBeamAtOffset(poseStack, builder, blockEntity, facing, partialTick, isFlipped, blockstate, isHanging, offset, centerX, centerY, z, beamWidth, beamHeight, beamLength, warmR, warmG, warmB, alpha);
                    }

                    for (float[] offs : rightWarmOffsets) {
                        float centerX = offs[0] + (quadW / 2f);
                        float centerY = offs[1] + (quadH / 2f);
                        drawBeamAtOffset(poseStack, builder, blockEntity, facing, partialTick, isFlipped, blockstate, isHanging, offset, centerX, centerY, z, beamWidth, beamHeight, beamLength, warmR, warmG, warmB, alpha);
                    }
                }

                @Override
                public Vec3 getPos(float partialTick) {
                    return blockEntity.getBlockPos().getCenter();
                }
            });
        }
    }

    private void drawBeamAtOffset(PoseStack poseStack, VertexConsumer builder, DWTPanelBlockEntity blockEntity, Direction facing, float partialTick, boolean isFlipped, BlockState blockstate, boolean isHanging, Vec3 offset, float centerX, float centerY, float z, float beamWidth, float beamHeight, float length, int r, int g, int b, float alpha) {
        poseStack.pushPose();
        poseStack.translate(offset.x, offset.y, offset.z);
        preparePoseStack(blockEntity, poseStack, facing, partialTick, isFlipped, blockstate, isHanging);
        poseStack.translate(centerX / 16f, centerY / 16f, z);
        Matrix4f m = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();
        int a = (int) (alpha * 255);

        addVertex(builder, m, normal, r, g, b, a, -beamWidth, beamHeight, 0f);
        addVertex(builder, m, normal, r, g, b, a, beamWidth, beamHeight, 0f);
        addVertex(builder, m, normal, r, g, b, a, beamWidth, -beamHeight, 0f);
        addVertex(builder, m, normal, r, g, b, a, -beamWidth, -beamHeight, 0f);

        renderLightBeam(builder, poseStack, alpha, beamWidth, beamHeight, length, r, g, b);

        poseStack.popPose();
    }

    protected void renderLightBeam(VertexConsumer builder, PoseStack stack, float alpha, float beamWidth, float beamHeight, float length, int r, int g, int b) {
        alpha *= (float) dev.imabad.theatrical.config.TheatricalConfig.INSTANCE.CLIENT.beamOpacity;
        int a = (int) (alpha * 255);
        Matrix4f m = stack.last().pose();
        Matrix3f normal = stack.last().normal();

        // (+X)
        addVertex(builder, m, normal, r, g, b, 0, beamWidth, -beamHeight, length);
        addVertex(builder, m, normal, r, g, b, a, beamWidth, -beamHeight, 0);
        addVertex(builder, m, normal, r, g, b, a, beamWidth, beamHeight, 0);
        addVertex(builder, m, normal, r, g, b, 0, beamWidth, beamHeight, length);

        // (-X)
        addVertex(builder, m, normal, r, g, b, 0, -beamWidth, beamHeight, length);
        addVertex(builder, m, normal, r, g, b, a, -beamWidth, beamHeight, 0);
        addVertex(builder, m, normal, r, g, b, a, -beamWidth, -beamHeight, 0);
        addVertex(builder, m, normal, r, g, b, 0, -beamWidth, -beamHeight, length);

        //  (+Y)
        addVertex(builder, m, normal, r, g, b, 0, beamWidth, beamHeight, length);
        addVertex(builder, m, normal, r, g, b, a, beamWidth, beamHeight, 0);
        addVertex(builder, m, normal, r, g, b, a, -beamWidth, beamHeight, 0);
        addVertex(builder, m, normal, r, g, b, 0, -beamWidth, beamHeight, length);

        // (-Y)
        addVertex(builder, m, normal, r, g, b, 0, -beamWidth, -beamHeight, length);
        addVertex(builder, m, normal, r, g, b, a, -beamWidth, -beamHeight, 0);
        addVertex(builder, m, normal, r, g, b, a, beamWidth, -beamHeight, 0);
        addVertex(builder, m, normal, r, g, b, 0, beamWidth, -beamHeight, length);
    }

    @Override
    protected void addVertex(VertexConsumer builder, Matrix4f m, Matrix3f nm, int r, int g, int b, int a, float x, float y, float z) {
        if (Beam2DRenderTypes.isShadersActive()) {
            builder.vertex(m, x, y, z).color(r, g, b, a).uv(0f, 0f).uv2(LightTexture.FULL_BRIGHT).endVertex();
        } else {
            super.addVertex(builder, m, nm, r, g, b, a, x, y, z);
        }
    }

    @Override
    public void preparePoseStack(DWTPanelBlockEntity blockEntity, PoseStack poseStack, Direction facing, float partialTicks, boolean isFlipped, BlockState blockState, boolean isHanging) {
        FixtureMountTransform.apply(poseStack, blockEntity);
        poseStack.translate(0.5F, 0, .5F);
        if (isHanging) {
            Direction hangDirection = blockState.getValue(HangableBlock.HANG_DIRECTION);
            poseStack.translate(0, 0.5, 0F);
            if (hangDirection.getAxis() != Direction.Axis.Y) {
                if (hangDirection.getAxis() == Direction.Axis.Z) {
                    poseStack.mulPose(hangDirection == Direction.SOUTH ? Axis.XP.rotationDegrees(-90) : Axis.XP.rotationDegrees(90));
                    poseStack.mulPose(Axis.YP.rotationDegrees(180));
                } else {
                    poseStack.mulPose(hangDirection == Direction.EAST ? Axis.ZN.rotationDegrees(-90) : Axis.ZN.rotationDegrees(90));
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
        poseStack.mulPose(Axis.YP.rotationDegrees(prevPan + (pan - prevPan) * partialTicks));
        poseStack.translate(-pans[0], -pans[1], -pans[2]);
    }
}