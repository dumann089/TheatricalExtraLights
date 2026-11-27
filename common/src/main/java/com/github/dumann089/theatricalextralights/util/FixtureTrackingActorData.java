
package com.github.dumann089.theatricalextralights.util;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

public class FixtureTrackingActorData extends SavedData {

    private static final String DATA_NAME =
            "theatricalextralights_fixture_tracking";

    private static final String TAG_ACTORS = "Actors";
    private static final String TAG_POS = "Pos";
    private static final String TAG_FILM = "Film";
    private static final String TAG_REPLAY = "Replay";

    private final Map<Long, BBSActorTracking.Identity> actors =
            new HashMap<>();

    public static FixtureTrackingActorData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                FixtureTrackingActorData::load,
                FixtureTrackingActorData::new,
                DATA_NAME
        );
    }

    public BBSActorTracking.Identity get(BlockPos pos) {
        return actors.get(pos.asLong());
    }

    public void set(
            BlockPos pos,
            BBSActorTracking.Identity identity
    ) {
        long key = pos.asLong();

        if (identity == null) {
            remove(pos);
            return;
        }

        BBSActorTracking.Identity previous = actors.put(key, identity);

        if (!identity.equals(previous)) {
            setDirty();
        }
    }

    public void remove(BlockPos pos) {
        if (actors.remove(pos.asLong()) != null) {
            setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();

        for (Map.Entry<Long, BBSActorTracking.Identity> entry : actors.entrySet()) {
            CompoundTag actorTag = new CompoundTag();
            BBSActorTracking.Identity identity = entry.getValue();

            actorTag.putLong(TAG_POS, entry.getKey());
            actorTag.putString(TAG_FILM, identity.filmId());
            actorTag.putString(TAG_REPLAY, identity.replayId());

            list.add(actorTag);
        }

        tag.put(TAG_ACTORS, list);
        return tag;
    }

    public static FixtureTrackingActorData load(CompoundTag tag) {
        FixtureTrackingActorData data = new FixtureTrackingActorData();

        ListTag list = tag.getList(TAG_ACTORS, Tag.TAG_COMPOUND);

        for (int i = 0; i < list.size(); i++) {
            CompoundTag actorTag = list.getCompound(i);

            String filmId = actorTag.getString(TAG_FILM);
            String replayId = actorTag.getString(TAG_REPLAY);

            if (filmId.isEmpty() || replayId.isEmpty()) {
                continue;
            }

            data.actors.put(
                    actorTag.getLong(TAG_POS),
                    new BBSActorTracking.Identity(filmId, replayId)
            );
        }

        return data;
    }
}