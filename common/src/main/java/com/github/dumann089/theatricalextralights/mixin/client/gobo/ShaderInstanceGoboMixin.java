package com.github.dumann089.theatricalextralights.mixin.client.gobo;

import com.github.dumann089.theatricalextralights.client.gobo.GoboDirectLightManager;
import com.github.dumann089.theatricalextralights.client.gobo.GoboShaderResourceProvider;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ShaderInstance.class)
public abstract class ShaderInstanceGoboMixin {

    @ModifyVariable(
            method = "<init>",
            at = @At("HEAD"),
            argsOnly = true,
            index = 1
    )
    private static ResourceProvider extralights$wrapShaderResources(
            ResourceProvider provider
    ) {
        if (provider == null
                || provider instanceof GoboShaderResourceProvider) {
            return provider;
        }

        return new GoboShaderResourceProvider(provider);
    }

    @Inject(
            method = "apply",
            at = @At("HEAD")
    )
    private void extralights$applyGoboLighting(
            CallbackInfo ci
    ) {
        GoboDirectLightManager.apply(
                (ShaderInstance) (Object) this
        );
    }

    @Inject(
            method = "apply",
            at = @At("TAIL")
    )
    private void extralights$bindGoboSamplers(
            CallbackInfo ci
    ) {
        GoboDirectLightManager.bindSamplers(
                (ShaderInstance) (Object) this
        );
    }
}