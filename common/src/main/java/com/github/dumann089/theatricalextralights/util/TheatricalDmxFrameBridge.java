package com.github.dumann089.theatricalextralights.util;

import net.minecraft.core.BlockPos;

/**
 * Optional hook into Theatrical's batched {@code DmxFrameSync} (soft dependency — compiles without it).
 */
public final class TheatricalDmxFrameBridge {

    private static final String SYNC_CLASS = "dev.imabad.theatrical.networks.dmxframe.DmxFrameSync";
    private static final String CONFIG_CLASS = "dev.imabad.theatrical.config.TheatricalConfig";

    private TheatricalDmxFrameBridge() {
    }

    /** Returns true if the fixture was queued for batched sync instead of vanilla block entity packets. */
    public static boolean markDirtyIfBatchEnabled(BlockPos pos) {
        if (pos == null) {
            return false;
        }
        try {
            Class<?> configClass = Class.forName(CONFIG_CLASS);
            Object instance = configClass.getField("INSTANCE").get(null);
            Object common = instance.getClass().getField("COMMON").get(instance);
            if (!common.getClass().getField("batchDmxFrameSync").getBoolean(common)) {
                return false;
            }
            Class<?> syncClass = Class.forName(SYNC_CLASS);
            syncClass.getMethod("markDirty", BlockPos.class).invoke(null, pos.immutable());
            return true;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }
}
