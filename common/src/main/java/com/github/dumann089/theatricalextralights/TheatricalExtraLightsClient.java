package com.github.dumann089.theatricalextralights;

import com.github.dumann089.theatricalextralights.blockentities.BlockEntities;
import com.github.dumann089.theatricalextralights.client.ConfettiBurstClient;
import com.github.dumann089.theatricalextralights.client.ModKeybinds;
import com.github.dumann089.theatricalextralights.client.firework.FireworkSmokeEffects;
import com.github.dumann089.theatricalextralights.client.ExtraLightsClientScreens;
import com.github.dumann089.theatricalextralights.client.ModParticleClient;
import com.github.dumann089.theatricalextralights.client.blockentities.*;
import com.github.dumann089.theatricalextralights.client.entities.FireworkRocketRenderer;
import com.github.dumann089.theatricalextralights.compat.FireworkLightCompat;
import com.github.dumann089.theatricalextralights.entities.ModEntities;
import com.github.dumann089.theatricalextralights.net.ModNetworking;
import com.github.dumann089.theatricalextralights.net.OpenExtraLightsScreenPacket;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry;


public class TheatricalExtraLightsClient {


    public static void init() {
        ModParticleClient.registerProviders();
        ModKeybinds.register();
        ModNetworking.register();

        BlockEntityRendererRegistry.register(BlockEntities.MOVING_SCAN.get(), MovingScanRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.MOVING_VL2C.get(), MovingVL2CRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.MOVING_VL6.get(), MovingVL6Renderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.MOVING_BEAM.get(), MovingBeamRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.LED_FOUNTAIN.get(), LEDfountainRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.PAR_LED.get(), ParLedRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.BIG_PANEL.get(), BigPanelRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.BIG_PANEL2.get(), BigPanel2Renderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.RGB_BAR.get(), RGBbarRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.LASER.get(), LaserRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.BLINDER.get(), BlinderRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.BLINDER_WARM.get(), BlinderwarmRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.STROBE.get(), StrobeRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.ATOMIC_STROBE.get(), AtomicStrobeRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.LED_PANEL_2.get(), LEDPanel2Renderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.LED_FACADE.get(), LedFacadeRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.TRUSS_3LIGHTS.get(), truss3lightsRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.BEAM_7R.get(), Beam7RRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.SOURCE_FOUR.get(), Source4Renderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.SOURCE_FOUR_WARM.get(), Source4warmRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.MAC_VIP.get(), MacVipRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.SHARPLUS.get(), SharplusRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.MOVING500.get(), Moving500Renderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.ROBITSPOT.get(), RobitspotRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.PAR1000_RED.get(), Par1000RedRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.PAR1000_BLUE.get(), Par1000BlueRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.PAR1000_GREEN.get(), Par1000GreenRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.PAR1000_MAGENTA.get(), Par1000MagentaRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.PAR1000_AMBER.get(), Par1000AmberRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.PAR1000.get(), Par1000Renderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.PAR1000_PURPLE.get(), Par1000PurpleRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.PAR1000_LIGHTBLUE.get(), Par1000LightblueRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.PAR1000_WHITE.get(), Par1000WhiteRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.PAR1000_ORANGE.get(), Par1000OrangeRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.VERVESPOT.get(), VervespotRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.VERTICAL_BAR.get(), VerticalbarRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.SEARCHLIGHT.get(), SearchlightRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.WASHLIGHT.get(), WashlightRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.ATOMICTILT.get(), AtomictiltRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.MINIWASH.get(), MiniwashRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.x8PAR_RED.get(), x8par_redRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.x8PAR_GREEN.get(), x8par_greenRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.x8PAR_BLUE.get(), x8par_blueRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.x8PAR_MAGENTA.get(), x8par_magentaRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.x8PAR_LIGHTBLUE.get(), x8par_lightblueRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.x8PAR_ORANGE.get(), x8par_orangeRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.x8PAR_PURPLE.get(), x8par_purpleRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.x8PAR_YELLOW.get(), x8par_yellowRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.x8PAR_WARM.get(), x8par_warmRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.x8PAR_WHITE.get(), x8par_whiteRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.PAR56_RED.get(), par56_redRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.PAR56_GREEN.get(), par56_greenRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.PAR56_BLUE.get(), par56_blueRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.PAR56_ORANGE.get(), par56_orangeRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.PAR56_MAGENTA.get(), par56_magentaRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.PAR56_LIGHTBLUE.get(), par56_lightblueRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.PAR56_PURPLE.get(), par56_purpleRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.PAR56_YELLOW.get(), par56_yellowRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.PAR56_WHITE.get(), par56_whiteRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.PAR56_WARM.get(), par56_warmRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.VL6000.get(), VL6000Renderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.FOLLOWSPOT.get(), FollowspotRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.A2X2PAR64_RED.get(), a2x2par64_redRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.A2X2PAR64_GREEN.get(), a2x2par64_greenRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.A2X2PAR64_BLUE.get(), a2x2par64_blueRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.A2X2PAR64_MAGENTA.get(), a2x2par64_magentaRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.A2X2PAR64_LIGHTBLUE.get(), a2x2par64_lightblueRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.A2X2PAR64_PURPLE.get(), a2x2par64_purpleRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.A2X2PAR64_ORANGE.get(), a2x2par64_orangeRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.A2X2PAR64_YELLOW.get(), a2x2par64_yellowRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.A2X2PAR64_WARM.get(), a2x2par64_warmRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.A2X2PAR64_WHITE.get(), a2x2par64_whiteRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.WASHLED.get(), washledRenderer::new);


        BlockEntityRendererRegistry.register(BlockEntities.BIGSCROLLER.get(), bigscrollerRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.HORIZONTALSCROLLER.get(), horizontalscrollerRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.VERTICALSCROLLER.get(), verticalscrollerRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.MOVING_BAR.get(), MovingbarRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.MOVING_MINI_BAR.get(), MovingMiniBarRenderer::new);

        BlockEntityRendererRegistry.register(BlockEntities.WATER_JET.get(), WaterJetRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.MOVING_JET.get(), MovingJetRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.WATER_JET_THIN.get(), WaterJetThinRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.SPINNER.get(), SpinnerRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.ORGANPIPES.get(), OrganPipesRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.ORGANPIPES_INV.get(), OrganPipesInvRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.WATER_JET_SPREAD.get(), WaterJetSpreadRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.WATER_JET_BIG.get(), WaterJetBigRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.WATER_JET_CENTRAL.get(), WaterJetCentralRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.WATER_JET_CONE.get(), WaterJetConeRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.WATER_JET_BLOOM.get(), WaterJetBloomRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.WATER_JET_FOG.get(), WaterJetFogRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.FAN_WATER_JET.get(), FanWaterJetRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.CAKE_WATER_JET.get(), CakeWaterJetRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.VASE_WATER_JET.get(), VaseWaterJetRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.WALTZES_WATER_JET.get(), WaltzesWaterJetRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.WALTZ_CURTAIN.get(), WaltzCurtainRenderer::new);


        BlockEntityRendererRegistry.register(BlockEntities.WHITE_STROBE.get(), WhiteStrobeRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.LASER_MIRROR.get(), LaserMirrorRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.PARSCROLLER.get(), ParScrollerRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.BLINDER2X2.get(), Blinder2x2Renderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.BLINDER2X2WARM.get(), Blinder2x2warmRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.MINI_BAR.get(), MiniBarRenderer::new);

        BlockEntityRendererRegistry.register(BlockEntities.MOVING_VL2C_BEAMS.get(), MovingVL2CBeamsRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.MOVING_SCAN_BEAMS.get(), MovingScanBeamsRenderer::new);

        BlockEntityRendererRegistry.register(BlockEntities.A1X1PAR64.get(), a1x1par64Renderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.A2X8PAR64.get(), a2x8par64Renderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.A6X3PAR64_VERTICAL.get(), a6x3par64_verticalRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.SPOT_XTREME_GOBO.get(), SpotXtremeGoboRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.VL6C_GOBO.get(), VL6CGoboRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.IRIS_700_GOBO.get(), Iris700GoboRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.PRO_SPOT_GOBO.get(), ProSpotGoboRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.MINI_SCAN_GOBO.get(), MiniScanGobosRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.MINI_SPOT_GOBO.get(), MiniSpotGobosRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.DWT_PANEL.get(), DWTPanelRenderer::new);


        BlockEntityRendererRegistry.register(BlockEntities.CONFETTI_CANNON.get(), ConfettiCannonRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.FLAME_PROJECTOR.get(), ctx -> new StaticFixtureRenderer<>(ctx));
        BlockEntityRendererRegistry.register(BlockEntities.GERB_GOLD.get(), ctx -> new StaticFixtureRenderer<>(ctx));
        BlockEntityRendererRegistry.register(BlockEntities.PYRO_FAN.get(), ctx -> new StaticFixtureRenderer<>(ctx));

        EntityRendererRegistry.register(ModEntities.FIREWORK_ROCKET, FireworkRocketRenderer::new);

        com.github.dumann089.theatricalextralights.client.followspot.FollowspotCameraClient.init();

        ClientTickEvent.CLIENT_POST.register(client -> {
            ConfettiBurstClient.tick();
            if (client.level != null) {
                FireworkSmokeEffects.beginClientTick(client.level.getGameTime());
            }
            FireworkLightCompat.flushPending();
        });
    }

    public static void handleOpenScreen(OpenExtraLightsScreenPacket packet) {
        ExtraLightsClientScreens.open(
                packet.getScreen(),
                packet.getPos()
        );
    }
}
