package com.github.dumann089.theatricalextralights.client.forge;

import com.github.dumann089.theatricalextralights.TheatricalExtraLightsClient;
import com.github.dumann089.theatricalextralights.client.ConfettiCannonClientSetup;
import com.github.dumann089.theatricalextralights.client.ModShaders;
import com.github.dumann089.theatricalextralights.client.gui.ExtraLightsSettingsScreen;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;
import com.github.dumann089.theatricalextralights.client.entities.FireworkRocketRenderer;
import com.github.dumann089.theatricalextralights.entities.ModEntities;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import java.io.IOException;

/**
 * Glue de cliente para Forge.
 *
 * <p>Esta clase toca eventos que sólo existen en el cliente (por ejemplo
 * {@link RegisterParticleProvidersEvent}, cuyo constructor referencia
 * {@code ParticleEngine}), así que NO debe cargarse en un servidor dedicado:
 * el RuntimeDistCleaner de Forge lanzaría
 * "Attempted to load class ... for invalid dist DEDICATED_SERVER" y el mod
 * fallaría al construirse. Sólo se referencia desde
 * {@code TheatricalExtraLightsForge} dentro de una comprobación de dist.
 */
public final class TheatricalExtraLightsForgeClient {

    private TheatricalExtraLightsForgeClient() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(TheatricalExtraLightsForgeClient::clientSetup);

        // RegisterClientCommandsEvent est diffuse sur le bus Forge, pas sur le bus du mod.
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(SettingsCommandForge::register);

        // Registramos el evento para cargar nuestros Shaders de GPU
        modEventBus.addListener(TheatricalExtraLightsForgeClient::registerShaders);

        // Registro nativo Forge del renderer del firework
        modEventBus.addListener(TheatricalExtraLightsForgeClient::registerEntityRenderers);
        modEventBus.addListener(TheatricalExtraLightsForgeClient::registerLayerDefinitions);
        modEventBus.addListener(TheatricalExtraLightsForgeClient::registerParticleProviders);
    }

    private static void registerParticleProviders(final RegisterParticleProvidersEvent event) {
        ModParticleClientImpl.registerForgeProviders(event);
    }

    private static void registerLayerDefinitions(final EntityRenderersEvent.RegisterLayerDefinitions event) {
        ConfettiCannonClientSetup.registerModelLayer(event::registerLayerDefinition);
    }

    private static void registerEntityRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.FIREWORK_ROCKET.get(), FireworkRocketRenderer::new);
    }

    private static void clientSetup(final FMLClientSetupEvent event) {
        // Inicializamos los renders generales a través de Architectury/común
        TheatricalExtraLightsClient.init();

        // Bouton « Configuration » dans la liste des mods de Forge.
        ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (minecraft, parent) -> new ExtraLightsSettingsScreen(parent))
        );
    }

    private static void registerShaders(final RegisterShadersEvent event) {
        try {
            // 1. Shader Original del Gobo Projector
            event.registerShader(
                    new ShaderInstance(
                            event.getResourceProvider(),
                            new ResourceLocation("theatricalextralights", "gobo_projector"),
                            DefaultVertexFormat.POSITION_COLOR
                    ),
                    shader -> ModShaders.goboProjectorShader = shader
            );

            // 2. NUEVO: Shader del Volumetric Beam
            // IMPORTANTE: Utiliza POSITION_COLOR_TEX porque enviamos coordenadas UV
            event.registerShader(
                    new ShaderInstance(
                            event.getResourceProvider(),
                            new ResourceLocation("theatricalextralights", "volumetric_beam"),
                            DefaultVertexFormat.POSITION_COLOR_TEX
                    ),
                    shader -> ModShaders.volumetricBeamShader = shader
            );

            event.registerShader(
                    new ShaderInstance(
                            event.getResourceProvider(),
                            new ResourceLocation("theatricalextralights", "beam_raymarch"),
                            DefaultVertexFormat.POSITION_COLOR_TEX
                    ),
                    shader -> ModShaders.beamRaymarchShader = shader
            );
        } catch (IOException e) {
            throw new RuntimeException("Error cargando los shaders para Theatrical Extra Lights", e);
        }
    }
}
