package com.github.dumann089.theatricalextralights.fixtures;

import dev.imabad.theatrical.api.dmx.DMXPersonality;
import dev.imabad.theatrical.fixtures.SharedSlots;

/** Personnalite « mode pixel » des barres LED : un dimmer et un RGB par pixel, sans dimmer general. */
public final class PixelBarPersonalities {

    private PixelBarPersonalities() {
    }

    /** Ajoute dimmer + RGB pour chaque pixel a une personnalite deja garnie de ses canaux d'en-tete. */
    public static DMXPersonality addPixels(DMXPersonality personality, int pixelCount) {
        for (int i = 0; i < pixelCount; i++) {
            personality.addSlot(SharedSlots.INTENSITY).addSlot(SharedSlots.RED)
                    .addSlot(SharedSlots.GREEN).addSlot(SharedSlots.BLUE);
        }
        return personality;
    }

    /** « 36-Channel Pixel Mode (9x Dim/RGB) », ou avec en-tete « 34-Channel Pixel Mode (Pan, Tilt + 8x Dim/RGB) ». */
    public static String pixelModeName(int channels, int pixelCount, String header) {
        return channels + "-Channel Pixel Mode (" + (header.isEmpty() ? "" : header + " + ") + pixelCount + "x Dim/RGB)";
    }
}
