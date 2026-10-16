package com.github.dumann089.theatricalextralights.forge;

import com.github.dumann089.theatricalextralights.TheatricalExtraLights;
import com.github.dumann089.theatricalextralights.client.firework.FireworkRocketLateRender;
import com.github.dumann089.theatricalextralights.client.LensRenderTypes;
import com.github.dumann089.theatricalextralights.client.LensBloomCompat;
import com.github.dumann089.theatricalextralights.client.firework.DetachedPyroSparks;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.imabad.theatrical.TheatricalClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        value = Dist.CLIENT,
        modid = TheatricalExtraLights.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class TheatricalExtraLightsForgeWorldRender {
    private TheatricalExtraLightsForgeWorldRender() {
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        TheatricalClient.renderWorldLast(event.getPoseStack(), event.getProjectionMatrix(), event.getCamera(), event.getPartialTick());

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        DetachedPyroSparks.render(poseStack, buffers, event.getCamera(), event.getPartialTick());
        FireworkRocketLateRender.render(poseStack, buffers, event.getCamera(), event.getPartialTick(), event.getFrustum());
        buffers.endBatch(LensRenderTypes.FLAME);
        buffers.endBatch(LensRenderTypes.LENS);
    }
}
