package com.github.dumann089.theatricalextralights.mixin.client.sfx;

import com.mojang.blaze3d.audio.SoundBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import javax.sound.sampled.AudioFormat;

/** Acces au format d'un echantillon decode, pour convertir les points de boucle en echantillons. */
@Mixin(SoundBuffer.class)
public interface SoundBufferAccessor {
    @Accessor("format")
    AudioFormat tel$getFormat();
}
