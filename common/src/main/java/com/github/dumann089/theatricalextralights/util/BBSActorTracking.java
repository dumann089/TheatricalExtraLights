
package com.github.dumann089.theatricalextralights.util;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

public final class BBSActorTracking {

    private static final String ACTOR_CLASS = "mchorse.bbs_mod.entity.ActorEntity";
    private static final long SEARCH_INTERVAL_TICKS = 10L;

    private static final Map<ServerLevel, Map<Identity, CachedActor>> CACHE =
            Collections.synchronizedMap(new WeakHashMap<>());

    private BBSActorTracking() {
    }

    public record Identity(String filmId, String replayId) {
    }

    private static final class CachedActor {
        private WeakReference<Entity> entity = new WeakReference<>(null);
        private long nextSearchTick;
    }

    /**
     * Obtiene la identidad estable de un Actor de BBS FS.
     * No requiere que BBS FS esté instalado.
     */
    public static Identity getIdentity(Entity entity) {
        if (entity == null || !isActorEntity(entity)) {
            return null;
        }

        try {
            Object filmValue = entity.getClass()
                    .getMethod("getFilmId")
                    .invoke(entity);

            Object replayValue = entity.getClass()
                    .getMethod("getReplayId")
                    .invoke(entity);

            if (!(filmValue instanceof String filmId)
                    || !(replayValue instanceof String replayId)
                    || filmId.isEmpty()
                    || replayId.isEmpty()) {
                return null;
            }

            return new Identity(filmId, replayId);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return null;
        }
    }

    private static boolean isActorEntity(Entity entity) {
        Class<?> type = entity.getClass();

        while (type != null) {
            if (ACTOR_CLASS.equals(type.getName())) {
                return true;
            }

            type = type.getSuperclass();
        }

        return false;
    }

    /**
     * Busca una nueva instancia del mismo Actor dentro del mundo.
     * Las búsquedas se limitan a una por identidad cada 10 ticks.
     */
    public static Entity findActor(ServerLevel level, Identity identity) {
        if (level == null || identity == null) {
            return null;
        }

        CachedActor cached;

        synchronized (CACHE) {
            Map<Identity, CachedActor> levelCache =
                    CACHE.computeIfAbsent(level, key -> new HashMap<>());

            cached = levelCache.computeIfAbsent(
                    identity,
                    key -> new CachedActor()
            );
        }

        Entity previous = cached.entity.get();

        if (previous != null
                && previous.level() == level
                && !previous.isRemoved()
                && previous.isAlive()) {
            return previous;
        }

        long gameTime = level.getGameTime();

        if (gameTime < cached.nextSearchTick) {
            return null;
        }

        cached.nextSearchTick = gameTime + SEARCH_INTERVAL_TICKS;

        for (Entity entity : level.getAllEntities()) {
            if (entity == null || entity.isRemoved() || !entity.isAlive()) {
                continue;
            }

            Identity candidate = getIdentity(entity);

            if (identity.equals(candidate)) {
                cached.entity = new WeakReference<>(entity);
                return entity;
            }
        }

        cached.entity = new WeakReference<>(null);
        return null;
    }
}
