package com.github.dumann089.theatricalextralights.client.followspot;

import com.github.dumann089.theatricalextralights.blockentities.FollowspotConsoleBlockEntity;
import com.github.dumann089.theatricalextralights.client.gui.TelUi;
import dev.imabad.theatrical.blockentities.light.BaseLightBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Surimpression du mode operateur, dans l'esprit d'un vrai pupitre de poursuite : reticule au
 * centre, cartouche machine en haut a gauche (pan / tilt / distance), barre de niveaux en bas
 * (intensite, focus, couleur) et rappel des commandes.
 */
public final class FollowspotHud {

    private static final int RETICLE_R = 16;
    private static final int RETICLE_GAP = 5;
    private static final int RETICLE_ARM = 10;
    private static final int RETICLE_COLOR = 0xCCF6ECD2;
    private static final int RETICLE_SHADOW = 0x66000000;

    private FollowspotHud() {
    }

    public static void render(GuiGraphics g, float partialTick) {
        FollowspotFixtureCameraSession session = FollowspotFixtureCameraSession.getActive();
        if (session == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) {
            return;
        }
        session.frame(mc);

        Font font = mc.font;
        int w = g.guiWidth();
        int h = g.guiHeight();

        drawReticle(g, w / 2, h / 2);
        drawFixtureCard(g, font, session, 10, 10);
        drawLevelsBar(g, font, session, w, h);
    }

    // ── Reticule ─────────────────────────────────────────────────────────────

    private static void drawReticle(GuiGraphics g, int cx, int cy) {
        // Anneau fin dessine ligne par ligne (exterieur - interieur)
        int rOut = RETICLE_R;
        int rIn = RETICLE_R - 1;
        for (int dy = -rOut; dy <= rOut; dy++) {
            double outerHalf = Math.sqrt(Math.max(0, rOut * rOut - dy * dy));
            double innerHalf = Math.abs(dy) <= rIn ? Math.sqrt(Math.max(0, rIn * rIn - dy * dy)) : 0;
            int y = cy + dy;
            int xo0 = (int) Math.round(cx - outerHalf);
            int xo1 = (int) Math.round(cx + outerHalf);
            int xi0 = (int) Math.round(cx - innerHalf);
            int xi1 = (int) Math.round(cx + innerHalf);
            if (innerHalf <= 0) {
                if (xo1 > xo0) g.fill(xo0, y, xo1, y + 1, RETICLE_COLOR);
            } else {
                if (xi0 > xo0) g.fill(xo0, y, xi0, y + 1, RETICLE_COLOR);
                if (xo1 > xi1) g.fill(xi1, y, xo1, y + 1, RETICLE_COLOR);
            }
        }
        // Croix avec trou central
        int a0 = RETICLE_GAP;
        int a1 = RETICLE_GAP + RETICLE_ARM;
        g.fill(cx - a1, cy, cx - a0, cy + 1, RETICLE_COLOR);
        g.fill(cx + a0 + 1, cy, cx + a1 + 1, cy + 1, RETICLE_COLOR);
        g.fill(cx, cy - a1, cx + 1, cy - a0, RETICLE_COLOR);
        g.fill(cx, cy + a0 + 1, cx + 1, cy + a1 + 1, RETICLE_COLOR);
        // Point central
        g.fill(cx - 1, cy - 1, cx + 2, cy + 2, RETICLE_SHADOW);
        g.fill(cx, cy, cx + 1, cy + 1, RETICLE_COLOR);
    }

    // ── Cartouche machine ────────────────────────────────────────────────────

    private static void drawFixtureCard(GuiGraphics g, Font font, FollowspotFixtureCameraSession session, int x, int y) {
        int w = 172;
        int h = 60;
        panel(g, x, y, w, h);

        BaseLightBlockEntity fixture = session.getFixture();
        Component name = fixture != null ? fixture.getBlockState().getBlock().getName() : Component.literal("?");

        g.drawString(font, Component.translatable("screen.followspot_console.hud.title").getString().toUpperCase(),
                x + 8, y + 6, TelUi.ACCENT, false);
        g.drawString(font, TelUi.ellipsize(font, name.getString(), w - 16), x + 8, y + 17, TelUi.TITLE, false);

        Component panTilt = Component.translatable("screen.followspot_console.pan_tilt",
                Integer.toString(session.getPan()), Integer.toString(session.getTilt()));
        g.drawString(font, panTilt, x + 8, y + 32, TelUi.TEXT, false);

        float dist = session.getBeamDistance();
        Component distance = Component.translatable("screen.followspot_console.hud.distance",
                String.format(java.util.Locale.ROOT, "%.1f", dist));
        g.drawString(font, distance, x + 8, y + 44, TelUi.SUB, false);

        // Voyant de sortie
        boolean live = session.getIntensity() > 0;
        int dot = session.isBlackout() ? TelUi.DANGER : (live ? TelUi.OK : TelUi.SUB);
        g.fill(x + w - 16, y + 7, x + w - 8, y + 15, dot);
        TelUi.outline(g, x + w - 17, y + 6, 10, 10, TelUi.BORDER);
    }

    // ── Barre de niveaux ─────────────────────────────────────────────────────

    private static void drawLevelsBar(GuiGraphics g, Font font, FollowspotFixtureCameraSession session, int sw, int sh) {
        int w = 360;
        int h = session.isPanTiltOnly() ? 34 : 52;
        int x = (sw - w) / 2;
        int y = sh - h - 26;
        // Laisse la place au bandeau des memoires au-dessus de la barre
        y -= 4;
        panel(g, x, y, w, h);

        if (session.isPanTiltOnly()) {
            TelUi.centered(g, font, Component.translatable("screen.followspot_console.hud.levels_desk"),
                    x + w / 2, y + 8, TelUi.SUB);
        } else {
            int colW = (w - 16 - 8) / 2;
            int cx1 = x + 8;
            int cx2 = cx1 + colW + 8;
            int rowY = y + 8;

            drawBar(g, font, cx1, rowY, colW - 40, Component.translatable("screen.followspot_console.hud.intensity"),
                    session.getIntensity(), session.isBlackout() ? TelUi.DANGER : 0xFF000000 | brighten(session.getColour()));
            drawBar(g, font, cx2, rowY, colW, Component.translatable("screen.followspot_console.hud.focus"),
                    session.getFocus(), TelUi.ACCENT);

            // Pastille couleur
            int swX = cx1 + colW - 30;
            g.fill(swX, rowY, swX + 24, rowY + 22, 0xFF000000 | session.getColour());
            TelUi.outline(g, swX, rowY, 24, 22, TelUi.BORDER);

            if (session.isBlackout()) {
                Component bo = Component.translatable("screen.followspot_console.hud.blackout");
                int bw = font.width(bo) + 12;
                int bx = x + w - bw - 8;
                g.fill(bx, y - 14, bx + bw, y - 2, TelUi.DANGER);
                g.drawString(font, bo, bx + 6, y - 12, 0xFFFFF2F0, false);
            }
        }

        drawPresetStrip(g, font, session, x, y - 20, w);

        Component hint = Component.translatable("screen.followspot_console.hud.hint");
        TelUi.centered(g, font, hint, sw / 2, sh - 20, TelUi.LABEL);
    }

    /** Huit memoires : case pleine = enregistree, bleue = derniere rappelee. */
    private static void drawPresetStrip(GuiGraphics g, Font font, FollowspotFixtureCameraSession session, int x, int y, int w) {
        FollowspotConsoleBlockEntity console = session.getConsole();
        int count = FollowspotConsoleBlockEntity.PRESET_COUNT;
        int cell = 18;
        int gap = 3;
        int stripW = count * cell + (count - 1) * gap;
        int sx = x + w - stripW;
        for (int i = 0; i < count; i++) {
            int cx = sx + i * (cell + gap);
            boolean stored = console != null && console.hasPreset(i);
            boolean last = session.getLastPreset() == i;
            int bg = last ? TelUi.ACCENT_DIM : (stored ? 0xD826262C : 0xB0131317);
            g.fill(cx, y, cx + cell, y + 16, bg);
            TelUi.outline(g, cx, y, cell, 16, last ? TelUi.ACCENT : (stored ? 0xFF4A4A54 : 0xFF2C2C33));
            String label = Integer.toString(i + 1);
            g.drawString(font, label, cx + (cell - font.width(label)) / 2, y + 4,
                    stored ? TelUi.TITLE : 0xFF5A5A64, false);
        }
        Component label = Component.translatable("screen.followspot_console.hud.presets");
        g.drawString(font, label, x, y + 4, TelUi.LABEL, false);
    }

    private static void drawBar(GuiGraphics g, Font font, int x, int y, int w, Component label, int value, int color) {
        g.drawString(font, label, x, y, TelUi.LABEL, false);
        String pct = Math.round(value / 2.55f) + "%";
        g.drawString(font, pct, x + w - font.width(pct), y, TelUi.TEXT, false);
        int barY = y + 12;
        g.fill(x, barY, x + w, barY + 8, TelUi.BG_SUNKEN);
        TelUi.outline(g, x, barY, w, 8, TelUi.BORDER_SOFT);
        int fill = (int) Math.round((w - 2) * (value / 255.0));
        if (fill > 0) {
            g.fill(x + 1, barY + 1, x + 1 + fill, barY + 7, color);
        }
    }

    private static void panel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, 0xD81C1C21);
        TelUi.outline(g, x, y, w, h, 0xFF34343C);
        g.fill(x, y, x + w, y + 1, TelUi.ACCENT);
    }

    private static int brighten(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int gr = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        if (r + gr + b < 90) {
            return 0xF6ECD2;
        }
        return rgb;
    }
}
