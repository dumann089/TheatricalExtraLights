package com.github.dumann089.theatricalextralights.client.broadcast;

import com.github.dumann089.theatricalextralights.TheatricalExtraLights;
import com.github.dumann089.theatricalextralights.entities.BroadcastCameraEntity;
import com.github.dumann089.theatricalextralights.entities.JibCameraEntity;
import com.github.dumann089.theatricalextralights.entities.ShoulderCameraEntity;
import com.github.dumann089.theatricalextralights.mixin.client.broadcast.GameRendererAccessor;
import com.github.dumann089.theatricalextralights.mixin.client.broadcast.PostChainAccessor;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * O "programa" da mesa de corte: mostra na tela do jogador APENAS o sinal de uma câmera (para onde ela
 * aponta, com o zoom e a exposição que ela transmite), sem poder controlá-la. Os mixins de câmera e FOV
 * leem daqui, como leem da {@link BroadcastCameraSession}.
 *
 * <p>Corte com fade = dissolução entre as duas câmeras, as duas ao vivo: durante a dissolução o mundo é
 * desenhado duas vezes por quadro (primeiro a câmera que sai, guardada numa imagem; depois a que entra,
 * na tela) e a imagem da que sai some por cima, pela opacidade.
 */
public final class SwitcherView {

    private static final ResourceLocation EXPOSURE_EFFECT =
            new ResourceLocation(TheatricalExtraLights.MOD_ID, "shaders/post/broadcast_exposure.json");

    /** O que uma câmera transmite, suavizado (os valores sincronizados chegam em degraus). */
    static final class Feed {
        UUID id;
        BroadcastCameraEntity cached;
        boolean smooth;
        long lastNanos;
        float yaw;
        float pitch;
        float zoom = 1f;
        float extension;
        float boomYaw;
        float boomPitch;
        float exposure;

        void reset(UUID newId) {
            id = newId;
            cached = null;
            smooth = false;
        }

        BroadcastCameraEntity resolve() {
            if (id == null) {
                return null;
            }
            if (cached != null && !cached.isRemoved() && cached.getUUID().equals(id)) {
                return cached;
            }
            cached = null;
            ClientLevel level = Minecraft.getInstance().level;
            if (level == null) {
                return null;
            }
            for (Entity entity : level.entitiesForRendering()) {
                if (entity instanceof BroadcastCameraEntity camera && camera.getUUID().equals(id)) {
                    cached = camera;
                    break;
                }
            }
            return cached;
        }

        /** Posição e direção do sinal; null se a câmera não está disponível aqui. */
        BroadcastCameraSession.CameraState compute(float partialTick) {
            BroadcastCameraEntity camera = resolve();
            long now = System.nanoTime();
            float dt = Mth.clamp((now - lastNanos) / 1.0e9f, 0f, 0.1f);
            lastNanos = now;
            if (camera == null) {
                return null;
            }
            float targetBoomYaw = 0f;
            float targetBoomPitch = 0f;
            if (camera instanceof JibCameraEntity jib) {
                targetBoomYaw = jib.getBoomYaw();
                targetBoomPitch = jib.getBoomPitch();
            }
            float targetExposure = camera.getOutputExposure() / 3f;
            if (!smooth) {
                smooth = true;
                yaw = camera.getHeadYaw();
                pitch = camera.getHeadPitch();
                zoom = camera.getOutputZoom();
                extension = camera.getTripodExtension();
                boomYaw = targetBoomYaw;
                boomPitch = targetBoomPitch;
                exposure = targetExposure;
            } else {
                float k = 1f - (float) Math.exp(-dt / 0.10f);
                yaw += Mth.wrapDegrees(camera.getHeadYaw() - yaw) * k;
                pitch += (camera.getHeadPitch() - pitch) * k;
                zoom += (camera.getOutputZoom() - zoom) * k;
                extension += (camera.getTripodExtension() - extension) * k;
                boomYaw += Mth.wrapDegrees(targetBoomYaw - boomYaw) * k;
                boomPitch += (targetBoomPitch - boomPitch) * k;
                exposure += (targetExposure - exposure) * k;
            }
            Vec3 eye = camera instanceof JibCameraEntity jib
                    ? jib.getHeadPivot(partialTick, extension, boomYaw, boomPitch)
                    : camera.getHeadPivot(partialTick, extension);
            return new BroadcastCameraSession.CameraState(eye, camera.getYRot() + yaw, pitch);
        }
    }

    private static boolean active;
    /** A câmera no ar (entra) e a que está saindo durante a dissolução. */
    private static Feed main = new Feed();
    private static Feed out = new Feed();

    private static boolean dissolving;
    private static long dissolveStartNanos;
    private static float dissolveTotal = 1f;
    private static float alpha;
    /** Antes de dissolver, espera o mundo da câmera nova terminar de carregar (senão luzes e feixes aparecem tarde). */
    private static boolean waitingForWorld;
    private static long waitStartNanos;
    private static long readySinceNanos;

    /** A câmera cujo mundo está sendo desenhado num passe extra (a que sai, ou uma sendo aquecida); null = o passe normal. */
    private static Feed extraFeed;

    // Aquecimento: de tempos em tempos desenha, escondido, o mundo de cada câmera vinculada que não está no ar,
    // para que o que depende do primeiro desenho (feixes, luzes, recursos da GPU) já esteja pronto no corte.
    private static final List<UUID> warmTargets = new ArrayList<>();
    private static final Feed warm = new Feed();
    private static int warmCursor = -1;
    private static int warmFramesLeft;
    private static long nextWarmNanos;
    private static boolean outgoingReady;
    private static TextureTarget outgoingImage;
    private static PostChain exposureChain;

    // Grade de chunks parada num ponto central das câmeras (ver ViewAreaMixin) e corte por oclusão desligado.
    private static boolean anchorSet;
    private static double anchorX;
    private static double anchorZ;
    private static boolean savedSmartCull;
    private static net.minecraft.resources.ResourceLocation anchorDimension;

    private SwitcherView() {
    }

    public static boolean isActive() {
        return active;
    }

    /**
     * O mundo das câmeras continua carregado depois de sair da mesa, para não recarregar a cada vez que
     * se opera. Solta a grade quando o jogador se afasta do palco, troca de dimensão ou sai do mundo.
     */
    public static void tick(Minecraft minecraft) {
        if (!anchorSet) {
            return;
        }
        boolean gone = minecraft.level == null || minecraft.player == null
                || !minecraft.level.dimension().location().equals(anchorDimension)
                || (!anchorPinned && Math.hypot(minecraft.player.getX() - anchorX, minecraft.player.getZ() - anchorZ) > 64.0);
        if (gone) {
            anchorSet = false;
            minecraft.smartCull = savedSmartCull;
            if (minecraft.levelRenderer != null) {
                minecraft.levelRenderer.allChanged(); // volta a grade para perto do jogador
            }
        }
    }

    /** Quando verdadeiro, a grade fixa não é solta por distância (jumbotron: a imagem não congela longe). */
    private static boolean anchorPinned;

    static void setAnchorPinned(boolean pinned) {
        anchorPinned = pinned;
    }

    /** O passe extra em curso é o da imagem do jumbotron (e não a dissolução da mesa). */
    private static boolean jumbotronPass;

    public static boolean isJumbotronPass() {
        return jumbotronPass;
    }

    /**
     * Move a grade fixa para (x, z) de uma vez (sem a histerese de 48 blocos do {@link #acquireAnchor}). Quem
     * chama decide quando vale a pena: mover reconstrói os chunks, então deve acontecer raramente.
     */
    static void retargetAnchor(double x, double z) {
        Minecraft client = Minecraft.getInstance();
        if (!anchorSet) {
            savedSmartCull = client.smartCull;
        }
        client.smartCull = false;
        if (anchorSet && Math.hypot(anchorX - x, anchorZ - z) < 4.0) {
            return;
        }
        anchorSet = true;
        anchorX = x;
        anchorZ = z;
        anchorDimension = client.level == null ? null : client.level.dimension().location();
        if (client.levelRenderer != null) {
            client.levelRenderer.allChanged();
        }
    }

    /** Há um passe extra (jumbotron, fade ou aquecimento) desenhando o mundo de uma câmera agora. */
    public static boolean isExtraPass() {
        return extraFeed != null;
    }

    /**
     * Fixa a grade de chunks perto das câmeras SEM assumir a tela do jogador (o que {@link #begin} faz).
     * Sem isso, cada passe do jumbotron faria o Minecraft recentrar a grade na câmera e, no quadro
     * seguinte, de volta no jogador, recompilando chunks sem parar. Chamado só no tick, nunca no desenho.
     */
    static void acquireAnchor(double x, double z) {
        Minecraft client = Minecraft.getInstance();
        if (!anchorSet) {
            savedSmartCull = client.smartCull;
        }
        client.smartCull = false;
        boolean moved = anchorSet && Math.hypot(anchorX - x, anchorZ - z) > 48.0;
        if (!anchorSet || moved) {
            anchorSet = true;
            anchorX = x;
            anchorZ = z;
            anchorDimension = client.level == null ? null : client.level.dimension().location();
            if (client.levelRenderer != null) {
                client.levelRenderer.allChanged();
            }
        }
    }

    /** Solta a grade fixa quando nada mais precisa dela (nenhuma mesa em operação). */
    static void releaseAnchorIfIdle() {
        if (!anchorSet || active) {
            return;
        }
        anchorSet = false;
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.smartCull = savedSmartCull;
        if (minecraft.levelRenderer != null) {
            minecraft.levelRenderer.allChanged();
        }
    }

    /**
     * Desenha uma vez o mundo do ponto de vista de {@code feed} na tela principal (quem chama copia o
     * resultado em seguida). Falso se a câmera não está disponível neste cliente.
     */
    static boolean renderFeedPass(GameRenderer renderer, float partialTick, long nanoTime, Feed feed) {
        if (feed.resolve() == null) {
            return false;
        }
        Feed previous = extraFeed;
        extraFeed = feed;
        jumbotronPass = true;
        try {
            com.github.dumann089.theatricalextralights.client.render.beam.raymarch.SceneDepthCopy.invalidate();
            renderer.renderLevel(partialTick, nanoTime, new PoseStack());
        } finally {
            extraFeed = previous;
            jumbotronPass = false;
            // A profundidade copiada aqui é da câmera, não do jogador: o desenho principal precisa recopiar.
            com.github.dumann089.theatricalextralights.client.render.beam.raymarch.SceneDepthCopy.invalidate();
        }
        return true;
    }

    public static boolean hasAnchor() {
        return anchorSet;
    }

    public static double anchorX() {
        return anchorX;
    }

    public static double anchorZ() {
        return anchorZ;
    }

    /**
     * Começa a operar a mesa. {@code (x, z)} é o centro das câmeras: a grade de chunks fica fixa aí, então
     * trocar de câmera (ou mostrar duas no fade) não recarrega o mundo. O corte por oclusão fica desligado
     * (o Minecraft só monta os chunks de trás de um muro depois de compilar o muro, em cascata, e é daí
     * que vinha o feixe aparecendo aos poucos); todos os chunks da grade são montados de antemão.
     */
    public static void begin(boolean hasCenter, double x, double z) {
        Minecraft client = Minecraft.getInstance();
        if (!anchorSet) {
            savedSmartCull = client.smartCull;
        }
        client.smartCull = false;
        // O mundo das câmeras é montado uma vez e fica: só se muda a grade se as câmeras se afastaram do centro.
        boolean moved = anchorSet && Math.hypot(anchorX - x, anchorZ - z) > 48.0;
        if (hasCenter && (!anchorSet || moved)) {
            anchorSet = true;
            anchorX = x;
            anchorZ = z;
            anchorDimension = client.level == null ? null : client.level.dimension().location();
            if (client.levelRenderer != null) {
                client.levelRenderer.allChanged(); // refaz a grade já centrada nas câmeras
            }
        }
        active = true;
        main.reset(null);
        out.reset(null);
        dissolving = false;
        outgoingReady = false;
        alpha = 0f;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gameRenderer instanceof GameRendererAccessor accessor) {
            accessor.tel$loadEffect(EXPOSURE_EFFECT);
            exposureChain = accessor.tel$getPostEffect();
        }
    }

    public static void end() {
        active = false;
        Minecraft minecraft = Minecraft.getInstance();
        if (exposureChain != null && minecraft.gameRenderer instanceof GameRendererAccessor accessor
                && accessor.tel$getPostEffect() == exposureChain) {
            minecraft.gameRenderer.shutdownEffect();
        }
        exposureChain = null;
        if (outgoingImage != null) {
            outgoingImage.destroyBuffers();
            outgoingImage = null;
        }
        main.reset(null);
        out.reset(null);
        dissolving = false;
        outgoingReady = false;
        extraFeed = null;
        warmTargets.clear();
        warmFramesLeft = 0;
        alpha = 0f;
    }

    /** Câmeras da mesa: todas ficam aquecidas, prontas para entrar no ar. */
    public static void setWarmTargets(List<UUID> cameras) {
        warmTargets.clear();
        warmTargets.addAll(cameras);
        warmCursor = -1;
        warmFramesLeft = 0;
        nextWarmNanos = System.nanoTime() + 1_500_000_000L;
    }

    public static UUID programId() {
        return main.id;
    }

    /** Esta câmera é a do quadro que está sendo desenhado: quem opera a mesa não vê o modelo dela (o olho fica dentro). */
    public static boolean isOnAir(Entity entity) {
        UUID id = (extraFeed != null ? extraFeed : main).id;
        // Também vale no passe da jumbotron (extraFeed): sem isso o cabeçote da câmera era desenhado colado
        // na lente, e a imagem parecia sair de dentro de uma câmera.
        return (active || extraFeed != null) && id != null && id.equals(entity.getUUID());
    }

    /**
     * Quem fica dentro do quadro da câmera mostrada e não deve aparecer: o operador montado nela e, na
     * shoulder camera, o dono (o olho fica no ombro dele).
     */
    public static boolean hidesEntity(Entity entity) {
        if (!active && extraFeed == null) {
            return false;
        }
        BroadcastCameraEntity camera = (extraFeed != null ? extraFeed : main).resolve();
        if (camera == null || entity == camera) {
            return false;
        }
        if (entity.getVehicle() == camera) {
            return true;
        }
        return camera instanceof ShoulderCameraEntity shoulder && shoulder.getOwnerId().isPresent()
                && entity.getUUID().equals(shoulder.getOwnerId().get());
    }

    /** Corte seco: a câmera entra na hora. */
    public static void cut(UUID camera) {
        main.reset(camera);
        out.reset(null);
        dissolving = false;
        waitingForWorld = false;
        outgoingReady = false;
        alpha = 0f;
    }

    /** Corte com fade: a câmera no ar passa a ser a que sai e some por cima da nova, que entra ao vivo. */
    public static void fadeTo(UUID camera, float seconds) {
        if (camera.equals(main.id)) {
            return;
        }
        Feed leaving = out;
        out = main; // guarda o estado suavizado da que sai: sem pulo na imagem
        main = leaving;
        main.reset(camera);
        dissolveTotal = Math.max(0.2f, seconds);
        dissolveStartNanos = System.nanoTime();
        dissolving = true;
        alpha = 1f;
    }

    /** Avança a dissolução pelo relógio; pode ser chamado várias vezes por frame. */
    public static void update() {
        if (!dissolving) {
            alpha = 0f;
            waitingForWorld = false;
            return;
        }
        if (waitingForWorld) {
            long now = System.nanoTime();
            boolean idle = Minecraft.getInstance().levelRenderer.hasRenderedAllChunks();
            if (!idle) {
                readySinceNanos = 0L;
            } else if (readySinceNanos == 0L) {
                readySinceNanos = now;
            }
            boolean loaded = readySinceNanos != 0L && now - readySinceNanos >= 500_000_000L
                    && now - waitStartNanos >= 1_200_000_000L;
            boolean timeout = now - waitStartNanos >= 4_000_000_000L;
            if (loaded || timeout || main.resolve() == null) {
                waitingForWorld = false;
                dissolveStartNanos = now; // só agora a dissolução começa a contar
            } else {
                alpha = 1f; // enquanto isso a câmera que sai continua inteira na tela
                return;
            }
        }
        float t = (System.nanoTime() - dissolveStartNanos) / 1.0e9f / dissolveTotal;
        if (t >= 1f) {
            dissolving = false;
            alpha = 0f;
            out.reset(null);
        } else {
            alpha = 1f - t;
        }
    }

    /** Proporção configurada na câmera que está no ar (0 = a da janela). */
    public static int programAspect() {
        BroadcastCameraEntity camera = main.resolve();
        return camera == null ? 0 : camera.getAspect();
    }

    public static boolean isFading() {
        return dissolving;
    }

    /** A câmera no ar existe e está carregada neste cliente. */
    public static boolean hasSignal() {
        return main.resolve() != null;
    }

    public static float fovMultiplier() {
        return 1f / Math.max(1f, (extraFeed != null ? extraFeed : main).zoom);
    }

    /** Posição e direção do sinal da câmera que está sendo desenhada; null se ela não está disponível. */
    public static BroadcastCameraSession.CameraState frameState(float partialTick) {
        update();
        if (extraFeed != null) {
            return extraFeed.compute(partialTick);
        }
        BroadcastCameraSession.CameraState state = main.compute(partialTick);
        if (state != null) {
            applyExposure(main.exposure);
        }
        return state;
    }

    private static void applyExposure(float exposure) {
        Minecraft minecraft = Minecraft.getInstance();
        if (exposureChain == null || !(minecraft.gameRenderer instanceof GameRendererAccessor accessor)
                || accessor.tel$getPostEffect() != exposureChain) {
            return; // outro efeito de tela assumiu: não mexe nos uniforms dele
        }
        float scale = (float) Math.pow(2.0, exposure);
        for (PostPass pass : ((PostChainAccessor) exposureChain).tel$getPasses()) {
            pass.getEffect().safeGetUniform("ColorScale").set(scale, scale, scale);
        }
    }

    /**
     * Chamado (pelo mixin) logo antes do mundo da câmera no ar ser desenhado. Durante a dissolução desenha
     * antes o mundo da câmera que sai e guarda a imagem dele, para ser misturada depois; fora da dissolução
     * aquece, escondido, as outras câmeras da mesa.
     */
    public static void renderExtraPasses(GameRenderer renderer, float partialTick, long nanoTime) {
        outgoingReady = false;
        if (!active || extraFeed != null || BroadcastCameraSession.isActive()) {
            return;
        }
        update();
        if (dissolving) {
            if (out.resolve() == null) {
                return; // sem sinal da que sai: só aparece a nova
            }
            extraFeed = out;
            try {
                renderer.renderLevel(partialTick, nanoTime, new PoseStack());
                capture();
                outgoingReady = true;
            } finally {
                extraFeed = null;
            }
            return;
        }
    }

    /** Um passe escondido do mundo de uma câmera vinculada, uns quadros seguidos, uma câmera por segundo. */
    private static void warmUp(GameRenderer renderer, float partialTick, long nanoTime) {
        if (warmTargets.isEmpty() || main.id == null) {
            return;
        }
        if (warmFramesLeft <= 0) {
            long now = System.nanoTime();
            if (now < nextWarmNanos) {
                return;
            }
            nextWarmNanos = now + 1_000_000_000L;
            for (int i = 0; i < warmTargets.size(); i++) {
                warmCursor = (warmCursor + 1) % warmTargets.size();
                UUID id = warmTargets.get(warmCursor);
                if (!id.equals(main.id)) {
                    warm.reset(id);
                    warmFramesLeft = 6;
                    break;
                }
            }
            if (warmFramesLeft <= 0) {
                return;
            }
        }
        if (warm.resolve() == null) {
            warmFramesLeft = 0; // essa câmera não está carregada aqui
            return;
        }
        warmFramesLeft--;
        extraFeed = warm;
        try {
            renderer.renderLevel(partialTick, nanoTime, new PoseStack());
        } finally {
            extraFeed = null;
        }
    }

    /** Copia a tela (o mundo da câmera que sai) para a imagem guardada. */
    private static void capture() {
        Minecraft minecraft = Minecraft.getInstance();
        RenderTarget screen = minecraft.getMainRenderTarget();
        if (outgoingImage == null) {
            outgoingImage = new TextureTarget(screen.width, screen.height, false, Minecraft.ON_OSX);
        } else if (outgoingImage.width != screen.width || outgoingImage.height != screen.height) {
            outgoingImage.resize(screen.width, screen.height, Minecraft.ON_OSX);
        }
        // O blit do Minecraft não escreve o canal alfa: limpa com alfa 1 para a imagem não sair transparente.
        outgoingImage.setClearColor(0f, 0f, 0f, 1f);
        outgoingImage.clear(Minecraft.ON_OSX);
        outgoingImage.bindWrite(true);
        screen.blitToScreen(screen.width, screen.height);
        screen.bindWrite(true);
    }

    /** Desenha a câmera que sai por cima da que entra, com a opacidade da dissolução. */
    public static void drawOutgoing(GuiGraphics graphics, int width, int height) {
        if (outgoingImage == null || !outgoingReady || !dissolving || alpha <= 0f) {
            return;
        }
        // A imagem guardada não passou pelo efeito de exposição da tela: aplica a dela na mistura.
        float scale = Math.min(4f, (float) Math.pow(2.0, out.exposure));
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, outgoingImage.getColorTextureId());
        Matrix4f matrix = graphics.pose().last().pose();
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        // A textura do framebuffer tem a origem embaixo: o V vai invertido.
        builder.vertex(matrix, 0f, 0f, 0f).uv(0f, 1f).color(scale, scale, scale, alpha).endVertex();
        builder.vertex(matrix, 0f, height, 0f).uv(0f, 0f).color(scale, scale, scale, alpha).endVertex();
        builder.vertex(matrix, width, height, 0f).uv(1f, 0f).color(scale, scale, scale, alpha).endVertex();
        builder.vertex(matrix, width, 0f, 0f).uv(1f, 1f).color(scale, scale, scale, alpha).endVertex();
        BufferUploader.drawWithShader(builder.end());
        RenderSystem.disableBlend();
    }
}
