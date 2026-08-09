package com.github.dumann089.theatricalextralights.blocks;

import com.github.dumann089.theatricalextralights.TheatricalExtraLightsRegistry;
import com.github.dumann089.theatricalextralights.blocks.rig.TrussBlock;
import com.github.dumann089.theatricalextralights.blocks.rig.TrussJointBlock;
import com.github.dumann089.theatricalextralights.blocks.rig.TrussCornerBlock;
import com.github.dumann089.theatricalextralights.blocks.rig.TrussCornerTBlock;
import com.github.dumann089.theatricalextralights.firework.FireworkPreset;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.imabad.theatrical.blocks.interfaces.ArtNetInterfaceBlock;
import dev.imabad.theatrical.blocks.light.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;


public class Blocks {
    public static final DeferredRegister<Block> BLOCKS = TheatricalExtraLightsRegistry.get(Registries.BLOCK);
    public static final RegistrySupplier<Block> MOVING_VL2C_BLOCK = BLOCKS.register("moving_vl2c", MovingVL2CBlock::new);
    public static final RegistrySupplier<Block> MOVING_VL6_BLOCK = BLOCKS.register("moving_vl6", MovingVL6Block::new);
    public static final RegistrySupplier<Block> MOVING_BEAM_BLOCK = BLOCKS.register("moving_beam", MovingBeamBlock::new);
    public static final RegistrySupplier<Block> MOVING_SCAN_BLOCK = BLOCKS.register("moving_scan", MovingScanBlock::new);
    public static final RegistrySupplier<Block> LED_PANEL_2 = BLOCKS.register("led_panel_2", LEDPanel2Block::new);
    public static final RegistrySupplier<Block> PAR_LED = BLOCKS.register("par_led", ParLedBlock::new);
    public static final RegistrySupplier<Block> LED_FOUNTAIN = BLOCKS.register("led_fountain", LEDfountainBlock::new);
    public static final RegistrySupplier<Block> RGB_BAR = BLOCKS.register("rgb_bar", RGBbarBlock::new);
    public static final RegistrySupplier<Block> BIG_PANEL = BLOCKS.register("big_panel", BigPanelBlock::new);
    public static final RegistrySupplier<Block> BIG_PANEL2 = BLOCKS.register("big_panel2", BigPanel2Block::new);
    public static final RegistrySupplier<Block> LASER_BLOCK = BLOCKS.register("laser", LaserBlock::new);
    public static final RegistrySupplier<Block> BLINDER = BLOCKS.register("blinder", BlinderBlock::new);
    public static final RegistrySupplier<Block> STROBE = BLOCKS.register("strobe", StrobeBlock::new);
    public static final RegistrySupplier<Block> ATOMIC_STROBE = BLOCKS.register("atomic_strobe", AtomicStrobeBlock::new);
    public static final RegistrySupplier<Block> TRUSS_3LIGHTS = BLOCKS.register("truss_3lights", truss3lightsBlock::new);
    public static final RegistrySupplier<Block> BEAM_7R_BLOCK = BLOCKS.register("beam_7r", Beam7RBlock::new);
    public static final RegistrySupplier<Block> SOURCE_FOUR_BLOCK = BLOCKS.register("source_four", Source4Block::new);
    public static final RegistrySupplier<Block> SOURCE_FOUR_WARM_BLOCK = BLOCKS.register("source_four_warm", Source4warmBlock::new);
    public static final RegistrySupplier<Block> MAC_VIP_BLOCK = BLOCKS.register("mac_vip", MacVipBlock::new);
    public static final RegistrySupplier<Block> SHARPLUS_BLOCK = BLOCKS.register("sharplus", SharplusBlock::new);
    public static final RegistrySupplier<Block> PAR1000_RED_BLOCK = BLOCKS.register("par1000_red", Par1000RedBlock::new);
    public static final RegistrySupplier<Block> PAR1000_BLUE_BLOCK = BLOCKS.register("par1000_blue", Par1000BlueBlock::new);
    public static final RegistrySupplier<Block> PAR1000_GREEN_BLOCK = BLOCKS.register("par1000_green", Par1000GreenBlock::new);
    public static final RegistrySupplier<Block> PAR1000_MAGENTA_BLOCK = BLOCKS.register("par1000_magenta", Par1000MagentaBlock::new);
    public static final RegistrySupplier<Block> PAR1000_AMBER_BLOCK = BLOCKS.register("par1000_amber", Par1000AmberBlock::new);
    public static final RegistrySupplier<Block> PAR1000_BLOCK = BLOCKS.register("par1000", Par1000Block::new);
    public static final RegistrySupplier<Block> PAR1000_PURPLE_BLOCK = BLOCKS.register("par1000_purple", Par1000PurpleBlock::new);
    public static final RegistrySupplier<Block> PAR1000_LIGHTBLUE_BLOCK = BLOCKS.register("par1000_lightblue", Par1000LightblueBlock::new);
    public static final RegistrySupplier<Block> PAR1000_WHITE_BLOCK = BLOCKS.register("par1000_white", Par1000WhiteBlock::new);
    public static final RegistrySupplier<Block> PAR1000_ORANGE_BLOCK = BLOCKS.register("par1000_orange", Par1000OrangeBlock::new);
    public static final RegistrySupplier<Block> MOVING500_BLOCK = BLOCKS.register("moving500", Moving500Block::new);
    public static final RegistrySupplier<Block> ROBITSPOT_BLOCK = BLOCKS.register("robitspot", RobitspotBlock::new);
    public static final RegistrySupplier<Block> VERVESPOT_BLOCK = BLOCKS.register("vervespot", VervespotBlock::new);
    public static final RegistrySupplier<Block> VERTICALBAR_BLOCK = BLOCKS.register("vertical_bar", VerticalbarBlock::new);
    public static final RegistrySupplier<Block> SEARCHLIGHT_BLOCK = BLOCKS.register("searchlight", SearchlightBlock::new);
    public static final RegistrySupplier<Block> BLINDER_WARM_BLOCK = BLOCKS.register("blinder_warm", BlinderwarmBlock::new);
    public static final RegistrySupplier<Block> WASHLIGHT_BLOCK = BLOCKS.register("washlight", WashlightBlock::new);
    public static final RegistrySupplier<Block> ATOMICTILT_BLOCK = BLOCKS.register("atomictilt", AtomictiltBlock::new);
    public static final RegistrySupplier<Block> MINIWASH_BLOCK = BLOCKS.register("miniwash", MiniwashBlock::new);
    public static final RegistrySupplier<Block> INVISIBLE_LIGHT_BLOCK = BLOCKS.register("invisiblelight", InvisiblelightBlock::new);
    public static final RegistrySupplier<Block> X8PAR_RED_BLOCK = BLOCKS.register("x8par_red", x8par_redBlock::new);
    public static final RegistrySupplier<Block> X8PAR_GREEN_BLOCK = BLOCKS.register("x8par_green", x8par_greenBlock::new);
    public static final RegistrySupplier<Block> X8PAR_BLUE_BLOCK = BLOCKS.register("x8par_blue", x8par_blueBlock::new);
    public static final RegistrySupplier<Block> X8PAR_MAGENTA_BLOCK = BLOCKS.register("x8par_magenta", x8par_magentaBlock::new);
    public static final RegistrySupplier<Block> X8PAR_LIGHTBLUE_BLOCK = BLOCKS.register("x8par_lightblue", x8par_lightblueBlock::new);
    public static final RegistrySupplier<Block> X8PAR_YELLOW_BLOCK = BLOCKS.register("x8par_yellow", x8par_yellowBlock::new);
    public static final RegistrySupplier<Block> X8PAR_WHITE_BLOCK = BLOCKS.register("x8par_white", x8par_whiteBlock::new);
    public static final RegistrySupplier<Block> X8PAR_PURPLE_BLOCK = BLOCKS.register("x8par_purple", x8par_purpleBlock::new);
    public static final RegistrySupplier<Block> X8PAR_WARM_BLOCK = BLOCKS.register("x8par_warm", x8par_warmBlock::new);
    public static final RegistrySupplier<Block> X8PAR_ORANGE_BLOCK = BLOCKS.register("x8par_orange", x8par_orangeBlock::new);
    public static final RegistrySupplier<Block> PAR56_RED_BLOCK = BLOCKS.register("par56_red", par56_redBlock::new);
    public static final RegistrySupplier<Block> PAR56_GREEN_BLOCK = BLOCKS.register("par56_green", par56_greenBlock::new);
    public static final RegistrySupplier<Block> PAR56_BLUE_BLOCK = BLOCKS.register("par56_blue", par56_blueBlock::new);
    public static final RegistrySupplier<Block> PAR56_ORANGE_BLOCK = BLOCKS.register("par56_orange", par56_orangeBlock::new);
    public static final RegistrySupplier<Block> PAR56_MAGENTA_BLOCK = BLOCKS.register("par56_magenta", par56_magentaBlock::new);
    public static final RegistrySupplier<Block> PAR56_LIGHTBLUE_BLOCK = BLOCKS.register("par56_lightblue", par56_lightblueBlock::new);
    public static final RegistrySupplier<Block> PAR56_PURPLE_BLOCK = BLOCKS.register("par56_purple", par56_purpleBlock::new);
    public static final RegistrySupplier<Block> PAR56_WHITE_BLOCK = BLOCKS.register("par56_white", par56_whiteBlock::new);
    public static final RegistrySupplier<Block> PAR56_WARM_BLOCK = BLOCKS.register("par56_warm", par56_warmBlock::new);
    public static final RegistrySupplier<Block> PAR56_YELLOW_BLOCK = BLOCKS.register("par56_yellow", par56_yellowBlock::new);
    public static final RegistrySupplier<Block> VL6000_BLOCK = BLOCKS.register("vl6000", VL6000Block::new);
    public static final RegistrySupplier<Block> A2X2PAR64_RED_BLOCK = BLOCKS.register("a2x2par64_red", a2x2par64_redBlock::new);
    public static final RegistrySupplier<Block> A2X2PAR64_GREEN_BLOCK = BLOCKS.register("a2x2par64_green", a2x2par64_greenBlock::new);
    public static final RegistrySupplier<Block> A2X2PAR64_BLUE_BLOCK = BLOCKS.register("a2x2par64_blue", a2x2par64_blueBlock::new);
    public static final RegistrySupplier<Block> A2X2PAR64_MAGENTA_BLOCK = BLOCKS.register("a2x2par64_magenta", a2x2par64_magentaBlock::new);
    public static final RegistrySupplier<Block> A2X2PAR64_LIGHTBLUE_BLOCK = BLOCKS.register("a2x2par64_lightblue", a2x2par64_lightblueBlock::new);
    public static final RegistrySupplier<Block> A2X2PAR64_PURPLE_BLOCK = BLOCKS.register("a2x2par64_purple", a2x2par64_purpleBlock::new);
    public static final RegistrySupplier<Block> A2X2PAR64_ORANGE_BLOCK = BLOCKS.register("a2x2par64_orange", a2x2par64_orangeBlock::new);
    public static final RegistrySupplier<Block> A2X2PAR64_YELLOW_BLOCK = BLOCKS.register("a2x2par64_yellow", a2x2par64_yellowBlock::new);
    public static final RegistrySupplier<Block> A2X2PAR64_WARM_BLOCK = BLOCKS.register("a2x2par64_warm", a2x2par64_warmBlock::new);
    public static final RegistrySupplier<Block> A2X2PAR64_WHITE_BLOCK = BLOCKS.register("a2x2par64_white", a2x2par64_whiteBlock::new);
    public static final RegistrySupplier<Block> DWT_PANEL_BLOCK = BLOCKS.register("dwt_panel", DWTPanelBlock::new);



    public static final RegistrySupplier<Block> FOLLOWSPOT_BLOCK = BLOCKS.register("followspot", FollowspotBlock::new);
    public static final RegistrySupplier<Block> BIGSCROLLER_BLOCK = BLOCKS.register("bigscroller", bigscrollerBlock::new);
    public static final RegistrySupplier<Block> HORIZONTALSCROLLER_BLOCK = BLOCKS.register("horizontalscroller", horizontalscrollerBlock::new);
    public static final RegistrySupplier<Block> VERTICALSCROLLER_BLOCK = BLOCKS.register("verticalscroller", verticalscrollerBlock::new);
    public static final RegistrySupplier<Block> WASHLED_BLOCK = BLOCKS.register("washled", washledBlock::new);
    public static final RegistrySupplier<Block> MOVING_BAR_BLOCK = BLOCKS.register("moving_bar", MovingbarBlock::new);
    public static final RegistrySupplier<Block> MOVING_MINI_BAR_BLOCK = BLOCKS.register("moving_mini_bar", MovingMiniBarBlock::new);

    public static final RegistrySupplier<Block> SPOT_XTREME_GOBO_BLOCK = BLOCKS.register("spot_xtreme_gobo", SpotXtremeGoboBlock::new);
    public static final RegistrySupplier<Block> VL6C_GOBO_BLOCK = BLOCKS.register("vl6c_gobo", VL6CGoboBlock::new);
    public static final RegistrySupplier<Block> IRIS_700_GOBO_BLOCK = BLOCKS.register("iris_700_gobo", Iris700GoboBlock::new);


    public static final RegistrySupplier<Block> WATER_JET_BLOCK = BLOCKS.register("water_jet", WaterJetBlock::new);
    public static final RegistrySupplier<Block> MOVING_JET_BLOCK = BLOCKS.register("moving_jet", MovingJetBlock::new);
    public static final RegistrySupplier<Block> WATER_JET_THIN_BLOCK = BLOCKS.register("water_jet_thin", WaterJetThinBlock::new);
    public static final RegistrySupplier<Block> SPINNER_BLOCK = BLOCKS.register("spinner", SpinnerBlock::new);
    public static final RegistrySupplier<Block> ORGANPIPES_BLOCK = BLOCKS.register("organpipes", OrganPipesBlock::new);
    public static final RegistrySupplier<Block> ORGANPIPES_INV_BLOCK = BLOCKS.register("organpipes_inv", OrganPipesInvBlock::new);
    public static final RegistrySupplier<Block> WATER_JET_SPREAD_BLOCK = BLOCKS.register("water_jet_spread", WaterJetSpreadBlock::new);
    public static final RegistrySupplier<Block> WATER_JET_BIG_BLOCK = BLOCKS.register("water_jet_big", WaterJetBigBlock::new);
    public static final RegistrySupplier<Block> WATER_JET_CENTRAL_BLOCK = BLOCKS.register("water_jet_central", WaterJetCentralBlock::new);
    public static final RegistrySupplier<Block> WATER_JET_CONE_BLOCK = BLOCKS.register("water_jet_cone", WaterJetConeBlock::new);
    public static final RegistrySupplier<Block> WATER_JET_BLOOM_BLOCK = BLOCKS.register("water_jet_bloom", WaterJetBloomBlock::new);
    public static final RegistrySupplier<Block> WATER_JET_FOG_BLOCK = BLOCKS.register("water_jet_fog", WaterJetFogBlock::new);
    public static final RegistrySupplier<Block> FAN_WATER_JET_BLOCK = BLOCKS.register("fan_water_jet", FanWaterJetBlock::new);
    public static final RegistrySupplier<Block> CAKE_WATER_JET_BLOCK = BLOCKS.register("cake_water_jet", CakeWaterJetBlock::new);
    public static final RegistrySupplier<Block> VASE_WATER_JET_BLOCK = BLOCKS.register("vase_water_jet", VaseWaterJetBlock::new);
    public static final RegistrySupplier<Block> WALTZES_WATER_JET_BLOCK = BLOCKS.register("waltzes_water_jet", WaltzesWaterJetBlock::new);
    public static final RegistrySupplier<Block> WALTZ_CURTAIN_BLOCK = BLOCKS.register("waltz_curtain", WaltzCurtainBlock::new);


    public static final RegistrySupplier<Block> WHITE_STROBE_BLOCK = BLOCKS.register("white_strobe", WhiteStrobeBlock::new);
    public static final RegistrySupplier<Block> FIREWORK_RED_COMET_BLOCK = BLOCKS.register("firework_red_comet", () -> new FireworkLauncherBlock(com.github.dumann089.theatricalextralights.firework.FireworkPreset.RED_COMET));
    public static final RegistrySupplier<Block> FIREWORK_BLUE_COMET_BLOCK = BLOCKS.register("firework_blue_comet", () -> new FireworkLauncherBlock(com.github.dumann089.theatricalextralights.firework.FireworkPreset.BLUE_COMET));
    public static final RegistrySupplier<Block> FIREWORK_GREEN_COMET_BLOCK = BLOCKS.register("firework_green_comet", () -> new FireworkLauncherBlock(com.github.dumann089.theatricalextralights.firework.FireworkPreset.GREEN_COMET));
    public static final RegistrySupplier<Block> FIREWORK_GOLD_COMET_BLOCK = BLOCKS.register("firework_gold_comet", () -> new FireworkLauncherBlock(com.github.dumann089.theatricalextralights.firework.FireworkPreset.GOLD_COMET));
    public static final RegistrySupplier<Block> FIREWORK_GOLD_BELL_COMET_BLOCK = BLOCKS.register("firework_gold_bell_comet", () -> new FireworkLauncherBlock(com.github.dumann089.theatricalextralights.firework.FireworkPreset.GOLD_BELL_COMET));
    public static final RegistrySupplier<Block> FIREWORK_RED_PEONY_BLOCK = BLOCKS.register("firework_red_peony", () -> new FireworkLauncherBlock(com.github.dumann089.theatricalextralights.firework.FireworkPreset.RED_PEONY));
    public static final RegistrySupplier<Block> FIREWORK_BLUE_PEONY_BLOCK = BLOCKS.register("firework_blue_peony", () -> new FireworkLauncherBlock(com.github.dumann089.theatricalextralights.firework.FireworkPreset.BLUE_PEONY));
    public static final RegistrySupplier<Block> FIREWORK_GREEN_PEONY_BLOCK = BLOCKS.register("firework_green_peony", () -> new FireworkLauncherBlock(com.github.dumann089.theatricalextralights.firework.FireworkPreset.GREEN_PEONY));
    public static final RegistrySupplier<Block> FIREWORK_GOLD_PEONY_BLOCK = BLOCKS.register("firework_gold_peony", () -> new FireworkLauncherBlock(com.github.dumann089.theatricalextralights.firework.FireworkPreset.GOLD_PEONY));
    public static final RegistrySupplier<Block> FIREWORK_WHITE_PEONY_BLOCK = BLOCKS.register("firework_white_peony", () -> new FireworkLauncherBlock(com.github.dumann089.theatricalextralights.firework.FireworkPreset.WHITE_PEONY));
    public static final RegistrySupplier<Block> FIREWORK_AMBER_PEONY_BLOCK = BLOCKS.register("firework_amber_peony", () -> new FireworkLauncherBlock(com.github.dumann089.theatricalextralights.firework.FireworkPreset.AMBER_PEONY));
    public static final RegistrySupplier<Block> FIREWORK_VIOLET_PEONY_BLOCK = BLOCKS.register("firework_violet_peony", () -> new FireworkLauncherBlock(com.github.dumann089.theatricalextralights.firework.FireworkPreset.VIOLET_PEONY));
    public static final RegistrySupplier<Block> FIREWORK_WHITE_STROBE_BURST_BLOCK = BLOCKS.register("firework_white_strobe_burst", () -> new FireworkLauncherBlock(com.github.dumann089.theatricalextralights.firework.FireworkPreset.WHITE_STROBE_BURST));
    public static final RegistrySupplier<Block> FIREWORK_WHITE_AERIAL_STROBE_BLOCK = BLOCKS.register("firework_white_aerial_strobe", () -> new FireworkLauncherBlock(com.github.dumann089.theatricalextralights.firework.FireworkPreset.WHITE_AERIAL_STROBE));
    public static final RegistrySupplier<Block> FIREWORK_RED_AERIAL_STROBE_BLOCK = BLOCKS.register("firework_red_aerial_strobe", () -> new FireworkLauncherBlock(com.github.dumann089.theatricalextralights.firework.FireworkPreset.RED_AERIAL_STROBE));
    public static final RegistrySupplier<Block> FIREWORK_BLUE_AERIAL_STROBE_BLOCK = BLOCKS.register("firework_blue_aerial_strobe", () -> new FireworkLauncherBlock(com.github.dumann089.theatricalextralights.firework.FireworkPreset.BLUE_AERIAL_STROBE));
    public static final RegistrySupplier<Block> FIREWORK_GREEN_AERIAL_STROBE_BLOCK = BLOCKS.register("firework_green_aerial_strobe", () -> new FireworkLauncherBlock(com.github.dumann089.theatricalextralights.firework.FireworkPreset.GREEN_AERIAL_STROBE));
    public static final RegistrySupplier<Block> FIREWORK_GOLD_AERIAL_STROBE_BLOCK = BLOCKS.register("firework_gold_aerial_strobe", () -> new FireworkLauncherBlock(com.github.dumann089.theatricalextralights.firework.FireworkPreset.GOLD_AERIAL_STROBE));
    public static final RegistrySupplier<Block> FIREWORK_AMBER_AERIAL_STROBE_BLOCK = BLOCKS.register("firework_amber_aerial_strobe", () -> new FireworkLauncherBlock(com.github.dumann089.theatricalextralights.firework.FireworkPreset.AMBER_AERIAL_STROBE));
    public static final RegistrySupplier<Block> FIREWORK_VIOLET_AERIAL_STROBE_BLOCK = BLOCKS.register("firework_violet_aerial_strobe", () -> new FireworkLauncherBlock(com.github.dumann089.theatricalextralights.firework.FireworkPreset.VIOLET_AERIAL_STROBE));
    public static final RegistrySupplier<Block> FIREWORK_SILVER_AERIAL_STROBE_BLOCK = BLOCKS.register("firework_silver_aerial_strobe", () -> new FireworkLauncherBlock(com.github.dumann089.theatricalextralights.firework.FireworkPreset.SILVER_AERIAL_STROBE));
    public static final RegistrySupplier<Block> FIREWORK_GOLD_WILLOW_BLOCK = BLOCKS.register("firework_gold_willow", () -> new FireworkLauncherBlock(com.github.dumann089.theatricalextralights.firework.FireworkPreset.GOLD_WILLOW));
    public static final RegistrySupplier<Block> FIREWORK_RED_WILLOW_BLOCK = BLOCKS.register("firework_red_willow", () -> new FireworkLauncherBlock(com.github.dumann089.theatricalextralights.firework.FireworkPreset.RED_WILLOW));
    public static final RegistrySupplier<Block> FIREWORK_BLUE_WILLOW_BLOCK = BLOCKS.register("firework_blue_willow", () -> new FireworkLauncherBlock(com.github.dumann089.theatricalextralights.firework.FireworkPreset.BLUE_WILLOW));
    public static final RegistrySupplier<Block> FIREWORK_GREEN_WILLOW_BLOCK = BLOCKS.register("firework_green_willow", () -> new FireworkLauncherBlock(com.github.dumann089.theatricalextralights.firework.FireworkPreset.GREEN_WILLOW));
    public static final RegistrySupplier<Block> FIREWORK_WHITE_WILLOW_BLOCK = BLOCKS.register("firework_white_willow", () -> new FireworkLauncherBlock(com.github.dumann089.theatricalextralights.firework.FireworkPreset.WHITE_WILLOW));
    public static final RegistrySupplier<Block> FIREWORK_AMBER_WILLOW_BLOCK = BLOCKS.register("firework_amber_willow", () -> new FireworkLauncherBlock(com.github.dumann089.theatricalextralights.firework.FireworkPreset.AMBER_WILLOW));
    public static final RegistrySupplier<Block> FIREWORK_VIOLET_WILLOW_BLOCK = BLOCKS.register("firework_violet_willow", () -> new FireworkLauncherBlock(com.github.dumann089.theatricalextralights.firework.FireworkPreset.VIOLET_WILLOW));
    public static final RegistrySupplier<Block> FIREWORK_MULTICOLOR_BURST_BLOCK = BLOCKS.register("firework_multicolor_burst", () -> new FireworkLauncherBlock(com.github.dumann089.theatricalextralights.firework.FireworkPreset.MULTICOLOR_BURST));
    public static final RegistrySupplier<Block> FIREWORK_PALM_GOLD_BLOCK = BLOCKS.register("firework_palm_gold", () -> new FireworkLauncherBlock(com.github.dumann089.theatricalextralights.firework.FireworkPreset.PALM_GOLD));
    public static final RegistrySupplier<Block> FIREWORK_CHRYSANTHEMUM_BLUE_BLOCK = BLOCKS.register("firework_chrysanthemum_blue", () -> new FireworkLauncherBlock(com.github.dumann089.theatricalextralights.firework.FireworkPreset.CHRYSANTHEMUM_BLUE));
    public static final RegistrySupplier<Block> FIREWORK_CHRYSANTHEMUM_RED_BLOCK = BLOCKS.register("firework_chrysanthemum_red", () -> new FireworkLauncherBlock(com.github.dumann089.theatricalextralights.firework.FireworkPreset.CHRYSANTHEMUM_RED));
    public static final RegistrySupplier<Block> FIREWORK_CHRYSANTHEMUM_GREEN_BLOCK = BLOCKS.register("firework_chrysanthemum_green", () -> new FireworkLauncherBlock(com.github.dumann089.theatricalextralights.firework.FireworkPreset.CHRYSANTHEMUM_GREEN));
    public static final RegistrySupplier<Block> FIREWORK_CHRYSANTHEMUM_GOLD_BLOCK = BLOCKS.register("firework_chrysanthemum_gold", () -> new FireworkLauncherBlock(com.github.dumann089.theatricalextralights.firework.FireworkPreset.CHRYSANTHEMUM_GOLD));
    public static final RegistrySupplier<Block> FIREWORK_CHRYSANTHEMUM_WHITE_BLOCK = BLOCKS.register("firework_chrysanthemum_white", () -> new FireworkLauncherBlock(com.github.dumann089.theatricalextralights.firework.FireworkPreset.CHRYSANTHEMUM_WHITE));
    public static final RegistrySupplier<Block> FIREWORK_CHRYSANTHEMUM_AMBER_BLOCK = BLOCKS.register("firework_chrysanthemum_amber", () -> new FireworkLauncherBlock(com.github.dumann089.theatricalextralights.firework.FireworkPreset.CHRYSANTHEMUM_AMBER));
    public static final RegistrySupplier<Block> FIREWORK_CHRYSANTHEMUM_VIOLET_BLOCK = BLOCKS.register("firework_chrysanthemum_violet", () -> new FireworkLauncherBlock(com.github.dumann089.theatricalextralights.firework.FireworkPreset.CHRYSANTHEMUM_VIOLET));
    public static final RegistrySupplier<Block> FIREWORK_HORSETAIL_SILVER_BLOCK = BLOCKS.register("firework_horsetail_silver", () -> new FireworkLauncherBlock(com.github.dumann089.theatricalextralights.firework.FireworkPreset.HORSETAIL_SILVER));
    public static final RegistrySupplier<Block> FIREWORK_RING_RED_BLOCK = BLOCKS.register("firework_ring_red", () -> new FireworkLauncherBlock(com.github.dumann089.theatricalextralights.firework.FireworkPreset.RING_RED));
    public static final RegistrySupplier<Block> FIREWORK_SPINNER_GOLD_BLOCK = BLOCKS.register("firework_spinner_gold", () -> new FireworkLauncherBlock(com.github.dumann089.theatricalextralights.firework.FireworkPreset.SPINNER_GOLD));
    public static final RegistrySupplier<Block> FIREWORK_CROSSETTE_RED_BLOCK = BLOCKS.register("firework_crossette_red", () -> new FireworkLauncherBlock(com.github.dumann089.theatricalextralights.firework.FireworkPreset.CROSSETTE_RED));
    public static final RegistrySupplier<Block> FIREWORK_CROSSETTE_BLUE_BLOCK = BLOCKS.register("firework_crossette_blue", () -> new FireworkLauncherBlock(com.github.dumann089.theatricalextralights.firework.FireworkPreset.CROSSETTE_BLUE));
    public static final RegistrySupplier<Block> FIREWORK_CROSSETTE_GREEN_BLOCK = BLOCKS.register("firework_crossette_green", () -> new FireworkLauncherBlock(com.github.dumann089.theatricalextralights.firework.FireworkPreset.CROSSETTE_GREEN));
    public static final RegistrySupplier<Block> FIREWORK_CROSSETTE_GOLD_BLOCK = BLOCKS.register("firework_crossette_gold", () -> new FireworkLauncherBlock(com.github.dumann089.theatricalextralights.firework.FireworkPreset.CROSSETTE_GOLD));
    public static final RegistrySupplier<Block> FIREWORK_CROSSETTE_WHITE_BLOCK = BLOCKS.register("firework_crossette_white", () -> new FireworkLauncherBlock(com.github.dumann089.theatricalextralights.firework.FireworkPreset.CROSSETTE_WHITE));
    public static final RegistrySupplier<Block> FIREWORK_CROSSETTE_AMBER_BLOCK = BLOCKS.register("firework_crossette_amber", () -> new FireworkLauncherBlock(com.github.dumann089.theatricalextralights.firework.FireworkPreset.CROSSETTE_AMBER));
    public static final RegistrySupplier<Block> FIREWORK_CROSSETTE_VIOLET_BLOCK = BLOCKS.register("firework_crossette_violet", () -> new FireworkLauncherBlock(com.github.dumann089.theatricalextralights.firework.FireworkPreset.CROSSETTE_VIOLET));
    public static final RegistrySupplier<Block> FIREWORK_MINE_BLUE_BLOCK = BLOCKS.register("firework_mine_blue", () -> new FireworkLauncherBlock(com.github.dumann089.theatricalextralights.firework.FireworkPreset.MINE_BLUE));
    public static final RegistrySupplier<Block> FIREWORK_MINE_RED_BLOCK = BLOCKS.register("firework_mine_red", () -> new FireworkLauncherBlock(com.github.dumann089.theatricalextralights.firework.FireworkPreset.MINE_RED));
    public static final RegistrySupplier<Block> FIREWORK_MINE_GREEN_BLOCK = BLOCKS.register("firework_mine_green", () -> new FireworkLauncherBlock(com.github.dumann089.theatricalextralights.firework.FireworkPreset.MINE_GREEN));
    public static final RegistrySupplier<Block> FIREWORK_MINE_GOLD_BLOCK = BLOCKS.register("firework_mine_gold", () -> new FireworkLauncherBlock(com.github.dumann089.theatricalextralights.firework.FireworkPreset.MINE_GOLD));
    public static final RegistrySupplier<Block> FIREWORK_MINE_WHITE_BLOCK = BLOCKS.register("firework_mine_white", () -> new FireworkLauncherBlock(com.github.dumann089.theatricalextralights.firework.FireworkPreset.MINE_WHITE));
    public static final RegistrySupplier<Block> FIREWORK_MINE_AMBER_BLOCK = BLOCKS.register("firework_mine_amber", () -> new FireworkLauncherBlock(com.github.dumann089.theatricalextralights.firework.FireworkPreset.MINE_AMBER));
    public static final RegistrySupplier<Block> FIREWORK_MINE_VIOLET_BLOCK = BLOCKS.register("firework_mine_violet", () -> new FireworkLauncherBlock(com.github.dumann089.theatricalextralights.firework.FireworkPreset.MINE_VIOLET));
    public static final RegistrySupplier<Block> FIREWORK_SPIDER_WHITE_BLOCK = BLOCKS.register("firework_spider_white", () -> new FireworkLauncherBlock(com.github.dumann089.theatricalextralights.firework.FireworkPreset.SPIDER_WHITE));
    public static final RegistrySupplier<Block> FIREWORK_DIADEM_BLUE_BLOCK = BLOCKS.register("firework_diadem_blue", () -> new FireworkLauncherBlock(com.github.dumann089.theatricalextralights.firework.FireworkPreset.DIADEM_BLUE));
    public static final RegistrySupplier<Block> FIREWORK_SALUTE_WHITE_BLOCK = BLOCKS.register("firework_salute_white", () -> new FireworkLauncherBlock(com.github.dumann089.theatricalextralights.firework.FireworkPreset.SALUTE_WHITE));
    public static final RegistrySupplier<Block> FIREWORK_HEART_PINK_BLOCK = BLOCKS.register("firework_heart_pink", () -> new FireworkLauncherBlock(com.github.dumann089.theatricalextralights.firework.FireworkPreset.HEART_PINK));
    public static final RegistrySupplier<Block> FIREWORK_DOUBLE_BURST_PURPLE_BLOCK = BLOCKS.register("firework_double_burst_purple", () -> new FireworkLauncherBlock(com.github.dumann089.theatricalextralights.firework.FireworkPreset.DOUBLE_BURST_PURPLE));
    public static final RegistrySupplier<Block> FIREWORK_WHISTLER_SILVER_BLOCK = BLOCKS.register("firework_whistler_silver", () -> new FireworkLauncherBlock(com.github.dumann089.theatricalextralights.firework.FireworkPreset.WHISTLER_SILVER));
    public static final RegistrySupplier<Block> FIREWORK_RGB_LAUNCHER_BLOCK = BLOCKS.register("firework_rgb_launcher", RgbFireworkLauncherBlock::new);
    public static final RegistrySupplier<Block> PYRO_FAN_BLOCK = BLOCKS.register("pyro_fan", PyroFanBlock::new);
    public static final RegistrySupplier<Block> FIREWORK_GOLD_LONG_COMET_BLOCK = BLOCKS.register("firework_gold_long_comet", () -> new FireworkLauncherBlock(com.github.dumann089.theatricalextralights.firework.FireworkPreset.GOLD_LONG_COMET));
    public static final RegistrySupplier<Block> FIREWORK_RED_LONG_COMET_BLOCK = BLOCKS.register("firework_red_long_comet", () -> new FireworkLauncherBlock(com.github.dumann089.theatricalextralights.firework.FireworkPreset.RED_LONG_COMET));
    public static final RegistrySupplier<Block> FIREWORK_BLUE_LONG_COMET_BLOCK = BLOCKS.register("firework_blue_long_comet", () -> new FireworkLauncherBlock(com.github.dumann089.theatricalextralights.firework.FireworkPreset.BLUE_LONG_COMET));
    public static final RegistrySupplier<Block> FIREWORK_GREEN_LONG_COMET_BLOCK = BLOCKS.register("firework_green_long_comet", () -> new FireworkLauncherBlock(com.github.dumann089.theatricalextralights.firework.FireworkPreset.GREEN_LONG_COMET));
    public static final RegistrySupplier<Block> FIREWORK_SILVER_LONG_COMET_BLOCK = BLOCKS.register("firework_silver_long_comet", () -> new FireworkLauncherBlock(com.github.dumann089.theatricalextralights.firework.FireworkPreset.SILVER_LONG_COMET));
    public static final RegistrySupplier<Block> FIREWORK_DAYTIME_POWDER_LIME_BLOCK = BLOCKS.register("firework_daytime_powder_lime", () -> new FireworkLauncherBlock(com.github.dumann089.theatricalextralights.firework.FireworkPreset.LIME_DAYTIME_POWDER));
    public static final RegistrySupplier<Block> FIREWORK_DAYTIME_POWDER_MAGENTA_BLOCK = BLOCKS.register("firework_daytime_powder_magenta", () -> new FireworkLauncherBlock(com.github.dumann089.theatricalextralights.firework.FireworkPreset.MAGENTA_DAYTIME_POWDER));
    public static final RegistrySupplier<Block> FIREWORK_DAYTIME_POWDER_YELLOW_BLOCK = BLOCKS.register("firework_daytime_powder_yellow", () -> new FireworkLauncherBlock(com.github.dumann089.theatricalextralights.firework.FireworkPreset.YELLOW_DAYTIME_POWDER));
    public static final RegistrySupplier<Block> FIREWORK_DAYTIME_POWDER_ORANGE_BLOCK = BLOCKS.register("firework_daytime_powder_orange", () -> new FireworkLauncherBlock(com.github.dumann089.theatricalextralights.firework.FireworkPreset.ORANGE_DAYTIME_POWDER));
    public static final RegistrySupplier<Block> FIREWORK_DAYTIME_POWDER_RED_BLOCK = BLOCKS.register("firework_daytime_powder_red", () -> new FireworkLauncherBlock(com.github.dumann089.theatricalextralights.firework.FireworkPreset.RED_DAYTIME_POWDER));
    public static final RegistrySupplier<Block> FIREWORK_DAYTIME_POWDER_BLUE_BLOCK = BLOCKS.register("firework_daytime_powder_blue", () -> new FireworkLauncherBlock(com.github.dumann089.theatricalextralights.firework.FireworkPreset.BLUE_DAYTIME_POWDER));
    public static final RegistrySupplier<Block> FIREWORK_MORTAR_HIT_BLOCK = BLOCKS.register("firework_mortar_hit", () -> new FireworkLauncherBlock(FireworkPreset.FIREWORK_MORTAR_HIT));
    public static final RegistrySupplier<Block> FIREWORK_FLAME_PROJECTOR_BLOCK = BLOCKS.register("firework_flame_projector", () -> new FireworkLauncherBlock(FireworkPreset.FIREWORK_FLAME_PROJECTOR));



    public static final RegistrySupplier<Block> FIREWORK_DAYTIME_POWDER_RAINBOW_BLOCK = BLOCKS.register("firework_daytime_powder_rainbow", () -> new FireworkLauncherBlock(com.github.dumann089.theatricalextralights.firework.FireworkPreset.RAINBOW_DAYTIME_POWDER_FAN));
    public static final RegistrySupplier<Block> GERB_GOLD_BLOCK = BLOCKS.register("gerb_gold", GerbBlock::new);

    public static final RegistrySupplier<Block> FIREWORK_SILVER_JET_BLOCK = BLOCKS.register("firework_silver_jet", () -> new FireworkLauncherBlock(com.github.dumann089.theatricalextralights.firework.FireworkPreset.SILVER_JET));

    public static final RegistrySupplier<Block> FLAME_PROJECTOR_BLOCK = BLOCKS.register("flame_projector", FlameProjectorBlock::new);
    public static final RegistrySupplier<Block> FLAME_THROWER_BLOCK = BLOCKS.register("flame_thrower", FlameThrowerBlock::new);
    public static final RegistrySupplier<Block> FLOW2JET_BLOCK = BLOCKS.register("flow2jet", Flow2JetBlock::new);
    public static final RegistrySupplier<Block> CONFETTI_CANNON_BLOCK = BLOCKS.register("confetti_cannon", ConfettiCannonBlock::new);
    public static final RegistrySupplier<Block> LASER_MIRROR_BLOCK = BLOCKS.register("laser_mirror", LaserMirrorBlock::new);
    public static final RegistrySupplier<Block> PARSCROLLER_BLOCK = BLOCKS.register("parscroller", ParScrollerBlock::new);
    public static final RegistrySupplier<Block> BLINDER2X2_BLOCK = BLOCKS.register("blinder2x2", Blinder2x2Block::new);
    public static final RegistrySupplier<Block> BLINDER2X2WARM_BLOCK = BLOCKS.register("blinder2x2warm", Blinder2x2warmBlock::new);
    public static final RegistrySupplier<Block> BLINDER1X1_BLOCK = BLOCKS.register("blinder1x1", Blinder1x1Block::new);
    public static final RegistrySupplier<Block> MINI_BAR_BLOCK = BLOCKS.register("mini_bar", MiniBarBlock::new);
    public static final RegistrySupplier<Block> MOVING_VL2C_BEAMS_BLOCK = BLOCKS.register("moving_vl2c_beams", MovingVL2CBeamsBlock::new);
    public static final RegistrySupplier<Block> MOVING_SCAN_BEAMS_BLOCK = BLOCKS.register("moving_scan_beams", MovingScanBeamsBlock::new);
    public static final RegistrySupplier<Block> PRO_SPOT_GOBO_BLOCK = BLOCKS.register("pro_spot_gobo", ProSpotGoboBlock::new);
    public static final RegistrySupplier<Block> MINI_SCAN_GOBO_BLOCK = BLOCKS.register("mini_scan_gobo", MiniScanGobosBlock::new);
    public static final RegistrySupplier<Block> MINI_SPOT_GOBO_BLOCK = BLOCKS.register("mini_spot_gobo", MiniSpotGobosBlock::new);


    public static final RegistrySupplier<Block> A1X1PAR64_BLOCK = BLOCKS.register("a1x1par64", a1x1par64Block::new);
    public static final RegistrySupplier<Block> A2X8PAR64_BLOCK = BLOCKS.register("a2x8par64", a2x8par64Block::new);
    public static final RegistrySupplier<Block> A6X3PAR64_VERTICAL_BLOCK = BLOCKS.register("a6x3par64_vertical", a6x3par64_verticalBlock::new);

    public static final RegistrySupplier<Block> TRUSS_BLOCK = BLOCKS.register("truss", TrussBlock::new);
    public static final RegistrySupplier<Block> TRUSS_JOINT_BLOCK = BLOCKS.register("truss_joint", TrussJointBlock::new);
    public static final RegistrySupplier<Block> TRUSS_CORNER_BLOCK = BLOCKS.register("truss_corner", TrussCornerBlock::new);
    public static final RegistrySupplier<Block> TRUSS_CORNER_T_BLOCK = BLOCKS.register("truss_corner_t", TrussCornerTBlock::new);
    public static final RegistrySupplier<Block> FOLLOWSPOT_CONSOLE_BLOCK = BLOCKS.register("followspot_console", FollowspotConsoleBlock::new);
    public static final RegistrySupplier<Block> LED_FACADE = BLOCKS.register("led_facade", LedFacadeBlock::new);






    public static void init(){
        BLOCKS.register();
    }
}
