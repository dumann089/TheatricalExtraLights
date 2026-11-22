package com.github.dumann089.theatricalextralights.blockentities.interfaces;

import net.minecraft.util.Mth;

public interface ManualGoboSettings extends HasGobo {

    int getManualGobo();

    int getManualZoom();

    void setManualGoboSettings(int gobo, int zoom);

    /** Continuous position of the gobo wheel. Manual fixtures default to the selected slot. */
    default float getManualGoboPosition() {
        return getManualGobo();
    }

    /** True when the wheel position is driven continuously by a DMX scroller channel. */
    default boolean isGoboScrollDmxControlled() {
        return false;
    }

    default float getManualZoomScale(float partialTicks) {
        float zoom = Mth.clamp(getPartialZoom(partialTicks) / 255.0F, 0.0F, 1.0F);
        return 1.0F - (0.75F * zoom);
    }

    /** Exact same half-angle is used by the GPU gobo projection and volumetric raymarch beam. */
    default float getManualZoomedHalfAngle(float fieldAngle, float partialTicks) {
        return Mth.clamp(fieldAngle * 0.5F * getManualZoomScale(partialTicks), 0.5F, 89.0F);
    }
}
