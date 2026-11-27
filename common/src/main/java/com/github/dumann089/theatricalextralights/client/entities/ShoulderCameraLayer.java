package com.github.dumann089.theatricalextralights.client.entities;

import com.github.dumann089.theatricalextralights.entities.ShoulderCameraEntity;
import com.github.dumann089.theatricalextralights.items.ShoulderCameraItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;

/** Desenha a shoulder camera no ombro direito de quem a segura (e a aponta para onde ela está apontando). */
public class ShoulderCameraLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    public ShoulderCameraLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack ps, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        boolean holding = player.getMainHandItem().getItem() instanceof ShoulderCameraItem
                || player.getOffhandItem().getItem() instanceof ShoulderCameraItem;
        if (!holding) {
            return;
        }
        float relativeYaw = netHeadYaw;
        float pitch = headPitch;
        boolean live = false;
        if (player.getVehicle() instanceof ShoulderCameraEntity camera) {
            // Operando: o jogador não gira (o mouse mexe a câmera), então a câmera aponta pelos dados dela.
            float body = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot);
            relativeYaw = Mth.wrapDegrees(camera.getYRot() + camera.getRenderYaw() - body);
            pitch = camera.getRenderPitch();
            live = true;
        }

        ps.pushPose();
        // O renderer de seres vivos vira o desenho de cabeça para baixo; desfaz para trabalhar de pé,
        // com a origem nos pés, o "à frente" em -Z e a direita do jogador em +X.
        ps.scale(-1f, -1f, 1f);
        ps.translate(0.0, -1.501, 0.0);
        ps.translate(ShoulderCameraEntity.SHOULDER_RIGHT, player.isCrouching() ? 1.25 : 1.45, 0.05);
        ps.scale(0.75f, 0.75f, 0.75f);
        ps.mulPose(Axis.YP.rotationDegrees(-Mth.clamp(relativeYaw, -75f, 75f)));
        ps.mulPose(Axis.XP.rotationDegrees(-pitch));
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(BroadcastCameraEntityRenderer.TEXTURE));
        BroadcastCameraEntityRenderer.drawCamera(ps, vc, light, OverlayTexture.NO_OVERLAY, live);
        ps.popPose();
    }
}
