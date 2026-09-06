package com.github.dumann089.theatricalextralights.firework;

import com.github.dumann089.theatricalextralights.config.TheatricalExtraLightsConfig;
import com.github.dumann089.theatricalextralights.entities.FireworkRocketEntity;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Limits concurrent firework rockets and reclaims orphans.
 * Avoids world-sized {@code getEntitiesOfClass} scans (was ~50% of server tick).
 */
public final class FireworkRocketTracker {
    private static final int ABSOLUTE_MAX_TICKS = 420;
    /** Full cleanup at most once every 2s per dimension. */
    private static final int CLEANUP_INTERVAL_TICKS = 40;

    private static final Map<ResourceKey<Level>, AtomicInteger> ACTIVE_COUNTS = new ConcurrentHashMap<>();

    private FireworkRocketTracker() {
    }

    public static void registerEvents() {
        TickEvent.SERVER_LEVEL_POST.register(FireworkRocketTracker::onServerLevelTick);
    }

    private static void onServerLevelTick(ServerLevel level) {
        // Cheap early-outs: most ticks do nothing.
        if (level.getGameTime() % CLEANUP_INTERVAL_TICKS != 0L) {
            return;
        }
        if (getActiveCount(level) <= 0 && !hasAnyLoadedRocket(level)) {
            activeCounter(level).set(0);
            return;
        }
        // No players → nothing to keep alive for; discard all loaded rockets cheaply.
        if (level.players().isEmpty()) {
            discardAllLoaded(level);
            activeCounter(level).set(0);
            return;
        }
        cleanupOrphans(level);
        reconcile(level);
        cleanupOrphans(level);
    }

    public static boolean tryRegisterLaunch(Level level) {
        int max = TheatricalExtraLightsConfig.getMaxConcurrentRockets();
        if (max <= 0) {
            return true;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return true;
        }

        int count = getActiveCount(serverLevel);
        if (count >= max) {
            purgeExpired(serverLevel);
            count = getActiveCount(serverLevel);
        } else if (count > (max * 4) / 5) {
            purgeFinished(serverLevel);
            reconcile(serverLevel);
            count = getActiveCount(serverLevel);
        }

        return count < max;
    }

    public static void registerLaunch(ServerLevel level) {
        activeCounter(level).incrementAndGet();
    }

    public static void cancelLaunch(Level level) {
    }

    public static void onRemoved(ServerLevel level) {
        activeCounter(level).updateAndGet(value -> Math.max(0, value - 1));
    }

    public static int countActive(ServerLevel level) {
        return collectLoadedRockets(level).size();
    }

    /** Reclaim rockets that flew too far/high or linger without nearby players. */
    public static void cleanupOrphans(ServerLevel level) {
        for (FireworkRocketEntity rocket : collectLoadedRockets(level)) {
            if (rocket.shouldForceCleanup(level)) {
                rocket.discard();
            }
        }
    }

    public static void purgeFinished(ServerLevel level) {
        for (FireworkRocketEntity rocket : collectLoadedRockets(level)) {
            int hold = rocket.getPreset().getPattern().getServerHoldTicks();
            if (rocket.tickCount > hold || rocket.tickCount > ABSOLUTE_MAX_TICKS || rocket.shouldForceCleanup(level)) {
                rocket.discard();
            }
        }
        reconcile(level);
    }

    public static void purgeExpired(ServerLevel level) {
        List<FireworkRocketEntity> rockets = collectLoadedRockets(level);
        // Drop finished first without re-scanning.
        rockets.removeIf(rocket -> {
            int hold = rocket.getPreset().getPattern().getServerHoldTicks();
            boolean drop = rocket.tickCount > hold
                    || rocket.tickCount > ABSOLUTE_MAX_TICKS
                    || rocket.shouldForceCleanup(level);
            if (drop) {
                rocket.discard();
            }
            return drop;
        });

        int max = TheatricalExtraLightsConfig.getMaxConcurrentRockets();
        if (max > 0 && rockets.size() > max) {
            rockets.sort(Comparator.comparingInt(r -> r.tickCount));
            int excess = rockets.size() - max;
            for (int i = 0; i < excess; i++) {
                rockets.get(i).discard();
            }
        }
        reconcile(level);
    }

    public static boolean hasNearbyPlayer(ServerLevel level, FireworkRocketEntity rocket) {
        return FireworkRenderDistances.isPlayerNearFirework(level, rocket);
    }

    private static int getActiveCount(ServerLevel level) {
        return activeCounter(level).get();
    }

    private static void reconcile(ServerLevel level) {
        activeCounter(level).set(collectLoadedRockets(level).size());
    }

    private static AtomicInteger activeCounter(ServerLevel level) {
        return ACTIVE_COUNTS.computeIfAbsent(level.dimension(), key -> new AtomicInteger());
    }

    /**
     * Iterate only currently loaded entities — never a world-sized AABB scan.
     */
    private static List<FireworkRocketEntity> collectLoadedRockets(ServerLevel level) {
        List<FireworkRocketEntity> rockets = new ArrayList<>(Math.max(16, getActiveCount(level)));
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof FireworkRocketEntity rocket && rocket.isAlive()) {
                rockets.add(rocket);
            }
        }
        return rockets;
    }

    private static boolean hasAnyLoadedRocket(ServerLevel level) {
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof FireworkRocketEntity) {
                return true;
            }
        }
        return false;
    }

    private static void discardAllLoaded(ServerLevel level) {
        for (FireworkRocketEntity rocket : collectLoadedRockets(level)) {
            rocket.discard();
        }
    }
}
