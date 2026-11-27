package com.github.dumann089.theatricalextralights.client.broadcast;

import net.minecraft.client.gui.GuiGraphics;

/** Proporção da imagem da câmera: barras pretas que deixam a tela em 16:9 ou 4:3. */
public final class AspectBars {

    private static final int BLACK = 0xFF000000;

    private AspectBars() {
    }

    /** 0 = a da janela (sem barras), 1 = 16:9, 2 = 4:3. */
    public static float ratio(int mode) {
        return mode == 1 ? 16f / 9f : mode == 2 ? 4f / 3f : 0f;
    }

    public static void draw(GuiGraphics graphics, int width, int height, float ratio) {
        if (ratio <= 0f || width <= 0 || height <= 0) {
            return;
        }
        float current = (float) width / (float) height;
        if (current > ratio + 0.001f) {
            int bar = Math.round((width - height * ratio) / 2f);
            graphics.fill(0, 0, bar, height, BLACK);
            graphics.fill(width - bar, 0, width, height, BLACK);
        } else if (current < ratio - 0.001f) {
            int bar = Math.round((height - width / ratio) / 2f);
            graphics.fill(0, 0, width, bar, BLACK);
            graphics.fill(0, height - bar, width, height, BLACK);
        }
    }
}
