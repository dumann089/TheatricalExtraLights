package com.github.dumann089.theatricalextralights.mixin.client;

import dev.imabad.theatrical.client.TheatricalRenderTypes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TheatricalRenderTypes.class)
public abstract class TheatricalRenderTypesMixin {
    @Shadow @Final @Mutable
    private static net.minecraft.client.renderer.RenderType BEAM;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void theatricalExtraLights$useVanillaBeamLayer(CallbackInfo callbackInfo) {
        BEAM = net.minecraft.client.renderer.RenderType.lightning();
    }
}
