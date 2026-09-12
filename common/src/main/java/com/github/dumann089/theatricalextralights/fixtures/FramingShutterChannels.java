package com.github.dumann089.theatricalextralights.fixtures;

import com.github.dumann089.theatricalextralights.util.FramingShutterState;
import dev.imabad.theatrical.api.dmx.DMXPersonality;
import dev.imabad.theatrical.fixtures.SharedSlots;

/**
 * Personnalite 19 canaux des lyres a gobos : les 10 canaux standards suivis du module
 * de couteaux a 4 lames en convention A/B (grandMA, Ayrton Diablo, Clay Paky) : coins A
 * et B de chaque lame, puis rotation du module.
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

    /** Ajoute les 9 slots couteaux (4 x coin A + coin B, puis rotation du module). */
    public static DMXPersonality addFramingSlots(DMXPersonality personality) {
        return personality
                .addSlot(ExtraLightsSlots.BLADE_1_A)
                .addSlot(ExtraLightsSlots.BLADE_1_B)
                .addSlot(ExtraLightsSlots.BLADE_2_A)
                .addSlot(ExtraLightsSlots.BLADE_2_B)
                .addSlot(ExtraLightsSlots.BLADE_3_A)
                .addSlot(ExtraLightsSlots.BLADE_3_B)
                .addSlot(ExtraLightsSlots.BLADE_4_A)
                .addSlot(ExtraLightsSlots.BLADE_4_B)
                .addSlot(ExtraLightsSlots.FRAMING_ROTATION);
    }
}
