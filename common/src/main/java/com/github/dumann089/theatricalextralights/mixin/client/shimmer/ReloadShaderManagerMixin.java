package com.github.dumann089.theatricalextralights.mixin.client.shimmer;

import com.github.dumann089.theatricalextralights.compat.shimmer.ClasspathGlslResourceProvider;
import com.lowdragmc.shimmer.client.shader.ReloadShaderManager;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = ReloadShaderManager.class, remap = false)
public class ReloadShaderManagerMixin {

    @ModifyVariable(
            method = {
                    "backupNewShaderInstance(Lnet/minecraft/server/packs/resources/ResourceProvider;Ljava/lang/String;Lcom/mojang/blaze3d/vertex/VertexFormat;)Lnet/minecraft/client/renderer/ShaderInstance;",
                    "backupNewShaderInstance(Lnet/minecraft/server/packs/resources/ResourceProvider;Lnet/minecraft/resources/ResourceLocation;Lcom/mojang/blaze3d/vertex/VertexFormat;)Lnet/minecraft/client/renderer/ShaderInstance;"
            },
            at = @At("HEAD"),
            argsOnly = true,
            index = 0,
            remap = false
    )
    private static ResourceProvider extralights$injectClasspathGlsl(ResourceProvider provider) {
        return new ClasspathGlslResourceProvider(provider);
    }
}
