package com.github.dumann089.theatricalextralights.forge;

import com.github.dumann089.theatricalextralights.TheatricalExtraLights;
import com.github.dumann089.theatricalextralights.client.forge.TheatricalExtraLightsForgeClient;
import dev.architectury.platform.forge.EventBuses;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.GameShuttingDownEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(TheatricalExtraLights.MOD_ID)
public class TheatricalExtraLightsForge {

    public TheatricalExtraLightsForge() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        EventBuses.registerModEventBus(TheatricalExtraLights.MOD_ID, modEventBus);
        TheatricalExtraLights.init();

        if (FMLEnvironment.dist == Dist.CLIENT) {
            TheatricalExtraLightsForgeClient.register(modEventBus);
            MinecraftForge.EVENT_BUS.register(ForgeHooksClient.ClientEvents.class);
        }
    }
}