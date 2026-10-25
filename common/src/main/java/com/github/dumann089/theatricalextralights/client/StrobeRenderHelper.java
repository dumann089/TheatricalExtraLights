package com.github.dumann089.theatricalextralights.client;

import com.github.dumann089.theatricalextralights.blockentities.ExtraLightsLightBlockEntity;
import com.github.dumann089.theatricalextralights.blockentities.interfaces.SmoothedLight;
import com.github.dumann089.theatricalextralights.util.DmxStrobeFixture;
import dev.imabad.theatrical.blockentities.light.BaseLightBlockEntity;
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

    /**
     * Ancien point d'invalidation de chunk des fixtures actives, appele a chaque trame DMX.
     *
     * <p>Il passait des coordonnees de bloc a {@code LevelRenderer.setSectionDirtyWithNeighbors},
     * qui attend des coordonnees de section : il marquait 27 sections sans rapport avec la
     * fixture (hors du monde sous Embeddium, donc sans effet ; reparties au hasard dans la vue
     * sous le rendu vanilla, qui les reconstruisait pour rien). La section de la fixture n'a
     * jamais ete marquee, et le rendu des block entities tourne a chaque image sans
     * reconstruction : l'appel ne changeait donc rien a l'image, il ne coutait que du temps.
     * Garde comme point d'entree unique au cas ou une vraie invalidation deviendrait utile.
     */
    public static void markSectionDirty(BlockPos pos) {
    }
}
