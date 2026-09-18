package com.github.dumann089.theatricalextralights.fixtures;

import com.github.dumann089.theatricalextralights.util.FramingShutterState;
import com.github.dumann089.theatricalextralights.util.ProfileHeadState;
import dev.imabad.theatrical.api.dmx.DMXPersonality;
import dev.imabad.theatrical.fixtures.SharedSlots;

/**
 * Personnalite « 29ch - Profile 16bit » des lyres a gobos : shutter, dimmer 16 bit, RGB,
 * gobo, prisme, roue d'animation, frost, zoom, focus, pan/tilt 16 bit, vitesse moteur, puis
 * le module de couteaux.
 */
public final class ProfileHeadChannels {

    public static final int TOTAL_CHANNELS = ProfileHeadState.CHANNELS_BEFORE_SHUTTERS + FramingShutterState.CHANNEL_COUNT;

    public static final DMXPersonality PERSONALITY_PROFILE = FramingShutterChannels.addFramingSlots(
            new DMXPersonality(TOTAL_CHANNELS, "29ch - Profile 16bit")
                    .addSlot(ExtraLightsSlots.SHUTTER_STROBE)   // 1 shutter
                    .addSlot(SharedSlots.INTENSITY)              // 2 dimmer coarse
                    .addSlot(ExtraLightsSlots.DIMMER_FINE)       // 3 dimmer fine
                    .addSlot(SharedSlots.RED)                    // 4
                    .addSlot(SharedSlots.GREEN)                  // 5
                    .addSlot(SharedSlots.BLUE)                   // 6
                    .addSlot(ExtraLightsSlots.GOBO_WHEEL)        // 7
                    .addSlot(ExtraLightsSlots.GOBO_ROTATION)     // 8
                    .addSlot(ExtraLightsSlots.PRISM)             // 9
                    .addSlot(ExtraLightsSlots.PRISM_ROTATION)    // 10
                    .addSlot(ExtraLightsSlots.ANIMATION_WHEEL)   // 11
                    .addSlot(ExtraLightsSlots.ANIMATION_ROTATION)// 12
                    .addSlot(ExtraLightsSlots.FROST)             // 13
                    .addSlot(ExtraLightsSlots.ZOOM)              // 14
                    .addSlot(SharedSlots.FOCUS)                  // 15
                    .addSlot(SharedSlots.PAN)                    // 16
                    .addSlot(ExtraLightsSlots.PAN_FINE)          // 17
                    .addSlot(SharedSlots.TILT)                   // 18
                    .addSlot(ExtraLightsSlots.TILT_FINE)         // 19
                    .addSlot(ExtraLightsSlots.PAN_TILT_SPEED)    // 20
    );

    /** Ancien nom conserve pour les fixtures deja ecrites. */
    public static final DMXPersonality PERSONALITY_28CH = PERSONALITY_PROFILE;

    private ProfileHeadChannels() {
    }
}
