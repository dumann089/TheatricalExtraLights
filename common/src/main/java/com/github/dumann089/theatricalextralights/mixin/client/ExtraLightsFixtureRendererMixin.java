package com.github.dumann089.theatricalextralights.mixin.client;

import com.github.dumann089.theatricalextralights.client.render.FixtureRenderAngleContext;
import dev.imabad.theatrical.blockentities.light.BaseLightBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
        targets = {
                "com.github.dumann089.theatricalextralights.client.blockentities.ExtraLightsFixtureRenderer",
                "com.github.dumann089.theatricalextralights.client.blockentities.ExtraLightsRenderer"
        },
        remap = false
)
public abstract class ExtraLightsFixtureRendererMixin {

    @Inject(
            method = "render",
            at = @At("HEAD"),
            remap = false
    )
    private void extralights$beginAngleInterpolation(
            BaseLightBlockEntity blockEntity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource multiBufferSource,
            int packedLight,
            int packedOverlay,
            CallbackInfo ci
    ) {
        FixtureRenderAngleContext.push(blockEntity, partialTick);
    }

    @Inject(
            method = "render",
            at = @At("RETURN"),
            remap = false
    )
    private void extralights$endAngleInterpolation(
            BaseLightBlockEntity blockEntity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource multiBufferSource,
            int packedLight,
            int packedOverlay,
            CallbackInfo ci
    ) {
        FixtureRenderAngleContext.pop();
    }
}