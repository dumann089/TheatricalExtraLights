package com.github.dumann089.theatricalextralights.compat.fabric;

import com.github.dumann089.theatricalextralights.compat.FireworkLightUpdateQueue;
import dev.imabad.theatrical.api.DynamicLightProvider;
import net.minecraft.core.BlockPos;

public final class FireworkLightCompatImpl {

    private FireworkLightCompatImpl() {
    }

    public static void sync(DynamicLightProvider provider) {
        FireworkLightUpdateQueue.queueSync(provider);
    }

    public static void remove(DynamicLightProvider provider) {
        FireworkLightUpdateQueue.queueRemove(provider.getOwnerPos());
    }

    public static void removeAt(BlockPos pos) {
        FireworkLightUpdateQueue.queueRemove(pos);
    }

    public static void flushPending() {
        // Vaciamos la cola consumiendo las actualizaciones sin aplicar ninguna lógica de Shimmer
        FireworkLightUpdateQueue.drain(update -> {});
    }
}