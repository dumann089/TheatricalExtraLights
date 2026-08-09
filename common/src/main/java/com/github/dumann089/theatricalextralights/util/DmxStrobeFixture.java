package com.github.dumann089.theatricalextralights.util;

/**
 * Fixtures avec canal strobe DMX — rendu sans interpolation linéaire (évite 50 % avec Sodium).
 */
public interface DmxStrobeFixture {

    int getRawDimmer();

    int getStrobeChannelValue();

    long getStrobeGameTime();

    default float getRenderedIntensity(float partialTick) {
        return DmxShutterStrobeHelper.computeEffectiveIntensity(
                getRawDimmer(),
                getStrobeChannelValue(),
                getStrobeGameTime(),
                partialTick
        );
    }

    default boolean shouldForceStrobeRepaint() {
        return DmxShutterStrobeHelper.isStrobing(getStrobeChannelValue());
    }
}
