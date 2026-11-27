package com.github.dumann089.theatricalextralights.mixin.client.broadcast;

import com.github.dumann089.theatricalextralights.client.broadcast.BroadcastCameraSession;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** O viewfinder da câmera tem a própria mira; some com a mira normal do jogo. */
@Mixin(Gui.class)
public abstract class GuiMixin {

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void tel$noCrosshairInBroadcastCamera(GuiGraphics graphics, CallbackInfo ci) {
        if (BroadcastCameraSession.isActive()) {
            ci.cancel();
        }
    }
}
