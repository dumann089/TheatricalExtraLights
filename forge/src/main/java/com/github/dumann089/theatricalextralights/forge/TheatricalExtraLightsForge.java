package com.github.dumann089.theatricalextralights.forge;

import com.github.dumann089.theatricalextralights.TheatricalExtraLights;
import dev.architectury.platform.forge.EventBuses;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import com.github.dumann089.theatricalextralights.client.forge.TheatricalExtraLightsForgeClient;
import dev.architectury.platform.forge.EventBuses;
import net.minecraftforge.api.distmarker.Dist;
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

        // Los listeners de cliente se registran en una clase aparte: sus eventos
        // (shaders, renderers, particle providers) referencian clases client-only
        // y en un servidor dedicado el RuntimeDistCleaner haría fallar la carga
        // del mod al resolverlas. Con esta comprobación nunca se cargan en server.
        if (FMLEnvironment.dist == Dist.CLIENT) {
            TheatricalExtraLightsForgeClient.register(modEventBus);
        }
    }
}
