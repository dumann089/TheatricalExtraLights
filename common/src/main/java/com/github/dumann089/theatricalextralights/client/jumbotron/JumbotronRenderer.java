package com.github.dumann089.theatricalextralights.client.jumbotron;

import com.github.dumann089.theatricalextralights.JumbotronDebug;
import com.github.dumann089.theatricalextralights.blockentities.JumbotronBlockEntity;
import com.github.dumann089.theatricalextralights.client.broadcast.JumbotronFeed;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.UUID;

/**
 * Desenha a tela do jumbotron: um único quad (ou dois, durante a dissolução) com a imagem pronta do canal.
 * O custo por quadro é o de um retângulo; o custo de verdade (desenhar o mundo da câmera) fica no
 * {@link JumbotronFeed}, compartilhado e limitado.
 */
public class JumbotronRenderer implements BlockEntityRenderer<JumbotronBlockEntity> {

    /** Distância da placa (4 px) até onde a tela começa, em blocos, a partir do centro da célula. */
    private static final float FRONT_PLANE = -0.25f + 0.002f;
    private static final float OVERLAY_PLANE = -0.25f + 0.005f;
    /** Meio campo de visão diagonal assumido (graus) para decidir se a tela está à vista. */
    private static final double HALF_FOV_DIAG_DEG = 75.0;

    public JumbotronRenderer(BlockEntityRendererProvider.Context context) {
    }

    // A tela passa dos limites do bloco: sempre passa pelo desenho (o corte de visão é feito aqui, e é barato).
    @Override
    public boolean shouldRenderOffScreen(JumbotronBlockEntity be) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 160;
    }

    @Override
    public void render(JumbotronBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers,
                       int light, int overlay) {
        if (JumbotronFeed.isCapturing()) {
            return; // desenhando a imagem de uma câmera: nada de tela dentro da tela
        }
        Minecraft minecraft = Minecraft.getInstance();
        Camera camera = minecraft.gameRenderer.getMainCamera();
        Vec3 camPos = camera.getPosition();

        AABB bounds = be.screenBounds();
        Vec3 center = bounds.getCenter();
        Direction facing = be.facing();
        Vec3 normal = new Vec3(facing.getStepX(), 0, facing.getStepZ());

        // De costas para a tela não se vê nada: não desenha nem pede imagem.
        Vec3 toCamera = camPos.subtract(center);
        if (toCamera.dot(normal) < -0.25) {
            JumbotronDebug.log("ber-skip", "de costas para a tela (facing=" + facing + ")");
            return;
        }
        if (!inView(camera, toCamera, bounds)) {
            JumbotronDebug.log("ber-skip", "fora do campo de visão");
            return;
        }

        int width = be.getWidth();
        int height = be.getHeight();
        double distSq = toCamera.lengthSqr();
        long now = System.nanoTime();

        // ---- qual imagem mostrar (e a anterior, durante o fade) ----
        UUID target = be.getProgram();
        JumbotronFeed.Channel targetChannel = target == null ? null
                : JumbotronFeed.demand(target, be.getQuality(), distSq);
        if (target == null) {
            be.clientShown = null;
            be.clientFrom = null;
        } else if (!target.equals(be.clientShown) && targetChannel != null && targetChannel.hasFrame()) {
            // Só troca quando a câmera nova já tem imagem: nunca pisca preto no corte.
            if (be.isProgramFade() && be.clientShown != null) {
                be.clientFrom = be.clientShown;
                be.clientFadeStartNanos = now;
                be.clientFadeSeconds = Math.max(0.2f, be.getProgramFadeSeconds());
            } else {
                be.clientFrom = null;
            }
            be.clientShown = target;
        }
        JumbotronFeed.Channel base = null;
        if (be.clientShown != null) {
            base = be.clientShown.equals(target) ? targetChannel
                    : JumbotronFeed.demand(be.clientShown, be.getQuality(), distSq);
        }
        JumbotronFeed.Channel from = null;
        float fromAlpha = 0f;
        if (be.clientFrom != null) {
            float t = (now - be.clientFadeStartNanos) / (be.clientFadeSeconds * 1.0e9f);
            if (t >= 1f) {
                be.clientFrom = null;
            } else {
                from = JumbotronFeed.demand(be.clientFrom, be.getQuality(), distSq);
                fromAlpha = 1f - t;
            }
        }

        JumbotronDebug.log("ber-state", "pos=" + be.getBlockPos() + " tela=" + width + "x" + height
                + " program=" + target + " shown=" + be.clientShown
                + " canal=" + (targetChannel == null ? "NULL (recurso desligado/sem slot)"
                : "slot" + targetChannel.slot() + " temImagem=" + targetChannel.hasFrame())
                + " dist=" + (int) Math.sqrt(distSq)
                + " ancora=" + com.github.dumann089.theatricalextralights.client.broadcast.SwitcherView.hasAnchor());

        // ---- geometria (coordenadas locais do bloco) ----
        Direction right = facing.getCounterClockWise();
        Vector3f r = new Vector3f(right.getStepX(), 0f, right.getStepZ());
        Vector3f n = new Vector3f(facing.getStepX(), 0f, facing.getStepZ());
        Matrix4f matrix = pose.last().pose();

        if (base == null || !base.hasFrame()) {
            VertexConsumer solid = buffers.getBuffer(JumbotronRenderTypes.solid());
            Vector3f[] c = corners(r, n, width, height, FRONT_PLANE);
            solid.vertex(matrix, c[0].x, c[0].y, c[0].z).color(0.02f, 0.02f, 0.04f, 1f).endVertex();
            solid.vertex(matrix, c[1].x, c[1].y, c[1].z).color(0.02f, 0.02f, 0.04f, 1f).endVertex();
            solid.vertex(matrix, c[2].x, c[2].y, c[2].z).color(0.02f, 0.02f, 0.04f, 1f).endVertex();
            solid.vertex(matrix, c[3].x, c[3].y, c[3].z).color(0.02f, 0.02f, 0.04f, 1f).endVertex();
            return;
        }
        float screenAspect = (float) width / (float) height;
        drawImage(buffers.getBuffer(JumbotronRenderTypes.opaque(base.slot())), matrix,
                corners(r, n, width, height, FRONT_PLANE), screenAspect, base.aspect(), base.exposureScale(), 1f);
        if (from != null && from.hasFrame() && from.slot() != base.slot() && fromAlpha > 0f) {
            drawImage(buffers.getBuffer(JumbotronRenderTypes.blend(from.slot())), matrix,
                    corners(r, n, width, height, OVERLAY_PLANE), screenAspect, from.aspect(), from.exposureScale(), fromAlpha);
        }
    }

    /** Cantos: [0]=embaixo-esq, [1]=embaixo-dir, [2]=em cima-dir, [3]=em cima-esq (vista de frente). */
    private static Vector3f[] corners(Vector3f right, Vector3f normal, int width, int height, float depth) {
        Vector3f bl = new Vector3f(0.5f, 0f, 0.5f)
                .fma(-0.5f, right)
                .fma(depth, normal);
        Vector3f br = new Vector3f(bl).fma((float) width, right);
        Vector3f tr = new Vector3f(br).add(0f, height, 0f);
        Vector3f tl = new Vector3f(bl).add(0f, height, 0f);
        return new Vector3f[]{bl, br, tr, tl};
    }

    /** Desenha a imagem preenchendo a tela, cortando as sobras (sem esticar). */
    private static void drawImage(VertexConsumer vc, Matrix4f m, Vector3f[] c, float screenAspect, float imageAspect,
                                  float exposure, float alpha) {
        float u0 = 0f;
        float u1 = 1f;
        float v0 = 0f;
        float v1 = 1f;
        if (screenAspect > imageAspect) { // tela mais larga que a imagem: corta em cima e embaixo
            float visible = imageAspect / screenAspect;
            v0 = 0.5f - visible * 0.5f;
            v1 = 0.5f + visible * 0.5f;
        } else if (screenAspect < imageAspect) { // tela mais estreita: corta dos lados
            float visible = screenAspect / imageAspect;
            u0 = 0.5f - visible * 0.5f;
            u1 = 0.5f + visible * 0.5f;
        }
        float e = exposure;
        // A textura de um framebuffer tem a origem embaixo: o topo da imagem é V alto.
        vc.vertex(m, c[0].x, c[0].y, c[0].z).color(e, e, e, alpha).uv(u0, v0).endVertex();
        vc.vertex(m, c[1].x, c[1].y, c[1].z).color(e, e, e, alpha).uv(u1, v0).endVertex();
        vc.vertex(m, c[2].x, c[2].y, c[2].z).color(e, e, e, alpha).uv(u1, v1).endVertex();
        vc.vertex(m, c[3].x, c[3].y, c[3].z).color(e, e, e, alpha).uv(u0, v1).endVertex();
    }

    /** Teste barato de campo de visão: esfera da tela contra um cone generoso em volta do olhar. */
    private static boolean inView(Camera camera, Vec3 toCamera, AABB bounds) {
        Vec3 toScreen = toCamera.scale(-1.0);
        double dist = toScreen.length();
        double radius = Math.sqrt(bounds.getXsize() * bounds.getXsize() + bounds.getYsize() * bounds.getYsize()
                + bounds.getZsize() * bounds.getZsize()) * 0.5;
        if (dist <= radius) {
            return true; // o jogador está dentro da caixa da tela
        }
        Vector3f look = camera.getLookVector();
        double cos = (toScreen.x * look.x() + toScreen.y * look.y() + toScreen.z * look.z()) / dist;
        double limit = Math.toRadians(HALF_FOV_DIAG_DEG) + Math.asin(Math.min(1.0, radius / dist));
        return limit >= Math.PI || cos >= Math.cos(limit);
    }
}
