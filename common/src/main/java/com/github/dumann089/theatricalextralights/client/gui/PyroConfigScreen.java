package com.github.dumann089.theatricalextralights.client.gui;

import com.github.dumann089.theatricalextralights.blockentities.FlameProjectorBlockEntity;
import com.github.dumann089.theatricalextralights.blockentities.FlameThrowerBlockEntity;
import com.github.dumann089.theatricalextralights.blockentities.interfaces.HasSafetyArm;
import com.github.dumann089.theatricalextralights.net.ModNetworkHandler;
import com.github.dumann089.theatricalextralights.net.SetFixtureArmedPacket;
import com.github.dumann089.theatricalextralights.util.DirectionOffset;
import dev.imabad.theatrical.blockentities.light.BaseDMXConsumerLightBlockEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/**
 * Menu des machines a flammes : patch DMX plus une section Securite avec la cle
 * d'armement (armee / desarmee), l'etat de sortie en direct et l'intensite.
 */
public class PyroConfigScreen extends ExtraLightsConfigScreen {

    private static final int STATUS_CARD_H = 58;
    private static final int ARM_H = 30;
    private static final int FLAME_COLOR = 0xFFFF8434;

    private final BaseDMXConsumerLightBlockEntity fixture;
    private final HasSafetyArm safety;

    private TelUi.FlatButton armButton;
    private int sectionY;
    private int statusCardY;

    public <T extends BaseDMXConsumerLightBlockEntity & HasSafetyArm> PyroConfigScreen(T fixture, BlockPos pos) {
        this(fixture, fixture, pos);
    }

    /** Variante sans generique pour le dispatch : {@code fixture} et {@code safety} sont le meme objet. */
    public PyroConfigScreen(BaseDMXConsumerLightBlockEntity fixture, HasSafetyArm safety, BlockPos pos) {
        super(fixture, pos, fixture.getTranslationKey(), false);
        this.fixture = fixture;
        this.safety = safety;
    }

    // ── Construction ─────────────────────────────────────────────────────────

    @Override
    protected int buildExtraWidgets(int y) {
        sectionY = y;
        y += LABEL_GAP + 3;

        statusCardY = y;
        y += STATUS_CARD_H + ROW_GAP;

        armButton = addRenderableWidget(new TelUi.FlatButton(contentLeft, y, contentWidth, ARM_H,
                armLabel(), armStyle(), b -> toggleArmed()));
        y += ARM_H + SECTION_GAP;
        return y;
    }

    private Component armLabel() {
        return safety.isArmed()
                ? Component.translatable("screen.pyro.disarm")
                : Component.translatable("screen.pyro.arm");
    }

    private TelUi.ButtonStyle armStyle() {
        return safety.isArmed() ? TelUi.ButtonStyle.DANGER : TelUi.ButtonStyle.PRIMARY;
    }

    private void toggleArmed() {
        boolean armed = !safety.isArmed();
        safety.setArmed(armed);
        ModNetworkHandler.CHANNEL.sendToServer(new SetFixtureArmedPacket(pos, armed));
        refreshArmButton();
    }

    private void refreshArmButton() {
        if (armButton == null) {
            return;
        }
        armButton.setMessage(armLabel());
        armButton.setStyle(armStyle());
    }

    // ── Rendu ────────────────────────────────────────────────────────────────

    @Override
    protected void renderExtraLabels(GuiGraphics g) {
        drawSectionLabel(g, Component.translatable("screen.pyro.section"), sectionY);
        refreshArmButton();

        int x = contentLeft;
        int y = statusCardY;
        int w = contentWidth;
        TelUi.card(g, x, y, w, STATUS_CARD_H);

        boolean armed = safety.isArmed();
        int intensity = Math.round(fixture.getIntensity());
        boolean live = armed && intensity > 0;

        // Ligne 1 : cle d'armement
        Component armState = armed
                ? Component.translatable("screen.pyro.status_armed")
                : Component.translatable("screen.pyro.status_disarmed");
        int armColor = armed ? TelUi.WARN : TelUi.OK;
        g.fill(x + 7, y + 8, x + 13, y + 14, armColor);
        TelUi.outline(g, x + 6, y + 7, 8, 8, TelUi.BORDER);
        TelUi.text(g, font, armState, x + 18, y + 7, armColor);

        // Etat de sortie a droite
        Component out = live
                ? Component.translatable("screen.pyro.output_live")
                : Component.translatable("screen.pyro.output_idle");
        int outColor = live ? FLAME_COLOR : TelUi.SUB;
        g.drawString(font, out, x + w - 6 - font.width(out), y + 7, outColor, false);

        // Ligne 2 : canal secondaire selon la machine
        Component line2;
        if (fixture instanceof FlameThrowerBlockEntity thrower) {
            line2 = Component.translatable("screen.pyro.head_angle",
                    Integer.toString(Math.round(DirectionOffset.panToAngle(thrower.getPan()))));
        } else if (fixture instanceof FlameProjectorBlockEntity projector) {
            line2 = Component.translatable("screen.pyro.flame_length",
                    Integer.toString(Math.round(projector.getFlameLengthRaw() / 2.55f)));
        } else {
            var personalities = fixture.getFixture().getDMXPersonalities();
            line2 = personalities != null && !personalities.isEmpty()
                    ? Component.literal(personalities.get(0).getDescription())
                    : Component.empty();
        }
        TelUi.text(g, font, line2, x + 6, y + 22, TelUi.TEXT);

        // Ligne 3 : barre d'intensite
        int barX = x + 6;
        int barY = y + STATUS_CARD_H - 16;
        int barW = w - 12;
        int barH = 8;
        g.fill(barX, barY, barX + barW, barY + barH, TelUi.BG);
        TelUi.outline(g, barX, barY, barW, barH, TelUi.BORDER_SOFT);
        int fill = (int) Math.round((barW - 2) * (intensity / 255.0));
        if (fill > 0) {
            g.fill(barX + 1, barY + 1, barX + 1 + fill, barY + barH - 1, armed ? FLAME_COLOR : TelUi.BORDER_SOFT);
        }
        Component pct = Component.translatable("screen.laser.intensity", Integer.toString(Math.round(intensity / 2.55f)));
        TelUi.text(g, font, pct, barX, barY - 10, TelUi.LABEL);
    }
}
