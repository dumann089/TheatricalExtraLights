package com.github.dumann089.theatricalextralights.client.sfx;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

/**
 * Client fixture SFX: at most one looping voice per block.
 * Call {@link #sustain} each tick while the effect is on; {@link #release} when it stops.
 */
@Environment(EnvType.CLIENT)
public final class FixtureLoopSfx {
    private static final Map<Long, AnchoredLoopSound> VOICES = new HashMap<>();

    private FixtureLoopSfx() {
    }

    public static void sustain(Level level, BlockPos pos, SoundEvent loop, float volume, float pitch) {
        if (level == null || !level.isClientSide || loop == null || pos == null) {
            return;
        }

        long key = pos.asLong();
        SoundManager sounds = Minecraft.getInstance().getSoundManager();
        AnchoredLoopSound voice = VOICES.get(key);

        if (voice != null && voice.usesSample(loop) && sounds.isActive(voice)) {
            voice.retune(volume, pitch);
            return;
        }

        if (voice != null) {
            voice.release();
            sounds.stop(voice);
            VOICES.remove(key);
        }

        AnchoredLoopSound started = new AnchoredLoopSound(loop, pos, volume, pitch);
        VOICES.put(key, started);
        sounds.play(started);
    }

    public static void release(BlockPos pos) {
        if (pos == null) {
            return;
        }
        AnchoredLoopSound voice = VOICES.remove(pos.asLong());
        if (voice == null) {
            return;
        }
        voice.release();
        Minecraft.getInstance().getSoundManager().stop(voice);
    }
}
