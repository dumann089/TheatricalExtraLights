package com.github.dumann089.theatricalextralights.client;

import com.github.dumann089.theatricalextralights.util.GlobalGoboManager;
import dev.architectury.event.events.client.ClientPlayerEvent;

public class ClientEventsHandler {
    public static void register() {
        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> {
            CustomGoboLoader.clearCache();
            GlobalGoboManager.getAllMappings().clear();
        });
    }
}