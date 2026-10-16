package com.github.dumann089.theatricalextralights.client;

import com.github.dumann089.theatricalextralights.blockentities.ExtraLightsLightBlockEntity;
import com.github.dumann089.theatricalextralights.blockentities.interfaces.SmoothedLight;
import com.github.dumann089.theatricalextralights.util.DmxStrobeFixture;
import dev.imabad.theatrical.blockentities.light.BaseLightBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

/**
 * Intensité rendue — carré pour le strobe, interpolation lisse pour le reste.
 */
public final class StrobeRenderHelper {

    private StrobeRenderHelper() {
    }

    public static float renderedIntensity(BaseLightBlockEntity blockEntity, float partialTick) {
        if (blockEntity instanceof DmxStrobeFixture strobeFixture) {
            return strobeFixture.getRenderedIntensity(partialTick);
        }
        return displayIntensity(blockEntity, partialTick);
    }

    /**
     * Intensidade (0-255) pra desenhar, já suavizada. Fixtures do Extra Lights usam o valor com
     * shutter/strobe por cima; qualquer outra BaseLightBlockEntity usa o suavizado do mixin e, sem
     * mixin, a interpolação original de 1 tick.
     */
    public static float displayIntensity(BaseLightBlockEntity blockEntity, float partialTick) {
        if (blockEntity instanceof ExtraLightsLightBlockEntity extra) {
            return extra.getDisplayIntensity(partialTick);
        }
        if (blockEntity instanceof SmoothedLight smoothed) {
            return smoothed.getDisplayIntensity(partialTick);
        }
        return blockEntity.getPrevIntensity()
                + (blockEntity.getIntensity() - blockEntity.getPrevIntensity()) * partialTick;
    }

    /** Cor RGB (0xRRGGBB) pra desenhar, suavizada quando possível; sem mixin, a cor crua de sempre. */
    public static int displayColour(BaseLightBlockEntity blockEntity, float partialTick) {
        if (blockEntity instanceof ExtraLightsLightBlockEntity extra) {
            return extra.getDisplayColour(partialTick);
        }
        if (blockEntity instanceof SmoothedLight smoothed) {
            return smoothed.getDisplayColour(partialTick);
        }
        return blockEntity.getColour();
    }

    /** True si le faisceau / la lumière dynamique doivent encore être actifs. */
    public static boolean isVisuallyLit(BaseLightBlockEntity blockEntity) {
        if (blockEntity == null) {
            return false;
        }
        if (blockEntity instanceof DmxStrobeFixture strobe) {
            if (strobe.getRawDimmer() <= 0) {
                return false;
            }
            return strobe.getStrobeChannelValue() > 0;
        }
        return blockEntity.getIntensity() > 0 || blockEntity.getPrevIntensity() > 0
                || displayIntensity(blockEntity, 1f) > 0.5f;
    }

    /** Force Sodium / vanilla à rafraîchir le chunk (strobe, faisceaux, lentilles). */
    public static void markSectionDirty(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.levelRenderer != null) {
            mc.levelRenderer.setSectionDirtyWithNeighbors(pos.getX(), pos.getY(), pos.getZ());
        }
    }
}
