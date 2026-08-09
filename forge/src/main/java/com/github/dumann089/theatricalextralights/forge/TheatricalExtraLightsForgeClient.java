package com.github.dumann089.theatricalextralights.forge;

import com.github.dumann089.theatricalextralights.TheatricalExtraLights;
import com.github.dumann089.theatricalextralights.TheatricalExtraLightsClient;
import com.github.dumann089.theatricalextralights.client.ConfettiCannonClientSetup;
import com.github.dumann089.theatricalextralights.client.ModShaders;
import com.github.dumann089.theatricalextralights.client.entities.FireworkRocketRenderer;
import com.github.dumann089.theatricalextralights.client.forge.ModParticleClientImpl;
import com.github.dumann089.theatricalextralights.entities.ModEntities;
import dev.imabad.theatrical.compat.ModCompat;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import java.io.IOException;

@Mod.EventBusSubscriber(
        value = Dist.CLIENT,
        modid = TheatricalExtraLights.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD
)
public final class TheatricalExtraLightsForgeClient {
    private TheatricalExtraLightsForgeClient() {
    }

    @SubscribeEvent
    public static void clientSetup(final FMLClientSetupEvent event) {
        TheatricalExtraLightsClient.init();
    }

    @SubscribeEvent
    public static void registerParticleProviders(final RegisterParticleProvidersEvent event) {
        ModParticleClientImpl.registerForgeProviders(event);
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(final EntityRenderersEvent.RegisterLayerDefinitions event) {
        ConfettiCannonClientSetup.registerModelLayer(event::registerLayerDefinition);
    }

    @SubscribeEvent
    public static void registerEntityRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.FIREWORK_ROCKET.get(), FireworkRocketRenderer::new);
    }

    @SubscribeEvent
    public static void registerShaders(final RegisterShadersEvent event) {
        // Shimmer 0.2.4 intercepte le reload global des shaders vanilla (particle, etc.)
        // et plante sur fog_distance ; on utilise les fallbacks beacon beam à la place.
        if (ModCompat.SHIMMER) {
            return;
        }

        try {
            event.registerShader(
                    new ShaderInstance(
                            event.getResourceProvider(),
                            new ResourceLocation("theatricalextralights", "gobo_projector"),
                            DefaultVertexFormat.POSITION_COLOR
                    ),
                    shader -> ModShaders.goboProjectorShader = shader
            );

            event.registerShader(
                    new ShaderInstance(
                            event.getResourceProvider(),
                            new ResourceLocation("theatricalextralights", "volumetric_beam"),
                            DefaultVertexFormat.POSITION_COLOR_TEX
                    ),
                    shader -> ModShaders.volumetricBeamShader = shader
            );
        } catch (IOException e) {
            throw new RuntimeException("Error cargando los shaders para Theatrical Extra Lights", e);
        }
    }
}
