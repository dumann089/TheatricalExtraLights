package com.github.dumann089.theatricalextralights.client.entities;

import com.github.dumann089.theatricalextralights.TheatricalExtraLights;
import com.github.dumann089.theatricalextralights.client.broadcast.BroadcastCameraSession;
import com.github.dumann089.theatricalextralights.entities.BroadcastCameraEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Câmera de broadcast de ombro sobre tripé com rodas, montada em caixas coloridas (paleta 4x4 em
 * {@code textures/block/broadcast_camera.png}). O carrinho anda com a entidade; a cabeça gira em
 * pan e o corpo da câmera inclina em tilt, seguindo o que o operador faz.
 *
 * <p>Medidas em pixels de bloco (1/16). Origem: centro do carrinho, no chão. A lente aponta para
 * -Z antes de girar o conjunto para a direção da entidade.
 */
public class BroadcastCameraEntityRenderer extends EntityRenderer<BroadcastCameraEntity> {

    public static final ResourceLocation TEXTURE =
            new ResourceLocation(TheatricalExtraLights.MOD_ID, "textures/block/broadcast_camera.png");

    // Células da paleta (grade 4x4, 4 px cada).
    static final int BODY = 0;
    static final int DARK = 1;
    static final int METAL = 2;
    static final int GLASS = 3;
    static final int GRIP = 4;
    static final int TALLY_OFF = 5;
    static final int TALLY_ON = 6;
    static final int LEG = 7;
    static final int LABEL = 8;

    private static final float TRIPOD_TOP = 17f;
    private static final float FOOT_Y = 3.2f;
    private static final float LEG_TILT_DEG = 22.5f;
    /** Pivô da cabeça. Tem que bater com {@code BroadcastCameraSession.PIVOT_Y}. */
    private static final float PIVOT_Y = 21.7f;

    public BroadcastCameraEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.45f;
    }

    @Override
    public ResourceLocation getTextureLocation(BroadcastCameraEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(BroadcastCameraEntity entity, float entityYaw, float partialTick, PoseStack ps,
                       MultiBufferSource buffers, int light) {
        entity.advanceRender();
        int overlay = OverlayTexture.NO_OVERLAY;
        boolean live = entity.isLive();
        float bodyYaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));

        ps.pushPose();
        // O modelo olha para -Z; isto o vira para a direção da entidade (yaw do Minecraft).
        ps.mulPose(Axis.YP.rotationDegrees(180f - bodyYaw));

        // Extensão da coluna do tripé, em pixels de bloco (negativa = mais baixa).
        float ext = entity.getRenderHeight() * 16f;
        drawDolly(ps, vc, light, overlay, ext);

        ps.pushPose();
        ps.translate(0, (PIVOT_Y + ext) / 16f, 0);
        ps.mulPose(Axis.YP.rotationDegrees(-entity.getRenderYaw()));
        boolean operating = BroadcastCameraSession.isOperating(entity.getId())
                || com.github.dumann089.theatricalextralights.client.broadcast.SwitcherView.isOnAir(entity);
        if (!operating) {
            drawPanHead(ps, vc, light, overlay); // quem opera: nada girando junto com o mouse perto do olho
        }

        ps.pushPose();
        ps.mulPose(Axis.XP.rotationDegrees(-entity.getRenderPitch()));
        if (!operating) {
            drawCamera(ps, vc, light, overlay, live); // quem opera vê pelo olho da câmera: sem corpo no meio da tela
        }
        ps.popPose();

        ps.popPose();
        ps.popPose();
        super.render(entity, entityYaw, partialTick, ps, buffers, light);
    }

    // ── Peças ────────────────────────────────────────────────────────────────

    /** Tripé sobre base triangular de rodízios. */
    static void drawDolly(PoseStack ps, VertexConsumer vc, int light, int overlay, float ext) {
        float legLength = (TRIPOD_TOP - FOOT_Y) / (float) Math.cos(Math.toRadians(LEG_TILT_DEG));
        for (int i = 0; i < 3; i++) {
            ps.pushPose();
            ps.mulPose(Axis.YP.rotationDegrees(i * 120f));

            // Perna inclinada para fora (o pé cai em -Z deste referencial).
            ps.pushPose();
            ps.translate(0, TRIPOD_TOP / 16f, 0);
            ps.mulPose(Axis.XP.rotationDegrees(LEG_TILT_DEG));
            box(ps, vc, -0.8f, -legLength, -0.8f, 0.8f, 0f, 0.8f, LEG, light, overlay);
            ps.popPose();

            // Braço da base e rodízio na ponta.
            box(ps, vc, -0.9f, 3.0f, -7.0f, 0.9f, 4.4f, 1.0f, LEG, light, overlay);
            drawCaster(ps, vc, light, overlay);
            ps.popPose();
        }
        // Cubo da base, coluna telescópica (estágio grosso fixo + estágio fino que sobe) e a placa
        // onde a câmera assenta.
        box(ps, vc, -1.8f, 3.0f, -1.8f, 1.8f, 4.6f, 1.8f, LEG, light, overlay);
        float top = TRIPOD_TOP + ext;
        box(ps, vc, -1.3f, 4.6f, -1.3f, 1.3f, Math.min(TRIPOD_TOP, top), 1.3f, LEG, light, overlay);
        if (ext > 0f) {
            box(ps, vc, -0.9f, TRIPOD_TOP - 6f, -0.9f, 0.9f, top, 0.9f, METAL, light, overlay);
            // Trava da coluna na boca do estágio grosso.
            box(ps, vc, -1.6f, TRIPOD_TOP - 0.8f, -1.6f, 1.6f, TRIPOD_TOP + 0.4f, 1.6f, GRIP, light, overlay);
        }
        box(ps, vc, -3f, top, -3f, 3f, top + 1.2f, 3f, DARK, light, overlay);
    }

    /** Rodízio: garfo, roda octogonal (dois quadrados, um girado 45 graus) e cubo. */
    private static void drawCaster(PoseStack ps, VertexConsumer vc, int light, int overlay) {
        final float z = -6.5f;
        box(ps, vc, -1.2f, 0.8f, z - 0.5f, -0.9f, 4.4f, z + 0.5f, DARK, light, overlay);
        box(ps, vc, 0.9f, 0.8f, z - 0.5f, 1.2f, 4.4f, z + 0.5f, DARK, light, overlay);
        box(ps, vc, -1.2f, 4.0f, z - 0.8f, 1.2f, 4.6f, z + 0.8f, DARK, light, overlay);

        box(ps, vc, -0.7f, 0f, z - 1.5f, 0.7f, 3.0f, z + 1.5f, GRIP, light, overlay);
        ps.pushPose();
        ps.translate(0, 1.5f / 16f, z / 16f);
        ps.mulPose(Axis.XP.rotationDegrees(45f));
        box(ps, vc, -0.7f, -1.15f, -1.15f, 0.7f, 1.15f, 1.15f, GRIP, light, overlay);
        ps.popPose();
        box(ps, vc, -0.95f, 1.1f, z - 0.4f, 0.95f, 1.9f, z + 0.4f, METAL, light, overlay);
    }

    /** Parte que gira em pan com a câmera: o braço de operação (a "barra") sai para trás. */
    private static void drawPanHead(PoseStack ps, VertexConsumer vc, int light, int overlay) {
        float base = 17f - PIVOT_Y;
        box(ps, vc, -2f, base + 1.2f, -2f, 2f, base + 2.4f, 2f, METAL, light, overlay);
        box(ps, vc, -2f, base + 0.4f, 2.5f, -1f, base + 1.4f, 15f, DARK, light, overlay);
        box(ps, vc, -2.3f, base + 0.2f, 10f, -0.7f, base + 1.6f, 15f, GRIP, light, overlay);
    }

    static void drawCamera(PoseStack ps, VertexConsumer vc, int light, int overlay, boolean live) {
        // Corpo e bateria.
        box(ps, vc, -3.2f, -3.5f, -5f, 3.2f, 3.5f, 8f, BODY, light, overlay);
        box(ps, vc, -2.8f, -3.0f, 8f, 2.8f, 3.0f, 11f, DARK, light, overlay);
        box(ps, vc, 3.2f, -1f, 0f, 3.25f, 1.5f, 4.5f, LABEL, light, overlay);

        // Alça superior.
        box(ps, vc, -0.8f, 3.5f, -4f, 0.8f, 5.5f, -3f, GRIP, light, overlay);
        box(ps, vc, -0.8f, 3.5f, 1f, 0.8f, 5.5f, 2f, GRIP, light, overlay);
        box(ps, vc, -1f, 5.5f, -4.5f, 1f, 6.5f, 2.5f, GRIP, light, overlay);

        // Visor (viewfinder) à esquerda e ocular atrás.
        box(ps, vc, -6.2f, 0.5f, -6f, -3.4f, 4.3f, -1f, BODY, light, overlay);
        box(ps, vc, -5.6f, 1.0f, -1f, -4f, 3.4f, 3f, DARK, light, overlay);

        // Microfone shotgun com presilha.
        box(ps, vc, -0.8f, 3.5f, -4f, 0.8f, 3.9f, -3.5f, METAL, light, overlay);
        box(ps, vc, -0.6f, 3.9f, -11f, 0.6f, 5.1f, -3f, DARK, light, overlay);

        // Lente: tubo, anel de zoom, matte box e vidro.
        box(ps, vc, -2.2f, -1.9f, -12f, 2.2f, 2.5f, -5f, DARK, light, overlay);
        box(ps, vc, -2.4f, -2.1f, -9f, 2.4f, 2.7f, -7.5f, METAL, light, overlay);
        box(ps, vc, -3.3f, -2.9f, -14f, 3.3f, 3.5f, -12f, BODY, light, overlay);
        box(ps, vc, -2f, -1.7f, -14.03f, 2f, 2.3f, -13.97f, GLASS, light, overlay);

        // Tally: frente e traseira. Aceso em vermelho vivo e sem sombra quando a câmera está no ar.
        int tallyCell = live ? TALLY_ON : TALLY_OFF;
        int tallyLight = live ? LightTexture.FULL_BRIGHT : light;
        box(ps, vc, -0.4f, 2.0f, -5.3f, 0.4f, 2.9f, -5f, tallyCell, tallyLight, overlay);
        box(ps, vc, -0.4f, 3.0f, 8.5f, 0.4f, 3.7f, 9.5f, tallyCell, tallyLight, overlay);
    }

    // ── Geometria ────────────────────────────────────────────────────────────

    /** Caixa em pixels de bloco, com uma cor chapada da paleta (cada face usa o centro da célula). */
    static void box(PoseStack ps, VertexConsumer vc, float x0, float y0, float z0, float x1, float y1, float z1,
                            int cell, int light, int overlay) {
        float u = ((cell % 4) * 4 + 2) / 16f;
        float v = ((cell / 4) * 4 + 2) / 16f;
        PoseStack.Pose pose = ps.last();
        float a = x0 / 16f, b = y0 / 16f, c = z0 / 16f, d = x1 / 16f, e = y1 / 16f, f = z1 / 16f;

        quad(vc, pose, d, b, c, d, b, f, d, e, f, d, e, c, 1, 0, 0, u, v, light, overlay);     // +X
        quad(vc, pose, a, b, f, a, b, c, a, e, c, a, e, f, -1, 0, 0, u, v, light, overlay);    // -X
        quad(vc, pose, a, e, c, d, e, c, d, e, f, a, e, f, 0, 1, 0, u, v, light, overlay);     // +Y
        quad(vc, pose, a, b, f, d, b, f, d, b, c, a, b, c, 0, -1, 0, u, v, light, overlay);    // -Y
        quad(vc, pose, d, b, f, a, b, f, a, e, f, d, e, f, 0, 0, 1, u, v, light, overlay);     // +Z
        quad(vc, pose, a, b, c, d, b, c, d, e, c, a, e, c, 0, 0, -1, u, v, light, overlay);    // -Z
    }

    private static void quad(VertexConsumer vc, PoseStack.Pose pose,
                             float x1, float y1, float z1, float x2, float y2, float z2,
                             float x3, float y3, float z3, float x4, float y4, float z4,
                             float nx, float ny, float nz, float u, float v, int light, int overlay) {
        vertex(vc, pose, x1, y1, z1, nx, ny, nz, u, v, light, overlay);
        vertex(vc, pose, x2, y2, z2, nx, ny, nz, u, v, light, overlay);
        vertex(vc, pose, x3, y3, z3, nx, ny, nz, u, v, light, overlay);
        vertex(vc, pose, x4, y4, z4, nx, ny, nz, u, v, light, overlay);
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float z,
                               float nx, float ny, float nz, float u, float v, int light, int overlay) {
        vc.vertex(pose.pose(), x, y, z)
                .color(255, 255, 255, 255)
                .uv(u, v)
                .overlayCoords(overlay)
                .uv2(light)
                .normal(pose.normal(), nx, ny, nz)
                .endVertex();
    }
}
