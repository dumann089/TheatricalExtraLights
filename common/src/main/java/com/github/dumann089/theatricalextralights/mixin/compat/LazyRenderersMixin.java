package com.github.dumann089.theatricalextralights.mixin.compat;

import com.github.dumann089.theatricalextralights.client.render.FixtureRenderAngleContext;
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

@Mixin(targets = "dev.imabad.theatrical.client.LazyRenderers", remap = false)
public class LazyRenderersMixin {

    @Shadow
    @Final
    private static List<LazyRenderers.LazyRenderer> renderers;

    @Inject(
            method = "addLazyRender",
            at = @At("HEAD"),
            require = 0
    )
    private static void extralights$captureFixture(
            LazyRenderers.LazyRenderer renderer,
            CallbackInfo ci
    ) {
        FixtureRenderAngleContext.captureLazyRenderer(renderer);
    }

    @Inject(
            method = "doRender",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void extralights$doRenderWithoutEndBatch(
            Camera camera,
            PoseStack poseStack,
            MultiBufferSource.BufferSource bufferSource,
            float partialTick,
            CallbackInfo ci
    ) {
        ci.cancel();

        if (renderers.isEmpty()) {
            return;
        }

        if (renderers.size() == 1) {
            renderWithContext(
                    renderers.get(0),
                    bufferSource,
                    poseStack,
                    camera,
                    partialTick
            );
        } else {
            List<Tuple<LazyRenderers.LazyRenderer, Double>> distanced =
                    new ArrayList<>();

            for (LazyRenderers.LazyRenderer renderer : renderers) {
                distanced.add(new Tuple<>(
                        renderer,
                        camera.getPosition().distanceToSqr(
                                renderer.getPos(partialTick)
                        )
                ));
            }

            distanced.sort(
                    Comparator.comparingDouble(entry -> -entry.getB())
            );

            for (Tuple<LazyRenderers.LazyRenderer, Double> entry : distanced) {
                renderWithContext(
                        entry.getA(),
                        bufferSource,
                        poseStack,
                        camera,
                        partialTick
                );
            }
        }

        renderers.clear();
    }

    private static void renderWithContext(
            LazyRenderers.LazyRenderer renderer,
            MultiBufferSource.BufferSource bufferSource,
            PoseStack poseStack,
            Camera camera,
            float partialTick
    ) {
        boolean pushed = FixtureRenderAngleContext.pushLazyRenderer(
                renderer,
                partialTick
        );

        try {
            renderer.render(
                    bufferSource,
                    poseStack,
                    camera,
                    partialTick
            );
        } finally {
            if (pushed) {
                FixtureRenderAngleContext.pop();
            }
        }
    }
}