package com.github.dumann089.theatricalextralights.client.sfx;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

/**
 * One looping sample pinned to a block center. Dies when the chunk unloads or the block is gone.
 */
@Environment(EnvType.CLIENT)
final class AnchoredLoopSound extends AbstractTickableSoundInstance {
    private final BlockPos anchor;
    private final ResourceLocation sampleId;
    private boolean released;

    AnchoredLoopSound(SoundEvent sample, BlockPos anchor, float volume, float pitch) {
        super(sample, SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.anchor = anchor.immutable();
        this.sampleId = sample.getLocation();
        this.x = anchor.getX() + 0.5;
        this.y = anchor.getY() + 0.5;
        this.z = anchor.getZ() + 0.5;
        this.volume = volume;
        this.pitch = pitch;
        this.looping = true;
        this.delay = 0;
        this.attenuation = SoundInstance.Attenuation.LINEAR;
        this.relative = false;
    }

    boolean usesSample(SoundEvent sample) {
        return sample != null && sampleId.equals(sample.getLocation());
    }

    void retune(float volume, float pitch) {
        this.volume = volume;
        this.pitch = pitch;
    }

    void release() {
        released = true;
        stop();
    }

    @Override
    public void tick() {
        if (released || !stillAnchored()) {
            stop();
        }
    }

    private boolean stillAnchored() {
        Level level = Minecraft.getInstance().level;
        if (level == null || !level.isLoaded(anchor)) {
            return false;
        }
        return !level.getBlockState(anchor).isAir();
    }
}
