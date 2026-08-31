package com.github.dumann089.theatricalextralights.fabric;

import com.github.dumann089.theatricalextralights.client.ExtraLightsSettingsAccess;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.minecraft.network.chat.Component;

/** Commande cliente {@code /tel config}, secours si ModMenu n'est pas installe. */
public final class SettingsCommandFabric {

    private SettingsCommandFabric() {}

    public static void init() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(
                        ClientCommandManager.literal("tel")
                                .then(ClientCommandManager.literal("config").executes(ctx -> {
                                    ExtraLightsSettingsAccess.openDeferred();
                                    ctx.getSource().sendFeedback(
                                            Component.translatable("tel.command.config.opened"));
                                    return 1;
                                }))
                )
        );
    }
}
