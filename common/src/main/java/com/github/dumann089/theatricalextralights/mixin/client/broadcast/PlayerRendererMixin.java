package com.github.dumann089.theatricalextralights.mixin.client.broadcast;

import com.github.dumann089.theatricalextralights.client.entities.ShoulderCameraLayer;
import com.github.dumann089.theatricalextralights.items.ShoulderCameraItem;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Põe a shoulder camera no ombro de quem a segura e levanta o braço dele para segurá-la. */
@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin
        extends LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    protected PlayerRendererMixin(EntityRendererProvider.Context context,
                                  PlayerModel<AbstractClientPlayer> model, float shadowRadius) {
        super(context, model, shadowRadius);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void tel$addShoulderCameraLayer(EntityRendererProvider.Context context, boolean slim, CallbackInfo ci) {
        this.addLayer(new ShoulderCameraLayer((PlayerRenderer) (Object) this));
    }

    /** Segurando a câmera: o braço sobe como num binóculo, firmando-a no ombro. */
    @Inject(method = "getArmPose", at = @At("HEAD"), cancellable = true)
    private static void tel$shoulderCameraArmPose(AbstractClientPlayer player, InteractionHand hand,
                                                  CallbackInfoReturnable<HumanoidModel.ArmPose> cir) {
        if (player.getItemInHand(hand).getItem() instanceof ShoulderCameraItem) {
            cir.setReturnValue(HumanoidModel.ArmPose.SPYGLASS);
        }
    }
}
