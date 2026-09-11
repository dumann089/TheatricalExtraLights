package com.github.dumann089.theatricalextralights.fixtures;

import com.github.dumann089.theatricalextralights.util.FramingShutterState;
import dev.imabad.theatrical.api.dmx.DMXPersonality;
import dev.imabad.theatrical.fixtures.SharedSlots;

/**
 * Personnalite 19 canaux des lyres a gobos : les 10 canaux standards suivis du module
 * de couteaux a 4 lames, dans l'ordre des protocoles Martin (MAC Encore Performance) et
 * Robe (T1 Profile) : insertion puis angle pour chaque lame, puis rotation du module.
 */
public final class FramingShutterChannels {

    public static final int BASE_CHANNELS = 10;
    public static final int TOTAL_CHANNELS = BASE_CHANNELS + FramingShutterState.CHANNEL_COUNT;

    public static final DMXPersonality PERSONALITY_19CH = addFramingSlots(
            new DMXPersonality(TOTAL_CHANNELS, "19ch - Framing Shutters")
                    .addSlot(SharedSlots.INTENSITY)
                    .addSlot(SharedSlots.RED)
                    .addSlot(SharedSlots.GREEN)
                    .addSlot(SharedSlots.BLUE)
                    .addSlot(SharedSlots.FOCUS)
                    .addSlot(SharedSlots.PAN)
                    .addSlot(SharedSlots.TILT)
                    .addSlot(SharedSlots.FOCUS) // Gobo wheel
                    .addSlot(SharedSlots.FOCUS) // Zoom
                    .addSlot(SharedSlots.FOCUS) // Gobo rotation
    );

    private FramingShutterChannels() {
    }

    /** Ajoute les 9 slots couteaux (4 x insertion + angle, puis rotation du module). */
    public static DMXPersonality addFramingSlots(DMXPersonality personality) {
        return personality
                .addSlot(ExtraLightsSlots.BLADE_1_INSERTION)
                .addSlot(ExtraLightsSlots.BLADE_1_ANGLE)
                .addSlot(ExtraLightsSlots.BLADE_2_INSERTION)
                .addSlot(ExtraLightsSlots.BLADE_2_ANGLE)
                .addSlot(ExtraLightsSlots.BLADE_3_INSERTION)
                .addSlot(ExtraLightsSlots.BLADE_3_ANGLE)
                .addSlot(ExtraLightsSlots.BLADE_4_INSERTION)
                .addSlot(ExtraLightsSlots.BLADE_4_ANGLE)
                .addSlot(ExtraLightsSlots.FRAMING_ROTATION);
    }
}
