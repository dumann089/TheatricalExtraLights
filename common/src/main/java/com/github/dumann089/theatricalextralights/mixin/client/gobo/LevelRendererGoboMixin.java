
package com.github.dumann089.theatricalextralights.mixin.client.gobo;

import com.github.dumann089.theatricalextralights.client.gobo.GoboDirectLightManager;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererGoboMixin {

    @Inject(
            method = "renderLevel",
            at = @At("HEAD")
    )
    private void extralights$beginGoboFrame(
            CallbackInfo ci
    ) {
        GoboDirectLightManager.beginFrame();
    }
}