package com.github.dumann089.theatricalextralights.client;

import com.github.dumann089.theatricalextralights.config.ConfigScreen;
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

    public static void register() {
        KeyMappingRegistry.register(CONFIG_MENU);
        ClientTickEvent.CLIENT_POST.register(minecraft -> {
            while (CONFIG_MENU.consumeClick()) {
                minecraft.setScreen(new ConfigScreen(minecraft.screen));
            }
        });
    }
}