package com.github.dumann089.theatricalextralights.client.forge;

import com.github.dumann089.theatricalextralights.client.ExtraLightsSettingsAccess;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;

/** Commande cliente {@code /tel config}, pendant Forge de la version Fabric. */
public final class SettingsCommandForge {

    private SettingsCommandForge() {}

    public static void register(final RegisterClientCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("tel")
                        .then(Commands.literal("config").executes(ctx -> {
                            ExtraLightsSettingsAccess.openDeferred();
                            ctx.getSource().sendSuccess(
                                    () -> Component.translatable("tel.command.config.opened"), false);
                            return 1;
                        }))
        );
    }
}
