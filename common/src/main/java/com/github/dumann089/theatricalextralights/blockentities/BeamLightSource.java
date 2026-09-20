package com.github.dumann089.theatricalextralights.blockentities;

import dev.imabad.theatrical.api.DynamicLightProvider;
import dev.imabad.theatrical.lighting.LightManager;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** One point emitter used to turn a focused beam into a lit tube in the world. */
public final class BeamLightSource implements DynamicLightProvider {
    private static final float SPREAD = 12.0f;
    private static final Set<BeamLightSource> ACTIVE = ConcurrentHashMap.newKeySet();

    private final ExtraLightsLightBlockEntity parent;
    private final int segment;
    private final int segmentCount;
    private final LongOpenHashSet trackedLitChunkPos = new LongOpenHashSet();
    private BlockPos previousBlock;
    private int previousLuminance;
    private float previousSpread;

    BeamLightSource(ExtraLightsLightBlockEntity parent, int segment, int segmentCount) {
        this.parent = parent;
        this.segment = segment;
        this.segmentCount = segmentCount;
    }

    void setRegistryActive(boolean active) {
        if (active) {
            ACTIVE.add(this);
        } else {
            ACTIVE.remove(this);
        }
    }
    public static int getActiveCount() {
        return ACTIVE.size();
    }

    public static double getDynamicLightLevel(BlockPos pos) {
        double result = 0.0;
        for (BeamLightSource source : ACTIVE) {
            int luminance = source.getLightLuminance();
            if (luminance <= 0) continue;
            Vector3f lightPos = source.getLightPos();
            double distance = Math.sqrt(
                    (pos.getX() + 0.5 - lightPos.x()) * (pos.getX() + 0.5 - lightPos.x())
                            + (pos.getY() + 0.5 - lightPos.y()) * (pos.getY() + 0.5 - lightPos.y())
                            + (pos.getZ() + 0.5 - lightPos.z()) * (pos.getZ() + 0.5 - lightPos.z()));
            if (distance <= source.getLightSpread()) {
                result = Math.max(result, luminance * (1.0 - distance / source.getLightSpread()));
            }
        }
        return Math.min(15.0, result);
    }

    @Override
    public BlockPos getOwnerPos() {
        return parent.getBlockPos();
    }

    @Override
    public Vector3f getLightPos() {
        Vec3 origin;
        Vec3 direction;
        double distance;
        if (parent.hasBeamLightGeometry()) {
            origin = Vec3.atLowerCornerOf(parent.getBlockPos()).add(parent.getBeamLightOrigin());
            direction = parent.getBeamLightDirection();
            distance = Math.max(1.0, parent.getBeamLightDistance());
        } else {
            origin = Vec3.atCenterOf(parent.getBlockPos());
            direction = dev.imabad.theatrical.blockentities.light.BaseLightBlockEntity.rayTraceDir(parent);
            distance = Math.max(1.0, parent.getDistance());
        }
        double fraction = (segment + 0.5) / segmentCount;
        Vec3 point = origin.add(direction.scale(distance * fraction));
        return new Vector3f((float) point.x, (float) point.y, (float) point.z);
    }

    @Override
    public Level getLightWorld() {
        return parent.getLevel();
    }

    @Override
    public int getLightLuminance() {
        if (!shouldUpdateLight()) return 0;
        return Math.min(15, Math.max(0, (int) (parent.getIntensity() / 255.0f * 15.0f)));
    }

    @Override
    public int getLightColour() {
        return (getLightLuminance() << 24) | (parent.getColour() & 0xFFFFFF);
    }

    @Override
    public float getLightSpread() {
        return SPREAD;
    }

    @Override
    public void resetLight() {
    }

    @Override
    public void lightTick() {
    }

    @Override
    public boolean shouldUpdateLight() {
        return LightManager.shouldUpdateDynamicLight()
                && parent.emitsLight()
                && parent.getLevel() != null
                && parent.getLevel().isClientSide
                && parent.getIntensity() > 0
                && parent.getDistance() > 0;
    }

    @Override
    public boolean updateDynamicLight(LevelRenderer renderer) {
        int luminance = getLightLuminance();
        float spread = getLightSpread();
        BlockPos currentBlock = BlockPos.containing(getLightPos().x(), getLightPos().y(), getLightPos().z());
        if (luminance == previousLuminance && spread == previousSpread && currentBlock.equals(previousBlock)) {
            return false;
        }

        previousLuminance = luminance;
        previousSpread = spread;
        previousBlock = currentBlock;
        LongOpenHashSet newTrackedChunks = new LongOpenHashSet();
        if (luminance > 0) {
            ChunkPos chunk = new ChunkPos(currentBlock);
            int sectionY = SectionPos.blockToSectionCoord(currentBlock.getY());
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos.MutableBlockPos section = new BlockPos.MutableBlockPos(
                            chunk.x + dx, sectionY, chunk.z + dz);
                    LightManager.scheduleChunkRebuild(renderer, section);
                    LightManager.updateTrackedChunks(section, trackedLitChunkPos, newTrackedChunks);
                }
            }
        }
        scheduleTrackedChunksRebuild(renderer);
        trackedLitChunkPos.clear();
        trackedLitChunkPos.addAll(newTrackedChunks);
        return true;
    }

    @Override
    public void scheduleTrackedChunksRebuild(LevelRenderer renderer) {
        for (long chunk : trackedLitChunkPos) {
            LightManager.scheduleChunkRebuild(renderer, chunk);
        }
    }
}
