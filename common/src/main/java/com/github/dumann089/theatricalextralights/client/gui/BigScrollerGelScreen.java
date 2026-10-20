
package com.github.dumann089.theatricalextralights.client.gui;

import com.github.dumann089.theatricalextralights.blockentities.bigscrollerBlockEntity;
import com.github.dumann089.theatricalextralights.blockentities.interfaces.GelScrollerConfigurable;
import com.github.dumann089.theatricalextralights.net.ModNetworkHandler;
import com.github.dumann089.theatricalextralights.net.SetBigScrollerGelConfigPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.Arrays;
import java.util.Locale;

public class BigScrollerGelScreen extends Screen {
    private static final int DESIGN_W = 492;
    private static final int DESIGN_H = 334;
    private static final int DESIGN_ROW_H = 16;
    private static final int VISIBLE_ROWS = 12;

    private static final int MIN_GELS = 2;
    private static final int MAX_GELS = 24;

    private static final int[] DEFAULT_COLORS = {
            0xFF0000,
            0xFF5000,
            0xFFB400,
            0xFFFF00,
            0x00FF00,
            0x00FFB4,
            0x00B4FF,
            0x0000FF,
            0x6400FF,
            0xB400FF,
            0xFF0078,
            0xFFFFFF
    };

    private final BlockPos pos;

    private int[] workingColors;
    private float workingGelHeight;
    private float workingSpeed;

    private int selectedGel;
    private int listScroll;

    private boolean horizontal;
    private float uiScale = 1.0F;
    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;

    private int listX;
    private int listY;
    private int listW;
    private int rowH;
    private int listH;
    private int rightX;

    private EditBox hexField;

    public BigScrollerGelScreen(GelScrollerConfigurable blockEntity, BlockPos pos) {
        super(Component.literal("Scroller - Gel Editor"));
        this.pos = pos;
        this.workingColors = blockEntity.getGelColors();
        this.workingGelHeight = blockEntity.getGelSize();
        this.workingSpeed = blockEntity.getScrollerSpeed();
        this.horizontal = blockEntity.isHorizontalGelScroller();
    }

    private int s(int value) {
        return Math.max(1, Math.round(value * uiScale));
    }

    private int px(int value) {
        return panelX + Math.round(value * uiScale);
    }

    private int py(int value) {
        return panelY + Math.round(value * uiScale);
    }

    @Override
    protected void init() {
        uiScale = Math.min(
                1.0F,
                Math.min(
                        (width - 12.0F) / DESIGN_W,
                        (height - 12.0F) / DESIGN_H
                )
        );
        uiScale = Math.max(0.50F, uiScale);

        panelW = s(DESIGN_W);
        panelH = s(DESIGN_H);
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;

        listX = px(12);
        listY = py(45);
        listW = s(198);
        rowH = s(DESIGN_ROW_H);
        listH = rowH * VISIBLE_ROWS;
        rightX = px(222);

        hexField = new EditBox(
                font,
                rightX,
                py(65),
                s(137),
                s(20),
                Component.literal("#RRGGBB")
        );
        hexField.setMaxLength(7);
        hexField.setFilter(value -> value.matches("#?[0-9a-fA-F]{0,6}"));
        hexField.setValue(formatColor(workingColors[selectedGel]));
        addRenderableWidget(hexField);

        addRenderableWidget(Button.builder(
                        Component.literal("Add RGB"),
                        button -> applyHex()
                )
                .bounds(px(365), py(65), s(115), s(20))
                .build());

        addRenderableWidget(Button.builder(
                        Component.literal("Add"),
                        button -> addGel()
                )
                .bounds(px(12), py(247), s(59), s(20))
                .build());

        addRenderableWidget(Button.builder(
                        Component.literal("Delete"),
                        button -> removeGel()
                )
                .bounds(px(74), py(247), s(59), s(20))
                .build());

        addRenderableWidget(Button.builder(
                        Component.literal("Reset"),
                        button -> resetGels()
                )
                .bounds(px(136), py(247), s(74), s(20))
                .build());

        addRenderableWidget(Button.builder(
                        Component.literal("Up"),
                        button -> moveGel(-1)
                )
                .bounds(px(12), py(271), s(95), s(20))
                .build());

        addRenderableWidget(Button.builder(
                        Component.literal("Down"),
                        button -> moveGel(1)
                )
                .bounds(px(113), py(271), s(97), s(20))
                .build());

        addRenderableWidget(Button.builder(
                        Component.literal("-"),
                        button -> workingSpeed = Mth.clamp(
                                workingSpeed - 0.05F, 0.05F, 3.0F)
                )
                .bounds(rightX, py(126), s(40), s(20))
                .build());

        addRenderableWidget(Button.builder(
                        Component.literal("+"),
                        button -> workingSpeed = Mth.clamp(
                                workingSpeed + 0.05F, 0.05F, 3.0F)
                )
                .bounds(px(440), py(126), s(40), s(20))
                .build());

        addRenderableWidget(Button.builder(
                        Component.literal("-"),
                        button -> workingGelHeight = Mth.clamp(
                                workingGelHeight - 0.1F, 0.25F, 4.0F)
                )
                .bounds(rightX, py(180), s(40), s(20))
                .build());

        addRenderableWidget(Button.builder(
                        Component.literal("+"),
                        button -> workingGelHeight = Mth.clamp(
                                workingGelHeight + 0.1F, 0.25F, 4.0F)
                )
                .bounds(px(440), py(180), s(40), s(20))
                .build());

        addRenderableWidget(Button.builder(
                        Component.literal("Save"),
                        button -> saveAndClose()
                )
                .bounds(px(270), py(301), s(102), s(20))
                .build());

        addRenderableWidget(Button.builder(
                        Component.literal("Cancel"),
                        button -> onClose()
                )
                .bounds(px(380), py(301), s(100), s(20))
                .build());
    }

    private void addGel() {
        if (workingColors.length >= MAX_GELS) {
            return;
        }

        workingColors = Arrays.copyOf(workingColors, workingColors.length + 1);
        workingColors[workingColors.length - 1] = 0xFFFFFF;

        selectedGel = workingColors.length - 1;
        listScroll = Math.max(0, workingColors.length - VISIBLE_ROWS);
        refreshHex();
    }

    private void removeGel() {
        if (workingColors.length <= MIN_GELS) {
            return;
        }

        int[] updated = new int[workingColors.length - 1];

        System.arraycopy(workingColors, 0, updated, 0, selectedGel);

        System.arraycopy(
                workingColors,
                selectedGel + 1,
                updated,
                selectedGel,
                workingColors.length - selectedGel - 1
        );

        workingColors = updated;
        selectedGel = Math.min(selectedGel, workingColors.length - 1);
        listScroll = Mth.clamp(
                listScroll, 0, Math.max(0, workingColors.length - VISIBLE_ROWS));

        refreshHex();
    }

    private void resetGels() {
        workingColors = DEFAULT_COLORS.clone();
        selectedGel = 0;
        listScroll = 0;
        refreshHex();
    }

    private void moveGel(int direction) {
        int target = selectedGel + direction;

        if (target < 0 || target >= workingColors.length) {
            return;
        }

        int color = workingColors[selectedGel];
        workingColors[selectedGel] = workingColors[target];
        workingColors[target] = color;
        selectedGel = target;

        listScroll = Mth.clamp(
                listScroll, 0, Math.max(0, workingColors.length - VISIBLE_ROWS));

        refreshHex();
    }

    private void refreshHex() {
        if (hexField != null
                && selectedGel >= 0
                && selectedGel < workingColors.length) {
            hexField.setValue(formatColor(workingColors[selectedGel]));
        }
    }

    private static String formatColor(int color) {
        return String.format(Locale.ROOT, "#%06X", color & 0xFFFFFF);
    }

    private boolean applyHex() {
        String value = hexField.getValue().trim().replace("#", "");

        if (value.length() != 6) {
            return false;
        }

        try {
            workingColors[selectedGel] = Integer.parseInt(value, 16) & 0xFFFFFF;
            return true;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private void saveAndClose() {
        if (!applyHex()) {
            return;
        }

        ModNetworkHandler.CHANNEL.sendToServer(
                new SetBigScrollerGelConfigPacket(
                        pos,
                        workingColors,
                        workingGelHeight,
                        workingSpeed
                )
        );

        Minecraft.getInstance().setScreen(null);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0
                && mouseX >= listX
                && mouseX < listX + listW
                && mouseY >= listY
                && mouseY < listY + listH) {

            int row = (int) ((mouseY - listY) / rowH);
            int index = listScroll + row;

            if (index >= 0 && index < workingColors.length) {
                selectedGel = index;
                refreshHex();
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double delta
    ) {
        if (mouseX >= listX
                && mouseX <= listX + listW
                && mouseY >= listY
                && mouseY <= listY + listH) {

            listScroll = Mth.clamp(
                    listScroll - (int) Math.signum(delta),
                    0,
                    Math.max(0, workingColors.length - VISIBLE_ROWS)
            );

            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        renderBackground(graphics);

        graphics.fill(
                panelX, panelY,
                panelX + panelW, panelY + panelH,
                0xF0222228
        );
        graphics.fill(
                panelX, panelY,
                panelX + panelW, py(27),
                0xFF34343C
        );

        graphics.drawString(
                font, title,
                px(12), py(9),
                0xFFFFFFFF
        );

        graphics.drawString(
                font,
                "Gels: " + workingColors.length + "/" + MAX_GELS,
                listX,
                py(32),
                0xFFDDDDDD
        );

        graphics.fill(
                listX, listY,
                listX + listW, listY + listH,
                0xFF101014
        );

        for (int row = 0; row < VISIBLE_ROWS; row++) {
            int index = listScroll + row;
            int y = listY + row * rowH;

            if (index >= workingColors.length) {
                continue;
            }

            if (index == selectedGel) {
                graphics.fill(
                        listX + 1, y,
                        listX + listW - 1, y + rowH,
                        0xFF425A78
                );
            } else if ((row & 1) == 0) {
                graphics.fill(
                        listX + 1, y,
                        listX + listW - 1, y + rowH,
                        0xFF1B1B20
                );
            }

            int rgb = 0xFF000000 | (workingColors[index] & 0xFFFFFF);
            int swatchWidth = s(20);
            int swatchY = y + Math.max(1, (rowH - s(10)) / 2);

            graphics.fill(
                    listX + s(5), swatchY,
                    listX + s(5) + swatchWidth, swatchY + s(10),
                    rgb
            );

            graphics.drawString(
                    font,
                    String.format(
                            Locale.ROOT,
                            "%02d  %s",
                            index + 1,
                            formatColor(workingColors[index])
                    ),
                    listX + s(31),
                    y + Math.max(0, (rowH - font.lineHeight) / 2),
                    0xFFFFFFFF
            );
        }

        graphics.drawString(
                font, "GELS COLORS",
                rightX, py(45), 0xFFDDDDDD
        );

        graphics.drawString(
                font, "Motor Velocity",
                rightX, py(111), 0xFFDDDDDD
        );

        graphics.drawString(
                font,
                String.format(Locale.ROOT, "%.2f gel/s", workingSpeed),
                px(285),
                py(132),
                0xFFFFFFFF
        );

        graphics.drawString(
                font, "Height of each gel",
                rightX, py(165), 0xFFDDDDDD
        );

        graphics.drawString(
                font,
                String.format(Locale.ROOT, "%.2f blocks", workingGelHeight),
                px(280),
                py(186),
                0xFFFFFFFF
        );

        graphics.drawString(
                font, "Sequence preview",
                rightX, py(222), 0xFFDDDDDD
        );

        int previewX = rightX;
        int previewY = py(240);
        int previewW = s(236);
        int previewH = s(25);
        int swatchW = Math.max(1, previewW / workingColors.length);

        for (int i = 0; i < workingColors.length; i++) {
            int x0 = previewX + i * swatchW;
            int x1 = i == workingColors.length - 1
                    ? previewX + previewW
                    : x0 + swatchW;

            graphics.fill(
                    x0, previewY,
                    x1, previewY + previewH,
                    0xFF000000 | (workingColors[i] & 0xFFFFFF)
            );
        }

        graphics.drawString(
                font,
                "CH4: DMX position · no fade between gels",
                rightX,
                py(276),
                0xFFBFBFBF
        );

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}