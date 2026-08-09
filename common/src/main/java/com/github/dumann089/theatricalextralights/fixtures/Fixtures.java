package com.github.dumann089.theatricalextralights.fixtures;

import com.github.dumann089.theatricalextralights.TheatricalExtraLights;
import com.github.dumann089.theatricalextralights.TheatricalExtraLightsRegistry;
import com.github.dumann089.theatricalextralights.firework.FireworkPreset;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.imabad.theatrical.Theatrical;
import dev.imabad.theatrical.api.Fixture;
import net.minecraft.resources.ResourceLocation;

public class Fixtures {
    public static final DeferredRegister<Fixture> FIXTURES = TheatricalExtraLightsRegistry.get(dev.imabad.theatrical.fixtures.Fixtures.FIXTURE_REGISTRY);

    public static final RegistrySupplier<Fixture> MOVING_VL6 =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "moving_vl6"), MovingVL6Fixture::new);

    public static final RegistrySupplier<Fixture> MOVING_BEAM =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "moving_beam"), MovingBeamFixture::new);

    public static final RegistrySupplier<Fixture> SEARCHLIGHT =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "searchlight"), SearchlightFixture::new);

    public static final RegistrySupplier<Fixture> BEAM_7R =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "beam_7r"), Beam7RFixture::new);

    public static final RegistrySupplier<Fixture> LASER =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "laser"), LaserFixture::new);

    public static final RegistrySupplier<Fixture> RGB_BAR =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "rgb_bar"), RGBbarFixture::new);
      
    public static final RegistrySupplier<Fixture> VERTICAL_BAR =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "vertical_bar"), VerticalbarFixture::new);

    public static final RegistrySupplier<Fixture> PAR1000_BLUE =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "par1000_blue"), Par1000BlueFixture::new);

    public static final RegistrySupplier<Fixture> PAR1000_MAGENTA =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "par1000_magenta"), Par1000MagentaFixture::new);

    public static final RegistrySupplier<Fixture> PAR1000_AMBER =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "par1000_amber"), Par1000AmberFixture::new);
 
    public static final RegistrySupplier<Fixture> PAR1000_PURPLE =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "par1000_purple"), Par1000PurpleFixture::new);

    public static final RegistrySupplier<Fixture> PAR1000_LIGHTBLUE =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "par1000_lightblue"), Par1000LightblueFixture::new);

    public static final RegistrySupplier<Fixture> PAR1000 =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "par1000"), Par1000Fixture::new);

    public static final RegistrySupplier<Fixture> PAR1000_GREEN =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "par1000_green"), Par1000GreenFixture::new);

    public static final RegistrySupplier<Fixture> PAR1000_RED =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "par1000_red"), Par1000RedFixture::new);

    public static final RegistrySupplier<Fixture> PAR1000_ORANGE =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "par1000_orange"), Par1000OrangeFixture::new);

    public static final RegistrySupplier<Fixture> PAR1000_WHITE =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "par1000_white"), Par1000WhiteFixture::new);

    public static final RegistrySupplier<Fixture> SOURCE_FOUR =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "source_four"), Source4Fixture::new);

    public static final RegistrySupplier<Fixture> SOURCE_FOUR_WARM =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "source_four_warm"), Source4warmFixture::new);

    public static final RegistrySupplier<Fixture> MAC_VIP =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "mac_vip"), MacVipFixture::new);

    public static final RegistrySupplier<Fixture> MOVING500 =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "moving500"), Moving500Fixture::new);

    public static final RegistrySupplier<Fixture> ROBITSPOT =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "robitspot"), RobitspotFixture::new); 
            
    public static final RegistrySupplier<Fixture> VERVESPOT =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "vervespot"), VervespotFixture::new);        

    public static final RegistrySupplier<Fixture> SHARPLUS =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "sharplus"), SharplusFixture::new);

    public static final RegistrySupplier<Fixture> BIG_PANEL =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "big_panel"), BigPanelFixture::new);

    public static final RegistrySupplier<Fixture> BIG_PANEL2 =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "big_panel2"), BigPanel2Fixture::new);

    public static final RegistrySupplier<Fixture> LED_FOUNTAIN =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "led_fountain"), LEDfountainFixture::new);

    public static final RegistrySupplier<Fixture> BLINDER =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "blinder"), BlinderFixture::new);

    public static final RegistrySupplier<Fixture> BLINDER_WARM =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "blinder_warm"), BlinderwarmFixture::new);

    public static final RegistrySupplier<Fixture> STROBE =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "strobe"), StrobeFixture::new);

    public static final RegistrySupplier<Fixture> ATOMIC_STROBE =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "atomic_strobe"), AtomicStrobeFixture::new);
            
    public static final RegistrySupplier<Fixture> TRUSS_3LIGHTS =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "truss_3lights"), truss3lightsFixture::new);

    public static final RegistrySupplier<Fixture> MOVING_VL2C =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "moving_vl2c"), MovingVL2CFixture::new);

    public static final RegistrySupplier<Fixture> MOVING_VL2C_BEAMS =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "moving_vl2c_beams"), MovingVL2CBeamsFixture::new);
    public static final RegistrySupplier<Fixture> MOVING_SCAN_BEAMS =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "moving_scan_beams"), MovingScanBeamsFixture::new);




    public static final RegistrySupplier<Fixture> WASHLIGHT =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "washlight"), WashlightFixture::new);

 public static final RegistrySupplier<Fixture> MINIWASH =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "miniwash"), MiniwashFixture::new);

 public static final RegistrySupplier<Fixture> INVISIBLELIGHT =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "invisiblelight"), InvisiblelightFixture::new);

    public static final RegistrySupplier<Fixture> ATOMICTILT =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "atomictilt"), AtomictiltFixture::new);


    public static final RegistrySupplier<Fixture> MOVING_SCAN =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "moving_scan"), MovingScanFixture::new);

    public static final RegistrySupplier<Fixture> LED_PANEL_2 =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "led_panel_2"), LEDPanel2Fixture::new);
    public static final RegistrySupplier<Fixture> PAR_LED =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "par_led"), ParLedFixture::new);
    public static final RegistrySupplier<Fixture> MOVING_JET =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "moving_jet"), MovingJetFixture::new);
    public static final RegistrySupplier<Fixture> WATER_JET_THIN =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "water_jet_thin"), WaterJetThinFixture::new);
    public static final RegistrySupplier<Fixture> WATER_JET_SPREAD =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "water_jet_spread"), WaterJetSpreadFixture::new);
    public static final RegistrySupplier<Fixture> WATER_JET_BLOOM =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "water_jet_bloom"), WaterJetBloomFixture::new);
    public static final RegistrySupplier<Fixture> WATER_JET_FOG =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "water_jet_fog"), WaterJetFogFixture::new);
    public static final RegistrySupplier<Fixture> FAN_WATER_JET =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "fan_water_jet"), FanWaterJetFixture::new);
    public static final RegistrySupplier<Fixture> CAKE_WATER_JET =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "cake_water_jet"), CakeWaterJetFixture::new);
    public static final RegistrySupplier<Fixture> VASE_WATER_JET =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "vase_water_jet"), VaseWaterJetFixture::new);
    public static final RegistrySupplier<Fixture> WALTZES_WATER_JET =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "waltzes_water_jet"), WaltzesWaterJetFixture::new);
    public static final RegistrySupplier<Fixture> WALTZ_CURTAIN =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "waltz_curtain"), WaltzCurtainFixture::new);


    public static final RegistrySupplier<Fixture> x8PAR_RED =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "x8par_red"), x8par_redFixture::new);
    public static final RegistrySupplier<Fixture> x8PAR_GREEN =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "x8par_green"), x8par_greenFixture::new);
    public static final RegistrySupplier<Fixture> x8PAR_BLUE =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "x8par_blue"), x8par_blueFixture::new);
    public static final RegistrySupplier<Fixture> x8PAR_MAGENTA =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "x8par_magenta"), x8par_magentaFixture::new);
    public static final RegistrySupplier<Fixture> x8PAR_LIGHTBLUE =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "x8par_lightblue"), x8par_lightblueFixture::new);
    public static final RegistrySupplier<Fixture> x8PAR_YELLOW =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "x8par_yellow"), x8par_yellowFixture::new);
    public static final RegistrySupplier<Fixture> x8PAR_WHITE =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "x8par_white"), x8par_whiteFixture::new);
    public static final RegistrySupplier<Fixture> x8PAR_WARM =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "x8par_warm"), x8par_warmFixture::new);
    public static final RegistrySupplier<Fixture> x8PAR_PURPLE =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "x8par_purple"), x8par_purpleFixture::new);
    public static final RegistrySupplier<Fixture> x8PAR_ORANGE =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "x8par_orange"), x8par_orangeFixture::new);

    public static final RegistrySupplier<Fixture> PAR56_RED =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "par56_red"), par56_redFixture::new);
    public static final RegistrySupplier<Fixture> PAR56_GREEN =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "par56_green"), par56_greenFixture::new);
    public static final RegistrySupplier<Fixture> PAR56_BLUE =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "par56_blue"), par56_blueFixture::new);
    public static final RegistrySupplier<Fixture> PAR56_ORANGE =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "par56_orange"), par56_orangeFixture::new);
    public static final RegistrySupplier<Fixture> PAR56_MAGENTA =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "par56_magenta"), par56_magentaFixture::new);
    public static final RegistrySupplier<Fixture> PAR56_LIGHTBLUE =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "par56_lightblue"), par56_lightblueFixture::new);
    public static final RegistrySupplier<Fixture> PAR56_PURPLE =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "par56_purple"), par56_purpleFixture::new);
    public static final RegistrySupplier<Fixture> PAR56_YELLOW =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "par56_yellow"), par56_yellowFixture::new);
    public static final RegistrySupplier<Fixture> PAR56_WHITE =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "par56_white"), par56_whiteFixture::new);
    public static final RegistrySupplier<Fixture> PAR56_WARM =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "par56_warm"), par56_warmFixture::new);
    public static final RegistrySupplier<Fixture> DWT_PANEL =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "dwt_panel"), DWTPanelFixture::new);


    public static final RegistrySupplier<Fixture> A2X2PAR64_RED =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "a2x2par64_red"), a2x2par64_redFixture::new);
    public static final RegistrySupplier<Fixture> A2X2PAR64_GREEN =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "a2x2par64_green"), a2x2par64_greenFixture::new);
    public static final RegistrySupplier<Fixture> A2X2PAR64_BLUE =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "a2x2par64_blue"), a2x2par64_blueFixture::new);
    public static final RegistrySupplier<Fixture> A2X2PAR64_MAGENTA =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "a2x2par64_magenta"), a2x2par64_magentaFixture::new);
    public static final RegistrySupplier<Fixture> A2X2PAR64_PURPLE =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "a2x2par64_purple"), a2x2par64_purpleFixture::new);
    public static final RegistrySupplier<Fixture> A2X2PAR64_LIGHTBLUE =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "a2x2par64_lightblue"), a2x2par64_lightblueFixture::new);
    public static final RegistrySupplier<Fixture> A2X2PAR64_ORANGE =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "a2x2par64_orange"), a2x2par64_orangeFixture::new);
    public static final RegistrySupplier<Fixture> A2X2PAR64_YELLOW =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "a2x2par64_yellow"), a2x2par64_yellowFixture::new);
    public static final RegistrySupplier<Fixture> A2X2PAR64_WARM =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "a2x2par64_warm"), a2x2par64_warmFixture::new);
    public static final RegistrySupplier<Fixture> A2X2PAR64_WHITE =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "a2x2par64_white"), a2x2par64_whiteFixture::new);


    public static final RegistrySupplier<Fixture> VL6000 =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "vl6000"), VL6000Fixture::new);
    public static final RegistrySupplier<Fixture> FOLLOWSPOT =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "followspot"), FollowspotFixture::new);
    public static final RegistrySupplier<Fixture> BIGSCROLLER =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "bigscroller"), bigscrollerFixture::new);
    public static final RegistrySupplier<Fixture> HORIZONTALSCROLLER =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "horizontalscroller"), horizontalscrollerFixture::new);
    public static final RegistrySupplier<Fixture> VERTICALSCROLLER =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "verticalscroller"), verticalscrollerFixture::new);
    public static final RegistrySupplier<Fixture> WASHLED =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "washled"), washledFixture::new);
    public static final RegistrySupplier<Fixture> MOVING_BAR =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "moving_bar"), MovingbarFixture::new);
    public static final RegistrySupplier<Fixture> MOVING_MINI_BAR =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "moving_mini_bar"), MovingMiniBarFixture::new);

    public static final RegistrySupplier<Fixture> WATER_JET =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "water_jet"), WaterJetFixture::new);
    public static final RegistrySupplier<Fixture> SPINNER =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "spinner"), SpinnerFixture::new);
    public static final RegistrySupplier<Fixture> ORGANPIPES =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "organpipes"), OrganPipesFixture::new);
    public static final RegistrySupplier<Fixture> ORGANPIPES_INV =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "organpipes_inv"), OrganPipesInvFixture::new);
    public static final RegistrySupplier<Fixture> WATER_JET_BIG =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "water_jet_inv"), WaterJetBigFixture::new);
    public static final RegistrySupplier<Fixture> WATER_JET_CENTRAL =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "water_jet_central"), WaterJetCentralFixture::new);
    public static final RegistrySupplier<Fixture> WATER_JET_CONE =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "water_jet_cone"), WaterJetConeFixture::new);

    public static final RegistrySupplier<Fixture> WHITE_STROBE =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "white_strobe"), WhiteStrobeFixture::new);
    public static final RegistrySupplier<Fixture> FIREWORK_RED_COMET =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "firework_red_comet"), () -> new FireworkLauncherFixture(FireworkPreset.RED_COMET));
    public static final RegistrySupplier<Fixture> FIREWORK_BLUE_COMET =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "firework_blue_comet"), () -> new FireworkLauncherFixture(FireworkPreset.BLUE_COMET));
    public static final RegistrySupplier<Fixture> FIREWORK_GREEN_COMET =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "firework_green_comet"), () -> new FireworkLauncherFixture(FireworkPreset.GREEN_COMET));
    public static final RegistrySupplier<Fixture> FIREWORK_GOLD_COMET =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "firework_gold_comet"), () -> new FireworkLauncherFixture(FireworkPreset.GOLD_COMET));
    public static final RegistrySupplier<Fixture> FIREWORK_GOLD_BELL_COMET =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "firework_gold_bell_comet"), () -> new FireworkLauncherFixture(FireworkPreset.GOLD_BELL_COMET));
    public static final RegistrySupplier<Fixture> FIREWORK_RED_PEONY =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "firework_red_peony"), () -> new FireworkLauncherFixture(FireworkPreset.RED_PEONY));
    public static final RegistrySupplier<Fixture> FIREWORK_BLUE_PEONY =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "firework_blue_peony"), () -> new FireworkLauncherFixture(FireworkPreset.BLUE_PEONY));
    public static final RegistrySupplier<Fixture> FIREWORK_GREEN_PEONY =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "firework_green_peony"), () -> new FireworkLauncherFixture(FireworkPreset.GREEN_PEONY));
    public static final RegistrySupplier<Fixture> FIREWORK_GOLD_PEONY =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "firework_gold_peony"), () -> new FireworkLauncherFixture(FireworkPreset.GOLD_PEONY));
    public static final RegistrySupplier<Fixture> FIREWORK_WHITE_PEONY =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "firework_white_peony"), () -> new FireworkLauncherFixture(FireworkPreset.WHITE_PEONY));
    public static final RegistrySupplier<Fixture> FIREWORK_AMBER_PEONY =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "firework_amber_peony"), () -> new FireworkLauncherFixture(FireworkPreset.AMBER_PEONY));
    public static final RegistrySupplier<Fixture> FIREWORK_VIOLET_PEONY =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "firework_violet_peony"), () -> new FireworkLauncherFixture(FireworkPreset.VIOLET_PEONY));
    public static final RegistrySupplier<Fixture> FIREWORK_WHITE_STROBE_BURST =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "firework_white_strobe_burst"), () -> new FireworkLauncherFixture(FireworkPreset.WHITE_STROBE_BURST));
    public static final RegistrySupplier<Fixture> FIREWORK_WHITE_AERIAL_STROBE =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "firework_white_aerial_strobe"), () -> new FireworkLauncherFixture(FireworkPreset.WHITE_AERIAL_STROBE));
    public static final RegistrySupplier<Fixture> FIREWORK_RED_AERIAL_STROBE =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "firework_red_aerial_strobe"), () -> new FireworkLauncherFixture(FireworkPreset.RED_AERIAL_STROBE));
    public static final RegistrySupplier<Fixture> FIREWORK_BLUE_AERIAL_STROBE =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "firework_blue_aerial_strobe"), () -> new FireworkLauncherFixture(FireworkPreset.BLUE_AERIAL_STROBE));
    public static final RegistrySupplier<Fixture> FIREWORK_GREEN_AERIAL_STROBE =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "firework_green_aerial_strobe"), () -> new FireworkLauncherFixture(FireworkPreset.GREEN_AERIAL_STROBE));
    public static final RegistrySupplier<Fixture> FIREWORK_GOLD_AERIAL_STROBE =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "firework_gold_aerial_strobe"), () -> new FireworkLauncherFixture(FireworkPreset.GOLD_AERIAL_STROBE));
    public static final RegistrySupplier<Fixture> FIREWORK_AMBER_AERIAL_STROBE =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "firework_amber_aerial_strobe"), () -> new FireworkLauncherFixture(FireworkPreset.AMBER_AERIAL_STROBE));
    public static final RegistrySupplier<Fixture> FIREWORK_VIOLET_AERIAL_STROBE =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "firework_violet_aerial_strobe"), () -> new FireworkLauncherFixture(FireworkPreset.VIOLET_AERIAL_STROBE));
    public static final RegistrySupplier<Fixture> FIREWORK_SILVER_AERIAL_STROBE =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "firework_silver_aerial_strobe"), () -> new FireworkLauncherFixture(FireworkPreset.SILVER_AERIAL_STROBE));
    public static final RegistrySupplier<Fixture> FIREWORK_GOLD_WILLOW =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "firework_gold_willow"), () -> new FireworkLauncherFixture(FireworkPreset.GOLD_WILLOW));
    public static final RegistrySupplier<Fixture> FIREWORK_RED_WILLOW =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "firework_red_willow"), () -> new FireworkLauncherFixture(FireworkPreset.RED_WILLOW));
    public static final RegistrySupplier<Fixture> FIREWORK_BLUE_WILLOW =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "firework_blue_willow"), () -> new FireworkLauncherFixture(FireworkPreset.BLUE_WILLOW));
    public static final RegistrySupplier<Fixture> FIREWORK_GREEN_WILLOW =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "firework_green_willow"), () -> new FireworkLauncherFixture(FireworkPreset.GREEN_WILLOW));
    public static final RegistrySupplier<Fixture> FIREWORK_WHITE_WILLOW =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "firework_white_willow"), () -> new FireworkLauncherFixture(FireworkPreset.WHITE_WILLOW));
    public static final RegistrySupplier<Fixture> FIREWORK_AMBER_WILLOW =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "firework_amber_willow"), () -> new FireworkLauncherFixture(FireworkPreset.AMBER_WILLOW));
    public static final RegistrySupplier<Fixture> FIREWORK_VIOLET_WILLOW =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "firework_violet_willow"), () -> new FireworkLauncherFixture(FireworkPreset.VIOLET_WILLOW));
    public static final RegistrySupplier<Fixture> FIREWORK_MULTICOLOR_BURST =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "firework_multicolor_burst"), () -> new FireworkLauncherFixture(FireworkPreset.MULTICOLOR_BURST));
    public static final RegistrySupplier<Fixture> FIREWORK_PALM_GOLD =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "firework_palm_gold"), () -> new FireworkLauncherFixture(FireworkPreset.PALM_GOLD));
    public static final RegistrySupplier<Fixture> FIREWORK_CHRYSANTHEMUM_BLUE =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "firework_chrysanthemum_blue"), () -> new FireworkLauncherFixture(FireworkPreset.CHRYSANTHEMUM_BLUE));
    public static final RegistrySupplier<Fixture> FIREWORK_CHRYSANTHEMUM_RED =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "firework_chrysanthemum_red"), () -> new FireworkLauncherFixture(FireworkPreset.CHRYSANTHEMUM_RED));
    public static final RegistrySupplier<Fixture> FIREWORK_CHRYSANTHEMUM_GREEN =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "firework_chrysanthemum_green"), () -> new FireworkLauncherFixture(FireworkPreset.CHRYSANTHEMUM_GREEN));
    public static final RegistrySupplier<Fixture> FIREWORK_CHRYSANTHEMUM_GOLD =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "firework_chrysanthemum_gold"), () -> new FireworkLauncherFixture(FireworkPreset.CHRYSANTHEMUM_GOLD));
    public static final RegistrySupplier<Fixture> FIREWORK_CHRYSANTHEMUM_WHITE =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "firework_chrysanthemum_white"), () -> new FireworkLauncherFixture(FireworkPreset.CHRYSANTHEMUM_WHITE));
    public static final RegistrySupplier<Fixture> FIREWORK_CHRYSANTHEMUM_AMBER =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "firework_chrysanthemum_amber"), () -> new FireworkLauncherFixture(FireworkPreset.CHRYSANTHEMUM_AMBER));
    public static final RegistrySupplier<Fixture> FIREWORK_CHRYSANTHEMUM_VIOLET =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "firework_chrysanthemum_violet"), () -> new FireworkLauncherFixture(FireworkPreset.CHRYSANTHEMUM_VIOLET));
    public static final RegistrySupplier<Fixture> FIREWORK_HORSETAIL_SILVER =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "firework_horsetail_silver"), () -> new FireworkLauncherFixture(FireworkPreset.HORSETAIL_SILVER));
    public static final RegistrySupplier<Fixture> FIREWORK_RING_RED =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "firework_ring_red"), () -> new FireworkLauncherFixture(FireworkPreset.RING_RED));
    public static final RegistrySupplier<Fixture> FIREWORK_SPINNER_GOLD =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "firework_spinner_gold"), () -> new FireworkLauncherFixture(FireworkPreset.SPINNER_GOLD));
    public static final RegistrySupplier<Fixture> FIREWORK_CROSSETTE_RED =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "firework_crossette_red"), () -> new FireworkLauncherFixture(FireworkPreset.CROSSETTE_RED));
    public static final RegistrySupplier<Fixture> FIREWORK_CROSSETTE_BLUE =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "firework_crossette_blue"), () -> new FireworkLauncherFixture(FireworkPreset.CROSSETTE_BLUE));
    public static final RegistrySupplier<Fixture> FIREWORK_CROSSETTE_GREEN =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "firework_crossette_green"), () -> new FireworkLauncherFixture(FireworkPreset.CROSSETTE_GREEN));
    public static final RegistrySupplier<Fixture> FIREWORK_CROSSETTE_GOLD =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "firework_crossette_gold"), () -> new FireworkLauncherFixture(FireworkPreset.CROSSETTE_GOLD));
    public static final RegistrySupplier<Fixture> FIREWORK_CROSSETTE_WHITE =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "firework_crossette_white"), () -> new FireworkLauncherFixture(FireworkPreset.CROSSETTE_WHITE));
    public static final RegistrySupplier<Fixture> FIREWORK_CROSSETTE_AMBER =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "firework_crossette_amber"), () -> new FireworkLauncherFixture(FireworkPreset.CROSSETTE_AMBER));
    public static final RegistrySupplier<Fixture> FIREWORK_CROSSETTE_VIOLET =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "firework_crossette_violet"), () -> new FireworkLauncherFixture(FireworkPreset.CROSSETTE_VIOLET));
    public static final RegistrySupplier<Fixture> FIREWORK_MINE_BLUE =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "firework_mine_blue"), () -> new FireworkLauncherFixture(FireworkPreset.MINE_BLUE));
    public static final RegistrySupplier<Fixture> FIREWORK_MINE_RED =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "firework_mine_red"), () -> new FireworkLauncherFixture(FireworkPreset.MINE_RED));
    public static final RegistrySupplier<Fixture> FIREWORK_MINE_GREEN =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "firework_mine_green"), () -> new FireworkLauncherFixture(FireworkPreset.MINE_GREEN));
    public static final RegistrySupplier<Fixture> FIREWORK_MINE_GOLD =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "firework_mine_gold"), () -> new FireworkLauncherFixture(FireworkPreset.MINE_GOLD));
    public static final RegistrySupplier<Fixture> FIREWORK_MINE_WHITE =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "firework_mine_white"), () -> new FireworkLauncherFixture(FireworkPreset.MINE_WHITE));
    public static final RegistrySupplier<Fixture> FIREWORK_MINE_AMBER =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "firework_mine_amber"), () -> new FireworkLauncherFixture(FireworkPreset.MINE_AMBER));
    public static final RegistrySupplier<Fixture> FIREWORK_MINE_VIOLET =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "firework_mine_violet"), () -> new FireworkLauncherFixture(FireworkPreset.MINE_VIOLET));
    public static final RegistrySupplier<Fixture> FIREWORK_SPIDER_WHITE =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "firework_spider_white"), () -> new FireworkLauncherFixture(FireworkPreset.SPIDER_WHITE));
    public static final RegistrySupplier<Fixture> FIREWORK_DIADEM_BLUE =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "firework_diadem_blue"), () -> new FireworkLauncherFixture(FireworkPreset.DIADEM_BLUE));
    public static final RegistrySupplier<Fixture> FIREWORK_SALUTE_WHITE =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "firework_salute_white"), () -> new FireworkLauncherFixture(FireworkPreset.SALUTE_WHITE));
    public static final RegistrySupplier<Fixture> FIREWORK_HEART_PINK =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "firework_heart_pink"), () -> new FireworkLauncherFixture(FireworkPreset.HEART_PINK));
    public static final RegistrySupplier<Fixture> FIREWORK_DOUBLE_BURST_PURPLE =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "firework_double_burst_purple"), () -> new FireworkLauncherFixture(FireworkPreset.DOUBLE_BURST_PURPLE));
    public static final RegistrySupplier<Fixture> FIREWORK_WHISTLER_SILVER =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "firework_whistler_silver"), () -> new FireworkLauncherFixture(FireworkPreset.WHISTLER_SILVER));
    public static final RegistrySupplier<Fixture> FIREWORK_RGB_LAUNCHER =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "firework_rgb_launcher"), RgbFireworkLauncherFixture::new);
    public static final RegistrySupplier<Fixture> PYRO_FAN =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "pyro_fan"), PyroFanFixture::new);
    public static final RegistrySupplier<Fixture> FIREWORK_GOLD_LONG_COMET =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "firework_gold_long_comet"), () -> new FireworkLauncherFixture(FireworkPreset.GOLD_LONG_COMET));
    public static final RegistrySupplier<Fixture> FIREWORK_RED_LONG_COMET =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "firework_red_long_comet"), () -> new FireworkLauncherFixture(FireworkPreset.RED_LONG_COMET));
    public static final RegistrySupplier<Fixture> FIREWORK_BLUE_LONG_COMET =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "firework_blue_long_comet"), () -> new FireworkLauncherFixture(FireworkPreset.BLUE_LONG_COMET));
    public static final RegistrySupplier<Fixture> FIREWORK_GREEN_LONG_COMET =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "firework_green_long_comet"), () -> new FireworkLauncherFixture(FireworkPreset.GREEN_LONG_COMET));
    public static final RegistrySupplier<Fixture> FIREWORK_SILVER_LONG_COMET =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "firework_silver_long_comet"), () -> new FireworkLauncherFixture(FireworkPreset.SILVER_LONG_COMET));
    public static final RegistrySupplier<Fixture> FIREWORK_DAYTIME_POWDER_LIME =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "firework_daytime_powder_lime"), () -> new FireworkLauncherFixture(FireworkPreset.LIME_DAYTIME_POWDER));
    public static final RegistrySupplier<Fixture> FIREWORK_DAYTIME_POWDER_MAGENTA =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "firework_daytime_powder_magenta"), () -> new FireworkLauncherFixture(FireworkPreset.MAGENTA_DAYTIME_POWDER));
    public static final RegistrySupplier<Fixture> FIREWORK_DAYTIME_POWDER_YELLOW =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "firework_daytime_powder_yellow"), () -> new FireworkLauncherFixture(FireworkPreset.YELLOW_DAYTIME_POWDER));
    public static final RegistrySupplier<Fixture> FIREWORK_DAYTIME_POWDER_ORANGE =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "firework_daytime_powder_orange"), () -> new FireworkLauncherFixture(FireworkPreset.ORANGE_DAYTIME_POWDER));
    public static final RegistrySupplier<Fixture> FIREWORK_DAYTIME_POWDER_RED =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "firework_daytime_powder_red"), () -> new FireworkLauncherFixture(FireworkPreset.RED_DAYTIME_POWDER));
    public static final RegistrySupplier<Fixture> FIREWORK_DAYTIME_POWDER_BLUE =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "firework_daytime_powder_blue"), () -> new FireworkLauncherFixture(FireworkPreset.BLUE_DAYTIME_POWDER));
    public static final RegistrySupplier<Fixture> FIREWORK_DAYTIME_POWDER_RAINBOW =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "firework_daytime_powder_rainbow"), () -> new FireworkLauncherFixture(FireworkPreset.RAINBOW_DAYTIME_POWDER_FAN));
    public static final RegistrySupplier<Fixture> FIREWORK_SILVER_JET =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "firework_silver_jet"), () -> new FireworkLauncherFixture(FireworkPreset.SILVER_JET));
    public static final RegistrySupplier<Fixture> FIREWORK_MORTAR_HIT =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "firework_mortar_hit"), () -> new FireworkLauncherFixture(FireworkPreset.FIREWORK_MORTAR_HIT));
    public static final RegistrySupplier<Fixture> FIREWORK_FLAME_PROJECTOR =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "firework_flame_projector"), () -> new FireworkLauncherFixture(FireworkPreset.FIREWORK_FLAME_PROJECTOR));
    public static final RegistrySupplier<Fixture> GERB_GOLD =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "gerb_gold"), GerbFixture::new);

    public static final RegistrySupplier<Fixture> FLAME_PROJECTOR =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "flame_projector"), FlameProjectorFixture::new);
    public static final RegistrySupplier<Fixture> FLAME_THROWER =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "flame_thrower"), FlameThrowerFixture::new);
    public static final RegistrySupplier<Fixture> FLOW2JET =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "flow2jet"), Flow2JetFixture::new);
    public static final RegistrySupplier<Fixture> CONFETTI_CANNON =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "confetti_cannon"), ConfettiCannonFixture::new);
    public static final RegistrySupplier<Fixture> LASER_MIRROR =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "laser_mirror"), LaserMirrorFixture::new);
    public static final RegistrySupplier<Fixture> PARSCROLLER =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "parscroller"), ParScrollerFixture::new);
    public static final RegistrySupplier<Fixture> BLINDER2X2 =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "blinder2x2"), Blinder2x2Fixture::new);
    public static final RegistrySupplier<Fixture> BLINDER2X2WARM =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "blinder2x2warm"), Blinder2x2warmFixture::new);
    public static final RegistrySupplier<Fixture> BLINDER1X1 =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "blinder1x1"), Blinder1x1Fixture::new);
    public static final RegistrySupplier<Fixture> MINI_BAR =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "mini_bar"), MiniBarFixture::new);
    public static final RegistrySupplier<Fixture> A1X1PAR64 =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "a1x1par64"), a1x1par64Fixture::new);
    public static final RegistrySupplier<Fixture> A2X8PAR64 =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "a2x8par64"), a2x8par64Fixture::new);
    public static final RegistrySupplier<Fixture> A6X3PAR64_VERTICAL =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "a6x3par64_vertical"), a6x3par64_verticalFixture::new);
    public static final RegistrySupplier<Fixture> SPOT_XTREME_GOBO =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "spot_xtreme_gobo"), SpotXtremeGoboFixture::new);
    public static final RegistrySupplier<Fixture> VL6C_GOBO =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "vl6c_gobo"), VL6CGoboFixture::new);
    public static final RegistrySupplier<Fixture> IRIS_700_GOBO =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "iris_700_gobo"), Iris700GoboFixture::new);
    public static final RegistrySupplier<Fixture> PRO_SPOT_GOBO =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "pro_spot_gobo"), ProSpotGoboFixture::new);
    public static final RegistrySupplier<Fixture> MINI_SCAN_GOBOS =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "mini_scan_gobos"), MiniScanGobosFixture::new);
    public static final RegistrySupplier<Fixture> MINI_SPOT_GOBOS =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "mini_spot_gobos"), MiniSpotGobosFixture::new);
    public static final RegistrySupplier<Fixture> LED_FACADE =
            FIXTURES.register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "led_facade"), LedFacadeFixture::new);


    public static Fixture getFireworkFixture(FireworkPreset preset) {
        return switch (preset) {
            case RED_COMET -> FIREWORK_RED_COMET.get();
            case BLUE_COMET -> FIREWORK_BLUE_COMET.get();
            case GREEN_COMET -> FIREWORK_GREEN_COMET.get();
            case GOLD_COMET -> FIREWORK_GOLD_COMET.get();
            case PYRO_FAN_COMET -> FIREWORK_GOLD_COMET.get();
            case GOLD_BELL_COMET -> FIREWORK_GOLD_BELL_COMET.get();
            case RED_PEONY -> FIREWORK_RED_PEONY.get();
            case BLUE_PEONY -> FIREWORK_BLUE_PEONY.get();
            case GREEN_PEONY -> FIREWORK_GREEN_PEONY.get();
            case GOLD_PEONY -> FIREWORK_GOLD_PEONY.get();
            case WHITE_PEONY -> FIREWORK_WHITE_PEONY.get();
            case AMBER_PEONY -> FIREWORK_AMBER_PEONY.get();
            case VIOLET_PEONY -> FIREWORK_VIOLET_PEONY.get();
            case WHITE_STROBE_BURST -> FIREWORK_WHITE_STROBE_BURST.get();
            case WHITE_AERIAL_STROBE -> FIREWORK_WHITE_AERIAL_STROBE.get();
            case RED_AERIAL_STROBE -> FIREWORK_RED_AERIAL_STROBE.get();
            case BLUE_AERIAL_STROBE -> FIREWORK_BLUE_AERIAL_STROBE.get();
            case GREEN_AERIAL_STROBE -> FIREWORK_GREEN_AERIAL_STROBE.get();
            case GOLD_AERIAL_STROBE -> FIREWORK_GOLD_AERIAL_STROBE.get();
            case AMBER_AERIAL_STROBE -> FIREWORK_AMBER_AERIAL_STROBE.get();
            case VIOLET_AERIAL_STROBE -> FIREWORK_VIOLET_AERIAL_STROBE.get();
            case SILVER_AERIAL_STROBE -> FIREWORK_SILVER_AERIAL_STROBE.get();
            case GOLD_WILLOW -> FIREWORK_GOLD_WILLOW.get();
            case RED_WILLOW -> FIREWORK_RED_WILLOW.get();
            case BLUE_WILLOW -> FIREWORK_BLUE_WILLOW.get();
            case GREEN_WILLOW -> FIREWORK_GREEN_WILLOW.get();
            case WHITE_WILLOW -> FIREWORK_WHITE_WILLOW.get();
            case AMBER_WILLOW -> FIREWORK_AMBER_WILLOW.get();
            case VIOLET_WILLOW -> FIREWORK_VIOLET_WILLOW.get();
            case MULTICOLOR_BURST -> FIREWORK_MULTICOLOR_BURST.get();
            case PALM_GOLD -> FIREWORK_PALM_GOLD.get();
            case CHRYSANTHEMUM_BLUE -> FIREWORK_CHRYSANTHEMUM_BLUE.get();
            case CHRYSANTHEMUM_RED -> FIREWORK_CHRYSANTHEMUM_RED.get();
            case CHRYSANTHEMUM_GREEN -> FIREWORK_CHRYSANTHEMUM_GREEN.get();
            case CHRYSANTHEMUM_GOLD -> FIREWORK_CHRYSANTHEMUM_GOLD.get();
            case CHRYSANTHEMUM_WHITE -> FIREWORK_CHRYSANTHEMUM_WHITE.get();
            case CHRYSANTHEMUM_AMBER -> FIREWORK_CHRYSANTHEMUM_AMBER.get();
            case CHRYSANTHEMUM_VIOLET -> FIREWORK_CHRYSANTHEMUM_VIOLET.get();
            case HORSETAIL_SILVER -> FIREWORK_HORSETAIL_SILVER.get();
            case RING_RED -> FIREWORK_RING_RED.get();
            case SPINNER_GOLD -> FIREWORK_SPINNER_GOLD.get();
            case CROSSETTE_RED -> FIREWORK_CROSSETTE_RED.get();
            case CROSSETTE_BLUE -> FIREWORK_CROSSETTE_BLUE.get();
            case CROSSETTE_GREEN -> FIREWORK_CROSSETTE_GREEN.get();
            case CROSSETTE_GOLD -> FIREWORK_CROSSETTE_GOLD.get();
            case CROSSETTE_WHITE -> FIREWORK_CROSSETTE_WHITE.get();
            case CROSSETTE_AMBER -> FIREWORK_CROSSETTE_AMBER.get();
            case CROSSETTE_VIOLET -> FIREWORK_CROSSETTE_VIOLET.get();
            case MINE_BLUE -> FIREWORK_MINE_BLUE.get();
            case MINE_RED -> FIREWORK_MINE_RED.get();
            case MINE_GREEN -> FIREWORK_MINE_GREEN.get();
            case MINE_GOLD -> FIREWORK_MINE_GOLD.get();
            case MINE_WHITE -> FIREWORK_MINE_WHITE.get();
            case MINE_AMBER -> FIREWORK_MINE_AMBER.get();
            case MINE_VIOLET -> FIREWORK_MINE_VIOLET.get();
            case SPIDER_WHITE -> FIREWORK_SPIDER_WHITE.get();
            case DIADEM_BLUE -> FIREWORK_DIADEM_BLUE.get();
            case SALUTE_WHITE -> FIREWORK_SALUTE_WHITE.get();
            case HEART_PINK -> FIREWORK_HEART_PINK.get();
            case DOUBLE_BURST_PURPLE -> FIREWORK_DOUBLE_BURST_PURPLE.get();
            case WHISTLER_SILVER -> FIREWORK_WHISTLER_SILVER.get();
            case GOLD_LONG_COMET -> FIREWORK_GOLD_LONG_COMET.get();
            case RED_LONG_COMET -> FIREWORK_RED_LONG_COMET.get();
            case BLUE_LONG_COMET -> FIREWORK_BLUE_LONG_COMET.get();
            case GREEN_LONG_COMET -> FIREWORK_GREEN_LONG_COMET.get();
            case SILVER_LONG_COMET -> FIREWORK_SILVER_LONG_COMET.get();
            case LIME_DAYTIME_POWDER -> FIREWORK_DAYTIME_POWDER_LIME.get();
            case MAGENTA_DAYTIME_POWDER -> FIREWORK_DAYTIME_POWDER_MAGENTA.get();
            case YELLOW_DAYTIME_POWDER -> FIREWORK_DAYTIME_POWDER_YELLOW.get();
            case ORANGE_DAYTIME_POWDER -> FIREWORK_DAYTIME_POWDER_ORANGE.get();
            case RED_DAYTIME_POWDER -> FIREWORK_DAYTIME_POWDER_RED.get();
            case BLUE_DAYTIME_POWDER -> FIREWORK_DAYTIME_POWDER_BLUE.get();
            case RAINBOW_DAYTIME_POWDER_FAN -> FIREWORK_DAYTIME_POWDER_RAINBOW.get();
            case SILVER_JET -> FIREWORK_SILVER_JET.get();
            case FIREWORK_MORTAR_HIT -> FIREWORK_MORTAR_HIT.get();
            case FIREWORK_FLAME_PROJECTOR -> FIREWORK_FLAME_PROJECTOR.get();

        };
    }

    public static ResourceLocation getFireworkFixtureId(FireworkPreset preset) {
        return switch (preset) {
            case RED_COMET -> FIREWORK_RED_COMET.getId();
            case BLUE_COMET -> FIREWORK_BLUE_COMET.getId();
            case GREEN_COMET -> FIREWORK_GREEN_COMET.getId();
            case GOLD_COMET -> FIREWORK_GOLD_COMET.getId();
            case PYRO_FAN_COMET -> FIREWORK_GOLD_COMET.getId();
            case GOLD_BELL_COMET -> FIREWORK_GOLD_BELL_COMET.getId();
            case RED_PEONY -> FIREWORK_RED_PEONY.getId();
            case BLUE_PEONY -> FIREWORK_BLUE_PEONY.getId();
            case GREEN_PEONY -> FIREWORK_GREEN_PEONY.getId();
            case GOLD_PEONY -> FIREWORK_GOLD_PEONY.getId();
            case WHITE_PEONY -> FIREWORK_WHITE_PEONY.getId();
            case AMBER_PEONY -> FIREWORK_AMBER_PEONY.getId();
            case VIOLET_PEONY -> FIREWORK_VIOLET_PEONY.getId();
            case WHITE_STROBE_BURST -> FIREWORK_WHITE_STROBE_BURST.getId();
            case WHITE_AERIAL_STROBE -> FIREWORK_WHITE_AERIAL_STROBE.getId();
            case RED_AERIAL_STROBE -> FIREWORK_RED_AERIAL_STROBE.getId();
            case BLUE_AERIAL_STROBE -> FIREWORK_BLUE_AERIAL_STROBE.getId();
            case GREEN_AERIAL_STROBE -> FIREWORK_GREEN_AERIAL_STROBE.getId();
            case GOLD_AERIAL_STROBE -> FIREWORK_GOLD_AERIAL_STROBE.getId();
            case AMBER_AERIAL_STROBE -> FIREWORK_AMBER_AERIAL_STROBE.getId();
            case VIOLET_AERIAL_STROBE -> FIREWORK_VIOLET_AERIAL_STROBE.getId();
            case SILVER_AERIAL_STROBE -> FIREWORK_SILVER_AERIAL_STROBE.getId();
            case GOLD_WILLOW -> FIREWORK_GOLD_WILLOW.getId();
            case RED_WILLOW -> FIREWORK_RED_WILLOW.getId();
            case BLUE_WILLOW -> FIREWORK_BLUE_WILLOW.getId();
            case GREEN_WILLOW -> FIREWORK_GREEN_WILLOW.getId();
            case WHITE_WILLOW -> FIREWORK_WHITE_WILLOW.getId();
            case AMBER_WILLOW -> FIREWORK_AMBER_WILLOW.getId();
            case VIOLET_WILLOW -> FIREWORK_VIOLET_WILLOW.getId();
            case MULTICOLOR_BURST -> FIREWORK_MULTICOLOR_BURST.getId();
            case PALM_GOLD -> FIREWORK_PALM_GOLD.getId();
            case CHRYSANTHEMUM_BLUE -> FIREWORK_CHRYSANTHEMUM_BLUE.getId();
            case CHRYSANTHEMUM_RED -> FIREWORK_CHRYSANTHEMUM_RED.getId();
            case CHRYSANTHEMUM_GREEN -> FIREWORK_CHRYSANTHEMUM_GREEN.getId();
            case CHRYSANTHEMUM_GOLD -> FIREWORK_CHRYSANTHEMUM_GOLD.getId();
            case CHRYSANTHEMUM_WHITE -> FIREWORK_CHRYSANTHEMUM_WHITE.getId();
            case CHRYSANTHEMUM_AMBER -> FIREWORK_CHRYSANTHEMUM_AMBER.getId();
            case CHRYSANTHEMUM_VIOLET -> FIREWORK_CHRYSANTHEMUM_VIOLET.getId();
            case HORSETAIL_SILVER -> FIREWORK_HORSETAIL_SILVER.getId();
            case RING_RED -> FIREWORK_RING_RED.getId();
            case SPINNER_GOLD -> FIREWORK_SPINNER_GOLD.getId();
            case CROSSETTE_RED -> FIREWORK_CROSSETTE_RED.getId();
            case CROSSETTE_BLUE -> FIREWORK_CROSSETTE_BLUE.getId();
            case CROSSETTE_GREEN -> FIREWORK_CROSSETTE_GREEN.getId();
            case CROSSETTE_GOLD -> FIREWORK_CROSSETTE_GOLD.getId();
            case CROSSETTE_WHITE -> FIREWORK_CROSSETTE_WHITE.getId();
            case CROSSETTE_AMBER -> FIREWORK_CROSSETTE_AMBER.getId();
            case CROSSETTE_VIOLET -> FIREWORK_CROSSETTE_VIOLET.getId();
            case MINE_BLUE -> FIREWORK_MINE_BLUE.getId();
            case MINE_RED -> FIREWORK_MINE_RED.getId();
            case MINE_GREEN -> FIREWORK_MINE_GREEN.getId();
            case MINE_GOLD -> FIREWORK_MINE_GOLD.getId();
            case MINE_WHITE -> FIREWORK_MINE_WHITE.getId();
            case MINE_AMBER -> FIREWORK_MINE_AMBER.getId();
            case MINE_VIOLET -> FIREWORK_MINE_VIOLET.getId();
            case SPIDER_WHITE -> FIREWORK_SPIDER_WHITE.getId();
            case DIADEM_BLUE -> FIREWORK_DIADEM_BLUE.getId();
            case SALUTE_WHITE -> FIREWORK_SALUTE_WHITE.getId();
            case HEART_PINK -> FIREWORK_HEART_PINK.getId();
            case DOUBLE_BURST_PURPLE -> FIREWORK_DOUBLE_BURST_PURPLE.getId();
            case WHISTLER_SILVER -> FIREWORK_WHISTLER_SILVER.getId();
            case GOLD_LONG_COMET -> FIREWORK_GOLD_LONG_COMET.getId();
            case RED_LONG_COMET -> FIREWORK_RED_LONG_COMET.getId();
            case BLUE_LONG_COMET -> FIREWORK_BLUE_LONG_COMET.getId();
            case GREEN_LONG_COMET -> FIREWORK_GREEN_LONG_COMET.getId();
            case SILVER_LONG_COMET -> FIREWORK_SILVER_LONG_COMET.getId();
            case LIME_DAYTIME_POWDER -> FIREWORK_DAYTIME_POWDER_LIME.getId();
            case MAGENTA_DAYTIME_POWDER -> FIREWORK_DAYTIME_POWDER_MAGENTA.getId();
            case YELLOW_DAYTIME_POWDER -> FIREWORK_DAYTIME_POWDER_YELLOW.getId();
            case ORANGE_DAYTIME_POWDER -> FIREWORK_DAYTIME_POWDER_ORANGE.getId();
            case RED_DAYTIME_POWDER -> FIREWORK_DAYTIME_POWDER_RED.getId();
            case BLUE_DAYTIME_POWDER -> FIREWORK_DAYTIME_POWDER_BLUE.getId();
            case RAINBOW_DAYTIME_POWDER_FAN -> FIREWORK_DAYTIME_POWDER_RAINBOW.getId();
            case SILVER_JET -> FIREWORK_SILVER_JET.getId();
            case FIREWORK_MORTAR_HIT -> FIREWORK_MORTAR_HIT.getId();
            case FIREWORK_FLAME_PROJECTOR -> FIREWORK_FLAME_PROJECTOR.getId();
        };
    }

    public static void init(){
        FIXTURES.register();
    }
}
