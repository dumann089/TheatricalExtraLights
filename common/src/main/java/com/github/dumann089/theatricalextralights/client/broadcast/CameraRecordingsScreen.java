package com.github.dumann089.theatricalextralights.client.broadcast;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.List;

/**
 * Menu das gravações (tecla R dentro da câmera): lista as gravações da variante, grava uma nova,
 * reproduz (com ou sem repetição) e apaga. Para parar uma gravação ou reprodução, R de novo.
 */
public class CameraRecordingsScreen extends Screen {

    private static final int ROWS = 5;
    private static final int ROW_HEIGHT = 22;

    /** A escolha de repetir fica para a próxima vez que o menu abrir. */
    private static boolean loop;

    private final BroadcastCameraSession session;
    private List<CameraRecording> recordings;
    private int selected = -1;
    private int page;
    private boolean confirmDelete;
    private String pendingName = "";
    private EditBox nameBox;

    public CameraRecordingsScreen(BroadcastCameraSession session) {
        super(Component.translatable("gui.theatricalextralights.recorder.title"));
        this.session = session;
        this.recordings = CameraRecordingStore.list(session.isJib());
    }

    @Override
    protected void init() {
        int left = width / 2 - 120;
        int top = Math.max(30, height / 2 - 118);

        int pages = Math.max(1, (recordings.size() + ROWS - 1) / ROWS);
        page = Mth.clamp(page, 0, pages - 1);
        for (int row = 0; row < ROWS; row++) {
            final int index = page * ROWS + row;
            if (index >= recordings.size()) {
                break;
            }
            CameraRecording recording = recordings.get(index);
            String label = (index == selected ? "> " : "") + recording.name
                    + "  (" + CameraRecording.clock(recording.durationSeconds()) + ")";
            addRenderableWidget(Button.builder(Component.literal(label), button -> {
                selected = index;
                confirmDelete = false;
                refresh();
            }).bounds(left, top + row * ROW_HEIGHT, 240, 20).build());
        }

        int navY = top + ROWS * ROW_HEIGHT + 2;
        Button previous = Button.builder(Component.literal("<"), button -> {
            page--;
            refresh();
        }).bounds(left, navY, 20, 20).build();
        previous.active = page > 0;
        addRenderableWidget(previous);
        Button next = Button.builder(Component.literal(">"), button -> {
            page++;
            refresh();
        }).bounds(left + 220, navY, 20, 20).build();
        next.active = page < pages - 1;
        addRenderableWidget(next);

        int y = navY + 26;
        Button play = Button.builder(Component.translatable("gui.theatricalextralights.recorder.play"),
                button -> play()).bounds(left, y, 78, 20).build();
        play.active = selected >= 0;
        addRenderableWidget(play);
        Button delete = Button.builder(Component.translatable(confirmDelete
                        ? "gui.theatricalextralights.recorder.confirm_delete"
                        : "gui.theatricalextralights.recorder.delete"),
                button -> delete()).bounds(left + 81, y, 78, 20).build();
        delete.active = selected >= 0;
        addRenderableWidget(delete);
        addRenderableWidget(Button.builder(Component.translatable(loop
                        ? "gui.theatricalextralights.recorder.loop_on"
                        : "gui.theatricalextralights.recorder.loop_off"),
                button -> {
                    loop = !loop;
                    refresh();
                }).bounds(left + 162, y, 78, 20).build());

        nameBox = new EditBox(font, left, y + 26, 160, 20, Component.empty());
        nameBox.setMaxLength(32);
        nameBox.setHint(Component.translatable("gui.theatricalextralights.recorder.name_hint"));
        nameBox.setValue(pendingName);
        addRenderableWidget(nameBox);
        addRenderableWidget(Button.builder(Component.translatable("gui.theatricalextralights.recorder.record"),
                button -> record()).bounds(left + 162, y + 26, 78, 20).build());

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds(width / 2 - 50, y + 54, 100, 20).build());
    }

    private void refresh() {
        if (nameBox != null) {
            pendingName = nameBox.getValue();
        }
        rebuildWidgets();
    }

    private void record() {
        session.startRecording(nameBox == null ? "" : nameBox.getValue().trim());
        onClose();
    }

    private void play() {
        if (selected >= 0 && selected < recordings.size()) {
            session.startPlayback(recordings.get(selected), loop);
            onClose();
        }
    }

    private void delete() {
        if (selected < 0 || selected >= recordings.size()) {
            return;
        }
        if (!confirmDelete) {
            confirmDelete = true; // segundo clique confirma
            refresh();
            return;
        }
        CameraRecordingStore.delete(recordings.get(selected));
        recordings = CameraRecordingStore.list(session.isJib());
        selected = -1;
        confirmDelete = false;
        refresh();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 14, 0xFFFFFF);
        if (recordings.isEmpty()) {
            graphics.drawCenteredString(font, Component.translatable("gui.theatricalextralights.recorder.empty"),
                    width / 2, Math.max(30, height / 2 - 118) + 40, 0xAAAAAA);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
