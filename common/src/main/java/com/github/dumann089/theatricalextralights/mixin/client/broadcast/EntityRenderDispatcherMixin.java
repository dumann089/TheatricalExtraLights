package com.github.dumann089.theatricalextralights.mixin.client.broadcast;

import com.github.dumann089.theatricalextralights.client.broadcast.SwitcherView;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Na mesa de corte o olho da câmera fica onde está o operador: quem está montado nela (e o dono de uma
 * shoulder camera) apareceria como uma silhueta dentro da própria imagem. Eles não são desenhados
 * enquanto aquela câmera é a que está sendo mostrada.
 */
@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {

    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void tel$hideOperatorOfProgramCamera(Entity entity, Frustum frustum, double x, double y, double z,
                                                 CallbackInfoReturnable<Boolean> cir) {
        if (SwitcherView.hidesEntity(entity)) {
            cir.setReturnValue(false);
        }
    }
}
