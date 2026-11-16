package com.github.dumann089.theatricalextralights.mixin.client.gobo.embeddium;

import com.github.dumann089.theatricalextralights.client.gobo.GoboDirectLightManager;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(
        targets = "org.embeddedt.embeddium.impl.gl.shader.GlProgram",
        remap = false
)
public abstract class EmbeddiumGlProgramMixin {

    @Inject(
            method = "bind",
            at = @At("TAIL"),
            remap = false,
            require = 0
    )
    private void extralights$bindGoboLighting(
            CallbackInfo ci
    ) {
        int program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);

        GoboDirectLightManager.bindExternalProgram(program);
    }
}