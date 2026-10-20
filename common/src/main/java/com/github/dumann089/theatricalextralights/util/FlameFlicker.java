package com.github.dumann089.theatricalextralights.util;

import com.github.dumann089.theatricalextralights.config.TheatricalExtraLightsConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Lumiere vacillante des machines a flammes. La luminance dynamique ne change qu'une fois
 * toutes les {@link #STEP_TICKS} ticks : chaque changement fait reconstruire les sections de
 * chunk autour de la source (lumiere dynamique de Theatrical), il ne faut pas en faire a
 * chaque image.
 */
public final class FlameFlicker {

    private static final int STEP_TICKS = 3;

    private FlameFlicker() {
    }

    /** Luminance 0..15 vacillante autour de {@code base} (70 a 100 %), cote client uniquement. */
    public static int luminance(Level level, BlockPos pos, int base) {
        if (base <= 0 || level == null || !level.isClientSide
                || !TheatricalExtraLightsConfig.isFlameLightFlickerEnabled()) {
            return base;
        }
        long step = level.getGameTime() / STEP_TICKS;
        long h = pos.asLong() * 0x9E3779B97F4A7C15L + step * 0xC2B2AE3D27D4EB4FL;
        h ^= (h >>> 29);
        h *= 0xBF58476D1CE4E5B9L;
        h ^= (h >>> 32);
        float r = (h & 0xFFFF) / 65535f;
        return Math.max(1, Math.round(base * (0.7f + 0.3f * r)));
    }
}
