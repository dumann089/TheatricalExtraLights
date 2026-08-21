package com.github.dumann089.theatricalextralights.fabric;

import com.github.dumann089.theatricalextralights.TheatricalExtraLightsClient;
import com.github.dumann089.theatricalextralights.client.LensRenderTypes;
import com.github.dumann089.theatricalextralights.client.ModShaders;
import com.github.dumann089.theatricalextralights.client.firework.DetachedPyroSparks;
import dev.imabad.theatrical.compat.ModCompat;
import com.github.dumann089.theatricalextralights.client.ConfettiCannonClientSetup;
import com.github.dumann089.theatricalextralights.client.ConfettiCannonItemRenderer;
import com.github.dumann089.theatricalextralights.client.model.ConfettiCannonModel;
import com.github.dumann089.theatricalextralights.items.Items;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

public class TheatricalExtraLightsClientFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityModelLayerRegistry.registerModelLayer(
                ConfettiCannonModel.LAYER_LOCATION,
                ConfettiCannonModel::createBodyLayer
        );
        TheatricalExtraLightsClient.init();
        registerConfettiCannonItemRenderer();
        com.github.dumann089.theatricalextralights.fabric.FollowspotCameraFabric.init();

        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level == null) {
                return;
            }
            var buffers = minecraft.renderBuffers().bufferSource();
            DetachedPyroSparks.render(context.matrixStack(), buffers, context.camera(), context.tickDelta());
            buffers.endBatch(LensRenderTypes.LENS);
        });

        CoreShaderRegistrationCallback.EVENT.register(context -> {
            context.register(
                    new ResourceLocation("theatricalextralights", "gobo_projector"),
                    DefaultVertexFormat.POSITION_COLOR_TEX,
                    shader -> ModShaders.goboProjectorShader = shader
            );

            context.register(
                    new ResourceLocation("theatricalextralights", "volumetric_beam"),
                    DefaultVertexFormat.POSITION_COLOR_TEX,
                    shader -> ModShaders.volumetricBeamShader = shader
            );
        });
    }

    private static void registerConfettiCannonItemRenderer() {
        BuiltinItemRendererRegistry.INSTANCE.register(
                Items.CONFETTI_CANNON.get(),
                (stack, displayContext, poseStack, buffer, packedLight, packedOverlay) -> getConfettiCannonItemRenderer()
                        .renderByItem(stack, displayContext, poseStack, buffer, packedLight, packedOverlay)
        );
    }

    private static ConfettiCannonItemRenderer confettiCannonItemRenderer;

    private static ConfettiCannonItemRenderer getConfettiCannonItemRenderer() {
        if (confettiCannonItemRenderer == null) {
            Minecraft minecraft = Minecraft.getInstance();
            confettiCannonItemRenderer = new ConfettiCannonItemRenderer(
                    minecraft.getBlockEntityRenderDispatcher(),
                    minecraft.getEntityModels()
            );
        }
        return confettiCannonItemRenderer;
    }
}