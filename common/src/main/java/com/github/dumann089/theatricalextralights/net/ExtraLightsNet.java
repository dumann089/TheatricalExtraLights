package com.github.dumann089.theatricalextralights.net;

import dev.architectury.networking.simple.MessageType;
import dev.architectury.networking.simple.SimpleNetworkManager;

public final class ExtraLightsNet {

    public static final SimpleNetworkManager MAIN =
            SimpleNetworkManager.create("theatricalextralights");

    public static final MessageType OPEN_SCREEN =
            MAIN.registerS2C(
                    "open_extra_lights_screen",
                    OpenExtraLightsScreenPacket::new
            );

    public static final MessageType LED_FACADE_FRAMES =
            MAIN.registerS2C(
                    "led_facade_frames",
                    LedFacadeFramesPacket::new
            );

    public static void init() {
        // Intentionally empty
        // Calling this forces class loading
    }
}