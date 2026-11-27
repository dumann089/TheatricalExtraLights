package com.github.dumann089.theatricalextralights.client.broadcast;

import com.github.dumann089.theatricalextralights.blockentities.VideoSwitcherConfig;
import com.github.dumann089.theatricalextralights.net.ModNetworkHandler;
import com.github.dumann089.theatricalextralights.net.SyncCameraListPacket;
import com.github.dumann089.theatricalextralights.net.SyncRequestCamerasPacket;
import com.github.dumann089.theatricalextralights.net.VideoSwitcherConfigPacket;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Menu da mesa de corte: lista as câmeras do mundo, vincula uma tecla a cada uma (clicar na câmera e
 * apertar a tecla; Delete desvincula), escolhe as teclas dos modos "corte seco" e "com fade" e a
 * duração do fade. "Operate" entra na mesa.
 */
public class VideoSwitcherScreen extends Screen {

    private static final int ROWS = 4;
    private static final int LISTEN_NONE = 0;
    private static final int LISTEN_CAMERA = 1;
    private static final int LISTEN_CUT = 2;
    private static final int LISTEN_FADE = 3;

    private record Row(UUID camera, String label) {
    }

    private final BlockPos pos;
    private final VideoSwitcherConfig config;
    private List<SyncCameraListPacket.Entry> cameras = new ArrayList<>();
    private boolean received;
    private boolean dirty;
    private int page;
    private int refreshTicks;

    private int listening = LISTEN_NONE;
    private UUID listeningCamera;
    private String listeningLabel = "";

    public VideoSwitcherScreen(BlockPos pos, VideoSwitcherConfig config) {
        super(Component.translatable("gui.theatricalextralights.switcher.title"));
        this.pos = pos;
        this.config = config;
        requestCameras();
    }

    public static void onCameraList(List<SyncCameraListPacket.Entry> list) {
        if (Minecraft.getInstance().screen instanceof VideoSwitcherScreen screen) {
            screen.update(list);
        }
    }

    private void requestCameras() {
        ModNetworkHandler.CHANNEL.sendToServer(new SyncRequestCamerasPacket(pos));
    }

    private void update(List<SyncCameraListPacket.Entry> list) {
        List<SyncCameraListPacket.Entry> sorted = new ArrayList<>(list);
        sorted.sort(Comparator.comparingDouble(e -> pos.distSqr(new BlockPos(e.x(), e.y(), e.z()))));
        if (!received || !sorted.equals(cameras)) {
            received = true;
            cameras = sorted;
            rebuildWidgets();
        }
    }

    @Override
    public void tick() {
        if (++refreshTicks >= 40) {
            refreshTicks = 0;
            requestCameras();
        }
    }

    private static String labelOf(SyncCameraListPacket.Entry camera) {
        String base = Component.translatable(camera.entityKey()).getString()
                + (camera.owner().isEmpty() ? "" : " (" + camera.owner() + ")");
        String kind = camera.name().isEmpty() ? base : camera.name() + " [" + base + "]";
        return kind + " " + camera.x() + ", " + camera.y() + ", " + camera.z();
    }

    /** As câmeras do mundo e, no fim, as vinculadas que não estão mais por aí (para poder desvincular). */
    private List<Row> rows() {
        List<Row> rows = new ArrayList<>();
        for (SyncCameraListPacket.Entry camera : cameras) {
            rows.add(new Row(camera.uuid(), labelOf(camera)));
        }
        for (VideoSwitcherConfig.Binding binding : config.bindings) {
            boolean present = false;
            for (Row row : rows) {
                if (row.camera().equals(binding.camera)) {
                    present = true;
                    break;
                }
            }
            if (!present) {
                rows.add(new Row(binding.camera, binding.label + " "
                        + Component.translatable("gui.theatricalextralights.switcher.missing").getString()));
            }
        }
        return rows;
    }

    private static String keyName(int key) {
        return key < 0 ? "—" : InputConstants.Type.KEYSYM.getOrCreate(key).getDisplayName().getString();
    }

    private String listenText() {
        return Component.translatable("gui.theatricalextralights.switcher.press_key").getString();
    }

    @Override
    protected void init() {
        int left = width / 2 - 170;
        int top = Math.max(34, height / 2 - 110);

        List<Row> rows = rows();
        int pages = Math.max(1, (rows.size() + ROWS - 1) / ROWS);
        page = Mth.clamp(page, 0, pages - 1);
        for (int i = 0; i < ROWS; i++) {
            int index = page * ROWS + i;
            if (index >= rows.size()) {
                break;
            }
            Row row = rows.get(index);
            VideoSwitcherConfig.Binding binding = config.bindingForCamera(row.camera());
            String key = listening == LISTEN_CAMERA && row.camera().equals(listeningCamera)
                    ? listenText() : binding == null ? "—" : keyName(binding.key);
            addRenderableWidget(Button.builder(Component.literal(row.label() + "   [" + key + "]"), button -> {
                listening = LISTEN_CAMERA;
                listeningCamera = row.camera();
                listeningLabel = row.label();
                rebuildWidgets();
            }).bounds(left, top + i * 22, 340, 20).build());
        }

        int navY = top + ROWS * 22 + 2;
        Button previous = Button.builder(Component.literal("<"), button -> {
            page--;
            rebuildWidgets();
        }).bounds(left, navY, 20, 20).build();
        previous.active = page > 0;
        addRenderableWidget(previous);
        Button next = Button.builder(Component.literal(">"), button -> {
            page++;
            rebuildWidgets();
        }).bounds(left + 320, navY, 20, 20).build();
        next.active = page < pages - 1;
        addRenderableWidget(next);

        int y = navY + 26;
        addRenderableWidget(Button.builder(Component.translatable("gui.theatricalextralights.switcher.cut_key",
                        listening == LISTEN_CUT ? listenText() : keyName(config.cutKey)), button -> {
            listening = LISTEN_CUT;
            rebuildWidgets();
        }).bounds(left, y, 110, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.theatricalextralights.switcher.fade_key",
                        listening == LISTEN_FADE ? listenText() : keyName(config.fadeKey)), button -> {
            listening = LISTEN_FADE;
            rebuildWidgets();
        }).bounds(left + 115, y, 110, 20).build());
        addRenderableWidget(Button.builder(Component.translatable(config.fadeMode
                        ? "gui.theatricalextralights.switcher.mode_fade"
                        : "gui.theatricalextralights.switcher.mode_cut"), button -> {
            config.fadeMode = !config.fadeMode;
            dirty = true;
            rebuildWidgets();
        }).bounds(left + 230, y, 110, 20).build());

        y += 24;
        addRenderableWidget(Button.builder(Component.literal("-"), button -> {
            config.changeFadeSeconds(-1);
            dirty = true;
            rebuildWidgets();
        }).bounds(left + 80, y, 40, 20).build());
        Button seconds = Button.builder(Component.translatable("gui.theatricalextralights.switcher.fade_seconds",
                config.fadeSeconds), button -> { }).bounds(left + 125, y, 90, 20).build();
        seconds.active = false;
        addRenderableWidget(seconds);
        addRenderableWidget(Button.builder(Component.literal("+"), button -> {
            config.changeFadeSeconds(1);
            dirty = true;
            rebuildWidgets();
        }).bounds(left + 220, y, 40, 20).build());

        y += 24;
        addRenderableWidget(Button.builder(Component.translatable("gui.theatricalextralights.switcher.aspect." + config.aspect),
                button -> {
                    config.aspect = (config.aspect + 1) % 3;
                    dirty = true;
                    rebuildWidgets();
                }).bounds(left, y, 340, 20).build());

        y += 28;
        addRenderableWidget(Button.builder(Component.translatable("gui.theatricalextralights.switcher.operate"),
                button -> Minecraft.getInstance().setScreen(new VideoSwitcherOperateScreen(pos, config)))
                .bounds(left + 40, y, 120, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds(left + 180, y, 120, 20).build());
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (listening != LISTEN_NONE) {
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                listening = LISTEN_NONE;
                rebuildWidgets();
                return true;
            }
            int bound = key == GLFW.GLFW_KEY_DELETE || key == GLFW.GLFW_KEY_BACKSPACE ? -1 : key;
            switch (listening) {
                case LISTEN_CAMERA -> config.bindCamera(listeningCamera, listeningLabel, bound);
                case LISTEN_CUT -> config.setCutKey(bound);
                case LISTEN_FADE -> config.setFadeKey(bound);
                default -> { }
            }
            listening = LISTEN_NONE;
            dirty = true;
            rebuildWidgets();
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public void removed() {
        if (dirty) {
            dirty = false;
            ModNetworkHandler.CHANNEL.sendToServer(new VideoSwitcherConfigPacket(pos, config.toTag()));
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 12, 0xFFFFFF);
        Component hint = !received
                ? Component.translatable("gui.theatricalextralights.switcher.searching")
                : cameras.isEmpty()
                ? Component.translatable("gui.theatricalextralights.switcher.none")
                : Component.translatable("gui.theatricalextralights.switcher.hint");
        graphics.drawCenteredString(font, hint, width / 2, 24, 0xAAAAAA);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
