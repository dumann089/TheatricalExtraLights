
package com.github.dumann089.theatricalextralights.util;

public final class BigScrollerGel {
    public static final float GEL_HEIGHT = 2.8F;

    private static final int[][] COLORS = {
            {255, 0, 0},
            {255, 80, 0},
            {255, 180, 0},
            {255, 255, 0},
            {0, 255, 0},
            {0, 255, 180},
            {0, 180, 255},
            {0, 0, 255},
            {100, 0, 255},
            {180, 0, 255},
            {255, 0, 120},
            {255, 255, 255}
    };

    private BigScrollerGel() {}

    public static int getGelCount() {
        return COLORS.length;
    }

    public static int[] getGelColor(int index) {
        return COLORS[Math.floorMod(index, COLORS.length)];
    }

    /**
     * Posición del rollo calculada exclusivamente desde el canal DMX 4.
     * El desplazamiento es negativo para que al aumentar el canal,
     * la tira se desplace hacia abajo en las coordenadas locales del modelo.
     */
    public static float getOffset(int dmxPosition) {
        int position = Math.max(0, Math.min(255, dmxPosition));
        return -(position / 255.0F) * (COLORS.length - 1);
    }

    /**
     * Devuelve la franja que ocupa una coordenada Y.
     * Este método se comparte entre la tira y los beams.
     */
    public static int getGelIndexAtY(float y, float offset) {
        return (int) Math.floor(y / GEL_HEIGHT - offset);
    }

    public static int[] getGelColorAtY(float y, int dmxPosition) {
        return getGelColor(getGelIndexAtY(y, getOffset(dmxPosition)));
    }

    /**
     * Color representativo de la sección central, útil para compatibilidad
     * con código que utiliza un único color RGB del BlockEntity.
     */
    public static int[] getShimmerColor(int dmxPosition) {
        return getGelColorAtY(0.46875F, dmxPosition);
    }
}