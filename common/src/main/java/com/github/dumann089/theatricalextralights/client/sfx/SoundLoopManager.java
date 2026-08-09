package com.github.dumann089.theatricalextralights.client.sfx;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

@Environment(EnvType.CLIENT)
public final class SoundLoopManager {
    private static final Map<BlockPos, SoundData> ACTIVE = new HashMap<>();

    private SoundLoopManager() {
    }

    public static void play(Level level, BlockPos pos, SoundEvent startSound, SoundEvent loopSound, float volume, float pitch) {
        if (!level.isClientSide) {
            return;
        }
        var soundManager = Minecraft.getInstance().getSoundManager();
        SoundData data = ACTIVE.computeIfAbsent(pos, ignored -> new SoundData());

        if (data.loopInstance != null) {
            if (soundManager.isActive(data.loopInstance)) {
                data.loopInstance.updateVolumeAndPitch(volume, pitch);
                return;
            }
            data.loopInstance = null;
        }

        if (!data.started && startSound != null) {
            playSingleSound(level, pos, startSound, volume, pitch);
            data.started = true;
        }
        Block expectedBlock = level.getBlockState(pos).getBlock();
        SoundLoopInstance loop = new SoundLoopInstance(
                loopSound,
                SoundSource.BLOCKS,
                Vec3.atCenterOf(pos),
                volume,
                pitch,
                pos,
                expectedBlock
        );
        data.loopInstance = loop;
        soundManager.play(loop);
    }

    public static void playSingleSound(Level level, BlockPos pos, SoundEvent start, float volume, float pitch) {
        if (!level.isClientSide) {
            return;
        }
        Vec3 center = Vec3.atCenterOf(pos);
        SimpleSoundInstance instance = new SimpleSoundInstance(
                start.getLocation(),
                SoundSource.BLOCKS,
                volume,
                pitch,
                SoundInstance.createUnseededRandom(),
                false,
                0,
                SoundInstance.Attenuation.LINEAR,
                center.x,
                center.y,
                center.z,
                false
        );
        Minecraft.getInstance().getSoundManager().play(instance);
    }

    public static void stopLoop(BlockPos pos) {
        SoundData data = ACTIVE.remove(pos);
        if (data != null && data.loopInstance != null) {
            data.loopInstance.requestStop();
            Minecraft.getInstance().getSoundManager().stop(data.loopInstance);
        }
    }

    public static void stopAll(BlockPos pos) {
        SoundData data = ACTIVE.remove(pos);
        if (data != null && data.loopInstance != null) {
            data.loopInstance.requestStop();
            Minecraft.getInstance().getSoundManager().stop(data.loopInstance);
        }
    }

    private static final class SoundData {
        boolean started;
        SoundLoopInstance loopInstance;
    }
}
