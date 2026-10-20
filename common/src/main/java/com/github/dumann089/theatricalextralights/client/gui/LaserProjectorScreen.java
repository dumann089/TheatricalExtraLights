package com.github.dumann089.theatricalextralights.client.gui;

import com.github.dumann089.theatricalextralights.blockentities.LaserProjectorBlockEntity;
import com.github.dumann089.theatricalextralights.laser.dac.LaserDacHub;
import com.github.dumann089.theatricalextralights.laser.dac.LaserDacStatus;
import com.github.dumann089.theatricalextralights.laser.dac.LaserProtocol;
import com.github.dumann089.theatricalextralights.net.ModNetworkHandler;
import com.github.dumann089.theatricalextralights.net.SetLaserProjectorSettingsPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public class LaserProjectorScreen extends Screen {

    private static final int PANEL_WIDTH = 280;
    private static final int PANEL_HEIGHT = 196;
    private static final int COLOR_PANEL_BG = 0xFFC6C6C6;
    private static final int COLOR_PANEL_BORDER = 0xFF1F1F1F;
    private static final int COLOR_TEXT = 0x404040;
    private static final int COLOR_OK = 0x2E7D32;
    private static final int COLOR_WARN = 0xB84000;

    private final LaserProjectorBlockEntity blockEntity;
    private final BlockPos pos;

    private float scanHalfAngle;
    private float projectionScale;
    private int panelLeft;
    private int panelTop;

    public LaserProjectorScreen(LaserProjectorBlockEntity blockEntity, BlockPos pos) {
        super(Component.translatable("screen.laser_projector.title"));
        this.blockEntity = blockEntity;
        this.pos = pos;
        this.scanHalfAngle = blockEntity.getScanHalfAngle();
        this.projectionScale = blockEntity.getProjectionScale();
    }

    @Override
    protected void init() {
        super.init();
        panelLeft = (width - PANEL_WIDTH) / 2;
        panelTop = (height - PANEL_HEIGHT) / 2;
        int x = panelLeft + 16;
        int w = PANEL_WIDTH - 32;

        addRenderableWidget(new AbstractSliderButton(x, panelTop + 96, w, 20,
                Component.empty(), (scanHalfAngle - 5f) / 55f) {
            {
                updateMessage();
            }

            @Override
            protected void updateMessage() {
                setMessage(Component.translatable("screen.laser_projector.scan", Math.round(scanHalfAngle)));
            }

            @Override
            protected void applyValue() {
                scanHalfAngle = 5f + (float) value * 55f;
            }
        });

        addRenderableWidget(new AbstractSliderButton(x, panelTop + 124, w, 20,
                Component.empty(), (projectionScale - 0.25f) / 2.75f) {
            {
                updateMessage();
            }

            @Override
            protected void updateMessage() {
                setMessage(Component.translatable("screen.laser_projector.scale",
                        String.format("%.2f", projectionScale)));
            }

            @Override
            protected void applyValue() {
                projectionScale = 0.25f + (float) value * 2.75f;
            }
        });

        addRenderableWidget(Button.builder(Component.translatable("screen.laser_projector.save"), b -> {
            ModNetworkHandler.CHANNEL.sendToServer(new SetLaserProjectorSettingsPacket(
                    pos, blockEntity.getDacId(), scanHalfAngle, projectionScale));
            onClose();
        }).bounds(x, panelTop + 156, w, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        g.fill(panelLeft, panelTop, panelLeft + PANEL_WIDTH, panelTop + PANEL_HEIGHT, COLOR_PANEL_BG);
        g.fill(panelLeft, panelTop, panelLeft + PANEL_WIDTH, panelTop + 1, COLOR_PANEL_BORDER);
        g.fill(panelLeft, panelTop + PANEL_HEIGHT - 1, panelLeft + PANEL_WIDTH, panelTop + PANEL_HEIGHT, COLOR_PANEL_BORDER);
        g.fill(panelLeft, panelTop, panelLeft + 1, panelTop + PANEL_HEIGHT, COLOR_PANEL_BORDER);
        g.fill(panelLeft + PANEL_WIDTH - 1, panelTop, panelLeft + PANEL_WIDTH, panelTop + PANEL_HEIGHT, COLOR_PANEL_BORDER);

        g.drawCenteredString(font, title, panelLeft + PANEL_WIDTH / 2, panelTop + 10, COLOR_TEXT);

        LaserDacStatus status = LaserDacHub.status(blockEntity.getDacId());
        int statusColor = status.playing() ? COLOR_OK : (status.connected ? COLOR_TEXT : COLOR_WARN);
        g.drawString(font, Component.translatable("screen.laser_projector.status", playbackLabel(status)),
                panelLeft + 16, panelTop + 28, statusColor, false);
        g.drawString(font, Component.translatable("screen.laser_projector.link",
                        status.listening
                                ? (status.connected ? status.remoteHost : Component.translatable("screen.laser_projector.listening").getString())
                                : Component.translatable("screen.laser_projector.offline").getString()),
                panelLeft + 16, panelTop + 42, COLOR_TEXT, false);
        g.drawString(font, Component.translatable("screen.laser_projector.buffer",
                        status.bufferFullness, status.bufferCapacity),
                panelLeft + 16, panelTop + 56, COLOR_TEXT, false);
        g.drawString(font, Component.translatable("screen.laser_projector.rate",
                        status.pointRate, status.pointCount),
                panelLeft + 16, panelTop + 70, COLOR_TEXT, false);

        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static String playbackLabel(LaserDacStatus status) {
        if (!status.listening) {
            return Component.translatable("screen.laser_projector.state.offline").getString();
        }
        if (status.lightEngineState == LaserProtocol.LIGHT_ESTOP) {
            return Component.translatable("screen.laser_projector.state.estop").getString();
        }
        return switch (status.playbackState) {
            case LaserProtocol.PLAYBACK_PREPARED ->
                    Component.translatable("screen.laser_projector.state.prepared").getString();
            case LaserProtocol.PLAYBACK_PLAYING ->
                    Component.translatable("screen.laser_projector.state.playing").getString();
            default -> Component.translatable("screen.laser_projector.state.idle").getString();
        };
    }
}
