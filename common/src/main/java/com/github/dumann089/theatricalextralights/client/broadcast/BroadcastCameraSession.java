package com.github.dumann089.theatricalextralights.client.broadcast;

import com.github.dumann089.theatricalextralights.TheatricalExtraLights;
import com.github.dumann089.theatricalextralights.client.ModKeybinds;
import com.github.dumann089.theatricalextralights.client.followspot.FollowspotInputHelper;
import com.github.dumann089.theatricalextralights.entities.BroadcastCameraEntity;
import com.github.dumann089.theatricalextralights.entities.JibCameraEntity;
import com.github.dumann089.theatricalextralights.entities.ShoulderCameraEntity;
import com.github.dumann089.theatricalextralights.mixin.client.broadcast.GameRendererAccessor;
import com.github.dumann089.theatricalextralights.mixin.client.broadcast.PostChainAccessor;
import com.github.dumann089.theatricalextralights.net.BroadcastCameraAimPacket;
import com.github.dumann089.theatricalextralights.net.BroadcastCameraExitPacket;
import com.github.dumann089.theatricalextralights.net.JibCameraBoomPacket;
import com.github.dumann089.theatricalextralights.net.ModNetworkHandler;
import net.minecraft.client.CameraType;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import com.github.dumann089.theatricalextralights.net.BroadcastCameraHeightPacket;
import com.github.dumann089.theatricalextralights.net.CameraPlaybackPacket;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import java.io.IOException;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Operador de câmera de broadcast. O jogador "monta" na entidade; a visão vai para a lente.
 *
 * <ul>
 *   <li>Mouse: pan/tilt da cabeça, com movimento fluido (estilo câmera cinematográfica).
 *       Roda ou teclas de zoom (configuráveis em Controles): zoom suavizado com ease-in/out.</li>
 *   <li>WASD: empurra o carrinho, sempre ao longo de um eixo exato (nunca na diagonal) e
 *       relativo ao corpo do carrinho, sem depender de para onde a cabeça aponta.</li>
 *   <li>Z: liga/desliga o modo de zoom rápido (roda com passo 10x maior e resposta mais ágil).</li>
 *   <li>Y: esconde o HUD. U: solta o mouse. Setas cima/baixo: exposição. Esc: sair.</li>
 * </ul>
 *
 * <p>A rotação do jogador serve de "sensor" do mouse e é zerada a cada leitura — mesmo truque do
 * modo operador do followspot. A câmera é posta por {@code CameraMixin}, a cada frame.
 */
public final class BroadcastCameraSession {

    /** Mesmas medidas do renderer (em pixels de bloco): pivô da cabeça e saída da lente. */
    public static final float PIVOT_Y = 21.7f / 16f;

    public static final float ZOOM_MIN = 1f;
    public static final float ZOOM_MAX = 100f;
    /** Passo de zoom por "clique" da roda, em fator de ampliação (0,1x). */
    private static final float ZOOM_STEP = 0.1f;
    /** Tempo de suavização do zoom (segundos): quanto maior, mais lento o ease-in/out. */
    private static final float ZOOM_SMOOTH_TIME = 0.5f;
    /** Zoom pelas teclas: multiplicador por segundo enquanto a tecla está apertada. */
    private static final float ZOOM_KEY_RATE = 1.8f;
    private static final float ZOOM_KEY_QUICK_RATE = 5.0f;
    /** Modo de zoom rápido (tecla Z): passo 10x maior e suavização bem mais curta. */
    private static final float ZOOM_QUICK_STEP = 1.0f;
    private static final float ZOOM_QUICK_SMOOTH_TIME = 0.15f;

    /** Velocidade do carrinho em blocos por tick (2 blocos/s). */
    private static final float MOVE_SPEED = 0.1f;
    /** Shoulder camera: velocidade de caminhada (blocos por tick). */
    private static final float WALK_SPEED = 0.15f;

    /** Tremor da câmera em graus no zoom máximo; cai com o cubo da fração de zoom. */
    private static final float SHAKE_DEGREES = 0.024f;

    public static final int EXPOSURE_MAX_THIRDS = 9;
    private static final ResourceLocation EXPOSURE_EFFECT =
            new ResourceLocation(TheatricalExtraLights.MOD_ID, "shaders/post/broadcast_exposure.json");

    private static final int SEND_INTERVAL_TICKS = 2;

    /** Jib: velocidades máximas do braço e tempo de suavização (aceleração / frenagem). */
    private static final float BOOM_SWING_RATE = 35f;   // graus por segundo
    private static final float BOOM_LIFT_RATE = 22f;    // graus por segundo
    private static final float BOOM_SMOOTH_TIME = 0.3f;

    /**
     * Suavização do pan/tilt, como a câmera cinematográfica do Minecraft: tempo de resposta (s) de uma
     * mola criticamente amortecida, então o movimento começa e termina com suavidade, sem "degraus".
     */
    private static final float AIM_SMOOTH_TIME = 0.25f;

    private static BroadcastCameraSession active;
    /** Depois do Esc: por alguns ticks, fecha o menu de pausa que o Minecraft abre sozinho. */
    private static int closePauseTicks;
    /** Esc foi apertado e o servidor ainda não nos tirou da câmera: não reiniciar a sessão. */
    private static boolean exitPending;

    private final int entityId;
    private final long startNanos = System.nanoTime();
    private long lastFrameNanos = startNanos;

    /** Direção que o mouse pede (alvo) e a direção que a câmera mostra: iguais, sem suavização. */
    private float yawTarget;
    private float pitchTarget;
    private float yaw;
    private float pitch;
    private float yawVelocity;
    private float pitchVelocity;

    private float zoom = ZOOM_MIN;
    private float zoomTarget = ZOOM_MIN;
    private float zoomVelocity;
    private boolean quickZoomMode;

    /** Altura do tripé: valor pedido pelo servidor e valor suavizado usado na imagem. */
    private float heightDisplay;

    private int exposureThirds;
    private float exposureDisplay;
    private PostChain exposureChain;

    private boolean hudHidden;
    private boolean savedHideGui;

    private float savedPlayerYaw;
    private float savedPlayerPitch;
    private CameraType savedCameraType;

    private int sendCooldown;
    private final boolean[] moveKeyDown = new boolean[4];
    private int moveKey = -1;

    // Jib camera: o braço é controlado aqui (sem esperar a rede) e enviado ao servidor.
    private final boolean jib;
    private float boomYaw;
    private float boomPitch;
    private float swingVelocity;
    private float liftVelocity;

    // Gravação e reprodução de movimentos (todas as variantes).
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAX_RECORDING_TICKS = CameraRecording.TICKS_PER_SECOND * 60 * 30; // 30 min
    private CameraRecording recording;
    private float recYaw;
    private float recLastYaw;
    private float recBoomYaw;
    private float recLastBoomYaw;
    private CameraRecording playing;
    private boolean playLoop;
    private int playTick;
    private float playBlend;
    private final float[] playFrom = new float[6];
    private float lastSentExtension = Float.NaN;
    private Vec3 lastControl = Vec3.ZERO;

    private BroadcastCameraSession(BroadcastCameraEntity camera) {
        this.entityId = camera.getId();
        this.yaw = this.yawTarget = camera.getHeadYaw();
        this.pitch = this.pitchTarget = camera.getHeadPitch();
        this.heightDisplay = camera.getTripodExtension();
        this.jib = camera instanceof JibCameraEntity;
        if (camera instanceof JibCameraEntity jibCamera) {
            this.boomYaw = jibCamera.getBoomYaw();
            this.boomPitch = jibCamera.getBoomPitch();
        }
    }

    // ── Estado estático ──────────────────────────────────────────────────────

    public static boolean isActive() {
        return active != null;
    }

    public static BroadcastCameraSession getActive() {
        return active;
    }

    /** Proporção da imagem da câmera que se opera (0 = a da janela). */
    public float aspectRatio() {
        BroadcastCameraEntity camera = getCamera(Minecraft.getInstance());
        return camera == null ? 0f : AspectBars.ratio(camera.getAspect());
    }

    public boolean isJib() {
        return jib;
    }

    /** Este cliente está operando a câmera {@code entityId}? Os renderers escondem o corpo dela nesse caso. */
    public static boolean isOperating(int entityId) {
        BroadcastCameraSession session = active;
        return session != null && session.entityId == entityId;
    }

    /** Chamado a cada tick do cliente: inicia a sessão ao montar na câmera e cuida dela depois. */
    public static void clientTick(Minecraft minecraft) {
        if (closePauseTicks > 0) {
            closePauseTicks--;
            if (minecraft.screen instanceof PauseScreen) {
                minecraft.setScreen(null); // no Forge o Esc não é engolido e abriria o menu de pausa por cima
                closePauseTicks = 0;
            }
        }
        if (active != null) {
            releaseStuckMouseButtons(minecraft); // sem auto-repetição de clique enquanto opera
        }
        LocalPlayer player = minecraft.player;
        boolean onCamera = player != null && player.getVehicle() instanceof BroadcastCameraEntity;
        if (!onCamera) {
            exitPending = false;
        }
        if (active != null) {
            active.tick(minecraft);
        } else if (onCamera && !exitPending && minecraft.level != null) {
            start(minecraft, (BroadcastCameraEntity) player.getVehicle());
        }
    }

    private static void start(Minecraft minecraft, BroadcastCameraEntity camera) {
        BroadcastCameraSession session = new BroadcastCameraSession(camera);
        session.captureInput(minecraft);
        session.loadExposureEffect(minecraft);
        active = session;
    }

    public static void stop() {
        BroadcastCameraSession session = active;
        active = null;
        if (session != null) {
            session.stopRecording(); // sair da câmera grava o que já foi gravado
            session.handOffPlayback(Minecraft.getInstance()); // e a reprodução continua tocando na câmera
            session.playing = null;
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft != null) {
                session.restoreInput(minecraft);
            }
        }
    }

    /** Esc: pede ao servidor para descer da câmera e fecha a sessão na hora. */
    public static void requestExit() {
        BroadcastCameraSession session = active;
        if (session == null) {
            return;
        }
        exitPending = true;
        closePauseTicks = 5;
        ModNetworkHandler.CHANNEL.sendToServer(new BroadcastCameraExitPacket(session.entityId));
        stop();
    }

    /** Usado por {@link BroadcastCameraEntity#clientControlHook}: velocidade desejada do carrinho. */
    public static Vec3 controlVelocity(BroadcastCameraEntity camera) {
        BroadcastCameraSession session = active;
        if (session == null || session.entityId != camera.getId()) {
            return null;
        }
        return session.computeMoveVelocity(camera);
    }

    // ── Entrada ──────────────────────────────────────────────────────────────

    private void captureInput(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        savedPlayerYaw = player.getYRot();
        savedPlayerPitch = player.getXRot();
        resetPlayerRotation(player);
        if (!minecraft.mouseHandler.isMouseGrabbed()) {
            minecraft.mouseHandler.grabMouse();
        }
        // Em terceira pessoa a câmera ficaria "destacada" e o jogador apareceria na imagem.
        savedCameraType = minecraft.options.getCameraType();
        minecraft.options.setCameraType(CameraType.FIRST_PERSON);
        savedHideGui = minecraft.options.hideGui;
        // Some com o HUD do Minecraft (hotbar, vida...). O HUD da câmera e as barras de proporção são
        // desenhados por um gancho próprio, que não depende disto (ver GameRendererMixin).
        minecraft.options.hideGui = true;
    }

    private void restoreInput(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (player != null) {
            player.setYRot(savedPlayerYaw);
            player.setXRot(savedPlayerPitch);
            player.yRotO = savedPlayerYaw;
            player.xRotO = savedPlayerPitch;
            player.yHeadRot = savedPlayerYaw;
            player.yHeadRotO = savedPlayerYaw;
        }
        if (savedCameraType != null) {
            minecraft.options.setCameraType(savedCameraType);
        }
        minecraft.options.hideGui = savedHideGui;
        releaseStuckMouseButtons(minecraft);
        unloadExposureEffect(minecraft);
        if (minecraft.screen == null && !minecraft.mouseHandler.isMouseGrabbed()) {
            minecraft.mouseHandler.grabMouse();
        }
    }

    /**
     * Durante a sessão os cliques são engolidos, então um botão que estava apertado ao montar (o botão
     * direito que montou na câmera) podia ficar "preso" no Minecraft e clicar sozinho, inclusive depois
     * de sair. Os botões de usar / atacar são soltos à força: se o botão ainda estiver físicamente
     * apertado, o Minecraft só volta a considerá-lo quando for apertado de novo.
     */
    private static void releaseStuckMouseButtons(Minecraft minecraft) {
        releaseKey(minecraft.options.keyUse);
        releaseKey(minecraft.options.keyAttack);
    }

    private static void releaseKey(KeyMapping key) {
        key.setDown(false);
        while (key.consumeClick()) {
            // descarta cliques acumulados durante a sessão
        }
    }

    private void resetPlayerRotation(LocalPlayer player) {
        player.setYRot(savedPlayerYaw);
        player.setXRot(0f);
        player.yRotO = savedPlayerYaw;
        player.xRotO = 0f;
    }

    /** Lê o mouse acumulado desde a última chamada e converte em pan/tilt. */
    private void consumeMouse(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }
        float dYaw = Mth.wrapDegrees(player.getYRot() - savedPlayerYaw);
        float dPitch = player.getXRot();
        if (Math.abs(dYaw) < 1.0e-4f && Math.abs(dPitch) < 1.0e-4f) {
            return;
        }
        resetPlayerRotation(player);
        float scale = sensitivityScale();
        // O mouse move o alvo; updateAim() faz a câmera seguir o alvo com suavização.
        yawTarget += dYaw * scale;
        pitchTarget = Mth.clamp(pitchTarget + dPitch * scale, BroadcastCameraEntity.PITCH_MIN,
                BroadcastCameraEntity.PITCH_MAX);
    }

    /**
     * Pan/tilt seguem o alvo do mouse com suavização exponencial (independente do FPS), como a
     * câmera cinematográfica do Minecraft: o movimento começa e termina com suavidade.
     */
    private void updateAim(float dt) {
        if (dt <= 0f) {
            return;
        }
        // Mola criticamente amortecida (a mesma do zoom): acelera e freia suavemente atrás do alvo do
        // mouse e não depende do FPS.
        float omega = 2f / AIM_SMOOTH_TIME;
        float x = omega * dt;
        float exp = 1f / (1f + x + 0.48f * x * x + 0.235f * x * x * x);

        float changeYaw = yaw - yawTarget;
        float tempYaw = (yawVelocity + omega * changeYaw) * dt;
        yawVelocity = (yawVelocity - omega * tempYaw) * exp;
        yaw = yawTarget + (changeYaw + tempYaw) * exp;

        float changePitch = pitch - pitchTarget;
        float tempPitch = (pitchVelocity + omega * changePitch) * dt;
        pitchVelocity = (pitchVelocity - omega * tempPitch) * exp;
        pitch = Mth.clamp(pitchTarget + (changePitch + tempPitch) * exp,
                BroadcastCameraEntity.PITCH_MIN, BroadcastCameraEntity.PITCH_MAX);

        // O yaw é contínuo (sem salto em ±180) e só é normalizado aqui, para não crescer sem limite.
        float turns = Math.round(yaw / 360f) * 360f;
        if (turns != 0f) {
            yaw -= turns;
            yawTarget -= turns;
        }
    }

    /** Zoom pelas teclas configuráveis: contínuo enquanto apertadas, mais rápido no modo rápido. */
    private void updateZoomKeys(Minecraft minecraft, float dt) {
        if (minecraft.screen != null || dt <= 0f || playing != null) {
            return;
        }
        boolean in = FollowspotInputHelper.isKeyDown(ModKeybinds.BROADCAST_ZOOM_IN);
        boolean out = FollowspotInputHelper.isKeyDown(ModKeybinds.BROADCAST_ZOOM_OUT);
        if (in == out) {
            return;
        }
        float rate = quickZoomMode ? ZOOM_KEY_QUICK_RATE : ZOOM_KEY_RATE;
        float factor = (float) Math.pow(rate, in ? dt : -dt);
        zoomTarget = Mth.clamp(zoomTarget * factor, ZOOM_MIN, ZOOM_MAX);
    }

    /** Com zoom forte o mesmo movimento de mouse gira menos, como numa cabeça fluida de verdade. */
    private float sensitivityScale() {
        return (float) Math.pow(fovMultiplier(), 0.8);
    }

    // ── Comandos de teclado ──────────────────────────────────────────────────

    public void onMouseScroll(double amount) {
        if (amount == 0 || playing != null) {
            return;
        }
        float step = (quickZoomMode ? ZOOM_QUICK_STEP : ZOOM_STEP) * (float) Math.abs(amount);
        zoomTarget = Mth.clamp(zoomTarget + (amount > 0 ? step : -step), ZOOM_MIN, ZOOM_MAX);
    }

    /** Z: liga/desliga o modo de zoom rápido. Não mexe no zoom atual. */
    public void toggleQuickZoom() {
        quickZoomMode = !quickZoomMode;
    }

    public void toggleHud() {
        hudHidden = !hudHidden; // só o HUD da câmera; as barras de proporção ficam
    }

    public void toggleMouse() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null || minecraft.player == null) {
            return;
        }
        if (minecraft.mouseHandler.isMouseGrabbed()) {
            minecraft.mouseHandler.releaseMouse();
        } else {
            resetPlayerRotation(minecraft.player);
            minecraft.mouseHandler.grabMouse();
        }
    }

    /** Setas cima/baixo: exposição em passos de 1/3 de stop. */
    public void adjustExposure(int steps) {
        if (playing != null) {
            return;
        }
        exposureThirds = Mth.clamp(exposureThirds + steps, -EXPOSURE_MAX_THIRDS, EXPOSURE_MAX_THIRDS);
    }

    public void resetExposure() {
        if (playing != null) {
            return;
        }
        exposureThirds = 0;
    }

    // ── Movimento do carrinho ────────────────────────────────────────────────

    /**
     * Velocidade desejada do carrinho. O rumo é o do corpo do carrinho (a direção em que foi
     * colocado), então o mouse não influencia. Só uma tecla vale por vez (a última apertada): o
     * carrinho nunca anda na diagonal.
     */
    private Vec3 computeMoveVelocity(BroadcastCameraEntity camera) {
        Vec3 velocity;
        if (playing != null) {
            velocity = camera instanceof JibCameraEntity ? Vec3.ZERO : playbackVelocity();
        } else {
            velocity = readMoveVelocity(camera);
        }
        lastControl = velocity; // é o que a gravação guarda
        return velocity;
    }

    /**
     * Shoulder camera: anda como uma pessoa, em qualquer direção (WASD juntas fazem diagonal), relativa
     * a para onde a câmera está olhando.
     */
    private Vec3 readWalkVelocity(Minecraft minecraft, BroadcastCameraEntity camera) {
        float forward = (FollowspotInputHelper.isKeyDown(minecraft.options.keyUp) ? 1f : 0f)
                - (FollowspotInputHelper.isKeyDown(minecraft.options.keyDown) ? 1f : 0f);
        float strafe = (FollowspotInputHelper.isKeyDown(minecraft.options.keyRight) ? 1f : 0f)
                - (FollowspotInputHelper.isKeyDown(minecraft.options.keyLeft) ? 1f : 0f);
        if (forward == 0f && strafe == 0f) {
            return Vec3.ZERO;
        }
        float length = (float) Math.sqrt(forward * forward + strafe * strafe);
        forward /= length; // a diagonal não é mais rápida
        strafe /= length;
        double look = Math.toRadians(camera.getYRot() + yaw);
        Vec3 ahead = new Vec3(-Math.sin(look), 0, Math.cos(look));
        Vec3 right = new Vec3(-Math.cos(look), 0, -Math.sin(look));
        return ahead.scale(forward * WALK_SPEED).add(right.scale(strafe * WALK_SPEED));
    }

    private Vec3 readMoveVelocity(BroadcastCameraEntity camera) {
        if (camera instanceof JibCameraEntity) {
            return Vec3.ZERO; // jib: o tripé fica parado, as teclas movem o braço
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null) {
            return Vec3.ZERO;
        }
        if (camera instanceof ShoulderCameraEntity) {
            return readWalkVelocity(minecraft, camera);
        }
        KeyMapping[] keys = {minecraft.options.keyUp, minecraft.options.keyDown,
                minecraft.options.keyLeft, minecraft.options.keyRight};
        boolean[] down = new boolean[4];
        for (int i = 0; i < 4; i++) {
            down[i] = FollowspotInputHelper.isKeyDown(keys[i]);
            if (down[i] && !moveKeyDown[i]) {
                moveKey = i; // a tecla recém-apertada ganha
            }
            moveKeyDown[i] = down[i];
        }
        if (moveKey >= 0 && !down[moveKey]) {
            moveKey = -1;
            for (int i = 0; i < 4; i++) {
                if (down[i]) {
                    moveKey = i;
                    break;
                }
            }
        }
        if (moveKey < 0) {
            return Vec3.ZERO;
        }
        Direction heading = Direction.fromYRot(camera.getYRot());
        Direction dir = switch (moveKey) {
            case 0 -> heading;
            case 1 -> heading.getOpposite();
            case 2 -> heading.getCounterClockWise();
            default -> heading.getClockWise();
        };
        return new Vec3(dir.getStepX() * MOVE_SPEED, 0, dir.getStepZ() * MOVE_SPEED);
    }

    // ── Gravação e reprodução ────────────────────────────────────────────────

    public boolean isRecording() {
        return recording != null;
    }

    public boolean isPlaying() {
        return playing != null;
    }

    public float recordingSeconds() {
        return recording == null ? 0f : recording.frames.size() / (float) CameraRecording.TICKS_PER_SECOND;
    }

    public float playbackSeconds() {
        return playTick / (float) CameraRecording.TICKS_PER_SECOND;
    }

    public float playbackDuration() {
        return playing == null ? 0f : playing.durationSeconds();
    }

    public String playbackName() {
        return playing == null ? "" : playing.name;
    }

    /** Tecla R: gravando → para e salva; reproduzindo → para; senão abre o menu das gravações. */
    public void onRecordKey() {
        Minecraft minecraft = Minecraft.getInstance();
        if (recording != null) {
            stopRecording();
        } else if (playing != null) {
            stopPlayback();
        } else if (minecraft.screen == null) {
            minecraft.setScreen(new CameraRecordingsScreen(this));
        }
    }

    public void startRecording(String requestedName) {
        Minecraft minecraft = Minecraft.getInstance();
        BroadcastCameraEntity camera = getCamera(minecraft);
        if (camera == null || playing != null) {
            return;
        }
        CameraRecording started = new CameraRecording();
        started.name = CameraRecordingStore.uniqueName(requestedName, jib);
        started.jib = jib;
        started.hasOrigin = true;
        started.originX = camera.getX();
        started.originY = camera.getY();
        started.originZ = camera.getZ();
        started.originYaw = camera.getYRot();
        started.dimension = minecraft.level == null ? "" : minecraft.level.dimension().location().toString();
        recYaw = recLastYaw = yaw;
        recBoomYaw = recLastBoomYaw = boomYaw;
        recording = started;
        sampleRecording(camera);
    }

    /** Um quadro por tick: tudo o que o operador controla. */
    private void sampleRecording(BroadcastCameraEntity camera) {
        recYaw += Mth.wrapDegrees(yaw - recLastYaw);
        recLastYaw = yaw;
        recBoomYaw += Mth.wrapDegrees(boomYaw - recLastBoomYaw);
        recLastBoomYaw = boomYaw;
        float[] frame = new float[CameraRecording.COLUMNS];
        frame[CameraRecording.YAW] = recYaw;
        frame[CameraRecording.PITCH] = pitch;
        frame[CameraRecording.ZOOM] = zoom;
        frame[CameraRecording.EXPOSURE] = exposureThirds;
        frame[CameraRecording.BOOM_YAW] = recBoomYaw;
        frame[CameraRecording.BOOM_PITCH] = boomPitch;
        frame[CameraRecording.EXTENSION] = camera.getTripodExtension();
        frame[CameraRecording.MOVE_X] = (float) lastControl.x;
        frame[CameraRecording.MOVE_Z] = (float) lastControl.z;
        recording.frames.add(frame);
    }

    private void stopRecording() {
        CameraRecording finished = recording;
        recording = null;
        if (finished == null || finished.frames.size() < 2) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        try {
            CameraRecordingStore.save(finished);
            if (minecraft != null && minecraft.player != null) {
                minecraft.player.displayClientMessage(Component.translatable(
                        "hud.theatricalextralights.recorder.saved", finished.name), true);
            }
        } catch (IOException e) {
            LOGGER.warn("Could not save camera recording {}", finished.name, e);
        }
    }

    public void startPlayback(CameraRecording toPlay, boolean repeat) {
        if (toPlay == null || !toPlay.isValid() || toPlay.jib != jib || recording != null) {
            return;
        }
        playing = toPlay;
        playLoop = repeat;
        playTick = 0;
        playBlend = 0f;
        playFrom[0] = yaw;
        playFrom[1] = pitch;
        playFrom[2] = zoom;
        playFrom[3] = exposureThirds;
        playFrom[4] = boomYaw;
        playFrom[5] = boomPitch;
        lastSentExtension = Float.NaN;
    }

    private void stopPlayback() {
        playing = null;
        swingVelocity = 0f;
        liftVelocity = 0f;
        moveKey = -1;
    }

    private void advancePlayback() {
        int last = playing.frames.size() - 1;
        playTick++;
        if (playTick >= last) {
            if (playLoop) {
                playTick = 0;
            } else {
                stopPlayback();
                return;
            }
        }
        // Altura do tripé / comprimento do braço: só o servidor guarda, então vai por pacote.
        float extension = playing.frames.get(playTick)[CameraRecording.EXTENSION];
        if (playTick % 2 == 0 && (Float.isNaN(lastSentExtension) || Math.abs(extension - lastSentExtension) > 0.002f)) {
            lastSentExtension = extension;
            ModNetworkHandler.CHANNEL.sendToServer(new BroadcastCameraHeightPacket(entityId, extension));
        }
    }

    /**
     * Saiu da câmera tocando uma gravação: manda o resto dela ao servidor, que continua tocando na
     * própria câmera (olhar, altura, braço e carrinho), visível para todos. Zoom e exposição só existem
     * na imagem do operador, então param. Quem montar na câmera de novo assume o controle.
     */
    private void handOffPlayback(Minecraft minecraft) {
        if (playing == null || minecraft == null || minecraft.level == null || minecraft.player == null
                || !minecraft.player.isAlive() || minecraft.level.getEntity(entityId) == null) {
            return;
        }
        List<float[]> frames = playing.frames;
        // Repetindo: manda tudo e começa de onde estava. Senão: só o que falta.
        int from = playLoop ? 0 : Math.min(playTick, frames.size() - 2);
        int startTick = playLoop ? Math.min(playTick, frames.size() - 1) : 0;
        int total = frames.size() - from;
        if (total < 2) {
            return;
        }
        for (int chunkStart = 0; chunkStart < total; chunkStart += CameraPlaybackPacket.CHUNK_FRAMES) {
            int count = Math.min(CameraPlaybackPacket.CHUNK_FRAMES, total - chunkStart);
            float[] data = new float[count * CameraRecording.COLUMNS];
            for (int i = 0; i < count; i++) {
                System.arraycopy(frames.get(from + chunkStart + i), 0, data, i * CameraRecording.COLUMNS,
                        CameraRecording.COLUMNS);
            }
            ModNetworkHandler.CHANNEL.sendToServer(
                    new CameraPlaybackPacket(entityId, total, startTick, playLoop, chunkStart, data));
        }
        minecraft.player.displayClientMessage(Component.translatable("hud.theatricalextralights.recorder.continues"), true);
    }

    private Vec3 playbackVelocity() {
        float[] frame = playing.frames.get(Math.min(playTick, playing.frames.size() - 1));
        return new Vec3(frame[CameraRecording.MOVE_X], 0, frame[CameraRecording.MOVE_Z]);
    }

    /** Aplica a gravação (interpolada entre os quadros) por cima de tudo, depois de uma transição suave. */
    private void applyPlayback(float dt, float partialTick) {
        List<float[]> frames = playing.frames;
        int last = frames.size() - 1;
        float position = Mth.clamp(playTick + partialTick, 0f, (float) last);
        int index = Math.min((int) position, last - 1);
        float t = position - index;
        float[] a = frames.get(index);
        float[] b = frames.get(index + 1);

        float weight = 1f;
        if (playBlend < 1f) {
            playBlend = Math.min(1f, playBlend + dt / 0.6f);
            weight = playBlend * playBlend * (3f - 2f * playBlend);
        }

        float recordedYaw = Mth.lerp(t, a[CameraRecording.YAW], b[CameraRecording.YAW]);
        yaw = blendAngle(playFrom[0], recordedYaw, weight);
        yawTarget = yaw;
        yawVelocity = 0f;

        float recordedPitch = Mth.lerp(t, a[CameraRecording.PITCH], b[CameraRecording.PITCH]);
        pitch = weight >= 1f ? recordedPitch : Mth.lerp(weight, playFrom[1], recordedPitch);
        pitchTarget = pitch;
        pitchVelocity = 0f;

        float recordedZoom = Mth.lerp(t, a[CameraRecording.ZOOM], b[CameraRecording.ZOOM]);
        zoom = Mth.clamp(weight >= 1f ? recordedZoom : Mth.lerp(weight, playFrom[2], recordedZoom), ZOOM_MIN, ZOOM_MAX);
        zoomTarget = zoom;
        zoomVelocity = 0f;

        float recordedExposure = Mth.lerp(t, a[CameraRecording.EXPOSURE], b[CameraRecording.EXPOSURE]);
        exposureThirds = Mth.clamp(Math.round(weight >= 1f ? recordedExposure
                : Mth.lerp(weight, playFrom[3], recordedExposure)), -EXPOSURE_MAX_THIRDS, EXPOSURE_MAX_THIRDS);

        if (jib) {
            float recordedBoomYaw = Mth.lerp(t, a[CameraRecording.BOOM_YAW], b[CameraRecording.BOOM_YAW]);
            boomYaw = Mth.wrapDegrees(blendAngle(playFrom[4], recordedBoomYaw, weight));
            float recordedBoomPitch = Mth.lerp(t, a[CameraRecording.BOOM_PITCH], b[CameraRecording.BOOM_PITCH]);
            boomPitch = Mth.clamp(weight >= 1f ? recordedBoomPitch : Mth.lerp(weight, playFrom[5], recordedBoomPitch),
                    JibCameraEntity.BOOM_PITCH_MIN, JibCameraEntity.BOOM_PITCH_MAX);
        }
    }

    /** Vai de {@code from} até {@code to} (ângulos) pelo caminho mais curto. */
    private static float blendAngle(float from, float to, float weight) {
        return weight >= 1f ? to : from + Mth.wrapDegrees(to - from) * weight;
    }

    // ── Braço da jib ─────────────────────────────────────────────────────────

    /**
     * A/D giram o braço e W/S sobem / descem a ponta. A velocidade acelera e freia suavemente,
     * como numa grua de verdade. O comprimento do braço só muda pelo menu (Shift + botão direito);
     * Espaço e Shift ficam livres (Shift desce da grua, como em qualquer montaria).
     */
    private void updateBoom(Minecraft minecraft, float dt) {
        if (minecraft.screen != null || dt <= 0f || playing != null) {
            return;
        }
        int swing = axis(minecraft.options.keyRight, minecraft.options.keyLeft);
        int lift = axis(minecraft.options.keyUp, minecraft.options.keyDown);
        float k = 1f - (float) Math.exp(-dt / BOOM_SMOOTH_TIME);
        swingVelocity += (swing * BOOM_SWING_RATE - swingVelocity) * k;
        liftVelocity += (lift * BOOM_LIFT_RATE - liftVelocity) * k;
        boomYaw = Mth.wrapDegrees(boomYaw + swingVelocity * dt);
        boomPitch = Mth.clamp(boomPitch + liftVelocity * dt,
                JibCameraEntity.BOOM_PITCH_MIN, JibCameraEntity.BOOM_PITCH_MAX);
    }

    private static int axis(KeyMapping positive, KeyMapping negative) {
        return (FollowspotInputHelper.isKeyDown(positive) ? 1 : 0)
                - (FollowspotInputHelper.isKeyDown(negative) ? 1 : 0);
    }

    // ── Tick ─────────────────────────────────────────────────────────────────

    private void tick(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        BroadcastCameraEntity camera = getCamera(minecraft);
        if (player == null || minecraft.level == null || camera == null || player.getVehicle() != camera
                || player.isDeadOrDying()) {
            stop(); // desceu (Shift, servidor, morte...) ou a câmera sumiu
            return;
        }
        if (minecraft.screen != null) {
            return; // menu aberto: a sessão espera
        }
        if (recording != null) {
            sampleRecording(camera);
            if (recording.frames.size() >= MAX_RECORDING_TICKS) {
                stopRecording();
            }
        }
        if (playing != null) {
            advancePlayback();
        }
        if (--sendCooldown <= 0) {
            sendCooldown = SEND_INTERVAL_TICKS;
            ModNetworkHandler.CHANNEL.sendToServer(new BroadcastCameraAimPacket(entityId, Mth.wrapDegrees(yaw), pitch, zoom, exposureThirds));
            if (jib) {
                ModNetworkHandler.CHANNEL.sendToServer(new JibCameraBoomPacket(entityId, boomYaw, boomPitch));
            }
        }
    }

    private BroadcastCameraEntity getCamera(Minecraft minecraft) {
        if (minecraft.level == null) {
            return null;
        }
        return minecraft.level.getEntity(entityId) instanceof BroadcastCameraEntity camera ? camera : null;
    }

    // ── Frame ────────────────────────────────────────────────────────────────

    /** Ponto/ângulos da câmera neste frame. Chamado pelo {@code CameraMixin}. */
    public record CameraState(Vec3 position, float yaw, float pitch) {
    }

    public CameraState frameState(float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        BroadcastCameraEntity camera = minecraft == null ? null : getCamera(minecraft);
        if (camera == null) {
            return null;
        }
        long now = System.nanoTime();
        float dt = Mth.clamp((now - lastFrameNanos) / 1.0e9f, 0f, 0.1f);
        lastFrameNanos = now;

        if (minecraft.screen == null && minecraft.mouseHandler.isMouseGrabbed()) {
            consumeMouse(minecraft);
        }
        if (playing != null) {
            applyPlayback(dt, partialTick);
        }
        updateAim(dt);
        updateZoomKeys(minecraft, dt);
        updateZoom(dt);
        updateExposure(minecraft, dt);
        heightDisplay += (camera.getTripodExtension() - heightDisplay) * (1f - (float) Math.exp(-dt / 0.12f));
        if (camera instanceof JibCameraEntity) {
            updateBoom(minecraft, dt);
        }

        float worldYaw = camera.getYRot() + yaw;
        Vec3 look = Vec3.directionFromRotation(pitch, worldYaw);
        Vec3 pivot = camera instanceof JibCameraEntity jibCamera
                ? jibCamera.getHeadPivot(partialTick, heightDisplay, boomYaw, boomPitch)
                : camera.getHeadPivot(partialTick, heightDisplay);
        // O olho é o próprio pivô da cabeça: pan e tilt só giram o olhar, a posição nunca muda
        // (nem desce quando o mouse vai para baixo). Quem opera não vê o corpo da câmera.
        Vec3 eye = pivot;

        // Tremor de mão: só aparece perto do zoom máximo e não altera a pose guardada.
        float t = (now - startNanos) / 1.0e9f;
        float fraction = Mth.clamp((zoom - ZOOM_MIN) / (ZOOM_MAX - ZOOM_MIN), 0f, 1f);
        float amplitude = SHAKE_DEGREES * fraction * fraction * fraction;
        float shakeYaw = amplitude * shake(t, 0.9f, 0.0f, 2.3f, 1.3f, 5.1f, 0.4f);
        float shakePitch = amplitude * shake(t, 1.1f, 2.0f, 2.9f, 0.2f, 4.7f, 1.1f);

        return new CameraState(eye, worldYaw + shakeYaw, pitch + shakePitch);
    }

    private static float shake(float t, float f1, float p1, float f2, float p2, float f3, float p3) {
        double tau = Math.PI * 2.0;
        return (float) (0.6 * Math.sin(tau * f1 * t + p1)
                + 0.3 * Math.sin(tau * f2 * t + p2)
                + 0.1 * Math.sin(tau * f3 * t + p3));
    }

    /** Amortecimento crítico (SmoothDamp): começa e termina devagar, sem passar do alvo. */
    private void updateZoom(float dt) {
        if (dt <= 0f) {
            return;
        }
        float omega = 2f / (quickZoomMode ? ZOOM_QUICK_SMOOTH_TIME : ZOOM_SMOOTH_TIME);
        float x = omega * dt;
        float exp = 1f / (1f + x + 0.48f * x * x + 0.235f * x * x * x);
        float change = zoom - zoomTarget;
        float temp = (zoomVelocity + omega * change) * dt;
        zoomVelocity = (zoomVelocity - omega * temp) * exp;
        zoom = Mth.clamp(zoomTarget + (change + temp) * exp, ZOOM_MIN, ZOOM_MAX);
    }

    private void updateExposure(Minecraft minecraft, float dt) {
        float target = exposureThirds / 3f;
        exposureDisplay += (target - exposureDisplay) * (1f - (float) Math.exp(-dt / 0.10f));
        if (exposureChain == null || !(minecraft.gameRenderer instanceof GameRendererAccessor accessor)
                || accessor.tel$getPostEffect() != exposureChain) {
            return; // outro efeito de tela assumiu: não mexe nos uniforms dele
        }
        float scale = (float) Math.pow(2.0, exposureDisplay);
        for (PostPass pass : ((PostChainAccessor) exposureChain).tel$getPasses()) {
            pass.getEffect().safeGetUniform("ColorScale").set(scale, scale, scale);
        }
    }

    private void loadExposureEffect(Minecraft minecraft) {
        if (minecraft.gameRenderer instanceof GameRendererAccessor accessor) {
            accessor.tel$loadEffect(EXPOSURE_EFFECT);
            exposureChain = accessor.tel$getPostEffect();
        }
    }

    private void unloadExposureEffect(Minecraft minecraft) {
        if (exposureChain != null && minecraft.gameRenderer instanceof GameRendererAccessor accessor
                && accessor.tel$getPostEffect() == exposureChain) {
            minecraft.gameRenderer.shutdownEffect();
        }
        exposureChain = null;
    }

    // ── Acessores para HUD / FOV ─────────────────────────────────────────────

    /** Multiplicador do campo de visão: 1 sem zoom, 1/65 no tele máximo. */
    public float fovMultiplier() {
        return 1f / zoom;
    }

    public float zoomFactor() {
        return zoom;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public float getExposureEv() {
        return exposureThirds / 3f;
    }

    public boolean isQuickZoomMode() {
        return quickZoomMode;
    }

    public boolean isHudHidden() {
        return hudHidden;
    }

    public boolean isMouseFree() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.screen == null && !minecraft.mouseHandler.isMouseGrabbed();
    }
}
