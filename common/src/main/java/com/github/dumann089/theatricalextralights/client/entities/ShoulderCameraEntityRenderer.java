package com.github.dumann089.theatricalextralights.client.entities;

import com.github.dumann089.theatricalextralights.client.broadcast.SwitcherView;
import com.github.dumann089.theatricalextralights.entities.ShoulderCameraEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

import java.util.UUID;

/**
 * Shoulder camera sem operador (a gravação tocando sozinha): desenha um boneco com a pele do dono,
 * andando como foi gravado, com a câmera no ombro. Com operador montado não desenha nada: quem aparece
 * é o próprio jogador, com a câmera desenhada por {@link ShoulderCameraLayer}.
 */
public class ShoulderCameraEntityRenderer extends EntityRenderer<ShoulderCameraEntity> {

    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);

    private final ModelPart wideRoot;
    private final ModelPart slimRoot;
    private final PlayerModel<LivingEntity> wideModel;
    private final PlayerModel<LivingEntity> slimModel;

    public ShoulderCameraEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.4f;
        this.wideRoot = context.bakeLayer(ModelLayers.PLAYER);
        this.slimRoot = context.bakeLayer(ModelLayers.PLAYER_SLIM);
        this.wideModel = new PlayerModel<>(wideRoot, false);
        this.slimModel = new PlayerModel<>(slimRoot, true);
        this.wideModel.young = false;
        this.slimModel.young = false;
    }

    private static PlayerInfo playerInfo(UUID owner) {
        Minecraft minecraft = Minecraft.getInstance();
        return owner == null || minecraft.getConnection() == null ? null : minecraft.getConnection().getPlayerInfo(owner);
    }

    @Override
    public ResourceLocation getTextureLocation(ShoulderCameraEntity entity) {
        UUID owner = entity.getOwnerId().orElse(null);
        PlayerInfo info = playerInfo(owner);
        return info != null ? info.getSkinLocation() : DefaultPlayerSkin.getDefaultSkin(owner == null ? new UUID(0L, 0L) : owner);
    }

    @Override
    public void render(ShoulderCameraEntity entity, float entityYaw, float partialTick, PoseStack ps,
                       MultiBufferSource buffers, int light) {
        entity.advanceRender();
        if (!entity.isMannequin() || entity.isVehicle() || SwitcherView.isOnAir(entity)) {
            return; // com operador: ele mesmo aparece; no ar na mesa de corte: o olho fica dentro do boneco
        }
        UUID owner = entity.getOwnerId().orElse(null);
        PlayerInfo info = playerInfo(owner);
        boolean slim = info != null && "slim".equals(info.getModelName());
        PlayerModel<LivingEntity> model = slim ? slimModel : wideModel;
        model.young = false; // por padrão o modelo vem em versão criança (cabeça grande, corpo pequeno)
        ModelPart root = slim ? slimRoot : wideRoot;

        float bodyYaw = entity.getRenderBodyYaw();
        float viewYaw = entity.getYRot() + entity.getRenderYaw();
        float pitch = entity.getRenderPitch();
        float relativeYaw = Mth.clamp(Mth.wrapDegrees(viewYaw - bodyYaw), -75f, 75f);

        // Pose: pernas e braço esquerdo andando, braço direito levantado segurando a câmera.
        root.getAllParts().forEach(ModelPart::resetPose);
        model.head.xRot = pitch * DEG_TO_RAD;
        model.head.yRot = relativeYaw * DEG_TO_RAD;
        float swing = entity.getLimbSwing(partialTick);
        float amount = entity.getLimbAmount(partialTick);
        model.rightLeg.xRot = Mth.cos(swing * 0.6662f) * 1.4f * amount;
        model.leftLeg.xRot = Mth.cos(swing * 0.6662f + (float) Math.PI) * 1.4f * amount;
        model.leftArm.xRot = Mth.cos(swing * 0.6662f) * amount;
        model.rightArm.xRot = Mth.clamp(model.head.xRot - 1.9198f, -2.4f, 3.3f);
        model.rightArm.yRot = model.head.yRot - 0.2618f;
        model.hat.copyFrom(model.head);
        model.jacket.copyFrom(model.body);
        model.leftPants.copyFrom(model.leftLeg);
        model.rightPants.copyFrom(model.rightLeg);
        model.leftSleeve.copyFrom(model.leftArm);
        model.rightSleeve.copyFrom(model.rightArm);

        ResourceLocation skin = info != null ? info.getSkinLocation()
                : DefaultPlayerSkin.getDefaultSkin(owner == null ? new UUID(0L, 0L) : owner);

        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(180f - bodyYaw));
        ps.pushPose();
        ps.scale(-1f, -1f, 1f);
        ps.translate(0.0, -1.501, 0.0);
        model.renderToBuffer(ps, buffers.getBuffer(RenderType.entityCutoutNoCull(skin)), light,
                OverlayTexture.NO_OVERLAY, 1f, 1f, 1f, 1f);
        ps.popPose();

        // A câmera no ombro direito (mesma geometria da camada dos jogadores), apontando para onde a câmera aponta.
        ps.translate(ShoulderCameraEntity.SHOULDER_RIGHT, 1.45, 0.05);
        ps.scale(0.75f, 0.75f, 0.75f);
        ps.mulPose(Axis.YP.rotationDegrees(-relativeYaw));
        ps.mulPose(Axis.XP.rotationDegrees(-pitch));
        VertexConsumer cameraBuffer = buffers.getBuffer(RenderType.entityCutoutNoCull(BroadcastCameraEntityRenderer.TEXTURE));
        BroadcastCameraEntityRenderer.drawCamera(ps, cameraBuffer, light, OverlayTexture.NO_OVERLAY, entity.isLive());
        ps.popPose();

        super.render(entity, entityYaw, partialTick, ps, buffers, light);
    }
}
