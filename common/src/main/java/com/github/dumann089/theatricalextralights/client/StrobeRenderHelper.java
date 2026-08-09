package com.github.dumann089.theatricalextralights.client;

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
        return blockEntity.getPrevIntensity()
                + (blockEntity.getIntensity() - blockEntity.getPrevIntensity()) * partialTick;
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
        return blockEntity.getIntensity() > 0 || blockEntity.getPrevIntensity() > 0;
    }

    /** Force Sodium / vanilla à rafraîchir le chunk (strobe, faisceaux, lentilles). */
    public static void markSectionDirty(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.levelRenderer != null) {
            mc.levelRenderer.setSectionDirtyWithNeighbors(pos.getX(), pos.getY(), pos.getZ());
        }
    }
}
