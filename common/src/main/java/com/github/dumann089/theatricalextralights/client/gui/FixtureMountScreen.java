package com.github.dumann089.theatricalextralights.client.gui;

import com.github.dumann089.theatricalextralights.blockentities.ExtraLightsLightBlockEntity;
import com.github.dumann089.theatricalextralights.client.StrobeRenderHelper;
import com.github.dumann089.theatricalextralights.net.ModNetworkHandler;
import com.github.dumann089.theatricalextralights.net.SetMountTransformPacket;
import com.github.dumann089.theatricalextralights.util.FixtureMountTransform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.Locale;
import java.util.function.DoubleConsumer;

/**
 * Side overlay mount editor — world stays visible while nudging the fixture.
 */
public class FixtureMountScreen extends Screen {

    private static final int PANEL_WIDTH = 220;
    private static final int PANEL_MARGIN = 10;
    private static final int ROW_HEIGHT = 20;
    private static final int ROW_GAP = 4;
    private static final int SECTION_GAP = 10;
    private static final int BTN_SIZE = 20;
    private static final int TRACK_PAD = 2;

    private static final int COLOR_TRACK = 0xAA2A2A2A;
    private static final int COLOR_TRACK_BORDER = 0xFF101010;
    private static final int COLOR_HANDLE = 0xFFC6C6C6;
    private static final int COLOR_HANDLE_BORDER = 0xFF373737;
    private static final int COLOR_SECTION = 0xFFD0D0D0;
    private static final int COLOR_X = 0xFFFF5555;
    private static final int COLOR_Y = 0xFF55FF55;
    private static final int COLOR_Z = 0xFF5555FF;

    private final ExtraLightsLightBlockEntity blockEntity;
    private final BlockPos pos;

    private SideMountSlider offsetXSlider;
    private SideMountSlider offsetYSlider;
    private SideMountSlider offsetZSlider;
    private SideMountSlider pitchSlider;
    private SideMountSlider yawSlider;
    private SideMountSlider rollSlider;

    private int panelLeft;
    private int panelTop;
    private int contentWidth;
    private int positionHeaderY;
    private int rotationHeaderY;

    public FixtureMountScreen(ExtraLightsLightBlockEntity blockEntity, BlockPos pos) {
        super(Component.translatable("screen.theatricalextralights.fixture_mount"));
        this.blockEntity = blockEntity;
        this.pos = pos;
    }

    @Override
    protected void init() {
        super.init();

        if (blockEntity == null) {
            Minecraft.getInstance().setScreen(null);
            return;
        }

        panelLeft = width - PANEL_WIDTH - PANEL_MARGIN;
        contentWidth = PANEL_WIDTH - 4;
        panelTop = 28;

        int y = panelTop;
        positionHeaderY = y;
        y += 12 + ROW_GAP;

        offsetXSlider = addSideSlider(y, blockEntity.getMountOffsetX(), FixtureMountTransform.MAX_OFFSET, 0.05f,
                'X', COLOR_X, this::applyOffsetX);
        y += ROW_HEIGHT + ROW_GAP;
        offsetYSlider = addSideSlider(y, blockEntity.getMountOffsetY(), FixtureMountTransform.MAX_OFFSET, 0.05f,
                'Y', COLOR_Y, this::applyOffsetY);
        y += ROW_HEIGHT + ROW_GAP;
        offsetZSlider = addSideSlider(y, blockEntity.getMountOffsetZ(), FixtureMountTransform.MAX_OFFSET, 0.05f,
                'Z', COLOR_Z, this::applyOffsetZ);
        y += ROW_HEIGHT + SECTION_GAP;

        rotationHeaderY = y;
        y += 12 + ROW_GAP;

        pitchSlider = addSideSlider(y, blockEntity.getMountPitch(), FixtureMountTransform.MAX_ANGLE, 1.0f,
                'X', COLOR_X, this::applyPitch);
        y += ROW_HEIGHT + ROW_GAP;
        yawSlider = addSideSlider(y, blockEntity.getMountYaw(), FixtureMountTransform.MAX_ANGLE, 1.0f,
                'Y', COLOR_Y, this::applyYaw);
        y += ROW_HEIGHT + ROW_GAP;
        rollSlider = addSideSlider(y, blockEntity.getMountRoll(), FixtureMountTransform.MAX_ANGLE, 1.0f,
                'Z', COLOR_Z, this::applyRoll);
        y += ROW_HEIGHT + SECTION_GAP;

        int half = (contentWidth - 4) / 2;
        addRenderableWidget(Button.builder(Component.translatable("screen.theatricalextralights.mount.reset"),
                        button -> resetMount())
                .bounds(panelLeft + 2, y, half, ROW_HEIGHT)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(panelLeft + 2 + half + 4, y, half, ROW_HEIGHT)
                .build());
    }

    private SideMountSlider addSideSlider(int y, float current, float maxAbs, float step,
                                          char axis, int axisColor, DoubleConsumer onChange) {
        SideMountSlider slider = new SideMountSlider(panelLeft + 2, y, contentWidth, current, maxAbs, step,
                axis, axisColor, onChange);
        addRenderableWidget(slider);
        return slider;
    }

    private void applyOffsetX(double value) {
        blockEntity.setMountTransform((float) value, blockEntity.getMountOffsetY(), blockEntity.getMountOffsetZ(),
                blockEntity.getMountYaw(), blockEntity.getMountPitch(), blockEntity.getMountRoll());
        sendUpdate();
    }

    private void applyOffsetY(double value) {
        blockEntity.setMountTransform(blockEntity.getMountOffsetX(), (float) value, blockEntity.getMountOffsetZ(),
                blockEntity.getMountYaw(), blockEntity.getMountPitch(), blockEntity.getMountRoll());
        sendUpdate();
    }

    private void applyOffsetZ(double value) {
        blockEntity.setMountTransform(blockEntity.getMountOffsetX(), blockEntity.getMountOffsetY(), (float) value,
                blockEntity.getMountYaw(), blockEntity.getMountPitch(), blockEntity.getMountRoll());
        sendUpdate();
    }

    private void applyYaw(double value) {
        blockEntity.setMountTransform(blockEntity.getMountOffsetX(), blockEntity.getMountOffsetY(), blockEntity.getMountOffsetZ(),
                (float) value, blockEntity.getMountPitch(), blockEntity.getMountRoll());
        sendUpdate();
    }

    private void applyPitch(double value) {
        blockEntity.setMountTransform(blockEntity.getMountOffsetX(), blockEntity.getMountOffsetY(), blockEntity.getMountOffsetZ(),
                blockEntity.getMountYaw(), (float) value, blockEntity.getMountRoll());
        sendUpdate();
    }

    private void applyRoll(double value) {
        blockEntity.setMountTransform(blockEntity.getMountOffsetX(), blockEntity.getMountOffsetY(), blockEntity.getMountOffsetZ(),
                blockEntity.getMountYaw(), blockEntity.getMountPitch(), (float) value);
        sendUpdate();
    }

    private void resetMount() {
        blockEntity.resetMountTransform();
        offsetXSlider.setValue(0.0F);
        offsetYSlider.setValue(0.0F);
        offsetZSlider.setValue(0.0F);
        yawSlider.setValue(0.0F);
        pitchSlider.setValue(0.0F);
        rollSlider.setValue(0.0F);
        sendUpdate();
    }

    private void sendUpdate() {
        // Sodium caches BE meshes — force a section rebuild so the model moves live
        StrobeRenderHelper.markSectionDirty(pos);
        ModNetworkHandler.CHANNEL.sendToServer(new SetMountTransformPacket(
                pos,
                blockEntity.getMountOffsetX(),
                blockEntity.getMountOffsetY(),
                blockEntity.getMountOffsetZ(),
                blockEntity.getMountYaw(),
                blockEntity.getMountPitch(),
                blockEntity.getMountRoll()
        ));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.drawString(font, title, panelLeft + 2, 10, 0xFFFFFF, true);

        guiGraphics.drawString(font, Component.translatable("screen.theatricalextralights.mount.position"),
                panelLeft + 2, positionHeaderY, COLOR_SECTION, true);
        guiGraphics.drawString(font, Component.translatable("screen.theatricalextralights.mount.rotation"),
                panelLeft + 2, rotationHeaderY, COLOR_SECTION, true);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics) {
        // Intentionally empty.
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static class SideMountSlider extends AbstractWidget {

        private final float maxAbsValue;
        private final float step;
        private final char axis;
        private final int axisColor;
        private final DoubleConsumer onChange;

        private double normalized;
        private boolean dragging;

        private SideMountSlider(int x, int y, int width, float currentValue, float maxAbsValue, float step,
                                char axis, int axisColor, DoubleConsumer onChange) {
            super(x, y, width, ROW_HEIGHT, Component.empty());
            this.maxAbsValue = maxAbsValue;
            this.step = step;
            this.axis = axis;
            this.axisColor = axisColor;
            this.onChange = onChange;
            setValue(currentValue);
        }

        void setValue(float value) {
            float clamped = Mth.clamp(value, -maxAbsValue, maxAbsValue);
            this.normalized = Mth.clamp((clamped + maxAbsValue) / (maxAbsValue * 2.0F), 0.0D, 1.0D);
        }

        private float getFloatValue() {
            return (float) Mth.clamp(-maxAbsValue + (normalized * maxAbsValue * 2.0F), -maxAbsValue, maxAbsValue);
        }

        private void nudge(float delta) {
            setValue(getFloatValue() + delta);
            onChange.accept(getFloatValue());
        }

        private void applyNormalized(double next) {
            this.normalized = Mth.clamp(next, 0.0D, 1.0D);
            onChange.accept(getFloatValue());
        }

        private int trackLeft() {
            return getX() + BTN_SIZE + TRACK_PAD;
        }

        private int trackRight() {
            return getX() + width - BTN_SIZE - TRACK_PAD;
        }

        private int trackWidth() {
            return Math.max(1, trackRight() - trackLeft());
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            int y = getY();
            int minusX = getX();
            int plusX = getX() + width - BTN_SIZE;
            int trackL = trackLeft();
            int trackR = trackRight();

            drawSquareButton(guiGraphics, minusX, y, "-", mouseX, mouseY);
            drawSquareButton(guiGraphics, plusX, y, "+", mouseX, mouseY);

            guiGraphics.fill(trackL - 1, y - 1, trackR + 1, y + height + 1, COLOR_TRACK_BORDER);
            guiGraphics.fill(trackL, y, trackR, y + height, COLOR_TRACK);

            int handleW = 6;
            int handleX = trackL + (int) Math.round(normalized * (trackWidth() - handleW));
            guiGraphics.fill(handleX, y, handleX + handleW, y + height, COLOR_HANDLE);
            guiGraphics.fill(handleX, y, handleX + 1, y + height, COLOR_HANDLE_BORDER);
            guiGraphics.fill(handleX + handleW - 1, y, handleX + handleW, y + height, COLOR_HANDLE_BORDER);

            String valueText = formatValue(getFloatValue());
            int axisW = Minecraft.getInstance().font.width(String.valueOf(axis));
            int valueW = Minecraft.getInstance().font.width(valueText);
            int totalW = axisW + 4 + valueW;
            int textX = trackL + (trackWidth() - totalW) / 2;
            int textY = y + (height - 8) / 2;
            guiGraphics.drawString(Minecraft.getInstance().font, String.valueOf(axis), textX, textY, axisColor, true);
            guiGraphics.drawString(Minecraft.getInstance().font, valueText, textX + axisW + 4, textY, 0xFFFFFF, true);
        }

        private void drawSquareButton(GuiGraphics guiGraphics, int x, int y, String label, int mouseX, int mouseY) {
            boolean hovered = mouseX >= x && mouseX < x + BTN_SIZE && mouseY >= y && mouseY < y + height;
            int bg = hovered ? 0xFFA0A0A0 : 0xFF8B8B8B;
            guiGraphics.fill(x, y, x + BTN_SIZE, y + height, 0xFF000000);
            guiGraphics.fill(x + 1, y + 1, x + BTN_SIZE - 1, y + height - 1, bg);
            guiGraphics.fill(x + 1, y + 1, x + BTN_SIZE - 1, y + 2, 0xFFFFFFFF);
            guiGraphics.fill(x + 1, y + 1, x + 2, y + height - 1, 0xFFFFFFFF);
            int tw = Minecraft.getInstance().font.width(label);
            guiGraphics.drawString(Minecraft.getInstance().font, label,
                    x + (BTN_SIZE - tw) / 2, y + (height - 8) / 2, 0x404040, false);
        }

        private static String formatValue(float value) {
            return String.format(Locale.GERMAN, "[%05.1f]", value).replace(' ', '0');
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            if (mouseX < getX() + BTN_SIZE) {
                nudge(-step);
                return;
            }
            if (mouseX >= getX() + width - BTN_SIZE) {
                nudge(step);
                return;
            }
            dragging = true;
            applyNormalized((mouseX - trackLeft()) / (double) trackWidth());
        }

        @Override
        protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
            if (dragging) {
                applyNormalized((mouseX - trackLeft()) / (double) trackWidth());
            }
        }

        @Override
        public void onRelease(double mouseX, double mouseY) {
            dragging = false;
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
            defaultButtonNarrationText(narrationElementOutput);
        }
    }
}
