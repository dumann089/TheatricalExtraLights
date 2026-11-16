package com.github.dumann089.theatricalextralights.mixin.client.gobo.embeddium;

import com.github.dumann089.theatricalextralights.client.gobo.GoboSodiumShaderPatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(
        targets = "org.embeddedt.embeddium.impl.gl.shader.ShaderLoader",
        remap = false
)
public abstract class EmbeddiumShaderLoaderMixin {

    @Inject(
            method = "getShaderSource",
            at = @At("RETURN"),
            cancellable = true,
            remap = false,
            require = 0
    )
    private static void extralights$patchTerrainShader(
            @Coerce Object identifier,
            CallbackInfoReturnable<String> cir
    ) {
        String source = cir.getReturnValue();

        if (source == null || identifier == null) {
            return;
        }

        String patched = GoboSodiumShaderPatcher.patch(
                identifier.toString(),
                source
        );

        if (patched != source) {
            cir.setReturnValue(patched);
        }
    }
}