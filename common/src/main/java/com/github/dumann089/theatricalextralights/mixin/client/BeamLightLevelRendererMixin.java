package com.github.dumann089.theatricalextralights.mixin.client;

import com.github.dumann089.theatricalextralights.blockentities.BeamLightSource;
import dev.imabad.theatrical.lighting.LightManager;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelRenderer.class)
public abstract class BeamLightLevelRendererMixin {
    @Inject(
            method = "getLightColor(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)I",
            at = @At("TAIL"),
            cancellable = true
    )
    private static void theatricalExtraLights$applyBeamLight(
            BlockAndTintGetter world, BlockState state, BlockPos pos,
            CallbackInfoReturnable<Integer> callbackInfo
    ) {
        double beamLight = BeamLightSource.getDynamicLightLevel(pos);
        if (beamLight > 0.0) {
            callbackInfo.setReturnValue(
                    LightManager.getLightmapWithDynamicLight(beamLight, callbackInfo.getReturnValue())
            );
        }
    }
}
