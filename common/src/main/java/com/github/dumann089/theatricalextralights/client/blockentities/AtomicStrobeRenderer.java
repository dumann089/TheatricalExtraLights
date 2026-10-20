package com.github.dumann089.theatricalextralights.client.blockentities;

import com.github.dumann089.theatricalextralights.blockentities.AtomicStrobeBlockEntity;
import com.github.dumann089.theatricalextralights.client.ExtraLightsRenderTypes;
import com.github.dumann089.theatricalextralights.client.StrobeVisualEffects;
import com.github.dumann089.theatricalextralights.util.AtomicStrobeEngine;
import com.github.dumann089.theatricalextralights.util.FixtureMountTransform;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.imabad.theatrical.TheatricalExpectPlatform;
import dev.imabad.theatrical.blocks.HangableBlock;
import dev.imabad.theatrical.client.LazyRenderers;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.Optional;

/**
 * Atomic Strobe : modele socle / lyre / corps, puis les LED de la face en emissif au niveau
 * instantane des flashs (tube blanc par segment, plaques RGB par zone), et le halo du strobe.
 */
public class AtomicStrobeRenderer extends ExtraLightsFixtureRenderer<AtomicStrobeBlockEntity> {

    // Face LED mesuree dans atomic_face_base.png (512 x 256, cadre de 24 px) rapportee au corps
    // x 1..15, y 3..11 : plaque haute lignes 25-111, tube 114-141, plaque basse 145-232.
    private static final float LED_X0 = 1.684f / 16f;
    private static final float LED_X1 = 14.34f / 16f;
    private static final float TOP_Y0 = 7.53f / 16f;
    private static final float TOP_Y1 = 10.22f / 16f;
    private static final float BAR_Y0 = 6.59f / 16f;
    private static final float BAR_Y1 = 7.44f / 16f;
    private static final float BOT_Y0 = 3.75f / 16f;
    private static final float BOT_Y1 = 6.47f / 16f;
    private static final float FACE_Z = 11.5f / 16f + 0.002f;
    /** Fond sombre d'une plaque allumee, juste devant la texture : les joints entre pixels. */
    private static final float BACKING_Z = 11.5f / 16f + 0.001f;

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
        placeBlock(blockEntity, poseStack, facing, isFlipped, blockState, isHanging);

        minecraftRenderModel(poseStack, vertexConsumer, blockState, cachedStaticModel, packedLight, packedOverlay);

        float[] pans = blockEntity.getFixture().getPanRotationPosition();
        poseStack.translate(pans[0], pans[1], pans[2]);
        poseStack.mulPose(Axis.YP.rotationDegrees(
                blockEntity.getPrevPan() + (blockEntity.getPan() - blockEntity.getPrevPan()) * partialTicks));
        poseStack.translate(-pans[0], -pans[1], -pans[2]);
        minecraftRenderModel(poseStack, vertexConsumer, blockState, cachedPanModel, packedLight, packedOverlay);

        float[] tilts = blockEntity.getFixture().getTiltRotationPosition();
        poseStack.translate(tilts[0], tilts[1], tilts[2]);
        poseStack.mulPose(Axis.XP.rotationDegrees(
                blockEntity.getPrevTilt() + (blockEntity.getTilt() - blockEntity.getPrevTilt()) * partialTicks));
        poseStack.translate(-tilts[0], -tilts[1], -tilts[2]);
        minecraftRenderModel(poseStack, vertexConsumer, blockState, cachedTiltModel, packedLight, packedOverlay);

        renderLeds(blockEntity, poseStack, partialTicks);
    }

    /** LED de la face en emissif, au niveau de cette image (un flash plus court qu'une image compte). */
    private void renderLeds(AtomicStrobeBlockEntity be, PoseStack poseStack, float partialTicks) {
        MultiBufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer vc = buffers.getBuffer(ExtraLightsRenderTypes.LED_FACE);
        Matrix4f m = poseStack.last().pose();
        double window = AtomicStrobeEngine.FRAME_SECONDS;

        float plate = be.plateLevel(partialTicks, window);
        if (plate > 0f) {
            // Grille de pixels, avec un joint sombre entre eux pour lire la matrice.
            float pw = (LED_X1 - LED_X0) / AtomicStrobeBlockEntity.PIXEL_COLS;
            float gapX = pw * 0.08f;
            for (int plateIdx = 0; plateIdx < 2; plateIdx++) {
                float y0 = plateIdx == 0 ? TOP_Y0 : BOT_Y0;
                float y1 = plateIdx == 0 ? TOP_Y1 : BOT_Y1;
                float ph = (y1 - y0) / AtomicStrobeBlockEntity.PIXEL_ROWS;
                float gapY = ph * 0.1f;
                quad(vc, m, LED_X0, y0, LED_X1, y1, BACKING_Z, 0x14, 0x18, 0x1e);
                for (int row = 0; row < AtomicStrobeBlockEntity.PIXEL_ROWS; row++) {
                    float top = y1 - row * ph;
                    for (int col = 0; col < AtomicStrobeBlockEntity.PIXEL_COLS; col++) {
                        int p = AtomicStrobeBlockEntity.pixelIndex(plateIdx, row, col);
                        float level = plate * be.pixelDim(p) / 255f;
                        if (level <= 0f) {
                            continue;
                        }
                        int r = Math.round(be.pixelRed(p) * level);
                        int g = Math.round(be.pixelGreen(p) * level);
                        int b = Math.round(be.pixelBlue(p) * level);
                        if ((r | g | b) == 0) {
                            continue;
                        }
                        float x0 = LED_X0 + col * pw;
                        quad(vc, m, x0 + gapX, top - ph + gapY, x0 + pw - gapX, top - gapY, FACE_Z, r, g, b);
                    }
                }
            }
        }
        float segW = (LED_X1 - LED_X0) / AtomicStrobeBlockEntity.WHITE_SEGMENT_COUNT;
        for (int i = 0; i < AtomicStrobeBlockEntity.WHITE_SEGMENT_COUNT; i++) {
            float level = be.barSegmentLevel(i, partialTicks, window);
            if (level <= 0f) {
                continue;
            }
            int w = Math.min(255, Math.round(level * 255f));
            quad(vc, m, LED_X0 + i * segW, BAR_Y0, LED_X0 + (i + 1) * segW, BAR_Y1, FACE_Z, w, w, w);
        }
    }

    private static void quad(VertexConsumer vc, Matrix4f m, float x0, float y0, float x1, float y1, float z, int r, int g, int b) {
        vc.vertex(m, x0, y0, z).color(r, g, b, 255).endVertex();
        vc.vertex(m, x1, y0, z).color(r, g, b, 255).endVertex();
        vc.vertex(m, x1, y1, z).color(r, g, b, 255).endVertex();
        vc.vertex(m, x0, y1, z).color(r, g, b, 255).endVertex();
    }

    @Override
    public void beforeRenderBeam(AtomicStrobeBlockEntity blockEntity, PoseStack poseStack, VertexConsumer vertexConsumer,
                                 MultiBufferSource multiBufferSource, Direction facing, float partialTicks,
                                 boolean isFlipped, BlockState blockstate, boolean isHanging,
                                 int packedLight, int packedOverlay) {
        if (!blockEntity.mayLight()) {
            return;
        }
        LazyRenderers.addLazyRender(new LazyRenderers.LazyRenderer() {
            @Override
            public void render(MultiBufferSource.BufferSource bufferSource, PoseStack poseStack, Camera camera, float partialTick) {
                double window = AtomicStrobeEngine.FRAME_SECONDS;
                float bar = blockEntity.barPeakLevel(partialTick, window);
                float plate = blockEntity.plateLevel(partialTick, window) * blockEntity.platePeakBrightness() / 255f;
                float level = Math.max(bar, plate);
                if (level <= 0f) {
                    return;
                }
                // Le tube blanc domine le halo ; la plaque le teinte a hauteur de sa part.
                int colour = mix(0xFFFFFF, blockEntity.plateMeanColour(), plate / (bar + plate));
                // La face est deja dessinee en emissif : seulement le halo.
                renderStrobeFlash(bufferSource, poseStack, camera, blockEntity,
                        pose -> applyHead(blockEntity, pose, facing, partialTick, isFlipped, blockstate, isHanging),
                        StrobeVisualEffects.Face.ATOMIC, colour, level, false);
            }

            @Override
            public Vec3 getPos(float partialTick) {
                return blockEntity.getBlockPos().getCenter();
            }
        });
    }

    private static int mix(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int r = Math.round(((a >> 16) & 0xFF) * (1f - t) + ((b >> 16) & 0xFF) * t);
        int g = Math.round(((a >> 8) & 0xFF) * (1f - t) + ((b >> 8) & 0xFF) * t);
        int bl = Math.round((a & 0xFF) * (1f - t) + (b & 0xFF) * t);
        return (r << 16) | (g << 8) | bl;
    }

    /** preparePoseStack s'arrete avant pan/tilt ; la tete, elle, les applique comme renderModel. */
    private void applyHead(AtomicStrobeBlockEntity blockEntity, PoseStack poseStack, Direction facing,
                           float partialTicks, boolean isFlipped, BlockState blockState, boolean isHanging) {
        preparePoseStack(blockEntity, poseStack, facing, partialTicks, isFlipped, blockState, isHanging);
        float[] pans = blockEntity.getFixture().getPanRotationPosition();
        poseStack.translate(pans[0], pans[1], pans[2]);
        poseStack.mulPose(Axis.YP.rotationDegrees(
                blockEntity.getPrevPan() + (blockEntity.getPan() - blockEntity.getPrevPan()) * partialTicks));
        poseStack.translate(-pans[0], -pans[1], -pans[2]);
        float[] tilts = blockEntity.getFixture().getTiltRotationPosition();
        poseStack.translate(tilts[0], tilts[1], tilts[2]);
        poseStack.mulPose(Axis.XP.rotationDegrees(
                blockEntity.getPrevTilt() + (blockEntity.getTilt() - blockEntity.getPrevTilt()) * partialTicks));
        poseStack.translate(-tilts[0], -tilts[1], -tilts[2]);
    }

    @Override
    public void preparePoseStack(AtomicStrobeBlockEntity blockEntity, PoseStack poseStack, Direction facing,
                                 float partialTicks, boolean isFlipped, BlockState blockState, boolean isHanging) {
        FixtureMountTransform.apply(poseStack, blockEntity);
        placeBlock(blockEntity, poseStack, facing, isFlipped, blockState, isHanging);
    }

    /** Accroche, orientation, decalage de support et retournement : commun au modele et au halo. */
    private static void placeBlock(AtomicStrobeBlockEntity blockEntity, PoseStack poseStack, Direction facing,
                                   boolean isFlipped, BlockState blockState, boolean isHanging) {
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
