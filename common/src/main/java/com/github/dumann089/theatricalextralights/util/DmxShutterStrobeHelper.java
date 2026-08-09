package com.github.dumann089.theatricalextralights.util;

/**
 * Canal strobe blinder :
 * 0 = fermé, 1-254 = clignotement (vitesse croissante), 255 = ouvert en continu.
 */
public final class DmxShutterStrobeHelper {

    private static final int OPEN = 255;
    private static final int SLOWEST_HALF_PERIOD = 20;
    private static final int FASTEST_HALF_PERIOD = 1;

    private DmxShutterStrobeHelper() {
    }

    public static boolean isStrobing(int strobe) {
        return strobe > 0 && strobe < OPEN;
    }

    public static float computeEffectiveIntensity(int dimmer, int strobe, long gameTime) {
        return computeEffectiveIntensity(dimmer, strobe, gameTime, 0.0f);
    }

    public static float computeEffectiveIntensity(int dimmer, int strobe, long gameTime, float partialTick) {
        if (strobe <= 0) {
            return 0f;
        }
        if (strobe >= OPEN) {
            return dimmer;
        }
        return isStrobePhaseOn(strobe, gameTime, partialTick) ? dimmer : 0f;
    }

    private static boolean isStrobePhaseOn(int strobe, long gameTime, float partialTick) {
        float speed = strobe / (OPEN - 1f);
        int halfPeriod = Math.max(
                FASTEST_HALF_PERIOD,
                Math.round(SLOWEST_HALF_PERIOD - speed * (SLOWEST_HALF_PERIOD - FASTEST_HALF_PERIOD))
        );
        long phase = (long) Math.floor((gameTime + partialTick) / halfPeriod);
        return phase % 2 == 0;
    }
}
