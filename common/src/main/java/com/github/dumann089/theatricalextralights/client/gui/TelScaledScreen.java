package com.github.dumann089.theatricalextralights.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Ecran dont le contenu est reduit uniformement quand il ne tient pas dans la fenetre
 * (grandes echelles d'interface). Les sous-classes travaillent dans l'espace virtuel
 * {@link #vw} x {@link #vh} et dessinent dans {@link #renderScaled} ; la souris est
 * convertie automatiquement.
 */
public abstract class TelScaledScreen extends Screen {

    private static final int MARGIN = 8;
    private static final float MIN_SCALE = 0.45f;

    protected float uiScale = 1.0f;
    /** Dimensions virtuelles de l'ecran apres mise a l'echelle. */
    protected int vw;
    protected int vh;

    protected TelScaledScreen(Component title) {
        super(title);
    }

    /** Choisit l'echelle pour qu'un contenu de {@code neededW} x {@code neededH} tienne, marge comprise. */
    protected void fitToScreen(int neededW, int neededH) {
        float s = Math.min(
                width / (float) (neededW + MARGIN * 2),
                height / (float) (neededH + MARGIN * 2)
        );
        uiScale = Math.max(MIN_SCALE, Math.min(1.0f, s));
        vw = (int) Math.ceil(width / uiScale);
        vh = (int) Math.ceil(height / uiScale);
    }

    protected void resetScale() {
        uiScale = 1.0f;
        vw = width;
        vh = height;
    }

    protected abstract void renderScaled(GuiGraphics g, int mouseX, int mouseY, float partialTick);

    /** Dessine les widgets en coordonnees virtuelles (remplace super.render, qui rebouclerait sur renderScaled). */
    protected void renderWidgets(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        for (GuiEventListener child : children()) {
            if (child instanceof Renderable renderable) {
                renderable.render(g, mouseX, mouseY, partialTick);
            }
        }
    }

    /** Clic en coordonnees virtuelles ; par defaut transmet aux widgets. */
    protected boolean scaledMouseClicked(double mouseX, double mouseY, int button) {
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** Molette en coordonnees virtuelles ; par defaut transmet aux widgets. */
    protected boolean scaledMouseScrolled(double mouseX, double mouseY, double delta) {
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public final void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        if (uiScale == 1.0f) {
            renderScaled(g, mouseX, mouseY, partialTick);
            return;
        }
        g.pose().pushPose();
        g.pose().scale(uiScale, uiScale, 1.0f);
        renderScaled(g, sx(mouseX), sy(mouseY), partialTick);
        g.pose().popPose();
    }

    private int sx(double x) {
        return (int) Math.floor(x / uiScale);
    }

    private int sy(double y) {
        return (int) Math.floor(y / uiScale);
    }

    @Override
    public final boolean mouseClicked(double mouseX, double mouseY, int button) {
        return scaledMouseClicked(mouseX / uiScale, mouseY / uiScale, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return super.mouseReleased(mouseX / uiScale, mouseY / uiScale, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return super.mouseDragged(mouseX / uiScale, mouseY / uiScale, button, dragX / uiScale, dragY / uiScale);
    }

    @Override
    public final boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return scaledMouseScrolled(mouseX / uiScale, mouseY / uiScale, delta);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        super.mouseMoved(mouseX / uiScale, mouseY / uiScale);
    }
}
