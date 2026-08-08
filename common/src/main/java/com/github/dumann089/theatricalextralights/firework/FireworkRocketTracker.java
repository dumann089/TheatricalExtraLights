package com.github.dumann089.theatricalextralights.firework;

import com.github.dumann089.theatricalextralights.config.TheatricalExtraLightsConfig;
import com.github.dumann089.theatricalextralights.entities.FireworkRocketEntity;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Limits concurrent firework rockets and reclaims orphans that stack in sky chunks.
 */
public final class FireworkRocketTracker {
    private static final AABB WORLD_BOUNDS = new AABB(-3.0E7, -64, -3.0E7, 3.0E7, 320, 3.0E7);
    private static final int ABSOLUTE_MAX_TICKS = 420;
    private static final double PLAYER_KEEP_RANGE = 160.0;
    private static final double RECONCILE_RANGE = 192.0;

    private static final Map<ResourceKey<Level>, AtomicInteger> ACTIVE_COUNTS = new ConcurrentHashMap<>();

    private FireworkRocketTracker() {
    }

    public static void registerEvents() {
        TickEvent.SERVER_LEVEL_POST.register(FireworkRocketTracker::onServerLevelTick);
    }

    private static void onServerLevelTick(ServerLevel level) {
        long time = level.getGameTime();
        if (time % 20L != 0L) {
            return;
        }
        // ── Fast path: sin cohetes activos, no hay nada que reconciliar ──────
        // reconcile()/cleanupOrphans() escanean entidades en un AABB de ±192
        // bloques por jugador (o el mundo entero sin jugadores) — un escaneo
        // de centenares de chunks. Si el contador propio ya está en 0, ese
        // escaneo no puede encontrar nada nuevo en el caso normal. Igual
        // hacemos una reconciliación de baja frecuencia (cada 600 ticks ≈ 30s)
        // como red de seguridad por si el contador se desincronizó (ej. un
        // cohete creado por otra vía que no pasó por registerLaunch).
        boolean periodicSafetyCheck = time % 600L == 0L;
        if (getActiveCount(level) == 0 && !periodicSafetyCheck) {
            return;
        }
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
        return scanRockets(level, showBounds(level));
    }

    /** Reclaim rockets that flew too far/high or linger without nearby players. */
    public static void cleanupOrphans(ServerLevel level) {
        for (FireworkRocketEntity rocket : List.copyOf(level.getEntitiesOfClass(FireworkRocketEntity.class, showBounds(level)))) {
            if (rocket.shouldForceCleanup(level)) {
                rocket.discard();
            }
        }
    }

    public static void purgeFinished(ServerLevel level) {
        for (FireworkRocketEntity rocket : level.getEntitiesOfClass(FireworkRocketEntity.class, showBounds(level))) {
            int hold = rocket.getPreset().getPattern().getServerHoldTicks();
            if (rocket.tickCount > hold || rocket.tickCount > ABSOLUTE_MAX_TICKS || rocket.shouldForceCleanup(level)) {
                rocket.discard();
            }
        }
    }

    public static void purgeExpired(ServerLevel level) {
        purgeFinished(level);

        int max = TheatricalExtraLightsConfig.getMaxConcurrentRockets();
        if (max <= 0) {
            return;
        }

        List<FireworkRocketEntity> rockets = new ArrayList<>(
                level.getEntitiesOfClass(FireworkRocketEntity.class, showBounds(level))
        );
        while (rockets.size() > max) {
            rockets.sort(Comparator.comparingInt(r -> r.tickCount));
            if (rockets.isEmpty()) {
                break;
            }
            rockets.remove(0).discard();
            rockets = new ArrayList<>(level.getEntitiesOfClass(FireworkRocketEntity.class, showBounds(level)));
        }
        reconcile(level);
    }

    public static boolean hasNearbyPlayer(ServerLevel level, FireworkRocketEntity rocket) {
        AABB box = rocket.getBoundingBox().inflate(PLAYER_KEEP_RANGE);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, box)) {
            if (!player.isSpectator() && !player.isDeadOrDying()) {
                return true;
            }
        }
        return false;
    }

    private static int getActiveCount(ServerLevel level) {
        return activeCounter(level).get();
    }

    private static void reconcile(ServerLevel level) {
        activeCounter(level).set(scanRockets(level, showBounds(level)));
    }

    private static AtomicInteger activeCounter(ServerLevel level) {
        return ACTIVE_COUNTS.computeIfAbsent(level.dimension(), key -> new AtomicInteger());
    }

    private static AABB showBounds(ServerLevel level) {
        if (level.players().isEmpty()) {
            return WORLD_BOUNDS;
        }
        AABB combined = null;
        for (ServerPlayer player : level.players()) {
            AABB box = player.getBoundingBox().inflate(RECONCILE_RANGE);
            combined = combined == null ? box : combined.minmax(box);
        }
        return combined == null ? WORLD_BOUNDS : combined;
    }

    private static int scanRockets(ServerLevel level, AABB bounds) {
        return level.getEntitiesOfClass(FireworkRocketEntity.class, bounds).size();
    }
}