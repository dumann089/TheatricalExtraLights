package com.github.dumann089.theatricalextralights.client.broadcast;

import com.github.dumann089.theatricalextralights.net.CameraSyncControlPacket;
import com.github.dumann089.theatricalextralights.net.ModNetworkHandler;
import com.github.dumann089.theatricalextralights.net.SyncCameraListPacket;
import com.github.dumann089.theatricalextralights.net.SyncRequestCamerasPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Menu do sincronizador de animações: lista as câmeras do mundo (o servidor informa), cada uma com a
 * gravação escolhida (clicar troca para a próxima, e volta a "nenhuma"). "Play all" entrega as gravações
 * escolhidas e começa todas no mesmo instante.
 */
public class AnimationSyncScreen extends Screen {

    private static final int ROWS = 5;

    private static boolean loop;
    /** Gravação escolhida de cada câmera (por nome); fica guardada entre uma abertura e outra. */
    private static final Map<Integer, String> ASSIGNED = new HashMap<>();

    private final List<CameraRecording> broadcastRecordings = CameraRecordingStore.list(false);
    private final List<CameraRecording> jibRecordings = CameraRecordingStore.list(true);
    private List<SyncCameraListPacket.Entry> cameras = new ArrayList<>();
    private boolean received;
    private int page;
    private int refreshTicks;

    public AnimationSyncScreen() {
        super(Component.translatable("gui.theatricalextralights.sync.title"));
        requestCameras();
    }

    /** Chamado quando o servidor manda a lista de câmeras. */
    public static void onCameraList(List<SyncCameraListPacket.Entry> list) {
        if (Minecraft.getInstance().screen instanceof AnimationSyncScreen screen) {
            screen.update(list);
        }
    }

    private static void requestCameras() {
        ModNetworkHandler.CHANNEL.sendToServer(new SyncRequestCamerasPacket());
    }

    private void update(List<SyncCameraListPacket.Entry> list) {
        LocalPlayer player = Minecraft.getInstance().player;
        List<SyncCameraListPacket.Entry> sorted = new ArrayList<>(list);
        if (player != null) {
            sorted.sort(Comparator.comparingDouble(e -> player.distanceToSqr(e.x() + 0.5, e.y(), e.z() + 0.5)));
        }
        if (!received || !sorted.equals(cameras)) {
            received = true;
            cameras = sorted;
            rebuildWidgets();
        }
    }

    @Override
    public void tick() {
        if (++refreshTicks >= 20) {
            refreshTicks = 0;
            requestCameras(); // mantém a lista e as marcas "tocando" em dia
        }
    }

    private List<CameraRecording> recordingsFor(SyncCameraListPacket.Entry camera) {
        return camera.jib() ? jibRecordings : broadcastRecordings;
    }

    private CameraRecording assignedRecording(SyncCameraListPacket.Entry camera) {
        String name = ASSIGNED.get(camera.id());
        if (name == null) {
            return null;
        }
        for (CameraRecording recording : recordingsFor(camera)) {
            if (recording.name.equals(name)) {
                return recording;
            }
        }
        return null;
    }

    private boolean anyAssigned() {
        for (SyncCameraListPacket.Entry camera : cameras) {
            if (assignedRecording(camera) != null) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void init() {
        int left = width / 2 - 170;
        int top = Math.max(30, height / 2 - 105);

        int pages = Math.max(1, (cameras.size() + ROWS - 1) / ROWS);
        page = Mth.clamp(page, 0, pages - 1);
        for (int row = 0; row < ROWS; row++) {
            int index = page * ROWS + row;
            if (index >= cameras.size()) {
                break;
            }
            SyncCameraListPacket.Entry camera = cameras.get(index);
            addRenderableWidget(Button.builder(rowLabel(camera), button -> cycle(camera))
                    .bounds(left, top + row * 22, 340, 20).build());
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
        Button playAll = Button.builder(Component.translatable("gui.theatricalextralights.sync.play_all"),
                button -> playAll()).bounds(left, y, 110, 20).build();
        playAll.active = anyAssigned();
        addRenderableWidget(playAll);
        addRenderableWidget(Button.builder(Component.translatable("gui.theatricalextralights.sync.stop_all"),
                button -> stopAll()).bounds(left + 115, y, 110, 20).build());
        addRenderableWidget(Button.builder(Component.translatable(loop
                        ? "gui.theatricalextralights.recorder.loop_on"
                        : "gui.theatricalextralights.recorder.loop_off"),
                button -> {
                    loop = !loop;
                    rebuildWidgets();
                }).bounds(left + 230, y, 110, 20).build());

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds(width / 2 - 50, y + 28, 100, 20).build());
    }

    private Component rowLabel(SyncCameraListPacket.Entry camera) {
        String base = Component.translatable(camera.entityKey()).getString()
                + (camera.owner().isEmpty() ? "" : " (" + camera.owner() + ")");
        String kind = camera.name().isEmpty() ? base : camera.name() + " [" + base + "]";
        CameraRecording recording = assignedRecording(camera);
        String animation = recording == null ? "—" : recording.name;
        LocalPlayer player = Minecraft.getInstance().player;
        int distance = player == null ? 0
                : (int) Math.sqrt(player.distanceToSqr(camera.x() + 0.5, camera.y(), camera.z() + 0.5));
        return Component.literal((camera.playing() ? "▶ " : "") + kind + "  " + camera.x() + ", " + camera.y()
                + ", " + camera.z() + "  (" + distance + "m)  →  " + animation);
    }

    /** Próxima gravação da variante; depois da última, volta a "nenhuma". */
    private void cycle(SyncCameraListPacket.Entry camera) {
        List<CameraRecording> options = recordingsFor(camera);
        CameraRecording current = assignedRecording(camera);
        int next = current == null ? 0 : options.indexOf(current) + 1;
        if (next >= options.size()) {
            ASSIGNED.remove(camera.id());
        } else {
            ASSIGNED.put(camera.id(), options.get(next).name);
        }
        rebuildWidgets();
    }

    private void playAll() {
        List<Integer> ids = new ArrayList<>();
        for (SyncCameraListPacket.Entry camera : cameras) {
            CameraRecording recording = assignedRecording(camera);
            if (recording != null) {
                // Cada câmera recebe e guarda a sua gravação, sem tocar ainda.
                CameraRecordingSender.send(camera.id(), recording.frames, 0, 0, loop, true, recording);
                ids.add(camera.id());
            }
        }
        if (!ids.isEmpty()) {
            // Chega depois de todos os pedaços (mesma conexão, em ordem) e dispara tudo junto.
            ModNetworkHandler.CHANNEL.sendToServer(new CameraSyncControlPacket(true, ids));
        }
    }

    private void stopAll() {
        List<Integer> ids = new ArrayList<>();
        for (SyncCameraListPacket.Entry camera : cameras) {
            ids.add(camera.id());
        }
        if (!ids.isEmpty()) {
            ModNetworkHandler.CHANNEL.sendToServer(new CameraSyncControlPacket(false, ids));
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 14, 0xFFFFFF);
        Component hint = !received
                ? Component.translatable("gui.theatricalextralights.sync.searching")
                : cameras.isEmpty()
                ? Component.translatable("gui.theatricalextralights.sync.none")
                : Component.translatable("gui.theatricalextralights.sync.hint");
        graphics.drawCenteredString(font, hint, width / 2, 26, 0xAAAAAA);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
