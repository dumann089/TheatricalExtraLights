package com.github.dumann089.theatricalextralights.mixin.client.broadcast;

import com.github.dumann089.theatricalextralights.client.broadcast.SwitcherView;
import net.minecraft.client.renderer.ViewArea;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * O Minecraft guarda os pedaços do mundo (onde ficam os feixes de luz e os objetos) numa grade centrada
 * na câmera: cada vez que a câmera muda de lugar a grade é refeita e os pedaços recompilados. Com a mesa
 * de corte, que troca de câmera (e às vezes mostra duas ao mesmo tempo no fade), isso fazia tudo
 * recarregar a cada troca. Enquanto a mesa está em operação a grade fica parada num ponto central das
 * câmeras: trocar de câmera não recarrega mais nada.
 */
@Mixin(ViewArea.class)
public abstract class ViewAreaMixin {

    @ModifyVariable(method = "repositionCamera", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private double tel$anchorX(double cameraX) {
        return SwitcherView.hasAnchor() ? SwitcherView.anchorX() : cameraX;
    }

    @ModifyVariable(method = "repositionCamera", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private double tel$anchorZ(double cameraZ) {
        return SwitcherView.hasAnchor() ? SwitcherView.anchorZ() : cameraZ;
    }
}
