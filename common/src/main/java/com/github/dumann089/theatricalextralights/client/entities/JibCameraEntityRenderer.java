package com.github.dumann089.theatricalextralights.client.entities;

import com.github.dumann089.theatricalextralights.client.broadcast.BroadcastCameraSession;
import com.github.dumann089.theatricalextralights.entities.JibCameraEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;

import static com.github.dumann089.theatricalextralights.client.entities.BroadcastCameraEntityRenderer.*;

/**
 * Jib camera: tripé de rodízios (o mesmo da câmera de broadcast, parado), um mastro com o pivô, o
 * braço (que gira e inclina) com contrapeso atrás e cabos, e a câmera pendurada na ponta.
 * Reaproveita a paleta e os helpers de caixa do {@link BroadcastCameraEntityRenderer}.
 *
 * <p>Medidas em pixels de bloco (1/16), origem no centro do carrinho, no chão, lente em -Z.
 * Tem que bater com {@link JibCameraEntity#getHeadPivot(float, float, float, float)}.
 */
public class JibCameraEntityRenderer extends EntityRenderer<JibCameraEntity> {

    /** Topo do tripé (placa em 17..18,2 px). O mastro sobe daí até o pivô do braço. */
    private static final float TRIPOD_TOP = 18.2f;

    public JibCameraEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.5f;
    }

    @Override
    public ResourceLocation getTextureLocation(JibCameraEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(JibCameraEntity entity, float entityYaw, float partialTick, PoseStack ps,
                       MultiBufferSource buffers, int light) {
        entity.advanceRender();
        int overlay = OverlayTexture.NO_OVERLAY;
        boolean live = entity.isLive();
        float bodyYaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));

        float lf = JibCameraEntity.armLengthPx(entity.getRenderHeight());
        float lr = Math.max(18f, lf * 0.4f);
        float fulcrum = JibCameraEntity.FULCRUM_Y_PX;
        float boomYaw = entity.getRenderBoomYaw();
        float boomPitch = entity.getRenderBoomPitch();
        double pitchRad = Math.toRadians(boomPitch);
        float rise = lf * (float) Math.sin(pitchRad);   // quanto a ponta sobe acima do pivô
        float reach = lf * (float) Math.cos(pitchRad);  // quanto ela avança na horizontal

        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(180f - bodyYaw));

        drawDolly(ps, vc, light, overlay, 0f);
        box(ps, vc, -1.1f, TRIPOD_TOP, -1.1f, 1.1f, fulcrum, 1.1f, METAL, light, overlay);

        // Prato giratório no topo do mastro: tudo daqui em diante gira com o braço.
        ps.pushPose();
        ps.translate(0, fulcrum / 16f, 0);
        ps.mulPose(Axis.YP.rotationDegrees(-boomYaw));
        drawBoom(ps, vc, light, overlay, boomPitch, lf, lr);

        // Cabeça pendurada na ponta. O giro do braço é desfeito: pan e tilt da câmera são
        // relativos ao carrinho, como na câmera de broadcast.
        ps.translate(0, (rise - JibCameraEntity.HANG_PX) / 16f, -reach / 16f);
        ps.mulPose(Axis.YP.rotationDegrees(boomYaw));
        box(ps, vc, -0.8f, 7.8f, -0.8f, 0.8f, JibCameraEntity.HANG_PX - 0.8f, 0.8f, METAL, light, overlay);

        // Quem opera vê pelo olho da câmera: nada que gira com o mouse (placa de pan, corpo) aparece.
        if (!BroadcastCameraSession.isOperating(entity.getId())
                && !com.github.dumann089.theatricalextralights.client.broadcast.SwitcherView.isOnAir(entity)) {
            ps.pushPose();
            ps.mulPose(Axis.YP.rotationDegrees(-entity.getRenderYaw()));
            box(ps, vc, -2.5f, 6.6f, -2.5f, 2.5f, 7.8f, 2.5f, DARK, light, overlay);

            ps.pushPose();
            ps.mulPose(Axis.XP.rotationDegrees(-entity.getRenderPitch()));
            drawCamera(ps, vc, light, overlay, live);
            ps.popPose();

            ps.popPose();
        }
        ps.popPose(); // prato giratório
        ps.popPose(); // corpo

        super.render(entity, entityYaw, partialTick, ps, buffers, light);
    }

    /** Garfo do pivô e braço inclinado: viga, contrapeso atrás, mastro e cabos. Origem: o pivô. */
    private static void drawBoom(PoseStack ps, VertexConsumer vc, int light, int overlay,
                                 float pitchDeg, float lf, float lr) {
        // Garfo do pivô (gira com o prato, mas não inclina).
        box(ps, vc, 2.0f, -3f, -2.5f, 3.0f, 3f, 2.5f, METAL, light, overlay);
        box(ps, vc, -3.0f, -3f, -2.5f, -2.0f, 3f, 2.5f, METAL, light, overlay);

        ps.pushPose();
        // Z negativo = frente. Rotação positiva em X levanta a frente.
        ps.mulPose(Axis.XP.rotationDegrees(pitchDeg));

        box(ps, vc, -1.2f, -1.4f, -lf, 1.2f, 1.4f, lr, DARK, light, overlay);
        box(ps, vc, -1.7f, -2.0f, -3.5f, 1.7f, 2.0f, 3.5f, METAL, light, overlay);
        box(ps, vc, -1.6f, -1.8f, -lf - 1.5f, 1.6f, 1.8f, -lf + 0.5f, METAL, light, overlay);

        // Contrapeso: três discos empilhados no fim do braço.
        for (int i = 0; i < 3; i++) {
            float z1 = lr + 1f - i * 2.6f;
            box(ps, vc, -3.6f, -3.6f, z1 - 1.8f, 3.6f, 3.6f, z1, i == 1 ? GRIP : LEG, light, overlay);
        }

        // Mastro e cabos até a ponta e até o contrapeso.
        box(ps, vc, -0.7f, 1.4f, -0.7f, 0.7f, 10f, 0.7f, METAL, light, overlay);
        strut(ps, vc, 0f, 10f, 0f, 0f, 1.4f, -lf + 2f, 0.3f, METAL, light, overlay);
        strut(ps, vc, 0f, 10f, 0f, 0f, 1.4f, lr - 3f, 0.3f, METAL, light, overlay);

        ps.popPose();
    }

    /** Barra fina entre dois pontos (pixels de bloco), sem rolagem. */
    private static void strut(PoseStack ps, VertexConsumer vc,
                              float x0, float y0, float z0, float x1, float y1, float z1,
                              float half, int cell, int light, int overlay) {
        float dx = x1 - x0, dy = y1 - y0, dz = z1 - z0;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1.0e-3f) {
            return;
        }
        ps.pushPose();
        ps.translate(x0 / 16f, y0 / 16f, z0 / 16f);
        ps.mulPose(new Quaternionf().rotationTo(0f, 0f, 1f, dx / len, dy / len, dz / len));
        box(ps, vc, -half, -half, 0f, half, half, len, cell, light, overlay);
        ps.popPose();
    }
}
