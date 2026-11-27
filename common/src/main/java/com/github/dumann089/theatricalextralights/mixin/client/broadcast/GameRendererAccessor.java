package com.github.dumann089.theatricalextralights.mixin.client.broadcast;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Acesso ao efeito de pós-processamento da tela, usado pela exposição da câmera. */
@Mixin(GameRenderer.class)
public interface GameRendererAccessor {
    @Accessor("postEffect")
    PostChain tel$getPostEffect();

    @Invoker("loadEffect")
    void tel$loadEffect(ResourceLocation location);
}
