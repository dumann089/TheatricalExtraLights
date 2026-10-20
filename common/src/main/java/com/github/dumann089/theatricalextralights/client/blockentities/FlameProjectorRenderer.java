package com.github.dumann089.theatricalextralights.client.blockentities;

import com.github.dumann089.theatricalextralights.blockentities.FlameProjectorBlockEntity;
import com.github.dumann089.theatricalextralights.client.render.pyro.FlameVolumeRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Flame Projector : le bloc reste un modele statique, la colonne de flamme part du dessus de
 * l'appareil vers le haut, en volume ({@link FlameVolumeRenderer}).
 */
public class FlameProjectorRenderer extends StaticFixtureRenderer<FlameProjectorBlockEntity> {

    /** Sortie de la buse : dessus du modele (10/16 de bloc), au centre. */
    private static final Vec3 NOZZLE = new Vec3(0.5, 10.0 / 16.0, 0.5);
    private static final Vec3 UP = new Vec3(0.0, 1.0, 0.0);

    public FlameProjectorRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void beforeRenderBeam(FlameProjectorBlockEntity blockEntity, PoseStack poseStack, VertexConsumer vertexConsumer,
                                 MultiBufferSource multiBufferSource, Direction facing, float partialTicks,
                                 boolean isFlipped, BlockState blockState, boolean isHanging,
                                 int packedLight, int packedOverlay) {
        if (!FlameVolumeRenderer.isAvailable()) {
            return;
        }
        float level = Math.max(0f, Math.min(255f, blockEntity.getIntensity())) / 255f;
        float length = 1.2f + 4.8f * Math.max(0, Math.min(255, blockEntity.getFlameLengthRaw())) / 255f;
        FlameVolumeRenderer.submit(
                blockEntity.getBlockPos(),
                Vec3.atLowerCornerOf(blockEntity.getBlockPos()).add(NOZZLE),
                UP,
                length,
                blockEntity.getFlameStyle(),
                0.55f + 0.45f * level,
                blockEntity.getIntensity() > 0,
                partialTicks);
    }

    @Override
    public boolean shouldRenderOffScreen(FlameProjectorBlockEntity blockEntity) {
        return true;
    }
}
