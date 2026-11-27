package com.github.dumann089.theatricalextralights.mixin.client.broadcast;

import com.github.dumann089.theatricalextralights.client.broadcast.BroadcastCameraSession;
import com.github.dumann089.theatricalextralights.client.broadcast.SwitcherView;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Depois do {@code Camera.setup} normal, move a câmera para a lente da câmera de broadcast.
 * Funciona em Forge e Fabric: o evento ComputeCameraAngles do Forge lê os ângulos daqui.
 */
@Mixin(Camera.class)
public abstract class CameraMixin {

    @Shadow
    protected abstract void setPosition(double x, double y, double z);

    @Shadow
    protected abstract void setRotation(float yaw, float pitch);

    @Inject(method = "setup", at = @At("RETURN"))
    private void tel$broadcastCamera(BlockGetter level, Entity entity, boolean detached, boolean thirdPersonReverse,
                                     float partialTick, CallbackInfo ci) {
        BroadcastCameraSession session = BroadcastCameraSession.getActive();
        BroadcastCameraSession.CameraState state;
        if (session != null) {
            state = session.frameState(partialTick);
        } else if (SwitcherView.isActive() || SwitcherView.isExtraPass()) {
            state = SwitcherView.frameState(partialTick); // mesa de corte: só o sinal da câmera no ar
        } else {
            return;
        }
        if (state == null) {
            return;
        }
        setPosition(state.position().x, state.position().y, state.position().z);
        setRotation(state.yaw(), state.pitch());
    }
}
