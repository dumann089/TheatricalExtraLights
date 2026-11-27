
package com.github.dumann089.theatricalextralights.util;

import com.github.dumann089.theatricalextralights.blockentities.ExtraLightsLightBlockEntity;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class FixtureTrackingRegistry {

    private static final Map<ResourceKey<Level>, Set<BlockPos>> TRACKING =
            new ConcurrentHashMap<>();

    private FixtureTrackingRegistry() {
    }

    public static void registerEvents() {
        TickEvent.SERVER_LEVEL_POST.register(
                FixtureTrackingRegistry::onServerLevelTick
        );
    }

    public static void setTracking(
            ServerLevel level,
            BlockPos pos,
            boolean tracking
    ) {
        Set<BlockPos> positions = positionsFor(level);

        if (tracking) {
            positions.add(pos.immutable());
        } else {
            positions.remove(pos);
        }
    }

    /**
     * Guarda la identidad de BBS FS asociada al fixture.
     * Si el objetivo no es un Actor de BBS, elimina cualquier identidad anterior.
     */
    public static void setActorTarget(
            ServerLevel level,
            BlockPos pos,
            Entity target
    ) {
        BBSActorTracking.Identity identity =
                BBSActorTracking.getIdentity(target);

        FixtureTrackingActorData.get(level).set(pos, identity);
    }

    public static void clearActorTarget(
            ServerLevel level,
            BlockPos pos
    ) {
        FixtureTrackingActorData.get(level).remove(pos);
    }

    private static void onServerLevelTick(ServerLevel level) {
        Set<BlockPos> positions = TRACKING.get(level.dimension());

        if (positions == null || positions.isEmpty()) {
            return;
        }

        for (BlockPos pos : positions.toArray(new BlockPos[0])) {
            if (!level.isLoaded(pos)) {
                continue;
            }

            BlockEntity blockEntity = level.getBlockEntity(pos);

            if (!(blockEntity instanceof ExtraLightsLightBlockEntity fixture)) {
                positions.remove(pos);
                clearActorTarget(level, pos);
                continue;
            }

            if (!fixture.supportsTracking()) {
                fixture.setTrackedEntity(null);
                clearActorTarget(level, pos);
                positions.remove(pos);
                continue;
            }

            UUID targetId = fixture.getTrackedEntity();

            if (targetId == null) {
                positions.remove(pos);
                clearActorTarget(level, pos);
                continue;
            }

            Entity target = level.getEntity(targetId);

            if (target == null || target.isRemoved() || !target.isAlive()) {
                BBSActorTracking.Identity identity =
                        FixtureTrackingActorData.get(level).get(pos);

                if (identity == null) {
                    continue;
                }

                Entity replacement =
                        BBSActorTracking.findActor(level, identity);

                if (replacement == null
                        || replacement.isRemoved()
                        || !replacement.isAlive()) {
                    continue;
                }

                /*
                 * setTrackedEntity() inicializa sus ángulos desde pan/tilt.
                 * Guardamos los valores float anteriores y los restauramos
                 * para que el cambio de UUID no provoque un salto angular.
                 */
                float previousPan = fixture.hasTrackAngles()
                        ? fixture.getTrackPan()
                        : fixture.getPan();

                float previousTilt = fixture.hasTrackAngles()
                        ? fixture.getTrackTilt()
                        : fixture.getTilt();

                fixture.setTrackedEntity(replacement.getUUID());

                fixture.applyTrackedAngles(
                        previousPan,
                        previousTilt,
                        true
                );

                target = replacement;
            } else {
                /*
                 * También aprende la identidad si el fixture ya estaba
                 * siguiendo un Actor antes de instalar esta integración.
                 */
                BBSActorTracking.Identity identity =
                        BBSActorTracking.getIdentity(target);

                if (identity != null) {
                    FixtureTrackingActorData.get(level).set(pos, identity);
                }
            }

            FixtureTrackingHelper.tick(
                    fixture,
                    level,
                    target.getUUID()
            );
        }
    }

    private static Set<BlockPos> positionsFor(ServerLevel level) {
        return TRACKING.computeIfAbsent(
                level.dimension(),
                key -> ConcurrentHashMap.newKeySet()
        );
    }
}
