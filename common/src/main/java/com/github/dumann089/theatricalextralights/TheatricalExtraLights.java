package com.github.dumann089.theatricalextralights;

import com.github.dumann089.theatricalextralights.blockentities.BlockEntities;
import com.github.dumann089.theatricalextralights.blocks.Blocks;
import com.github.dumann089.theatricalextralights.client.ModKeybinds;
import com.github.dumann089.theatricalextralights.config.ConfigManager;
import com.github.dumann089.theatricalextralights.config.TheatricalExtraLightsConfig;
import com.github.dumann089.theatricalextralights.entities.ModEntities;
import com.github.dumann089.theatricalextralights.fixtures.Fixtures;
import com.github.dumann089.theatricalextralights.firework.FireworkRocketTracker;
import com.github.dumann089.theatricalextralights.items.Items;
import com.github.dumann089.theatricalextralights.net.ExtraLightsNet;
import com.github.dumann089.theatricalextralights.net.ModNetworkHandler;
import com.github.dumann089.theatricalextralights.net.ModNetworking;
import com.github.dumann089.theatricalextralights.particle.ModParticle;
import com.github.dumann089.theatricalextralights.sounds.ModSounds;
import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TheatricalExtraLights {
    public static final String MOD_ID = "theatricalextralights";
    public static final DeferredRegister<CreativeModeTab> TABS = TheatricalExtraLightsRegistry.get(Registries.CREATIVE_MODE_TAB);
    public static final RegistrySupplier<CreativeModeTab> TAB = TABS.register(
            TheatricalExtraLights.MOD_ID,
            () -> CreativeTabRegistry.create(
                    Component.translatable("itemGroup." + TheatricalExtraLights.MOD_ID),
                    () -> new ItemStack(Items.BIG_PANEL.get())
            )
    );
    public static final RegistrySupplier<CreativeModeTab> PYRO_TAB = TABS.register(
            "theatrical_pyro",
            () -> CreativeTabRegistry.create(
                    Component.translatable("itemGroup." + TheatricalExtraLights.MOD_ID + ".pyro"),
                    () -> new ItemStack(Items.FIREWORK_RED_PEONY.get())
            )
    );
    public static final RegistrySupplier<CreativeModeTab> MISC_TAB = TABS.register(
            "theatrical_misc",
            () -> CreativeTabRegistry.create(
                    Component.translatable("itemGroup." + TheatricalExtraLights.MOD_ID + ".misc"),
                    () -> new ItemStack(Items.DWT_PANEL.get())
            )
    );

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static void init() {
        TABS.register();
        Blocks.init();
        Fixtures.init();
        Items.init();
        BlockEntities.init();
        ModEntities.init();
        ModParticle.initialize();
        ModSounds.initialize();
        ModNetworkHandler.register();
        ExtraLightsNet.init();
        ConfigManager.load();
        FireworkRocketTracker.registerEvents();
        ModNetworking.register();
    }
}


