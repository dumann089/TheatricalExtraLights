package com.github.dumann089.theatricalextralights.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.function.Consumer;

/**
 * Kit d'interface plat et sombre partage par les ecrans Extra Lights : palette, panneaux,
 * boutons, curseurs et champs. Meme famille visuelle que la console followspot.
 */
public final class TelUi {

    public static final int BG = 0xFF1C1C21;
    public static final int BG_RAISED = 0xFF26262C;
    public static final int BG_SUNKEN = 0xFF131317;
    public static final int BORDER = 0xFF08080C;
    public static final int BORDER_SOFT = 0xFF34343C;
    public static final int HEADER = 0xFF2E2E36;
    public static final int TITLE = 0xFFF4F4F8;
    public static final int TEXT = 0xFFE4E4EA;
    public static final int SUB = 0xFF9A9AA8;
    public static final int LABEL = 0xFF80808C;
    public static final int ACCENT = 0xFF6AAEF0;
    public static final int ACCENT_DIM = 0xFF3C6A98;
    public static final int WARN = 0xFFEA9468;
    public static final int OK = 0xFF7FCB8C;

    public static final int WIDGET_H = 20;

    private TelUi() {
    }

    // ── Primitives ───────────────────────────────────────────────────────────

    /** Panneau principal : bordure sombre, fond, liseret d'accent en haut. */
    public static void panel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, BORDER);
        g.fill(x, y, x + w, y + h, BG);
        g.fill(x, y, x + w, y + 2, ACCENT);
    }

    /** Carte secondaire dans un panneau. */
    public static void card(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, BG_SUNKEN);
        outline(g, x, y, w, h, BORDER_SOFT);
    }

    public static void outline(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }

    public static void hairline(GuiGraphics g, int x, int y, int w) {
        g.fill(x, y, x + w, y + 1, BORDER_SOFT);
    }

    /** Libelle de section en petites capitales grises suivi d'un filet. */
    public static void sectionLabel(GuiGraphics g, Font font, Component label, int x, int y, int w) {
        String text = label.getString().toUpperCase();
        g.drawString(font, text, x, y, LABEL, false);
        int textW = font.width(text);
        g.fill(x + textW + 6, y + 4, x + w, y + 5, BORDER_SOFT);
    }

    public static void label(GuiGraphics g, Font font, Component label, int x, int y) {
        g.drawString(font, label, x, y, SUB, false);
    }

    public static void text(GuiGraphics g, Font font, Component c, int x, int y, int color) {
        g.drawString(font, c, x, y, color, false);
    }

    public static void centered(GuiGraphics g, Font font, Component c, int cx, int y, int color) {
        g.drawString(font, c, cx - font.width(c) / 2, y, color, false);
    }

    public static String ellipsize(Font font, String s, int maxWidth) {
        if (font.width(s) <= maxWidth) {
            return s;
        }
        String dots = "…";
        return font.plainSubstrByWidth(s, Math.max(0, maxWidth - font.width(dots))) + dots;
    }

    /** Petite pastille arrondie (fond releve) avec texte. */
    public static int pill(GuiGraphics g, Font font, Component c, int rightX, int y, int color) {
        int w = font.width(c) + 10;
        int x = rightX - w;
        g.fill(x, y, x + w, y + 12, BG_RAISED);
        outline(g, x, y, w, 12, BORDER_SOFT);
        g.drawString(font, c, x + 5, y + 2, color, false);
        return w;
    }

    // ── Champs ───────────────────────────────────────────────────────────────

    /**
     * EditBox sans bordure vanilla, positionne pour tenir dans le cadre (x, y, w, h)
     * dessine par {@link #fieldFrame}.
     */
    public static EditBox field(Font font, int x, int y, int w, int h, Component label) {
        EditBox box = new EditBox(font, x + 6, y + (h - 8) / 2, w - 12, 10, label);
        box.setBordered(false);
        box.setTextColor(TEXT);
        box.setTextColorUneditable(SUB);
        return box;
    }

    public static void fieldFrame(GuiGraphics g, EditBox box, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, BG_SUNKEN);
        outline(g, x, y, w, h, box.isFocused() ? ACCENT : BORDER_SOFT);
    }

    // ── Boutons ──────────────────────────────────────────────────────────────

    public enum ButtonStyle { NORMAL, PRIMARY, GHOST }

    public static class FlatButton extends AbstractButton {
        private final Consumer<FlatButton> onPress;
        private ButtonStyle style;
        private boolean selected;

        public FlatButton(int x, int y, int w, int h, Component message, ButtonStyle style, Consumer<FlatButton> onPress) {
            super(x, y, w, h, message);
            this.style = style;
            this.onPress = onPress;
        }

        public FlatButton setSelected(boolean selected) {
            this.selected = selected;
            return this;
        }

        public boolean isSelected() {
            return selected;
        }

        public void setStyle(ButtonStyle style) {
            this.style = style;
        }

        @Override
        public void onPress() {
            if (onPress != null) {
                onPress.accept(this);
            }
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            int x = getX();
            int y = getY();
            int w = getWidth();
            int h = getHeight();
            boolean hover = active && isHoveredOrFocused();

            int bg;
            int border;
            int fg;
            switch (style) {
                case PRIMARY -> {
                    bg = hover ? 0xFF7FBBF4 : ACCENT;
                    border = ACCENT_DIM;
                    fg = 0xFF0E1620;
                }
                case GHOST -> {
                    bg = selected ? ACCENT_DIM : (hover ? BG_RAISED : BG_SUNKEN);
                    border = selected ? ACCENT : BORDER_SOFT;
                    fg = selected ? TITLE : (hover ? TEXT : SUB);
                }
                default -> {
                    bg = hover ? 0xFF33333B : BG_RAISED;
                    border = hover ? 0xFF4A4A54 : BORDER_SOFT;
                    fg = TEXT;
                }
            }
            if (!active) {
                bg = BG_SUNKEN;
                border = BORDER_SOFT;
                fg = 0xFF5A5A64;
            }

            g.fill(x, y, x + w, y + h, bg);
            outline(g, x, y, w, h, border);

            Font font = Minecraft.getInstance().font;
            String text = ellipsize(font, getMessage().getString(), w - 8);
            g.drawString(font, text, x + (w - font.width(text)) / 2, y + (h - 8) / 2, fg, false);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }

    // ── Curseur ──────────────────────────────────────────────────────────────

    /** Curseur plat : piste sombre, remplissage d'accent, valeur centree. */
    public abstract static class FlatSlider extends AbstractSliderButton {

        protected FlatSlider(int x, int y, int w, int h, double value) {
            super(x, y, w, h, Component.empty(), Mth.clamp(value, 0.0, 1.0));
        }

        public double getRawValue() {
            return value;
        }

        public void setRawValue(double v) {
            this.value = Mth.clamp(v, 0.0, 1.0);
            updateMessage();
        }

        @Override
        public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            int x = getX();
            int y = getY();
            int w = getWidth();
            int h = getHeight();
            boolean hover = active && isHoveredOrFocused();

            g.fill(x, y, x + w, y + h, BG_SUNKEN);
            outline(g, x, y, w, h, hover ? 0xFF4A4A54 : BORDER_SOFT);

            int fillW = (int) Math.round((w - 2) * value);
            g.fill(x + 1, y + 1, x + 1 + fillW, y + h - 1, hover ? ACCENT_DIM : 0xFF2F5478);

            int knobX = x + 1 + (int) Math.round((w - 6) * value);
            g.fill(knobX, y + 1, knobX + 4, y + h - 1, hover ? 0xFF7FBBF4 : ACCENT);

            Font font = Minecraft.getInstance().font;
            String text = getMessage().getString();
            g.drawString(font, text, x + (w - font.width(text)) / 2, y + (h - 8) / 2, TITLE, false);
        }
    }
}
