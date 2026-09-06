package com.github.dumann089.theatricalextralights.fabric;

import com.github.dumann089.theatricalextralights.client.gui.ExtraLightsSettingsScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * Bouton de reglages dans la liste des mods de ModMenu.
 *
 * <p>ModMenu est une dependance de compilation seulement : Fabric ne charge cette classe
 * que si ModMenu interroge le point d'entree « modmenu », donc son absence est sans effet.
 * La commande {@code /tel config} reste disponible dans ce cas.
 */
public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ExtraLightsSettingsScreen::new;
    }
}
