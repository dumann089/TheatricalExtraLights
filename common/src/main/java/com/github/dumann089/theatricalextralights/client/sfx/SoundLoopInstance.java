package com.github.dumann089.theatricalextralights.client.sfx;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

@Environment(EnvType.CLIENT)
public class SoundLoopInstance extends AbstractTickableSoundInstance {
    private boolean shouldStop;
    private final BlockPos blockPos;
    private final Block expectedBlock;

    public SoundLoopInstance(
            SoundEvent sound,
            SoundSource category,
            Vec3 pos,
            float volume,
            float pitch,
            BlockPos blockPos,
            Block expectedBlock
    ) {
        super(sound, category, SoundInstance.createUnseededRandom());
        this.x = pos.x;
        this.y = pos.y;
        this.z = pos.z;
        this.volume = volume;
        this.pitch = pitch;
        this.looping = true;
        this.delay = 0;
        this.attenuation = SoundInstance.Attenuation.LINEAR;
        this.relative = false;
        this.blockPos = blockPos;
        this.expectedBlock = expectedBlock;
    }

    @Override
    public void tick() {
        if (shouldStop || !isSourceStillValid()) {
            stop();
        }
    }

    private boolean isSourceStillValid() {
        if (blockPos == null || expectedBlock == null) {
            return true;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return false;
        }
        if (!minecraft.level.isLoaded(blockPos)) {
            return false;
        }
        return minecraft.level.getBlockState(blockPos).is(expectedBlock);
    }

    public void updateVolumeAndPitch(float volume, float pitch) {
        this.volume = volume;
        this.pitch = pitch;
    }

    public void requestStop() {
        shouldStop = true;
        stop();
    }

    public void updatePosition(Vec3 pos) {
        this.x = pos.x;
        this.y = pos.y;
        this.z = pos.z;
    }
}
