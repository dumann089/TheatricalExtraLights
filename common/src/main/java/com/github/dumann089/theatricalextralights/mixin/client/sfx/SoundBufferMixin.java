package com.github.dumann089.theatricalextralights.mixin.client.sfx;

import com.github.dumann089.theatricalextralights.client.sfx.SampleLoopPoints;
import com.mojang.blaze3d.audio.SoundBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.OptionalInt;

/**
 * Pose les points de boucle des que le tampon OpenAL existe : {@code getAlBuffer} le cree a son
 * premier appel, avant que quiconque (vanilla ou un autre mod son) l'attache a une source. OpenAL
 * refuse les points sur un tampon deja attache.
 */
@Mixin(SoundBuffer.class)
public abstract class SoundBufferMixin {
    @Inject(method = "getAlBuffer", at = @At("RETURN"))
    private void tel$applyLoopPoints(CallbackInfoReturnable<OptionalInt> cir) {
        OptionalInt id = cir.getReturnValue();
        if (id != null && id.isPresent()) {
            SampleLoopPoints.onAlBufferReady((SoundBuffer) (Object) this, id.getAsInt());
        }
    }
}
