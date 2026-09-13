package com.github.dumann089.theatricalextralights.client.gui;

import com.github.dumann089.theatricalextralights.blockentities.LaserBlockEntity;
import com.github.dumann089.theatricalextralights.laser.LaserPattern;
import com.github.dumann089.theatricalextralights.net.ModNetworkHandler;
import com.github.dumann089.theatricalextralights.net.SetLaserSafetyPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/**
 * Menu du laser : patch DMX classique plus une section Laser avec l'etat de sortie en
 * direct (motif, couleurs, intensite, taille, vitesse) et un arret d'urgence qui coupe
 * le faisceau quel que soit le DMX, jusqu'au rearmement.
 */
public class LaserConfigScreen extends ExtraLightsConfigScreen {

    private static final int STATUS_CARD_H = 58;
    private static final int ESTOP_H = 30;
    private static final int SWATCH = 12;

    private final LaserBlockEntity laser;

    private TelUi.FlatButton estopButton;
    private int laserSectionY;
    private int statusCardY;

    public LaserConfigScreen(LaserBlockEntity laser, BlockPos pos) {
        super(laser, pos, laser.getTranslationKey(), false);
        this.laser = laser;
    }

    // ── Construction ─────────────────────────────────────────────────────────

    @Override
    protected int buildExtraWidgets(int y) {
        laserSectionY = y;
        y += LABEL_GAP + 3;

        statusCardY = y;
        y += STATUS_CARD_H + ROW_GAP;

        estopButton = addRenderableWidget(new TelUi.FlatButton(contentLeft, y, contentWidth, ESTOP_H,
                estopLabel(), laser.isEmergencyStop() ? TelUi.ButtonStyle.PRIMARY : TelUi.ButtonStyle.DANGER,
                b -> toggleEmergencyStop()));
        y += ESTOP_H + SECTION_GAP;
        return y;
    }

    private Component estopLabel() {
        return laser.isEmergencyStop()
                ? Component.translatable("screen.laser.rearm")
                : Component.translatable("screen.laser.estop");
    }

    private void toggleEmergencyStop() {
        boolean stop = !laser.isEmergencyStop();
        // Retour immediat cote client, puis le serveur confirme par sync du block entity.
        laser.setEmergencyStop(stop);
        ModNetworkHandler.CHANNEL.sendToServer(new SetLaserSafetyPacket(pos, stop));
        refreshEstopButton();
    }

    private void refreshEstopButton() {
        if (estopButton == null) {
            return;
        }
        estopButton.setMessage(estopLabel());
        estopButton.setStyle(laser.isEmergencyStop() ? TelUi.ButtonStyle.PRIMARY : TelUi.ButtonStyle.DANGER);
    }

    // ── Rendu ────────────────────────────────────────────────────────────────

    @Override
    protected void renderExtraLabels(GuiGraphics g) {
        drawSectionLabel(g, Component.translatable("screen.laser.section"), laserSectionY);
        refreshEstopButton();

        int x = contentLeft;
        int y = statusCardY;
        int w = contentWidth;
        TelUi.card(g, x, y, w, STATUS_CARD_H);

        boolean stopped = laser.isEmergencyStop();
        boolean live = laser.isOutputActive();
        int intensity = Math.round(laser.getIntensity());

        // Ligne 1 : etat de sortie + voyant
        Component state;
        int stateColor;
        if (stopped) {
            state = Component.translatable("screen.laser.status_blocked");
            stateColor = TelUi.DANGER;
        } else if (live) {
            state = Component.translatable("screen.laser.status_live");
            stateColor = TelUi.OK;
        } else {
            state = Component.translatable("screen.laser.status_off");
            stateColor = TelUi.SUB;
        }
        g.fill(x + 7, y + 8, x + 13, y + 14, stateColor);
        TelUi.outline(g, x + 6, y + 7, 8, 8, TelUi.BORDER);
        TelUi.text(g, font, state, x + 18, y + 7, stateColor);

        // Couleurs C1 / C2 / C3 a droite de la ligne 1
        int c1 = laser.getColour();
        int c2 = laser.getColour2();
        int c3 = laser.getColour3();
        if (c2 == 0) c2 = c1;
        if (c3 == 0) c3 = c2;
        int sx = x + w - 6 - SWATCH * 3 - 4 * 2;
        drawSwatch(g, sx, y + 6, c1);
        drawSwatch(g, sx + SWATCH + 4, y + 6, c2);
        drawSwatch(g, sx + (SWATCH + 4) * 2, y + 6, c3);

        // Ligne 2 : motif, taille, vitesse
        String pattern = prettyPattern(laser.getPattern());
        Component line2 = Component.translatable("screen.laser.summary",
                pattern,
                Integer.toString(Math.round(laser.getSizeRaw() / 2.55f)),
                Integer.toString(Math.round(laser.getSpeedRaw() / 2.55f)));
        TelUi.text(g, font, Component.literal(TelUi.ellipsize(font, line2.getString(), w - 12)), x + 6, y + 22, TelUi.TEXT);

        // Ligne 3 : barre d'intensite
        int barX = x + 6;
        int barY = y + STATUS_CARD_H - 16;
        int barW = w - 12;
        int barH = 8;
        g.fill(barX, barY, barX + barW, barY + barH, TelUi.BG);
        TelUi.outline(g, barX, barY, barW, barH, TelUi.BORDER_SOFT);
        int fill = (int) Math.round((barW - 2) * (intensity / 255.0));
        if (fill > 0) {
            int barColor = stopped ? TelUi.DANGER_DIM : (c1 == 0 ? TelUi.ACCENT : 0xFF000000 | c1);
            g.fill(barX + 1, barY + 1, barX + 1 + fill, barY + barH - 1, barColor);
        }
        Component pct = Component.translatable("screen.laser.intensity", Integer.toString(Math.round(intensity / 2.55f)));
        TelUi.text(g, font, pct, barX, barY - 10, TelUi.LABEL);
    }

    private static void drawSwatch(GuiGraphics g, int x, int y, int rgb) {
        g.fill(x, y, x + SWATCH, y + SWATCH, 0xFF000000 | rgb);
        TelUi.outline(g, x, y, SWATCH, SWATCH, TelUi.BORDER);
    }

    private static String prettyPattern(LaserPattern pattern) {
        String raw = pattern.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
    }
}
