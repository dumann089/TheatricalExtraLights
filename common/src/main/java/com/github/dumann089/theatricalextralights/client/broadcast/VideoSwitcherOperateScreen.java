package com.github.dumann089.theatricalextralights.client.broadcast;

import com.github.dumann089.theatricalextralights.blockentities.VideoSwitcherConfig;
import com.github.dumann089.theatricalextralights.entities.BroadcastCameraEntity;
import com.github.dumann089.theatricalextralights.net.ModNetworkHandler;
import com.github.dumann089.theatricalextralights.net.VideoSwitcherConfigPacket;
import com.github.dumann089.theatricalextralights.net.VideoSwitcherProgramPacket;
import com.github.dumann089.theatricalextralights.net.VideoSwitcherSequencePacket;
import com.github.dumann089.theatricalextralights.blockentities.VideoSwitcherBlockEntity;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.lwjgl.glfw.GLFW;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Mesa de corte em operação: a tela mostra só o sinal da câmera no ar (ver {@link SwitcherView}); não dá
 * para controlar as câmeras daqui. Teclas: a de cada câmera corta para ela; as de modo escolhem corte
 * seco ou com fade; no fade, as setas mudam a duração de 1 em 1 segundo. Esc sai.
 */
public class VideoSwitcherOperateScreen extends Screen {

    private final BlockPos pos;
    private final VideoSwitcherConfig config;
    private final Set<Integer> down = new HashSet<>();
    private boolean started;
    /** Última troca de câmera vista na mesa (a sequência troca sem o operador mexer: o sinal acompanha). */
    private int lastRevision;
    private boolean dirty;
    private boolean hudHidden;
    private boolean savedHideGui;
    private CameraType savedCameraType;

    public VideoSwitcherOperateScreen(BlockPos pos, VideoSwitcherConfig config) {
        super(Component.translatable("gui.theatricalextralights.switcher.title"));
        this.pos = pos;
        this.config = config;
    }

    @Override
    protected void init() {
        if (started) {
            return; // redimensionar a janela chama init de novo
        }
        started = true;
        Minecraft minecraft = Minecraft.getInstance();
        savedHideGui = minecraft.options.hideGui;
        savedCameraType = minecraft.options.getCameraType();
        minecraft.options.hideGui = true;
        minecraft.options.setCameraType(CameraType.FIRST_PERSON);
        // Centro das câmeras vinculadas que estão carregadas: a grade de chunks fica parada aí.
        double sumX = 0;
        double sumZ = 0;
        int count = 0;
        ClientLevel level = minecraft.level;
        if (level != null) {
            for (Entity entity : level.entitiesForRendering()) {
                if (entity instanceof BroadcastCameraEntity camera && config.bindingForCamera(camera.getUUID()) != null) {
                    sumX += camera.getX();
                    sumZ += camera.getZ();
                    count++;
                }
            }
        }
        SwitcherView.begin(count > 0, count > 0 ? sumX / count : 0, count > 0 ? sumZ / count : 0);
        java.util.List<UUID> bound = new java.util.ArrayList<>();
        for (VideoSwitcherConfig.Binding binding : config.bindings) {
            bound.add(binding.camera);
        }
        VideoSwitcherBlockEntity desk = desk();
        if (desk != null) {
            lastRevision = desk.getProgramRevision();
        }
        UUID onAir = desk != null ? desk.getProgram() : null;
        if (onAir != null && config.bindingForCamera(onAir) != null) {
            SwitcherView.cut(onAir); // abrir a mesa não muda o que já está no ar (nem interrompe a sequência)
        } else if (!config.bindings.isEmpty()) {
            SwitcherView.cut(config.bindings.get(0).camera); // começa na primeira câmera vinculada
            // Os jumbotrons vinculados passam a mostrar esta câmera também.
            ModNetworkHandler.CHANNEL.sendToServer(new VideoSwitcherProgramPacket(pos, config.bindings.get(0).camera, false));
        }
    }

    /** A mesa deste bloco, como o cliente a conhece (null se o chunk não está carregado). */
    private VideoSwitcherBlockEntity desk() {
        ClientLevel level = Minecraft.getInstance().level;
        return level != null && level.getBlockEntity(pos) instanceof VideoSwitcherBlockEntity desk ? desk : null;
    }

    private void sequence(byte action) {
        ModNetworkHandler.CHANNEL.sendToServer(new VideoSwitcherSequencePacket(pos, action));
    }

    private static String clock(long ticks) {
        long seconds = Math.max(0, ticks) / 20;
        return (seconds / 60) + ":" + String.format("%02d", seconds % 60);
    }

    @Override
    public void removed() {
        if (!started) {
            return;
        }
        started = false;
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.options.hideGui = savedHideGui;
        if (savedCameraType != null) {
            minecraft.options.setCameraType(savedCameraType);
        }
        SwitcherView.end();
        if (dirty) {
            dirty = false;
            ModNetworkHandler.CHANNEL.sendToServer(new VideoSwitcherConfigPacket(pos, config.toTag()));
        }
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            return super.keyPressed(key, scanCode, modifiers); // fecha a tela
        }
        if (!down.add(key)) {
            return true; // tecla segurada (repetição): só vale o primeiro aperto
        }
        if (key == config.cutKey) {
            config.fadeMode = false;
            dirty = true;
            return true;
        }
        if (key == config.fadeKey) {
            config.fadeMode = true;
            dirty = true;
            return true;
        }
        VideoSwitcherConfig.Binding binding = config.bindingForKey(key);
        if (binding != null) {
            take(binding.camera);
            return true;
        }
        // Gravador de sequências (as teclas das câmeras e de modo, acima, têm prioridade).
        VideoSwitcherBlockEntity desk = desk();
        if (key == GLFW.GLFW_KEY_R) {
            sequence(desk != null && desk.isRecording()
                    ? VideoSwitcherSequencePacket.RECORD_STOP : VideoSwitcherSequencePacket.RECORD_START);
            return true;
        }
        if (key == GLFW.GLFW_KEY_P) {
            sequence(desk != null && desk.isPlaying()
                    ? VideoSwitcherSequencePacket.STOP : VideoSwitcherSequencePacket.PLAY);
            return true;
        }
        if (key == GLFW.GLFW_KEY_L) {
            sequence(VideoSwitcherSequencePacket.TOGGLE_LOOP);
            return true;
        }
        if (key == GLFW.GLFW_KEY_BACKSPACE) {
            sequence(VideoSwitcherSequencePacket.CLEAR);
            return true;
        }
        if (key == GLFW.GLFW_KEY_Y) {
            hudHidden = !hudHidden; // esconde / mostra o HUD (só o sinal da câmera fica na tela)
            return true;
        }
        if (config.fadeMode) {
            if (key == GLFW.GLFW_KEY_UP || key == GLFW.GLFW_KEY_RIGHT) {
                config.changeFadeSeconds(1);
                dirty = true;
            } else if (key == GLFW.GLFW_KEY_DOWN || key == GLFW.GLFW_KEY_LEFT) {
                config.changeFadeSeconds(-1);
                dirty = true;
            }
        }
        return true;
    }

    @Override
    public boolean keyReleased(int key, int scanCode, int modifiers) {
        down.remove(key);
        return true;
    }

    private void take(UUID camera) {
        if (config.fadeMode) {
            if (!camera.equals(SwitcherView.programId())) {
                SwitcherView.fadeTo(camera, config.fadeSeconds);
                ModNetworkHandler.CHANNEL.sendToServer(new VideoSwitcherProgramPacket(pos, camera, true));
            }
        } else {
            SwitcherView.cut(camera);
            ModNetworkHandler.CHANNEL.sendToServer(new VideoSwitcherProgramPacket(pos, camera, false));
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        SwitcherView.update();
        VideoSwitcherBlockEntity desk = desk();
        if (desk != null && desk.getProgramRevision() != lastRevision) {
            lastRevision = desk.getProgramRevision();
            // Troca feita pela sequência tocando: o sinal da mesa acompanha (fade incluído).
            if (desk.isPlaying() && desk.getProgram() != null) {
                if (desk.isProgramFade()) {
                    SwitcherView.fadeTo(desk.getProgram(), desk.getProgramFadeSeconds());
                } else {
                    SwitcherView.cut(desk.getProgram());
                }
            }
        }
        // Sem renderBackground: o mundo já é a imagem da câmera.
        boolean signal = SwitcherView.hasSignal();
        if (!signal) {
            graphics.fill(0, 0, width, height, 0xFF000000);
            graphics.drawCenteredString(font, Component.translatable("gui.theatricalextralights.switcher.no_signal"),
                    width / 2, height / 2 - 4, 0xFFFF5555);
        }
        // Dissolução: a câmera que sai (também ao vivo) some por cima da que entra.
        SwitcherView.drawOutgoing(graphics, width, height);
        // Proporção da imagem: a da mesa, ou a da câmera que está no ar. Fica mesmo com o HUD escondido.
        AspectBars.draw(graphics, width, height,
                AspectBars.ratio(config.aspect != 0 ? config.aspect : SwitcherView.programAspect()));

        if (!hudHidden) {
            String label = "—";
            VideoSwitcherConfig.Binding onAir = SwitcherView.programId() == null ? null
                    : config.bindingForCamera(SwitcherView.programId());
            if (onAir != null) {
                label = onAir.label;
            }
            Component mode = config.fadeMode
                    ? Component.translatable("gui.theatricalextralights.switcher.hud_fade", config.fadeSeconds)
                    : Component.translatable("gui.theatricalextralights.switcher.hud_cut");
            graphics.drawString(font, Component.literal("● PGM  " + label + "   [").append(mode).append("]"),
                    10, height - 24, 0xFFFF6666, true);
            if (desk != null && minecraft() != null && minecraft().level != null) {
                long now = minecraft().level.getGameTime();
                String status = null;
                int color = 0xFFAAAAAA;
                String loopMark = desk.isLoop() ? "  ↻" : "";
                if (desk.isRecording()) {
                    status = "● REC " + clock(now - desk.getRecordStartTick()) + "  (" + desk.getSequenceSize() + ")";
                    color = 0xFFFF4444;
                } else if (desk.isPlaying()) {
                    long elapsed = Math.min(now - desk.getPlayStartTick(), desk.getSequenceLength());
                    status = "▶ PLAY " + clock(elapsed) + " / " + clock(desk.getSequenceLength()) + loopMark;
                    color = 0xFF55FF55;
                } else if (desk.getSequenceSize() > 0) {
                    status = "■ SEQ " + desk.getSequenceSize() + " · " + clock(desk.getSequenceLength()) + loopMark;
                }
                if (status != null) {
                    graphics.drawString(font, Component.literal(status), 10, height - 46, color, true);
                }
            }
            graphics.drawString(font, Component.translatable("gui.theatricalextralights.switcher.hud_hint"),
                    10, height - 13, 0x99FFFFFF, true);
            if (!Minecraft.getInstance().levelRenderer.hasRenderedAllChunks()) {
                graphics.drawString(font, Component.translatable("gui.theatricalextralights.switcher.hud_loading"),
                        10, height - 35, 0xFFFFD84A, true);
            }
        }
        // Os botões e o resto da tela não existem: não há nada para clicar aqui.
    }

    private static Minecraft minecraft() {
        return Minecraft.getInstance();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
