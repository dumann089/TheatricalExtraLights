package com.github.dumann089.theatricalextralights.mixin.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.imabad.theatrical.client.LazyRenderers;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Tuple;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Substitui o {@code doRender} original por uma cópia idêntica, SEM o {@code endBatch()} no fim.
 * O flush de verdade agora acontece em {@link TheatricalClientMixin}, depois do composite do
 * bloom — sem isso o miolo branco é desenhado antes do bloom e fica coberto por ele.
 *
 * <p>(Não usamos @Redirect no {@code endBatch} porque o nome desse método é do Minecraft e
 * mudaria entre dev/produção — assim a string de injeção só cita nomes do próprio Theatrical.)
 */
@Mixin(targets = "dev.imabad.theatrical.client.LazyRenderers", remap = false)
public class LazyRenderersMixin {

    @Shadow @Final private static List<LazyRenderers.LazyRenderer> renderers;

    @Inject(method = "doRender", at = @At("HEAD"), cancellable = true)
    private static void extralights$doRenderWithoutEndBatch(Camera camera, PoseStack poseStack,
                                                            MultiBufferSource.BufferSource renderer,
                                                            float partialTick, CallbackInfo ci) {
        ci.cancel();
        if (renderers.isEmpty()) {
            return;
        }
        if (renderers.size() == 1) {
            renderers.get(0).render(renderer, poseStack, camera, partialTick);
        } else {
            List<Tuple<LazyRenderers.LazyRenderer, Double>> distanced = new ArrayList<>();
            for (LazyRenderers.LazyRenderer lazyRenderer : renderers) {
                distanced.add(new Tuple<>(lazyRenderer, camera.getPosition().distanceToSqr(lazyRenderer.getPos(partialTick))));
            }
            distanced.sort(Comparator.comparingDouble(t -> -t.getB()));
            for (Tuple<LazyRenderers.LazyRenderer, Double> entry : distanced) {
                entry.getA().render(renderer, poseStack, camera, partialTick);
            }
        }
        // (o renderer.endBatch() original fica de fora de propósito)
        renderers.clear();
    }
}
