package com.github.dumann089.theatricalextralights.client.broadcast;

import com.github.dumann089.theatricalextralights.JumbotronDebug;
import com.github.dumann089.theatricalextralights.blockentities.JumbotronBlockEntity;
import com.github.dumann089.theatricalextralights.config.TheatricalExtraLightsConfig;
import com.github.dumann089.theatricalextralights.entities.BroadcastCameraEntity;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Imagens ao vivo dos jumbotrons. Cada câmera "no ar" tem um CANAL: uma imagem pequena (TextureTarget)
 * que é redesenhada de vez em quando e mostrada por quantos jumbotrons quiserem — ligar 10 telas na mesma
 * câmera custa o mesmo que ligar 1.
 *
 * <p>Por que isso não deve dar lag:
 * <ul>
 *   <li><b>Só desenha o que alguém está vendo.</b> Os jumbotrons avisam {@link #demand} quando estão no campo
 *       de visão e perto; sem ninguém olhando, nenhum passe extra de mundo acontece (custo zero).</li>
 *   <li><b>No máximo UM passe de mundo por quadro</b>, para qualquer número de canais/telas.</li>
 *   <li><b>Orçamento de tempo:</b> o intervalo entre passes é, no mínimo, 2x o tempo que o último passe
 *       levou (a imagem nunca come mais que ~50% do tempo do jogo) e cresce se o FPS do jogador cai.</li>
 *   <li><b>Imagem pequena</b> (256x144 a 960x540), reduzida com filtro linear; a tela em si é só um quad.</li>
 *   <li><b>Qualidade e taxa caem com a distância.</b></li>
 *   <li>Os passes reaproveitam a grade de chunks fixa do {@link SwitcherView}: sem recompilar chunks.</li>
 * </ul>
 */
public final class JumbotronFeed {

    public static final int MAX_CHANNELS = 3;
    /** Taxa máxima (quadros/s) de uma imagem que ninguém está olhando agora. */
    private static final double BACKGROUND_FPS = 6.0;
    private static final int[] QUALITY_WIDTH = {256, 384, 640, 960};
    private static final int[] QUALITY_FPS = {10, 15, 20, 30};

    /** Uma câmera no ar e a imagem dela. */
    public static final class Channel {
        final UUID camera;
        final int slot;
        final SwitcherView.Feed feed = new SwitcherView.Feed();
        TextureTarget target;
        int quality;
        float minDistSq = Float.MAX_VALUE;
        boolean hasFrame;
        float exposureScale = 1f;
        long nextDueNanos;
        long demandFrame = -1000L;
        long demandTick = -1000L;
        /** Último tick em que alguém estava OLHANDO esta imagem (não só perto dela). */
        long visibleTick = -1000L;

        Channel(UUID camera, int slot) {
            this.camera = camera;
            this.slot = slot;
            this.feed.reset(camera);
        }

        public int slot() {
            return slot;
        }

        /** Já existe ao menos uma imagem desta câmera. */
        public boolean hasFrame() {
            return hasFrame && target != null;
        }

        public int textureId() {
            return target == null ? 0 : target.getColorTextureId();
        }

        /** Largura / altura da imagem. */
        public float aspect() {
            return target == null ? 16f / 9f : (float) target.width / (float) target.height;
        }

        /** A exposição que a câmera transmite (2^EV), para escurecer/clarear a imagem ao mostrar. */
        public float exposureScale() {
            return exposureScale;
        }
    }

    private static final Map<UUID, Channel> CHANNELS = new LinkedHashMap<>();
    private static final boolean[] SLOT_USED = new boolean[MAX_CHANNELS];

    private static boolean capturing;
    /** Algo deu errado no passe: imagem desligada até reentrar no mundo (o jogo continua normal). */
    private static boolean failed;
    private static long frameCounter;
    private static long tickCounter;
    private static long lastFrameNanos;
    /** Tempo médio entre quadros do jogo (ms) e do último passe do jumbotron (ms). */
    private static double frameMsEma = 16.0;
    private static double passMsEma = 4.0;
    private static boolean ownsAnchor;
    private static long lastAnyDemandTick = Long.MIN_VALUE;

    private JumbotronFeed() {
    }

    /** O mundo está sendo desenhado para uma imagem de jumbotron: os próprios jumbotrons não se desenham. */
    public static boolean isCapturing() {
        return capturing;
    }

    // ------------------------------------------------------------------ pedido de quem desenha a tela

    /**
     * Chamado pelo desenho de cada jumbotron visível (todo quadro). Devolve o canal da câmera, ou null se
     * o recurso está desligado / não há canal livre.
     *
     * @param quality    qualidade pedida pela tela (0..3)
     * @param distanceSq distância (ao quadrado) do jogador à tela, para reduzir a qualidade de longe
     */
    public static Channel demand(UUID camera, int quality, double distanceSq) {
        return demand(camera, quality, distanceSq, true);
    }

    /**
     * @param visible verdadeiro quando alguém está olhando a tela agora (atualiza na taxa cheia); falso para
     *                a manutenção em segundo plano (perto, mas fora da mira): atualiza devagar, só para a
     *                imagem não ficar velha quando você olhar de novo.
     */
    public static Channel demand(UUID camera, int quality, double distanceSq, boolean visible) {
        if (camera == null || !TheatricalExtraLightsConfig.isJumbotronEnabled()) {
            return null;
        }
        Channel channel = CHANNELS.get(camera);
        if (channel == null) {
            int slot = freeSlot();
            if (slot < 0) {
                slot = evictOldest();
            }
            if (slot < 0) {
                return null; // todos os canais estão sendo usados agora
            }
            channel = new Channel(camera, slot);
            CHANNELS.put(camera, channel);
            SLOT_USED[slot] = true;
        }
        if (channel.demandFrame != frameCounter) { // primeiro pedido deste quadro: zera os mínimos/máximos
            channel.demandFrame = frameCounter;
            channel.minDistSq = Float.MAX_VALUE;
            channel.quality = 0;
        }
        channel.demandTick = tickCounter;
        if (visible) {
            channel.visibleTick = tickCounter;
        }
        lastAnyDemandTick = tickCounter;
        // Qualidade cai com a distância: -1 depois de 24 blocos, -2 depois de 40.
        int effective = quality;
        if (distanceSq > 40.0 * 40.0) {
            effective -= 2;
        } else if (distanceSq > 24.0 * 24.0) {
            effective -= 1;
        }
        channel.quality = Math.max(channel.quality, Mth.clamp(effective, 0, QUALITY_WIDTH.length - 1));
        channel.minDistSq = Math.min(channel.minDistSq, (float) distanceSq);
        return channel;
    }

    private static int freeSlot() {
        for (int i = 0; i < MAX_CHANNELS; i++) {
            if (!SLOT_USED[i]) {
                return i;
            }
        }
        return -1;
    }

    /** Solta o canal que ninguém pede há mais tempo (só se não foi pedido neste quadro nem no anterior). */
    private static int evictOldest() {
        Channel oldest = null;
        for (Channel channel : CHANNELS.values()) {
            if (tickCounter - channel.demandTick <= 1) {
                continue;
            }
            if (oldest == null || channel.demandTick < oldest.demandTick) {
                oldest = channel;
            }
        }
        if (oldest == null) {
            return -1;
        }
        int slot = oldest.slot;
        dispose(oldest);
        CHANNELS.remove(oldest.camera);
        return slot;
    }

    // ------------------------------------------------------------------ tick (grade de chunks, limpeza)

    /** Roda no tick do cliente: segura/solta a grade de chunks fixa e libera canais esquecidos. */
    public static void tick(Minecraft minecraft) {
        tickCounter++;
        backgroundDemand(minecraft);
        if (CHANNELS.isEmpty() && !ownsAnchor) {
            return;
        }
        // Canais que ninguém pede há 30 s liberam a imagem (memória de vídeo).
        for (Iterator<Channel> it = CHANNELS.values().iterator(); it.hasNext(); ) {
            Channel channel = it.next();
            if (tickCounter - channel.demandTick > 600) {
                SLOT_USED[channel.slot] = false;
                dispose(channel);
                it.remove();
            }
        }
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        if (SwitcherView.isActive() || BroadcastCameraSession.isActive()) {
            ownsAnchor = false; // quem opera a mesa assumiu a grade
            return;
        }
        boolean demanded = tickCounter - lastAnyDemandTick <= 2;
        if (demanded) {
            // A grade fica no centro da câmera mais próxima do jogador que está sendo pedida.
            Channel best = null;
            double bestDist = Double.MAX_VALUE;
            for (Channel channel : CHANNELS.values()) {
                if (tickCounter - channel.demandTick > 2) {
                    continue;
                }
                BroadcastCameraEntity camera = channel.feed.resolve();
                if (camera == null) {
                    continue;
                }
                double dist = Math.hypot(camera.getX() - minecraft.player.getX(), camera.getZ() - minecraft.player.getZ());
                if (dist < bestDist) {
                    bestDist = dist;
                    best = channel;
                }
            }
            if (best != null) {
                BroadcastCameraEntity camera = best.feed.resolve();
                // A grade de chunks fixa cobre o raio de renderização em volta da âncora. Mover a âncora
                // reconstrói todos os chunks (a tela do mundo pisca), então ela só se mexe quando o jogador
                // OU a câmera saem da grade; se cabem juntos, fica entre os dois, senão fica na câmera.
                double reach = Math.max(64.0, minecraft.options.getEffectiveRenderDistance() * 16.0 - 32.0) - 16.0;
                boolean have = SwitcherView.hasAnchor();
                double px = minecraft.player.getX();
                double pz = minecraft.player.getZ();
                boolean playerOk = have && Math.hypot(SwitcherView.anchorX() - px, SwitcherView.anchorZ() - pz) <= reach;
                boolean cameraOk = have && Math.hypot(SwitcherView.anchorX() - camera.getX(), SwitcherView.anchorZ() - camera.getZ()) <= reach;
                if (!(playerOk && cameraOk)) {
                    double ax = camera.getX();
                    double az = camera.getZ();
                    if (bestDist > 24.0 && bestDist <= 2.0 * reach) {
                        ax = (camera.getX() + px) * 0.5;
                        az = (camera.getZ() + pz) * 0.5;
                    }
                    JumbotronDebug.log("tick", "camera a " + (int) bestDist + " blocos; ancora movida para "
                            + (int) ax + "," + (int) az);
                    SwitcherView.retargetAnchor(ax, az);
                    if (!have) {
                        ownsAnchor = true;
                    }
                }
                SwitcherView.setAnchorPinned(true);
            }
        } else if (ownsAnchor && tickCounter - lastAnyDemandTick > 200) {
            ownsAnchor = false;
            SwitcherView.setAnchorPinned(false);
            SwitcherView.releaseAnchorIfIdle();
        }
        if (!SwitcherView.hasAnchor()) {
            ownsAnchor = false; // a grade foi solta por outro motivo (mudou de dimensão, etc.)
            SwitcherView.setAnchorPinned(false);
        }
    }

    /**
     * Mantém viva a imagem de toda jumbotron PERTO do jogador, mesmo fora da mira (o desenho da tela só é
     * chamado quando ela está à vista). A taxa em segundo plano é baixa, então o custo é pequeno.
     */
    private static void backgroundDemand(Minecraft minecraft) {
        if (!TheatricalExtraLightsConfig.isJumbotronEnabled() || minecraft.level == null || minecraft.player == null) {
            return;
        }
        for (JumbotronBlockEntity jumbotron : JumbotronBlockEntity.all()) {
            if (jumbotron.getLevel() != minecraft.level || jumbotron.isRemoved() || jumbotron.getProgram() == null) {
                continue;
            }
            double distSq = jumbotron.screenBounds().getCenter().distanceToSqr(minecraft.player.position());
            demand(jumbotron.getProgram(), jumbotron.getQuality(), distSq, false);
        }
    }

    /** Sair do mundo: solta todas as imagens. */
    public static void disposeAll() {
        for (Channel channel : CHANNELS.values()) {
            dispose(channel);
        }
        CHANNELS.clear();
        java.util.Arrays.fill(SLOT_USED, false);
        ownsAnchor = false;
        failed = false;
    }

    private static void dispose(Channel channel) {
        if (channel.target != null) {
            channel.target.destroyBuffers();
            channel.target = null;
        }
        channel.hasFrame = false;
    }

    // ------------------------------------------------------------------ o passe de mundo

    /**
     * Chamado (pelo mixin) antes do mundo principal de cada quadro. Faz NO MÁXIMO um passe extra, e só se
     * houver um canal pedido e na hora.
     */
    public static void beforeLevel(GameRenderer renderer, float partialTick, long nanoTime) {
        frameCounter++;
        long now = System.nanoTime();
        if (lastFrameNanos != 0L) {
            double ms = Mth.clamp((now - lastFrameNanos) / 1.0e6, 1.0, 250.0);
            frameMsEma += (ms - frameMsEma) * 0.05;
        }
        lastFrameNanos = now;

        if (failed || capturing || CHANNELS.isEmpty() || !TheatricalExtraLightsConfig.isJumbotronEnabled()) {
            return;
        }
        if (SwitcherView.isActive() || BroadcastCameraSession.isActive() || !SwitcherView.hasAnchor()) {
            JumbotronDebug.log("passe-bloqueado", "operando mesa=" + SwitcherView.isActive()
                    + " sessaoCamera=" + BroadcastCameraSession.isActive() + " temAncora=" + SwitcherView.hasAnchor());
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null || SwitcherView.isActive()
                || BroadcastCameraSession.isActive() || !SwitcherView.hasAnchor()) {
            return;
        }

        // Escolhe o canal pedido (neste quadro ou no anterior) cuja hora chegou há mais tempo.
        Channel pick = null;
        for (Channel channel : CHANNELS.values()) {
            if (tickCounter - channel.demandTick > 2 || now < channel.nextDueNanos) {
                continue;
            }
            if (channel.feed.resolve() == null) {
                continue; // a entidade da câmera ainda não existe neste cliente
            }
            if (pick == null || channel.nextDueNanos < pick.nextDueNanos) {
                pick = channel;
            }
        }
        if (pick == null) {
            JumbotronDebug.log("passe-sem-canal", "nenhum canal pronto (camera fora da grade fixa, ou ainda nao na hora)");
            return;
        }

        long start = System.nanoTime();
        capturing = true;
        RenderTarget screen = minecraft.getMainRenderTarget();
        // Erros de GL que já estavam pendentes não são nossos: limpa para não acusar (nem ser acusado).
        glDrain("antes-do-passe");
        try {
            ensureTarget(pick);
            // Criar/redimensionar a imagem deixa o viewport e o framebuffer nela: volta para a tela principal.
            screen.bindWrite(true);
            glDrain("criar-imagem");
            if (SwitcherView.renderFeedPass(renderer, partialTick, nanoTime, pick.feed)) {
                glDrain("desenhar-mundo-da-camera");
                copyToChannel(screen, pick);
                pick.hasFrame = true;
                pick.exposureScale = Math.min(4f, (float) Math.pow(2.0, pick.feed.exposure));
            }
        } catch (RuntimeException | Error failure) {
            // Nunca derruba o jogo por causa da jumbotron: desliga o recurso até reentrar no mundo.
            JumbotronDebug.log("passe-falhou", "desligando a imagem da jumbotron: " + failure);
            failed = true;
        } finally {
            capturing = false;
            screen.bindWrite(true);
            GlStateManager._activeTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0); // o desenho principal espera a unidade 0
            glDrain("fim-do-passe");
        }
        double passMs = (System.nanoTime() - start) / 1.0e6;
        JumbotronDebug.log("passe", "camera=" + pick.camera + " imagem=" + pick.hasFrame
                + " tempo=" + String.format("%.1f", passMs) + "ms qualidade=" + pick.quality);
        passMsEma += (passMs - passMsEma) * 0.2;

        // Próximo passe deste canal: a taxa da qualidade (limitada pela config), com 3 freios:
        double fps = Math.min(QUALITY_FPS[pick.quality], TheatricalExtraLightsConfig.getJumbotronMaxFps());
        if (tickCounter - pick.visibleTick > 2) {
            fps = Math.min(fps, BACKGROUND_FPS); // ninguém olhando agora: só mantém a imagem fresca
        }
        if (pick.minDistSq > 40.0 * 40.0) {
            fps *= 0.5;
        } else if (pick.minDistSq > 24.0 * 24.0) {
            fps *= 0.75;
        }
        double intervalMs = 1000.0 / Math.max(1.0, fps);
        intervalMs = Math.max(intervalMs, passMsEma * 2.0);      // nunca mais que ~50% do tempo
        if (frameMsEma > 50.0) {                                   // abaixo de 20 FPS: bem devagar
            intervalMs *= 4.0;
        } else if (frameMsEma > 33.0) {                            // abaixo de 30 FPS: metade da taxa
            intervalMs *= 2.0;
        }
        pick.nextDueNanos = now + (long) (intervalMs * 1.0e6);
    }

    private static void ensureTarget(Channel channel) {
        int width = QUALITY_WIDTH[channel.quality];
        int height = width * 9 / 16;
        if (channel.target == null) {
            channel.target = new TextureTarget(width, height, false, Minecraft.ON_OSX);
            channel.target.setFilterMode(GL11.GL_LINEAR);
            channel.target.setClearColor(0f, 0f, 0f, 1f);
            channel.hasFrame = false;
        } else if (channel.target.width != width || channel.target.height != height) {
            channel.target.resize(width, height, Minecraft.ON_OSX);
            channel.target.setFilterMode(GL11.GL_LINEAR);
            channel.hasFrame = false;
        }
    }

    /** Copia o centro da tela principal (cortado para 16:9) para a imagem pequena do canal. */
    private static void copyToChannel(RenderTarget screen, Channel channel) {
        int mw = screen.width;
        int mh = screen.height;
        int tw = channel.target.width;
        int th = channel.target.height;
        float targetAspect = (float) tw / th;
        int sw = mw;
        int sh = mh;
        if ((float) mw / mh > targetAspect) {
            sw = Math.round(mh * targetAspect);
        } else {
            sh = Math.round(mw / targetAspect);
        }
        int sx = (mw - sw) / 2;
        int sy = (mh - sh) / 2;
        glDrain("antes-da-copia");
        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, screen.frameBufferId);
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, channel.target.frameBufferId);
        // Outros mods podem deixar o buffer de leitura em "nenhum" (GL_INVALID_OPERATION no blit): força o 0.
        GL30.glReadBuffer(GL30.GL_COLOR_ATTACHMENT0);
        GL30.glDrawBuffer(GL30.GL_COLOR_ATTACHMENT0);
        glDrain("selecionar-buffers");
        GL30.glBlitFramebuffer(sx, sy, sx + sw, sy + sh, 0, 0, tw, th, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_LINEAR);
        if (glDrain("blit")) {
            JumbotronDebug.log("blit-detalhe", "origem " + mw + "x" + mh + " recorte " + sw + "x" + sh
                    + " destino " + tw + "x" + th + " statusDestino=0x"
                    + Integer.toHexString(GL30.glCheckFramebufferStatus(GL30.GL_DRAW_FRAMEBUFFER))
                    + " statusOrigem=0x" + Integer.toHexString(GL30.glCheckFramebufferStatus(GL30.GL_READ_FRAMEBUFFER)));
        }
        // O blit copia o alfa da tela (que pode ser 0) e o shader da tela do jumbotron descarta alfa baixo:
        // força alfa = 1 só no canal alfa.
        GlStateManager._colorMask(false, false, false, true);
        GlStateManager._clearColor(0f, 0f, 0f, 1f);
        GlStateManager._clear(GL11.GL_COLOR_BUFFER_BIT, Minecraft.ON_OSX);
        GlStateManager._colorMask(true, true, true, true);
        glDrain("alfa");
        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, screen.frameBufferId);
    }

    /** Lê e descarta os erros de OpenGL pendentes (e registra qual etapa). Verdadeiro se havia algum. */
    private static boolean glDrain(String step) {
        boolean had = false;
        int error = GL11.glGetError();
        int guard = 0;
        while (error != GL11.GL_NO_ERROR && guard++ < 16) {
            had = true;
            JumbotronDebug.log("gl-" + step, "erro OpenGL 0x" + Integer.toHexString(error) + " apos: " + step);
            error = GL11.glGetError();
        }
        return had;
    }

    /** Id da textura GL do canal que ocupa este slot (0 se vazio). Usado pelo render type da tela. */
    public static int textureIdForSlot(int slot) {
        for (Channel channel : CHANNELS.values()) {
            if (channel.slot == slot) {
                return channel.textureId();
            }
        }
        return 0;
    }

    /** Para o código de testes/depuração: quantos canais existem agora. */
    public static List<UUID> activeCameras() {
        return new ArrayList<>(CHANNELS.keySet());
    }
}
