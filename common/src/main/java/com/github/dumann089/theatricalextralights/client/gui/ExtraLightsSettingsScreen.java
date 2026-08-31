package com.github.dumann089.theatricalextralights.client.gui;

import com.github.dumann089.theatricalextralights.config.TheatricalExtraLightsConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Ecran de reglages du mod, accessible depuis le menu « Mods » (ModMenu sur Fabric, support
 * natif sur Forge) et par la commande {@code /tel config}.
 *
 * <p>Les modifications sont appliquees en direct pour permettre de regler un faisceau en le
 * regardant, mais le fichier de configuration n'est reecrit qu'a la fermeture : voir
 * {@link TheatricalExtraLightsConfig#beginBatch()}.
 */
public class ExtraLightsSettingsScreen extends Screen {

    private static final int PANEL_WIDTH = 330;
    private static final int PANEL_PADDING = 10;
    private static final int COLUMN_GAP = 6;

    /** Bande reservee au titre, en haut du panneau. */
    private static final int TITLE_BAND = 22;
    /** Ecart entre la rangee d'onglets et le premier reglage. */
    private static final int TAB_GAP = 8;
    /** Bande reservee a la note de bas de panneau. */
    private static final int NOTE_BAND = 13;

    /**
     * Nombre de rangees reservees, cale sur l'onglet le plus charge. Le panneau garde ainsi la
     * meme taille et les boutons la meme place quand on change d'onglet.
     */
    private static final int ROWS = 8;

    private static final int MAX_WIDGET_HEIGHT = 20;
    private static final int MIN_WIDGET_HEIGHT = 13;
    private static final int MAX_ROW_GAP = 4;

    private static final int COLOR_PANEL_BG = 0xE8101014;
    private static final int COLOR_PANEL_BORDER = 0xFF2A2A2E;
    private static final int COLOR_TITLE = 0xFFFFFF;
    private static final int COLOR_NOTE = 0x9A9A9A;

    private static final int TAB_GENERAL = 0;
    private static final int TAB_BEAM = 1;
    private static final int TAB_SPOT = 2;

    private static final String[] TAB_KEYS = {
            "tel.settings.tab.general", "tel.settings.tab.beam", "tel.settings.tab.spot"
    };

    private static final String[] ENGINES = {"RAYMARCH", "LEGACY_SLICES"};
    private static final String[] QUALITIES = {"LOW", "MEDIUM", "HIGH", "ULTRA"};

    private final Screen parent;
    private int tab = TAB_BEAM;

    private boolean batchOpen;

    private int widgetHeight = MAX_WIDGET_HEIGHT;
    private int rowGap = MAX_ROW_GAP;

    private int panelLeft;
    private int panelTop;
    private int panelHeight;
    private int contentLeft;
    private int contentWidth;
    private int noteY;

    public ExtraLightsSettingsScreen(Screen parent) {
        super(Component.translatable("tel.settings.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        if (!batchOpen) {
            TheatricalExtraLightsConfig.beginBatch();
            batchOpen = true;
        }

        fitToScreen();

        this.panelHeight = neededHeight(widgetHeight, rowGap);
        this.panelLeft = (this.width - PANEL_WIDTH) / 2;
        this.panelTop = Math.max(2, (this.height - panelHeight) / 2);
        this.contentLeft = panelLeft + PANEL_PADDING;
        this.contentWidth = PANEL_WIDTH - PANEL_PADDING * 2;

        int half = (contentWidth - COLUMN_GAP) / 2;
        int y = panelTop + TITLE_BAND;

        int tabWidth = (contentWidth - COLUMN_GAP * (TAB_KEYS.length - 1)) / TAB_KEYS.length;
        for (int t = 0; t < TAB_KEYS.length; t++) {
            final int target = t;
            addRenderableWidget(Button.builder(tabLabel(TAB_KEYS[t], target), b -> switchTab(target))
                    .bounds(contentLeft + (tabWidth + COLUMN_GAP) * t, y, tabWidth, widgetHeight).build());
        }

        y += widgetHeight + TAB_GAP;

        switch (tab) {
            case TAB_GENERAL -> buildGeneralTab(y, half);
            case TAB_SPOT -> buildSpotTab(y, half);
            default -> buildBeamTab(y, half);
        }

        int contentBottom = y + ROWS * (widgetHeight + rowGap);
        this.noteY = contentBottom + 2;

        int buttonsY = contentBottom + NOTE_BAND;
        addRenderableWidget(Button.builder(Component.translatable("tel.settings.reset"), b -> resetDefaults())
                .bounds(contentLeft, buttonsY, half, widgetHeight).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                .bounds(contentLeft + half + COLUMN_GAP, buttonsY, half, widgetHeight).build());
    }

    private void resetDefaults() {
        TheatricalExtraLightsConfig.resetDefaults();
        rebuildWidgets();
    }

    private static int neededHeight(int widgetH, int gap) {
        return TITLE_BAND + widgetH + TAB_GAP + ROWS * (widgetH + gap) + NOTE_BAND + widgetH + PANEL_PADDING;
    }

    /**
     * Reduit l'ecartement puis la hauteur des widgets jusqu'a ce que le panneau tienne dans la
     * fenetre. A grande echelle d'interface la hauteur utile descend vite sous les 300 px que
     * demande la mise en page nominale.
     */
    private void fitToScreen() {
        int available = this.height - 4;
        widgetHeight = MAX_WIDGET_HEIGHT;
        rowGap = MAX_ROW_GAP;

        while (neededHeight(widgetHeight, rowGap) > available) {
            if (rowGap > 0) {
                rowGap--;
            } else if (widgetHeight > MIN_WIDGET_HEIGHT) {
                widgetHeight--;
            } else {
                break;
            }
        }
    }

    // Onglet General -------------------------------------------------------

    private void buildGeneralTab(int y, int half) {
        addRenderableWidget(toggle(contentLeft, y, half, "tel.settings.volumetric",
                TheatricalExtraLightsConfig::isVolumetricBeamEnabled,
                TheatricalExtraLightsConfig::setVolumetricBeamEnabled));
        addRenderableWidget(toggle(contentLeft + half + COLUMN_GAP, y, half, "tel.settings.lens",
                TheatricalExtraLightsConfig::shouldRenderLens,
                TheatricalExtraLightsConfig::setRenderLens));
        y += widgetHeight + rowGap;

        addRenderableWidget(toggle(contentLeft, y, half, "tel.settings.beam2d",
                TheatricalExtraLightsConfig::shouldRender2DBeam,
                TheatricalExtraLightsConfig::setRender2DBeam));
        y += widgetHeight + rowGap;

        addRenderableWidget(slider(contentLeft, y, contentWidth, "tel.settings.brightness",
                0.01f, 1.0f, TheatricalExtraLightsConfig.getVolumetricBeamBrightness(), 2, "",
                TheatricalExtraLightsConfig::setVolumetricBeamBrightness));
        y += widgetHeight + rowGap;

        addRenderableWidget(slider(contentLeft, y, contentWidth, "tel.settings.distance",
                8f, 256f, TheatricalExtraLightsConfig.getVolumetricBeamDistance(), 0, " m",
                TheatricalExtraLightsConfig::setVolumetricBeamDistance));
        y += widgetHeight + rowGap;

        addRenderableWidget(slider(contentLeft, y, contentWidth, "tel.settings.fade",
                0f, 48f, TheatricalExtraLightsConfig.getVolumetricBeamFadeLength(), 1, " m",
                TheatricalExtraLightsConfig::setVolumetricBeamFadeLength));
        y += widgetHeight + rowGap;

        addRenderableWidget(slider(contentLeft, y, contentWidth, "tel.settings.gobodistance",
                50f, 1000f, TheatricalExtraLightsConfig.getMaxGoboDistance(), 0, " m",
                TheatricalExtraLightsConfig::setMaxGoboDistance));
    }

    // Onglet Faisceau ------------------------------------------------------

    private void buildBeamTab(int y, int half) {
        addRenderableWidget(cycle(contentLeft, y, half, "tel.settings.engine", ENGINES,
                TheatricalExtraLightsConfig::getVolumetricEngine,
                TheatricalExtraLightsConfig::setVolumetricEngine));
        addRenderableWidget(cycle(contentLeft + half + COLUMN_GAP, y, half, "tel.settings.quality", QUALITIES,
                TheatricalExtraLightsConfig::getRaymarchQuality,
                TheatricalExtraLightsConfig::setRaymarchQuality));
        y += widgetHeight + rowGap;

        addRenderableWidget(slider(contentLeft, y, contentWidth, "tel.settings.anisotropy",
                -0.9f, 0.9f, TheatricalExtraLightsConfig.getRaymarchAnisotropy(), 2, "",
                TheatricalExtraLightsConfig::setRaymarchAnisotropy));
        y += widgetHeight + rowGap;

        addRenderableWidget(slider(contentLeft, y, contentWidth, "tel.settings.dust",
                0f, 1f, TheatricalExtraLightsConfig.getRaymarchDustAmount(), 2, "",
                TheatricalExtraLightsConfig::setRaymarchDustAmount));
        y += widgetHeight + rowGap;

        addRenderableWidget(slider(contentLeft, y, contentWidth, "tel.settings.maxbeams",
                1f, 128f, TheatricalExtraLightsConfig.getRaymarchMaxBeamsPerFrame(), 0, "",
                v -> TheatricalExtraLightsConfig.setRaymarchMaxBeamsPerFrame(Math.round(v))));
        y += widgetHeight + rowGap;

        addRenderableWidget(slider(contentLeft, y, contentWidth, "tel.settings.density",
                0f, 2f, TheatricalExtraLightsConfig.getVolumetricBeamDensity(), 2, "",
                TheatricalExtraLightsConfig::setVolumetricBeamDensity));
        y += widgetHeight + rowGap;

        addRenderableWidget(slider(contentLeft, y, contentWidth, "tel.settings.maxalpha",
                0f, 1f, TheatricalExtraLightsConfig.getVolumetricBeamMaxAlpha(), 3, "",
                TheatricalExtraLightsConfig::setVolumetricBeamMaxAlpha));
        y += widgetHeight + rowGap;

        addRenderableWidget(slider(contentLeft, y, contentWidth, "tel.settings.slices",
                16f, 512f, TheatricalExtraLightsConfig.getVolumetricBeamSlices(), 0, "",
                v -> TheatricalExtraLightsConfig.setVolumetricBeamSlices(Math.round(v))));
    }

    // Onglet Tache ---------------------------------------------------------

    private void buildSpotTab(int y, int half) {
        addRenderableWidget(toggle(contentLeft, y, half, "tel.settings.spotfollow",
                TheatricalExtraLightsConfig::doesSpotFollowBeam,
                TheatricalExtraLightsConfig::setSpotFollowsBeam));
        y += widgetHeight + rowGap;

        addRenderableWidget(slider(contentLeft, y, contentWidth, "tel.settings.spotmax",
                4f, 128f, TheatricalExtraLightsConfig.getSpotMaxRadius(), 0, " m",
                TheatricalExtraLightsConfig::setSpotMaxRadius));
    }

    // Widgets --------------------------------------------------------------

    private void switchTab(int target) {
        if (this.tab == target) return;
        this.tab = target;
        rebuildWidgets();
    }

    private Component tabLabel(String key, int target) {
        Component label = Component.translatable(key);
        return this.tab == target ? Component.literal("> ").append(label) : label;
    }

    private Button toggle(int x, int y, int width, String labelKey,
                          BooleanSupplier getter, Consumer<Boolean> setter) {
        return Button.builder(toggleLabel(labelKey, getter.getAsBoolean()), button -> {
            boolean next = !getter.getAsBoolean();
            setter.accept(next);
            button.setMessage(toggleLabel(labelKey, next));
        }).bounds(x, y, width, widgetHeight).build();
    }

    private static Component toggleLabel(String labelKey, boolean value) {
        return Component.translatable(labelKey)
                .append(Component.literal(": "))
                .append(value ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF);
    }

    /** Bouton qui fait defiler une liste de valeurs textuelles (moteur, qualite). */
    private Button cycle(int x, int y, int width, String labelKey, String[] values,
                         Supplier<String> getter, Consumer<String> setter) {
        return Button.builder(cycleLabel(labelKey, getter.get()), button -> {
            String current = getter.get();
            int index = 0;
            for (int i = 0; i < values.length; i++) {
                if (values[i].equalsIgnoreCase(current)) {
                    index = i;
                    break;
                }
            }
            setter.accept(values[(index + 1) % values.length]);
            button.setMessage(cycleLabel(labelKey, getter.get()));
        }).bounds(x, y, width, widgetHeight).build();
    }

    private static Component cycleLabel(String labelKey, String value) {
        return Component.translatable(labelKey)
                .append(Component.literal(": "))
                .append(Component.translatable(labelKey + "." + value.toLowerCase()));
    }

    private ConfigSlider slider(int x, int y, int width, String labelKey,
                                float min, float max, float current, int decimals,
                                String unit, Consumer<Float> setter) {
        return new ConfigSlider(x, y, width, widgetHeight, labelKey, min, max, current, decimals, unit, setter);
    }

    // Rendu ----------------------------------------------------------------

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Volontairement pas de renderBackground() quand un monde est charge : vanilla y peint
        // un voile noir a 75 % sur tout l'ecran, ce qui rend les faisceaux — deja tres peu
        // opaques — invisibles et donc l'apercu en direct inutilisable.
        if (this.minecraft == null || this.minecraft.level == null) {
            renderBackground(graphics);
        }

        graphics.fill(panelLeft - 1, panelTop - 1, panelLeft + PANEL_WIDTH + 1, panelTop + panelHeight + 1, COLOR_PANEL_BORDER);
        graphics.fill(panelLeft, panelTop, panelLeft + PANEL_WIDTH, panelTop + panelHeight, COLOR_PANEL_BG);

        graphics.drawCenteredString(this.font, this.title, panelLeft + PANEL_WIDTH / 2, panelTop + 6, COLOR_TITLE);

        if (tab == TAB_BEAM) {
            graphics.drawString(this.font, Component.translatable("tel.settings.beam.note"),
                    contentLeft, noteY, COLOR_NOTE, false);
        } else if (tab == TAB_SPOT) {
            graphics.drawString(this.font, Component.translatable("tel.settings.spot.note"),
                    contentLeft, noteY, COLOR_NOTE, false);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        closeBatch();
        this.minecraft.setScreen(parent);
    }

    @Override
    public void removed() {
        closeBatch();
        super.removed();
    }

    private void closeBatch() {
        if (batchOpen) {
            batchOpen = false;
            TheatricalExtraLightsConfig.endBatch();
        }
    }

    /** Apercu en direct : l'ecran ne met pas la partie solo en pause. */
    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static final class ConfigSlider extends AbstractSliderButton {

        private final String labelKey;
        private final float min;
        private final float max;
        private final int decimals;
        private final String unit;
        private final Consumer<Float> setter;

        private ConfigSlider(int x, int y, int width, int height, String labelKey,
                             float min, float max, float current, int decimals,
                             String unit, Consumer<Float> setter) {
            super(x, y, width, height, Component.empty(), 0.0D);
            this.labelKey = labelKey;
            this.min = min;
            this.max = max;
            this.decimals = decimals;
            this.unit = unit;
            this.setter = setter;
            this.value = Mth.clamp((current - min) / (double) (max - min), 0.0D, 1.0D);
            updateMessage();
        }

        private float floatValue() {
            return (float) (min + this.value * (max - min));
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable(labelKey)
                    .append(Component.literal(": " + format(floatValue()) + unit)));
        }

        @Override
        protected void applyValue() {
            setter.accept(floatValue());
        }

        private String format(float v) {
            if (decimals <= 0) return Integer.toString(Math.round(v));
            return String.format("%." + decimals + "f", v);
        }
    }
}
