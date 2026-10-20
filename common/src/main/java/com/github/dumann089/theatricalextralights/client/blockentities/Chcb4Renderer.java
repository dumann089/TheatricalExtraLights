package com.github.dumann089.theatricalextralights.client.blockentities;

import com.github.dumann089.theatricalextralights.blockentities.Chcb4BlockEntity;
import com.github.dumann089.theatricalextralights.client.LensRenderTypes;
import com.github.dumann089.theatricalextralights.fixtures.Chcb4Fixture;
import com.github.dumann089.theatricalextralights.util.FixtureMountTransform;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.imabad.theatrical.TheatricalExpectPlatform;
import dev.imabad.theatrical.blocks.HangableBlock;
import dev.imabad.theatrical.client.LazyRenderers;
import com.github.dumann089.theatricalextralights.client.ExtraLightsRenderTypes;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.Optional;

public class Chcb4Renderer extends ExtraLightsFixtureRenderer<Chcb4BlockEntity> {

    private static final float LENS_SIZE = 0.072f;
    private static final float GLOW_SIZE = 0.095f;
    private static final float FACE_OFFSET = 0.004f;

    private BakedModel cachedStaticModel;

    public Chcb4Renderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void renderModel(Chcb4BlockEntity blockEntity, PoseStack poseStack, VertexConsumer vertexConsumer,
                            Direction facing, float partialTicks, boolean isFlipped, BlockState blockState,
                            boolean isHanging, int packedLight, int packedOverlay) {
        if (cachedStaticModel == null) {
            cachedStaticModel = TheatricalExpectPlatform.getBakedModel(blockEntity.getFixture().getStaticModel());
        }
        applyBodyPose(poseStack, blockEntity, facing, isFlipped, blockState, isHanging);
        minecraftRenderModel(poseStack, vertexConsumer, blockState, cachedStaticModel, packedLight, packedOverlay);
    }

    @Override
    public void beforeRenderBeam(Chcb4BlockEntity blockEntity, PoseStack poseStack, VertexConsumer vertexConsumer,
                                 MultiBufferSource multiBufferSource, Direction facing, float partialTicks,
                                 boolean isFlipped, BlockState blockstate, boolean isHanging,
                                 int packedLight, int packedOverlay) {
        boolean anyOn = false;
        for (int cell = 0; cell < Chcb4Fixture.CELL_COUNT; cell++) {
            if (blockEntity.isCellOn(cell)) {
                anyOn = true;
                break;
            }
        }
        if (!anyOn) {
            return;
        }

        LazyRenderers.addLazyRender(new LazyRenderers.LazyRenderer() {
            @Override
            public void render(MultiBufferSource.BufferSource bufferSource, PoseStack lazyPose, Camera camera, float partialTick) {
                lazyPose.pushPose();
                Vec3 blockOffset = Vec3.atLowerCornerOf(blockEntity.getBlockPos()).subtract(camera.getPosition());
                lazyPose.translate(blockOffset.x, blockOffset.y, blockOffset.z);
                preparePoseStack(blockEntity, lazyPose, facing, partialTick, isFlipped, blockstate, isHanging);

                float[][] positions = Chcb4Fixture.BEAM_POSITIONS;
                for (int cell = 0; cell < positions.length; cell++) {
                    float intensity = blockEntity.getCellIntensity(cell);
                    if (intensity <= 0.0F) {
                        continue;
                    }
                    int color = blockEntity.getCellColour(cell);
                    int r = (color >> 16) & 0xFF;
                    int g = (color >> 8) & 0xFF;
                    int b = color & 0xFF;
                    int a = (int) (intensity * 255.0F);
                    float[] pos = positions[cell];

                    lazyPose.pushPose();
                    lazyPose.translate(pos[0], pos[1], pos[2] + FACE_OFFSET);

                    VertexConsumer glow = bufferSource.getBuffer(ExtraLightsRenderTypes.BEAM);
                    renderDisc(glow, lazyPose, r, g, b, (int) (a * 0.55f), GLOW_SIZE);

                    VertexConsumer lens = bufferSource.getBuffer(LensRenderTypes.LENS);
                    renderLensDisc(lens, lazyPose, r, g, b, (int) (a * 0.92f), LENS_SIZE);

                    lazyPose.popPose();
                }
                lazyPose.popPose();
            }

            @Override
            public Vec3 getPos(float partialTick) {
                return blockEntity.getBlockPos().getCenter();
            }
        });
    }

    @Override
    public void preparePoseStack(Chcb4BlockEntity blockEntity, PoseStack poseStack, Direction facing,
                                 float partialTicks, boolean isFlipped, BlockState blockState, boolean isHanging) {
        FixtureMountTransform.apply(poseStack, blockEntity);
        applyBodyPose(poseStack, blockEntity, facing, isFlipped, blockState, isHanging);
    }

    private static void applyBodyPose(PoseStack poseStack, Chcb4BlockEntity blockEntity, Direction facing,
                                      boolean isFlipped, BlockState blockState, boolean isHanging) {
        poseStack.translate(0.5F, 0, 0.5F);
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
                } else if (hangDirection == Direction.EAST) {
                    poseStack.mulPose(Axis.ZN.rotationDegrees(90));
                } else {
                    poseStack.mulPose(Axis.ZN.rotationDegrees(-90));
                }
            }
            poseStack.translate(0, -0.35, 0F);
        }
        poseStack.mulPose(Axis.YP.rotationDegrees(facing.toYRot()));
        poseStack.translate(-0.5F, 0, -0.5F);
        if (isHanging) {
            Optional<BlockState> optionalSupport = blockEntity.getSupportingStructure();
            if (optionalSupport.isPresent()) {
                float[] transforms = blockEntity.getFixture().getTransforms(blockState, optionalSupport.get());
                poseStack.translate(transforms[0], transforms[1], transforms[2]);
            } else {
                poseStack.translate(0, 0.19, 0);
            }
        }
        if (isFlipped) {
            poseStack.translate(0.5F, 0.5, 0.5F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(180));
            poseStack.translate(-0.5F, -0.5, -0.5F);
        }
    }

    private static void renderDisc(VertexConsumer consumer, PoseStack poseStack, int r, int g, int b, int a, float size) {
        Matrix4f matrix = poseStack.last().pose();
        consumer.vertex(matrix, -size, size, 0f).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, size, size, 0f).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, size, -size, 0f).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, -size, -size, 0f).color(r, g, b, a).endVertex();
    }

    private static void renderLensDisc(VertexConsumer consumer, PoseStack poseStack, int r, int g, int b, int a, float size) {
        Matrix4f matrix = poseStack.last().pose();
        addLensVertex(consumer, matrix, r, g, b, a, -size, size, 0f, 0f, 0f);
        addLensVertex(consumer, matrix, r, g, b, a, size, size, 0f, 1f, 0f);
        addLensVertex(consumer, matrix, r, g, b, a, size, -size, 0f, 1f, 1f);
        addLensVertex(consumer, matrix, r, g, b, a, -size, -size, 0f, 0f, 1f);
    }

    private static void addLensVertex(VertexConsumer consumer, Matrix4f matrix,
                                      int r, int g, int b, int a,
                                      float x, float y, float z, float u, float v) {
        consumer.vertex(matrix, x, y, z)
                .color(r, g, b, a)
                .uv(u, v)
                .uv2(LightTexture.FULL_BRIGHT)
                .endVertex();
    }
}
