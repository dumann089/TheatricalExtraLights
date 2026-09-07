package com.github.dumann089.theatricalextralights.client.blockentities;

import com.github.dumann089.theatricalextralights.blockentities.AtomicStrobeBlockEntity;
import com.github.dumann089.theatricalextralights.util.FixtureMountTransform;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.imabad.theatrical.TheatricalExpectPlatform;
import dev.imabad.theatrical.blocks.HangableBlock;
import com.github.dumann089.theatricalextralights.client.blockentities.ExtraLightsRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;

import java.util.Optional;

/**
 * Renders the atomic strobe with per-zone colored overlays on the front face.
 * Base model = static/pan/tilt (texture has zones drawn dim by default).
 * Overlay = 8 RGB zones (4 top + 4 bottom) and 9 white LED bar segments,
 * each lit according to live DMX values.
 */
public class AtomicStrobeRenderer extends ExtraLightsRenderer<AtomicStrobeBlockEntity> {

    private BakedModel cachedPanModel, cachedTiltModel, cachedStaticModel;

    public AtomicStrobeRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void renderModel(AtomicStrobeBlockEntity blockEntity, PoseStack poseStack, VertexConsumer vertexConsumer,
                            Direction facing, float partialTicks, boolean isFlipped, BlockState blockState,
                            boolean isHanging, int packedLight, int packedOverlay) {
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

        // Static parts (no rotation)
        minecraftRenderModel(poseStack, vertexConsumer, blockState, cachedStaticModel, packedLight, packedOverlay);

        // Pan rotation around Y at the pan pivot, then render the yoke
        float[] pans = blockEntity.getFixture().getPanRotationPosition();
        poseStack.translate(pans[0], pans[1], pans[2]);
        int prevPan = blockEntity.getPrevPan();
        int pan = blockEntity.getPan();
        poseStack.mulPose(Axis.YP.rotationDegrees(prevPan + (pan - prevPan) * partialTicks));
        poseStack.translate(-pans[0], -pans[1], -pans[2]);
        minecraftRenderModel(poseStack, vertexConsumer, blockState, cachedPanModel, packedLight, packedOverlay);

        // Tilt rotation around X at the tilt pivot, then render the body
        float[] tilts = blockEntity.getFixture().getTiltRotationPosition();
        poseStack.translate(tilts[0], tilts[1], tilts[2]);
        int prevTilt = blockEntity.getPrevTilt();
        int tilt = blockEntity.getTilt();
        poseStack.mulPose(Axis.XP.rotationDegrees(prevTilt + (tilt - prevTilt) * partialTicks));
        poseStack.translate(-tilts[0], -tilts[1], -tilts[2]);
        minecraftRenderModel(poseStack, vertexConsumer, blockState, cachedTiltModel, packedLight, packedOverlay);

        // Per-zone emissive overlay on the front (north) face of the new model.
        // Front bezel plane: x∈[2.3,13.8], z=4.9/16. Bands: bottom y∈[2.2,4],
        // middle bar y∈[4,5], top y∈[5,6.8]. We draw just IN FRONT of the
        // bezel boxes (slightly lower z) so the colour reads as emissive LEDs.
        renderZoneOverlays(blockEntity, poseStack);
    }

    private void renderZoneOverlays(AtomicStrobeBlockEntity be, PoseStack poseStack) {
        MultiBufferSource buffers = net.minecraft.client.Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer vc = buffers.getBuffer(RenderType.lightning());

        // The face texture has a 24px outer plastic frame on a 512x256 sheet,
        // which is 1.5 UV units / ~10% on each side. The bezel boxes span
        // x∈[2.3,13.8] and the LED bands span the full bezel height, but the
        // actual LED pixels in the texture are inset by that frame margin.
        // Match the emissive overlay to the LED pixels, not the bezel edges,
        // so the plastic frame stays dark.
        final float x0 = 2.85f / 16f;     // inset 0.55 from bezel x=2.3
        final float x1 = 13.25f / 16f;    // inset 0.55 from bezel x=13.8
        final float topBandTopY = 6.65f / 16f;  // texture LED panel top edge
        final float topBandBotY = 5.05f / 16f;
        final float barTopY = 4.95f / 16f;      // texture white bar edges
        final float barBotY = 4.10f / 16f;
        final float botBandTopY = 3.95f / 16f;
        final float botBandBotY = 2.40f / 16f;  // texture LED panel bottom edge
        // Front face is at z=11.1/16 (south side, high Z after Z-flip). Place
        // the overlay just OUTSIDE the bezel toward +Z so it sits in front.
        final float z = (11.2f / 16f) + 0.002f;
        final float zoneW = (x1 - x0) / 4f;

        Matrix4f m = poseStack.last().pose();

        // Top 4 RGB zones (DMX zones 0..3)
        for (int i = 0; i < 4; i++) {
            int r = be.getZoneRed(i);
            int g = be.getZoneGreen(i);
            int b = be.getZoneBlue(i);
            if ((r | g | b) == 0) continue;
            float qx0 = x0 + i * zoneW;
            float qx1 = qx0 + zoneW;
            quad(vc, m, qx0, topBandBotY, qx1, topBandTopY, z, r, g, b, 255);
        }
        // Bottom 4 RGB zones (DMX zones 4..7)
        for (int i = 0; i < 4; i++) {
            int r = be.getZoneRed(4 + i);
            int g = be.getZoneGreen(4 + i);
            int b = be.getZoneBlue(4 + i);
            if ((r | g | b) == 0) continue;
            float qx0 = x0 + i * zoneW;
            float qx1 = qx0 + zoneW;
            quad(vc, m, qx0, botBandBotY, qx1, botBandTopY, z, r, g, b, 255);
        }
        // 9 white bar segments along the middle band
        final float segW = (x1 - x0) / 9f;
        for (int i = 0; i < 9; i++) {
            int w = be.getWhiteSegment(i);
            if (w == 0) continue;
            float qx0 = x0 + i * segW;
            float qx1 = qx0 + segW;
            quad(vc, m, qx0, barBotY, qx1, barTopY, z, w, w, w, 255);
        }
    }

    private static void quad(VertexConsumer vc, Matrix4f m,
                             float x0, float y0, float x1, float y1, float z,
                             int r, int g, int b, int a) {
        vc.vertex(m, x0, y0, z).color(r, g, b, a).endVertex();
        vc.vertex(m, x1, y0, z).color(r, g, b, a).endVertex();
        vc.vertex(m, x1, y1, z).color(r, g, b, a).endVertex();
        vc.vertex(m, x0, y1, z).color(r, g, b, a).endVertex();
    }

    @Override
    public void beforeRenderBeam(AtomicStrobeBlockEntity blockEntity, PoseStack poseStack, VertexConsumer vertexConsumer,
                                 MultiBufferSource multiBufferSource, Direction facing, float partialTicks,
                                 boolean isFlipped, BlockState blockstate, boolean isHanging,
                                 int packedLight, int packedOverlay) {
        // No projected beam — atomic strobe is a wash-style fixture
    }

    @Override
    public void preparePoseStack(AtomicStrobeBlockEntity blockEntity, PoseStack poseStack, Direction facing,
                                 float partialTicks, boolean isFlipped, BlockState blockState, boolean isHanging) {
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
    }
}
