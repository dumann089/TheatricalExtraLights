package com.github.dumann089.theatricalextralights.client;

import com.github.dumann089.theatricalextralights.blockentities.Flow2JetBlockEntity;
import com.github.dumann089.theatricalextralights.client.particle.Flow2JetDissipation;
import com.github.dumann089.theatricalextralights.client.particle.Flow2JetParticleSpawner;
import com.github.dumann089.theatricalextralights.client.sfx.SoundLoopManager;
import com.github.dumann089.theatricalextralights.firework.FireworkRenderDistances;
import com.github.dumann089.theatricalextralights.fixtures.Flow2JetFixture;
import com.github.dumann089.theatricalextralights.sounds.ModSounds;
import com.github.dumann089.theatricalextralights.util.FixtureJetDirection;
import com.github.dumann089.theatricalextralights.util.FixtureMountTransform;
import dev.imabad.theatrical.TheatricalClient;
import dev.imabad.theatrical.blocks.HangableBlock;
import dev.imabad.theatrical.blocks.light.BaseLightBlock;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Environment(EnvType.CLIENT)
public final class Flow2JetClientEffects {
    private static final double HEAR_DISTANCE = 48.0;
    private static final double HEAR_DISTANCE_SQ = HEAR_DISTANCE * HEAR_DISTANCE;
    /** Volume très bas — le fichier co2.ogg est très fort à la source. */
    private static final float LOOP_VOLUME_MIN = 0.006f;
    private static final float LOOP_VOLUME_RANGE = 0.009f;
    private static final Map<BlockPos, Boolean> WAS_ACTIVE = new ConcurrentHashMap<>();

    private Flow2JetClientEffects() {
    }

    public static void tick(Flow2JetBlockEntity blockEntity) {
        boolean active = blockEntity.getIntensity() > 0;
        Minecraft minecraft = Minecraft.getInstance();
        BlockPos pos = blockEntity.getBlockPos();
        Vec3 center = pos.getCenter();

        boolean playerCanHear = minecraft.player != null
                && minecraft.player.distanceToSqr(center) <= HEAR_DISTANCE_SQ;
        float intensityFactor = FixtureJetDirection.intensityFactor((int) blockEntity.getIntensity());
        float volume = LOOP_VOLUME_MIN + LOOP_VOLUME_RANGE * intensityFactor;
        boolean shouldPlaySound = active && playerCanHear;

        if (shouldPlaySound) {
            SoundLoopManager.play(
                    blockEntity.getLevel(),
                    pos,
                    null,
                    ModSounds.FLOW2JET_LOOP.get(),
                    volume,
                    1.0f
            );
        } else {
            SoundLoopManager.stopLoop(pos);
        }

        if (!(blockEntity.getLevel() instanceof ClientLevel level)) {
            return;
        }

        float partial = minecraft.getFrameTime();
        float pan = blockEntity.getInterpolatedPan(partial);
        float userTilt = blockEntity.getInterpolatedTilt(partial);
        float[] beamStart = blockEntity.getFixture().getBeamStartPosition();
        float[] headPivot = blockEntity.getFixture().getPanRotationPosition();
        Direction facing = blockEntity.getBlockState().getValue(BaseLightBlock.FACING);
        boolean isRigged = blockEntity.getBlockState().getValue(HangableBlock.HANGING);
        boolean isFlipped = blockEntity.isUpsideDown();
        boolean isMounted = ((HangableBlock) blockEntity.getBlockState().getBlock())
                .isHanging(blockEntity.getLevel(), pos);
        boolean bodyFlip = Flow2JetFixture.shouldApplyBodyFlip(isFlipped, isMounted);

        Vector3f jetDirection = FixtureJetDirection.directionFromFlow2JetPose(
                pos,
                facing,
                pan,
                userTilt,
                headPivot,
                beamStart,
                isRigged,
                bodyFlip
        );
        Vec3 nozzle = FixtureJetDirection.beamWorldPositionFlow2Jet(
                pos,
                facing,
                pan,
                userTilt,
                headPivot,
                beamStart,
                isRigged,
                bodyFlip
        );
        nozzle = Flow2JetParticleSpawner.adjustNozzleForFacing(facing, pos, nozzle);
        jetDirection = Flow2JetParticleSpawner.adjustDirectionForFacing(facing, jetDirection);
        // Keep CO₂ aligned with wrench mount (same transform as the model).
        nozzle = FixtureMountTransform.transformWorldPoint(blockEntity, pos, nozzle);
        jetDirection = FixtureMountTransform.transformDirection(blockEntity, jetDirection);

        if (active) {
            Flow2JetDissipation.markRunning(pos);
            WAS_ACTIVE.put(pos, true);

            if (!FireworkRenderDistances.isWithinClientFlameRange(nozzle.x, nozzle.y, nozzle.z)) {
                return;
            }

            Flow2JetParticleSpawner.spawnJet(
                    level,
                    pos,
                    facing,
                    pan,
                    userTilt,
                    headPivot,
                    beamStart,
                    isRigged,
                    bodyFlip,
                    (int) blockEntity.getIntensity(),
                    level.random
            );
        } else {
            boolean wasPumping = Boolean.TRUE.equals(WAS_ACTIVE.get(pos))
                    || blockEntity.getPrevIntensity() > 0;
            if (wasPumping) {
                WAS_ACTIVE.put(pos, false);
                if (!Flow2JetDissipation.isDissipating(pos)) {
                    Flow2JetDissipation.beginStop(pos, nozzle, jetDirection, level.getGameTime());
                }
            }
        }
    }

    public static void stop(BlockPos pos) {
        WAS_ACTIVE.remove(pos);
        Flow2JetDissipation.clear(pos);
        SoundLoopManager.stopAll(pos);
    }

    /** Client-only debug overlay toggle — never call from the dedicated server. */
    public static void toggleDebugOverlay(BlockPos pos) {
        if (TheatricalClient.DEBUG_BLOCKS.contains(pos)) {
            TheatricalClient.DEBUG_BLOCKS.remove(pos);
        } else {
            TheatricalClient.DEBUG_BLOCKS.add(pos);
        }
    }

    public static void onBlockRemoved(BlockPos pos) {
        TheatricalClient.DEBUG_BLOCKS.remove(pos);
        stop(pos);
    }
}
