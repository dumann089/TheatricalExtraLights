package com.github.dumann089.theatricalextralights.forge;

import com.github.dumann089.theatricalextralights.TheatricalExtraLights;
import com.github.dumann089.theatricalextralights.TheatricalExtraLightsClient;
import com.github.dumann089.theatricalextralights.client.ConfettiCannonClientSetup;
import com.github.dumann089.theatricalextralights.client.ModShaders;
import com.github.dumann089.theatricalextralights.client.entities.FireworkRocketRenderer;
import com.github.dumann089.theatricalextralights.client.forge.ModParticleClientImpl;
import com.github.dumann089.theatricalextralights.client.render.beam.raymarch.SceneDepthCopy;
import com.github.dumann089.theatricalextralights.config.TheatricalExtraLightsConfig;
import com.github.dumann089.theatricalextralights.entities.ModEntities;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import dev.imabad.theatrical.compat.ModCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.MinecraftForge;
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
        MinecraftForge.EVENT_BUS.addListener(
                TheatricalExtraLightsForgeClient::onRenderLevelStage
        );
    }

    /**
     * Captura la profundidad justo después de las block entities,
     * antes del renderizado translúcido y partículas.
     */
    private static void onRenderLevelStage(final RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) {
            return;
        }

        if (!TheatricalExtraLightsConfig.isRaymarchEngine()
                || !ModShaders.canUseRaymarch()) {
            return;
        }

        // Flush de los batches pendientes para incluir su profundidad.
        Minecraft.getInstance()
                .renderBuffers()
                .bufferSource()
                .endBatch();

        SceneDepthCopy.capture();
    }

    @SubscribeEvent
    public static void registerParticleProviders(
            final RegisterParticleProvidersEvent event
    ) {
        ModParticleClientImpl.registerForgeProviders(event);
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(
            final EntityRenderersEvent.RegisterLayerDefinitions event
    ) {
        ConfettiCannonClientSetup.registerModelLayer(
                event::registerLayerDefinition
        );
    }

    @SubscribeEvent
    public static void registerEntityRenderers(
            final EntityRenderersEvent.RegisterRenderers event
    ) {
        event.registerEntityRenderer(
                ModEntities.FIREWORK_ROCKET.get(),
                FireworkRocketRenderer::new
        );
    }

    @SubscribeEvent
    public static void registerShaders(final RegisterShadersEvent event) {
        try {
            event.registerShader(
                    new ShaderInstance(
                            event.getResourceProvider(),
                            new ResourceLocation(
                                    "theatricalextralights",
                                    "gobo_projector"
                            ),
                            DefaultVertexFormat.POSITION_COLOR_TEX
                    ),
                    shader -> ModShaders.goboProjectorShader = shader
            );

            event.registerShader(
                    new ShaderInstance(
                            event.getResourceProvider(),
                            new ResourceLocation(
                                    "theatricalextralights",
                                    "volumetric_beam"
                            ),
                            DefaultVertexFormat.POSITION_COLOR_TEX
                    ),
                    shader -> ModShaders.volumetricBeamShader = shader
            );

            event.registerShader(
                    new ShaderInstance(
                            event.getResourceProvider(),
                            new ResourceLocation(
                                    "theatricalextralights",
                                    "beam_raymarch"
                            ),
                            DefaultVertexFormat.POSITION_COLOR_TEX
                    ),
                    shader -> ModShaders.beamRaymarchShader = shader
            );

        } catch (IOException e) {
            throw new RuntimeException(
                    "Error Loading Shader Theatrical Extra Lights",
                    e
            );
        }
    }
}