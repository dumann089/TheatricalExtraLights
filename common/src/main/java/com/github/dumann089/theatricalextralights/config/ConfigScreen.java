package com.github.dumann089.theatricalextralights.config;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class ConfigScreen extends Screen {
    private final Screen parent;
    private final List<Runnable> widgetInitializers = new ArrayList<>();

    private final int PANEL_WIDTH = 250;

    // Scrolling properties
    private double scrollAmount = 0;
    private int maxScroll = 0;
    private boolean isDraggingScrollbar = false;
    private final List<AbstractWidget> configWidgets = new ArrayList<>();
    private final List<Integer> widgetOriginalYs = new ArrayList<>();

    public ConfigScreen(Screen parent) {
        super(Component.literal("Theatrical Extra Lights Config"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        widgetInitializers.clear();
        configWidgets.clear();
        widgetOriginalYs.clear();
        scrollAmount = 0;

        int startY = 45;
        int x = 25;
        int y = startY;

        // Initialize left-panel configuration options
        for (Field field : TheatricalExtraLightsConfig.class.getDeclaredFields()) {
            if (!field.isAnnotationPresent(ConfigOption.class)) continue;

            ConfigOption option = field.getAnnotation(ConfigOption.class);
            field.setAccessible(true);

            try {
                Object value = field.get(ConfigManager.getInstance());
                AbstractWidget widget = null;

                if (value instanceof Boolean) {
                    widget = CycleButton.onOffBuilder((Boolean) value)
                            .displayOnlyValue()
                            .withTooltip(val -> Tooltip.create(Component.literal(getTooltipDescription(option.name()))))
                            .create(x, y, 200, 20, Component.literal(option.name()), (button, val) -> {
                                setValue(field, val);
                            });

                } else if (value instanceof Number) {
                    widget = new NumberSlider(
                            x, y, 200, 20,
                            Component.literal(option.name()),
                            field,
                            option
                    );

                } else if (value instanceof String) {
                    if (field.getName().equals("volumetricEngine")) {
                        widget = CycleButton.builder(Component::literal)
                                .withValues("RAYMARCH", "LEGACY_SLICES")
                                .withInitialValue((String) value)
                                .withTooltip(val -> Tooltip.create(
                                        Component.literal("Selects the volumetric rendering engine.")
                                ))
                                .create(x, y, 200, 20,
                                        Component.literal(option.name()),
                                        (button, val) -> setValue(field, val));

                    } else if (field.getName().equals("raymarchQuality")) {
                        widget = CycleButton.builder(Component::literal)
                                .withValues("LOW", "MEDIUM", "HIGH", "ULTRA")
                                .withInitialValue((String) value)
                                .withTooltip(val -> Tooltip.create(
                                        Component.literal("Controls raymarch quality.")
                                ))
                                .create(x, y, 200, 20,
                                        Component.literal(option.name()),
                                        (button, val) -> setValue(field, val));
                    }
                }

                if (widget != null) {
                    configWidgets.add(widget);
                    widgetOriginalYs.add(y);
                    addRenderableWidget(widget);
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }

            y += 26;
        }

        // Calculate maximum scroll depth based on the last widget's Y position
        maxScroll = Math.max(0, y - this.height + 20);

        // Bottom-Right Action Buttons
        int btnWidth = 150;
        int btnX = this.width - btnWidth - 20; // 20px margin from the right edge
        int bottomY = this.height - 90;

        addRenderableWidget(Button.builder(Component.literal("Reset to Defaults"), btn -> {
                    ConfigManager.resetToDefaults();
                    this.minecraft.setScreen(new ConfigScreen(this.parent));
                }).bounds(btnX, bottomY, btnWidth, 20)
                .tooltip(Tooltip.create(Component.literal("Restores all settings to their default values.")))
                .build());

        addRenderableWidget(Button.builder(Component.literal("Reload JSON"), btn -> {
                    ConfigManager.reload();
                    this.minecraft.setScreen(new ConfigScreen(this.parent));
                }).bounds(btnX, bottomY + 24, btnWidth, 20)
                .tooltip(Tooltip.create(Component.literal("Reloads the configuration from the JSON file without restarting.")))
                .build());

        addRenderableWidget(Button.builder(Component.literal("Done"), btn -> {
                    this.minecraft.setScreen(parent);
                }).bounds(btnX, bottomY + 48, btnWidth, 20)
                .tooltip(Tooltip.create(Component.literal("Saves current changes and closes the menu.")))
                .build());
    }

    private String getTooltipDescription(String optionName) {
        switch (optionName.toLowerCase()) {
            case "lens":
                return "Enables or disables the fixture Lens rendering.";
            case "beam2d":
                return "Enables 2D mode Beam.";
            case "beam":
                return "Toggles full volumetric 3D Beams.";
            default:
                return "Toggles the " + optionName + " feature.";
        }
    }

    private void setValue(Field field, Object val) {
        try {
            field.set(ConfigManager.getInstance(), val);
            ConfigManager.notifyChange(field.getName());
            ConfigManager.save();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private void updateWidgetPositions() {
        for (int i = 0; i < configWidgets.size(); i++) {
            configWidgets.get(i).setY(widgetOriginalYs.get(i) - (int) scrollAmount);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (maxScroll > 0 && mouseX < PANEL_WIDTH) {
            scrollAmount -= delta * 20; // Scroll speed
            scrollAmount = Math.max(0, Math.min(scrollAmount, maxScroll));
            updateWidgetPositions();
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Detect click on the custom scrollbar
        if (maxScroll > 0 && mouseX >= PANEL_WIDTH - 10 && mouseX <= PANEL_WIDTH && mouseY >= 35) {
            isDraggingScrollbar = true;
            return true;
        }

        // Prevent clicking invisible widgets hidden behind the top title header
        if (mouseX < PANEL_WIDTH && mouseY < 35) {
            return false;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            isDraggingScrollbar = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isDraggingScrollbar && maxScroll > 0) {
            int scrollbarArea = this.height - 35;
            int scrollbarHeight = Math.max(20, (int) (((float) this.height / (this.height + maxScroll)) * this.height));
            double scrollFactor = (double) maxScroll / (scrollbarArea - scrollbarHeight);

            scrollAmount += dragY * scrollFactor;
            scrollAmount = Math.max(0, Math.min(scrollAmount, maxScroll));
            updateWidgetPositions();

            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Render left panel background
        graphics.fill(0, 0, PANEL_WIDTH, this.height, 0xDD000000);
        graphics.fill(PANEL_WIDTH - 2, 0, PANEL_WIDTH, this.height, 0xFF555555);

        // Render widgets
        super.render(graphics, mouseX, mouseY, partialTick);

        // Render solid top header (Hides scrolling widgets when they move up)
        graphics.fill(0, 0, PANEL_WIDTH, 35, 0xFF151515);
        graphics.drawCenteredString(this.font, this.title, PANEL_WIDTH / 2, 15, 0xFFFFFF);

        // Render visual scrollbar if needed
        if (maxScroll > 0) {
            int scrollbarX = PANEL_WIDTH - 6;
            int scrollbarHeight = Math.max(20, (int) (((float) this.height / (this.height + maxScroll)) * this.height));
            int scrollbarY = 35 + (int) ((scrollAmount / maxScroll) * (this.height - 35 - scrollbarHeight));

            // Scrollbar Track
            graphics.fill(scrollbarX, 35, scrollbarX + 4, this.height, 0x88000000);
            // Scrollbar Thumb
            graphics.fill(scrollbarX, scrollbarY, scrollbarX + 4, scrollbarY + scrollbarHeight, 0xFFAAAAAA);
        }
    }

    private class NumberSlider extends AbstractSliderButton {
        private final Field field;
        private final ConfigOption option;

        public NumberSlider(int x, int y, int w, int h, Component title, Field field, ConfigOption option) {
            super(x, y, w, h, title, 0.0);
            this.field = field;
            this.option = option;
            try {
                double current = ((Number) field.get(ConfigManager.getInstance())).doubleValue();
                this.value = (current - option.min()) / (option.max() - option.min());
            } catch (Exception ignored) {}
            this.updateMessage();
        }

        @Override
        protected void updateMessage() {
            double actualVal = option.min() + (this.value * (option.max() - option.min()));
            Class<?> type = field.getType();

            boolean isInteger = type == Integer.class || type == int.class;
            String formatted = isInteger ? String.format("%.0f", actualVal) : String.format("%.2f", actualVal);

            this.setMessage(Component.literal(option.name() + ": " + formatted));
        }

        @Override
        protected void applyValue() {
            double actualVal = option.min() + (this.value * (option.max() - option.min()));
            try {
                Class<?> type = field.getType();

                if (type == Integer.class || type == int.class) {
                    setValue(field, (int) Math.round(actualVal));
                } else if (type == Float.class || type == float.class) {
                    setValue(field, (float) actualVal);
                } else {
                    setValue(field, actualVal);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}