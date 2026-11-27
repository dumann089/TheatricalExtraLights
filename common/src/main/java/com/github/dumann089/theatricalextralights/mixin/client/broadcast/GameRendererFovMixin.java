package com.github.dumann089.theatricalextralights.mixin.client.broadcast;

import com.github.dumann089.theatricalextralights.client.broadcast.BroadcastCameraSession;
import com.github.dumann089.theatricalextralights.client.broadcast.SwitcherView;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * O sinal da mesa de corte (e a imagem do jumbotron) é o da CÂMERA: o FOV do jogador (correr, voar, lupa,
 * mods de zoom, e até a configuração de FOV dele) não pode entrar. Prioridade alta: roda depois dos outros
 * que mexem no {@code getFov}; e o ponto de uso (a matriz de projeção) é corrigido por último.
 */
@Mixin(value = GameRenderer.class, priority = 10000)
public abstract class GameRendererFovMixin {

    /** FOV base de uma câmera de transmissão vista no jumbotron: igual para todo mundo (o "Normal" do jogo). */
    private static final double JUMBOTRON_BASE_FOV = 70.0;

    private static double cameraFov() {
        double base = SwitcherView.isJumbotronPass() ? JUMBOTRON_BASE_FOV : Minecraft.getInstance().options.fov().get();
        double half = Math.toRadians(base * 0.5);
        return Math.toDegrees(2.0 * Math.atan(Math.tan(half) * SwitcherView.fovMultiplier()));
    }

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void tel$cameraFovIgnoresPlayer(Camera camera, float partialTick, boolean useFovSetting,
                                            CallbackInfoReturnable<Double> cir) {
        if (!useFovSetting || BroadcastCameraSession.getActive() != null) {
            return;
        }
        if (SwitcherView.isExtraPass() || SwitcherView.isActive()) {
            cir.setReturnValue(cameraFov());
        }
    }

    /** Garantia: o que chega à matriz de projeção no passe do jumbotron é o FOV da câmera, não o do jogador. */
    @ModifyVariable(method = "getProjectionMatrix", at = @At("HEAD"), argsOnly = true)
    private double tel$projectionFov(double fov) {
        return SwitcherView.isJumbotronPass() && BroadcastCameraSession.getActive() == null ? cameraFov() : fov;
    }
}
