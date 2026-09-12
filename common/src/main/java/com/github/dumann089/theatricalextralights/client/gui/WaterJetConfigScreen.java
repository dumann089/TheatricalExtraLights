package com.github.dumann089.theatricalextralights.client.gui;

import com.github.dumann089.theatricalextralights.blockentities.interfaces.HasJetConeAngle;
import com.github.dumann089.theatricalextralights.blockentities.interfaces.HasJetHeight;
import com.github.dumann089.theatricalextralights.blockentities.interfaces.HasJetThickness;
import com.github.dumann089.theatricalextralights.net.ModNetworkHandler;
import com.github.dumann089.theatricalextralights.net.SetJetConeAnglePacket;
import com.github.dumann089.theatricalextralights.net.SetJetHeightPacket;
import com.github.dumann089.theatricalextralights.net.SetJetThicknessPacket;
import dev.imabad.theatrical.blockentities.light.BaseDMXConsumerLightBlockEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class WaterJetConfigScreen extends ExtraLightsConfigScreen {

    public enum Mode {
        GENERIC(false, false),
        MANUAL(true, false),
        CONE(true, true);

        private final boolean panTilt;
        private final boolean coneAngle;

        Mode(boolean panTilt, boolean coneAngle) {
            this.panTilt = panTilt;
            this.coneAngle = coneAngle;
        }
    }

    private final Mode mode;
    private EditBox heightField;
    private JetFloatSlider thicknessSlider;
    private JetFloatSlider coneAngleSlider;

    private int jetSectionY;
    private int heightLabelY;
    private int thicknessLabelY;
    private int coneLabelY;

    public WaterJetConfigScreen(BaseDMXConsumerLightBlockEntity blockEntity, BlockPos pos, String title, Mode mode) {
        super(blockEntity, pos, title, mode.panTilt);
        this.mode = mode;
    }

    @Override
    protected int extraLayoutRows() {
        int rows = 1;
        rows += 2;
        if (mode.coneAngle) {
            rows += 1;
        }
        return rows;
    }

    @Override
    protected int buildExtraWidgets(int y) {
        jetSectionY = y;
        y += LABEL_GAP + 3;

        heightLabelY = y;
        y += LABEL_GAP;
        heightField = createField(contentLeft, y, contentWidth, WIDGET_HEIGHT,
                Component.translatable("screen.waterjet.height"));
        heightField.setFilter(value -> value.isEmpty() || value.matches("\\d*(\\.\\d*)?"));
        if (blockEntity instanceof HasJetHeight jet) {
            heightField.setValue(Float.toString(jet.getJetHeight()));
        } else {
            heightField.setValue("20.0");
        }
        y += WIDGET_HEIGHT + ROW_GAP;

        thicknessLabelY = y;
        y += LABEL_GAP;
        float thickness = blockEntity instanceof HasJetThickness jetThickness
                ? jetThickness.getJetThickness() : 0.12f;
        thicknessSlider = addRenderableWidget(new JetFloatSlider(
                contentLeft, y, contentWidth, 0.02f, 1.0f, thickness
        ));
        y += WIDGET_HEIGHT + ROW_GAP;

        if (mode.coneAngle) {
            coneLabelY = y;
            y += LABEL_GAP;
            float angle = blockEntity instanceof HasJetConeAngle jetCone
                    ? jetCone.getJetConeAngle() : 45f;
            coneAngleSlider = addRenderableWidget(new JetFloatSlider(
                    contentLeft, y, contentWidth, 5f, 90f, angle
            ));
            y += WIDGET_HEIGHT + ROW_GAP;
        } else {
            coneAngleSlider = null;
        }

        return y + SECTION_GAP - ROW_GAP;
    }

    @Override
    protected void renderExtraLabels(GuiGraphics guiGraphics) {
        drawSectionLabel(guiGraphics, Component.translatable("screen.waterjet.section"), jetSectionY);
        drawFieldLabel(guiGraphics, Component.translatable("screen.waterjet.height"), heightLabelY);
        drawFieldLabel(guiGraphics, Component.translatable("screen.waterjet.thickness"), thicknessLabelY);
        if (mode.coneAngle) {
            drawFieldLabel(guiGraphics, Component.translatable("screen.waterjet.cone_angle"), coneLabelY);
        }
    }

    @Override
    protected void commitExtraChanges() {
        if (blockEntity instanceof HasJetHeight) {
            try {
                float height = Float.parseFloat(heightField.getValue());
                ModNetworkHandler.CHANNEL.sendToServer(new SetJetHeightPacket(blockEntity.getBlockPos(), height));
            } catch (NumberFormatException ignored) {
            }
        }
        if (blockEntity instanceof HasJetThickness && thicknessSlider != null) {
            ModNetworkHandler.CHANNEL.sendToServer(
                    new SetJetThicknessPacket(blockEntity.getBlockPos(), thicknessSlider.getValue())
            );
        }
        if (mode.coneAngle && blockEntity instanceof HasJetConeAngle && coneAngleSlider != null) {
            ModNetworkHandler.CHANNEL.sendToServer(
                    new SetJetConeAnglePacket(blockEntity.getBlockPos(), coneAngleSlider.getValue())
            );
        }
    }

    private static class JetFloatSlider extends TelUi.FlatSlider {

        private final float minValue;
        private final float maxValue;

        private JetFloatSlider(int x, int y, int width, float minValue, float maxValue, float value) {
            super(x, y, width, WIDGET_HEIGHT, (value - minValue) / (double) (maxValue - minValue));
            this.minValue = minValue;
            this.maxValue = maxValue;
            updateMessage();
        }

        float getValue() {
            return minValue + (maxValue - minValue) * (float) value;
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.literal(String.format("%.2f", getValue())));
        }

        @Override
        protected void applyValue() {
        }
    }
}
