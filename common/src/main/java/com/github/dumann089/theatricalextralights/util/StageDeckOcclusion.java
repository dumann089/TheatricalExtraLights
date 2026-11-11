package com.github.dumann089.theatricalextralights.util;

import com.github.dumann089.theatricalextralights.config.TheatricalExtraLightsConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

/**
 * Keeps a downward beam from carrying on through a stage deck when the exact block under the
 * fixture is missing.
 *
 * <p>SEF stage platforms are assembled from 3x3 decks; builds often end up with a one- or
 * three-block gap between two platforms or at a stair opening. The ray trace correctly goes
 * through the gap, so the volumetric beam is allowed to continue into the understage. This
 * helper treats a nearby deck as a horizontal light barrier: if there are enough stage-deck
 * blocks around the beam at the height it crosses, the beam is cut at that deck surface.
 *
 * <p>Only downward beams whose origin is above the deck are affected. The result is cached per
 * tick so this is not a per-fragment cost.
 */
public final class StageDeckOcclusion {

    private static final int SEARCH_RADIUS = 3;
    private static final int MIN_DECK_BLOCKS = 6;

    private record Memo(long tick, Vec3 origin, Vec3 dir, float length, float clamped) {}

    private static final Map<BlockPos, Memo> CACHE = new HashMap<>();

    private StageDeckOcclusion() {
    }

    public static float clampBeamLength(Level level, BlockPos fixturePos, Vec3 origin, Vec3 dir, float length) {
        if (level == null || fixturePos == null || length <= 0.0f || dir.y > -0.02
                || !TheatricalExtraLightsConfig.isStopBeamsAtStageDecks()) {
            return length;
        }

        long tick = level.getGameTime();
        Memo memo = CACHE.get(fixturePos);
        if (memo != null && memo.tick == tick && memo.length == length
                && memo.origin.equals(origin) && memo.dir.equals(dir)) {
            return memo.clamped;
        }

        float clamped = findDeckPlane(level, origin, dir, length);
        CACHE.put(fixturePos.immutable(), new Memo(tick, origin, dir, length, clamped));
        return clamped;
    }

    private static float findDeckPlane(Level level, Vec3 origin, Vec3 dir, float length) {
        int steps = Math.max(4, (int) Math.ceil(length * 2.0));
        double step = length / (double) steps;
        float best = length;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int i = 1; i <= steps; i++) {
            double t = i * step;
            int bx = (int) Math.floor(origin.x + dir.x * t);
            int by = (int) Math.floor(origin.y + dir.y * t);
            int bz = (int) Math.floor(origin.z + dir.z * t);

            int deckCount = 0;
            for (int dx = -SEARCH_RADIUS; dx <= SEARCH_RADIUS && deckCount < MIN_DECK_BLOCKS; dx++) {
                for (int dz = -SEARCH_RADIUS; dz <= SEARCH_RADIUS; dz++) {
                    BlockState state = level.getBlockState(cursor.set(bx + dx, by, bz + dz));
                    if (isStageDeck(state)) {
                        deckCount++;
                        if (deckCount >= MIN_DECK_BLOCKS) {
                            break;
                        }
                    }
                }
            }
            if (deckCount < MIN_DECK_BLOCKS) {
                continue;
            }

            // Top face of the deck block layer the beam is crossing.
            double planeY = by + 1.0;
            if (origin.y <= planeY + 1.0e-3) {
                continue;
            }
            double tCross = (planeY - origin.y) / dir.y;
            if (tCross > 0.05 && tCross < best) {
                best = (float) tCross;
            }
        }
        return best;
    }

    public static boolean isStageDeck(BlockState state) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (id == null) {
            return false;
        }
        String namespace = id.getNamespace();
        String path = id.getPath();
        if ("sef".equals(namespace)) {
            return path.startsWith("stage_") || path.contains("stage_");
        }
        if ("stagetents".equals(namespace) || "concertstage".equals(namespace)) {
            return path.contains("stage") || path.contains("deck");
        }
        return false;
    }
}
