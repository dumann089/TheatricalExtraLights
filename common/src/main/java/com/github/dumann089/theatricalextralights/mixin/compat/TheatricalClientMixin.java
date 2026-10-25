package com.github.dumann089.theatricalextralights.mixin.compat;

import com.github.dumann089.theatricalextralights.client.LensBloomCompat;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * No fim do {@code renderWorldLast} (depois do {@code LazyRenderers.doRender}), compõe o bloom e
 * só então descarrega os buffers pendentes — tudo no mesmo frame, na ordem certa. Ver
 * {@link LazyRenderersMixin} pra outra metade dessa correção.
 */
@Mixin(targets = "dev.imabad.theatrical.TheatricalClient", remap = false)
public class TheatricalClientMixin {

    @Inject(method = "renderWorldLast", at = @At("RETURN"))
    private static void extralights$flushBloomAndBuffers(PoseStack poseStack, Matrix4f projectionMatrix,
                                                         Camera camera, float tickDelta, CallbackInfo ci) {
        LensBloomCompat.flushBloomPost();
        Minecraft.getInstance().renderBuffers().bufferSource().endBatch();
    }
}
