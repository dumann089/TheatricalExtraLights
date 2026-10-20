package com.github.dumann089.theatricalextralights.client;

import com.github.dumann089.theatricalextralights.client.gui.ExtraLightsSettingsScreen;
import com.mojang.blaze3d.platform.InputConstants;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class ModKeybinds {

    public static final KeyMapping CONFIG_MENU = new KeyMapping(
            "key.theatricalextralights.config",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_O,
            "category.theatricalextralights"
    );

    private static boolean registered;

    /**
     * Idempotent : Forge l'appelle des la construction du mod (avant RegisterKeyMappingsEvent, sinon
     * Architectury avertit « registered after event »), puis TheatricalExtraLightsClient.init() le
     * rappelle sans effet.
     */
    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        KeyMappingRegistry.register(CONFIG_MENU);
        ClientTickEvent.CLIENT_POST.register(minecraft -> {
            while (CONFIG_MENU.consumeClick()) {
                minecraft.setScreen(new ExtraLightsSettingsScreen(minecraft.screen));
            }
        });
    }
}