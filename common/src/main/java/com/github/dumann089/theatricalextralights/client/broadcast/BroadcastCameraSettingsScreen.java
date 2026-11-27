package com.github.dumann089.theatricalextralights.client.broadcast;

import com.github.dumann089.theatricalextralights.entities.BroadcastCameraEntity;
import com.github.dumann089.theatricalextralights.net.BroadcastCameraHeightPacket;
import com.github.dumann089.theatricalextralights.net.BroadcastCameraSettingsPacket;
import com.github.dumann089.theatricalextralights.net.ModNetworkHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.Locale;

/** Menu da câmera de broadcast (Shift + botão direito nela): altura do tripé. */
public class BroadcastCameraSettingsScreen extends Screen {

    private static final int WIDTH = 200;

    private final BroadcastCameraEntity camera;
    private HeightSlider slider;
    /** Última altura enviada e a mais recente pedida pelo slider. */
    private float sentExtension;
    private float pendingExtension;
    private EditBox nameBox;
    private String name;
    private int aspect;
    private String sentName;
    private int sentAspect;

    public BroadcastCameraSettingsScreen(BroadcastCameraEntity camera) {
        super(Component.translatable(camera.getSettingsTitleKey()));
        this.camera = camera;
        this.sentExtension = camera.getTripodExtension();
        this.pendingExtension = sentExtension;
        this.name = camera.getCameraName();
        this.aspect = camera.getAspect();
        this.sentName = name;
        this.sentAspect = aspect;
    }

    @Override
    protected void init() {
        int x = (width - WIDTH) / 2;
        int y = height / 2 - 52;
        nameBox = new EditBox(font, x, y, WIDTH, 20, Component.empty());
        nameBox.setMaxLength(24);
        nameBox.setHint(Component.translatable("gui.theatricalextralights.broadcast_camera.name_hint"));
        nameBox.setValue(name);
        nameBox.setResponder(value -> name = value);
        addRenderableWidget(nameBox);

        boolean hasHeight = camera.getExtensionMax() > camera.getExtensionMin();
        if (hasHeight) {
            slider = addRenderableWidget(new HeightSlider(x, y + 26, WIDTH, 20, pendingExtension));
        }
        Button aspectButton = Button.builder(aspectLabel(), button -> {
            aspect = (aspect + 1) % 3;
            button.setMessage(aspectLabel());
        }).bounds(x, y + 52, WIDTH, 20).build();
        addRenderableWidget(aspectButton);

        Button reset = Button.builder(Component.translatable("gui.theatricalextralights.broadcast_camera.reset"),
                button -> {
                    if (slider != null) {
                        slider.setExtension(0f);
                    }
                }).bounds(x, y + 78, WIDTH / 2 - 2, 20).build();
        reset.active = hasHeight;
        addRenderableWidget(reset);
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds(x + WIDTH / 2 + 2, y + 78, WIDTH / 2 - 2, 20).build());
    }

    private Component aspectLabel() {
        return Component.translatable("gui.theatricalextralights.aspect." + aspect);
    }

    private void sendSettingsIfChanged() {
        if (!name.equals(sentName) || aspect != sentAspect) {
            sentName = name;
            sentAspect = aspect;
            ModNetworkHandler.CHANNEL.sendToServer(new BroadcastCameraSettingsPacket(camera.getId(), name, aspect));
        }
    }

    @Override
    public void tick() {
        if (camera.isRemoved() || minecraft == null || minecraft.player == null
                || minecraft.player.distanceToSqr(camera) > 64.0) {
            super.onClose();
            return;
        }
        // Limita a um envio por tick enquanto o slider é arrastado.
        if (pendingExtension != sentExtension) {
            send(pendingExtension);
        }
        sendSettingsIfChanged();
    }

    @Override
    public void onClose() {
        if (pendingExtension != sentExtension) {
            send(pendingExtension);
        }
        sendSettingsIfChanged();
        super.onClose();
    }

    /** O jogo continua rodando atrás do menu: dá para ver a câmera subindo e descendo. */
    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void send(float extension) {
        sentExtension = extension;
        ModNetworkHandler.CHANNEL.sendToServer(new BroadcastCameraHeightPacket(camera.getId(), extension));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        g.drawCenteredString(font, title, width / 2, height / 2 - 68, 0xFFFFFF);
        super.render(g, mouseX, mouseY, partialTick);
    }

    private final class HeightSlider extends AbstractSliderButton {
        HeightSlider(int x, int y, int w, int h, float extension) {
            super(x, y, w, h, Component.empty(), toValue(extension));
            updateMessage();
        }

        void setExtension(float extension) {
            this.value = toValue(extension);
            updateMessage();
            applyValue();
        }

        @Override
        protected void updateMessage() {
            float extension = fromValue(value);
            setMessage(Component.translatable(camera.getSettingsValueKey(),
                    String.format(Locale.ROOT, "%.2f", camera.getSettingsDisplayValue(extension))));
        }

        @Override
        protected void applyValue() {
            pendingExtension = fromValue(value);
        }
    }

    private double toValue(float extension) {
        return Mth.clamp((extension - camera.getExtensionMin())
                / (camera.getExtensionMax() - camera.getExtensionMin()), 0f, 1f);
    }

    /** Valor do slider (0..1) → extensão, em passos de 0,05 bloco. */
    private float fromValue(double value) {
        float raw = camera.getExtensionMin()
                + (float) value * (camera.getExtensionMax() - camera.getExtensionMin());
        float snapped = Math.round(raw / 0.05f) * 0.05f;
        // Garante que o "0" do botão Reset e o padrão caiam exatamente em 0.
        return Math.abs(snapped) < 0.001f ? 0f : snapped;
    }
}
