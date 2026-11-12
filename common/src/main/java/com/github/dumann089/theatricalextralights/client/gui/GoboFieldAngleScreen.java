
package com.github.dumann089.theatricalextralights.client.gui;

import com.github.dumann089.theatricalextralights.blockentities.ExtraLightsLightBlockEntity;
import com.github.dumann089.theatricalextralights.net.ModNetworkHandler;
import com.github.dumann089.theatricalextralights.net.SetGoboFieldAnglePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.function.IntConsumer;

public final class GoboFieldAngleScreen extends TelScaledScreen {

    private static final int PANEL_WIDTH = 310;
    private static final int PANEL_HEIGHT = 148;

    private final ExtraLightsLightBlockEntity blockEntity;
    private final BlockPos pos;
    private final Screen parent;

    private FieldAngleSlider angleSlider;

    public GoboFieldAngleScreen(
            ExtraLightsLightBlockEntity blockEntity,
            BlockPos pos,
            Screen parent
    ) {
        super(Component.literal("Gobo Field Angle"));
        this.blockEntity = blockEntity;
        this.pos = pos;
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        resetScale();
        fitToScreen(PANEL_WIDTH, PANEL_HEIGHT);

        int left = (vw - PANEL_WIDTH) / 2;
        int top = (vh - PANEL_HEIGHT) / 2;

        angleSlider = addRenderableWidget(
                new FieldAngleSlider(
                        left + 15,
                        top + 69,
                        PANEL_WIDTH - 30,
                        20,
                        Math.round(blockEntity.getGoboFieldAngle()),
                        this::applyFieldAngle
                )
        );

        addRenderableWidget(
                new TelUi.FlatButton(
                        left + 15,
                        top + 108,
                        120,
                        20,
                        Component.literal("Reset"),
                        TelUi.ButtonStyle.NORMAL,
                        button -> angleSlider.setAngle(130)
                )
        );

        addRenderableWidget(
                new TelUi.FlatButton(
                        left + PANEL_WIDTH - 115,
                        top + 108,
                        100,
                        20,
                        Component.literal("Back"),
                        TelUi.ButtonStyle.PRIMARY,
                        button -> onClose()
                )
        );
    }

    private void applyFieldAngle(int angle) {
        blockEntity.setGoboFieldAngle(angle);

        ModNetworkHandler.CHANNEL.sendToServer(
                new SetGoboFieldAnglePacket(pos, angle)
        );
    }

    @Override
    protected void renderScaled(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        int left = (vw - PANEL_WIDTH) / 2;
        int top = (vh - PANEL_HEIGHT) / 2;

        TelUi.panel(
                graphics,
                left,
                top,
                PANEL_WIDTH,
                PANEL_HEIGHT
        );

        TelUi.centered(
                graphics,
                font,
                Component.literal("GOBO FIELD ANGLE"),
                vw / 2,
                top + 12,
                TelUi.TITLE
        );

        TelUi.sectionLabel(
                graphics,
                font,
                Component.literal("PROJECTION SETTINGS"),
                left + 15,
                top + 38,
                PANEL_WIDTH - 30
        );

        TelUi.label(
                graphics,
                font,
                Component.literal("Field angle"),
                left + 15,
                top + 55
        );

        renderWidgets(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        if (parent != null) {
            Minecraft.getInstance().setScreen(parent);
        } else {
            super.onClose();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static final class FieldAngleSlider extends TelUi.FlatSlider {

        private static final int MIN_ANGLE = 1;
        private static final int MAX_ANGLE = 130;

        private final IntConsumer onChanged;

        private int lastAppliedAngle;

        private FieldAngleSlider(
                int x,
                int y,
                int width,
                int height,
                int initialAngle,
                IntConsumer onChanged
        ) {
            super(
                    x,
                    y,
                    width,
                    height,
                    toProgress(initialAngle)
            );

            this.onChanged = onChanged;
            this.lastAppliedAngle = getAngle();

            updateMessage();
        }

        private static double toProgress(int angle) {
            int clamped = Math.max(
                    MIN_ANGLE,
                    Math.min(MAX_ANGLE, angle)
            );

            return (clamped - MIN_ANGLE)
                    / (double) (MAX_ANGLE - MIN_ANGLE);
        }

        private int getAngle() {
            return MIN_ANGLE + (int) Math.round(
                    getRawValue() * (MAX_ANGLE - MIN_ANGLE)
            );
        }

        public void setAngle(int angle) {
            setRawValue(toProgress(angle));
            applyValue();
        }

        @Override
        protected void updateMessage() {
            setMessage(
                    Component.literal(
                            getAngle() + "°"
                    )
            );
        }

        @Override
        protected void applyValue() {
            int angle = getAngle();

            if (angle == lastAppliedAngle) {
                return;
            }

            lastAppliedAngle = angle;
            onChanged.accept(angle);
        }
    }
}
