package com.github.dumann089.theatricalextralights.client;

import com.github.dumann089.theatricalextralights.client.gui.ExtraLightsSettingsScreen;
import net.minecraft.client.Minecraft;

/**
 * Ouverture de l'ecran de reglages depuis un point d'entree qui n'est pas un ecran :
 * la commande {@code /tel config} et, cote Forge, la fabrique du menu Mods.
 */
public final class ExtraLightsSettingsAccess {

    private ExtraLightsSettingsAccess() {}

    /**
     * Ouvre l'ecran au tick suivant. Une commande s'execute pendant que le chat est
     * encore affiche : poser l'ecran immediatement le ferait remplacer aussitot par la
     * fermeture du chat, d'ou le passage par la file du client.
     */
    public static void openDeferred() {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.tell(() -> minecraft.setScreen(new ExtraLightsSettingsScreen(null)));
    }
}
