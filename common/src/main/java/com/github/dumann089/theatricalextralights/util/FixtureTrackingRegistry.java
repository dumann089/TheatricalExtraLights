package com.github.dumann089.theatricalextralights.util;

import com.github.dumann089.theatricalextralights.blockentities.ExtraLightsLightBlockEntity;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Drives the Fixture Tracker's aiming every server tick.
 *
 * <p>The addon gives every concrete fixture its own static {@code tick(Level, BlockPos, BlockState, T)},
 * wired straight to that fixture's block {@code getTicker} (see e.g. {@code AtomicStrobeBlockEntity}) -
 * {@code ExtraLightsLightBlockEntity} has no common per-instance ticker to hook into, and the published
 * Theatrical artifact this addon builds against does not expose one either. So instead of touching every
 * fixture class, this mirrors {@link com.github.dumann089.theatricalextralights.firework.FireworkRocketTracker}:
 * a small registry of block positions, driven by its own {@code TickEvent.SERVER_LEVEL_POST}.
 */
public final class FixtureTrackingRegistry {
    private static final Map<ResourceKey<Level>, Set<BlockPos>> TRACKING = new ConcurrentHashMap<>();

    private FixtureTrackingRegistry() {
    }

    public static void registerEvents() {
        TickEvent.SERVER_LEVEL_POST.register(FixtureTrackingRegistry::onServerLevelTick);
    }

    /** Called from {@code ExtraLightsLightBlockEntity#setTrackedEntity} and on NBT load. Idempotent. */
    public static void setTracking(ServerLevel level, BlockPos pos, boolean tracking) {
        Set<BlockPos> positions = positionsFor(level);
        if (tracking) {
            positions.add(pos.immutable());
        } else {
            positions.remove(pos);
        }
    }

    private static void onServerLevelTick(ServerLevel level) {
        Set<BlockPos> positions = TRACKING.get(level.dimension());
        if (positions == null || positions.isEmpty()) {
            return;
        }
        // Snapshot: a fixture may lose its target (and unregister) while we iterate.
        for (BlockPos pos : positions.toArray(new BlockPos[0])) {
            if (!level.isLoaded(pos)) {
                continue;
            }
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (!(blockEntity instanceof ExtraLightsLightBlockEntity fixture) || fixture.getTrackedEntity() == null) {
                positions.remove(pos);
                continue;
            }
            FixtureTrackingHelper.tick(fixture, level, fixture.getTrackedEntity());
        }
    }

    private static Set<BlockPos> positionsFor(ServerLevel level) {
        return TRACKING.computeIfAbsent(level.dimension(), key -> ConcurrentHashMap.newKeySet());
    }
}
