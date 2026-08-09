package com.github.dumann089.theatricalextralights.blockentities;

import com.github.dumann089.theatricalextralights.TheatricalExtraLightsRegistry;
import com.github.dumann089.theatricalextralights.blocks.Blocks;
import com.github.dumann089.theatricalextralights.blocks.WaltzCurtainBlock;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class BlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = TheatricalExtraLightsRegistry.get(Registries.BLOCK_ENTITY_TYPE);
    public static final RegistrySupplier<BlockEntityType<MovingVL2CBlockEntity>> MOVING_VL2C = BLOCK_ENTITIES.register("moving_vl2c", () -> BlockEntityType.Builder.of(MovingVL2CBlockEntity::new, Blocks.MOVING_VL2C_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<MovingVL6BlockEntity>> MOVING_VL6 = BLOCK_ENTITIES.register("moving_vl6", () -> BlockEntityType.Builder.of(MovingVL6BlockEntity::new, Blocks.MOVING_VL6_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<MovingBeamBlockEntity>> MOVING_BEAM = BLOCK_ENTITIES.register("moving_beam", () -> BlockEntityType.Builder.of(MovingBeamBlockEntity::new, Blocks.MOVING_BEAM_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<MovingScanBlockEntity>> MOVING_SCAN = BLOCK_ENTITIES.register("moving_scan", () -> BlockEntityType.Builder.of(MovingScanBlockEntity::new, Blocks.MOVING_SCAN_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<RGBBarBlockEntity>> RGB_BAR = BLOCK_ENTITIES.register("rgb_bar", () -> BlockEntityType.Builder.of(RGBBarBlockEntity::new, Blocks.RGB_BAR.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<LEDFountainBlockEntity>> LED_FOUNTAIN = BLOCK_ENTITIES.register("led_fountain", () -> BlockEntityType.Builder.of(LEDFountainBlockEntity::new, Blocks.LED_FOUNTAIN.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<LEDPanel2BlockEntity>> LED_PANEL_2 = BLOCK_ENTITIES.register("led_panel_2", () -> BlockEntityType.Builder.of(LEDPanel2BlockEntity::new, Blocks.LED_PANEL_2.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<BigPanelBlockEntity>> BIG_PANEL = BLOCK_ENTITIES.register("big_panel", () -> BlockEntityType.Builder.of(BigPanelBlockEntity::new, Blocks.BIG_PANEL.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<BigPanel2BlockEntity>> BIG_PANEL2 = BLOCK_ENTITIES.register("big_panel2", () -> BlockEntityType.Builder.of(BigPanel2BlockEntity::new, Blocks.BIG_PANEL2.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<ParLedBlockEntity>> PAR_LED = BLOCK_ENTITIES.register("par_led", () -> BlockEntityType.Builder.of(ParLedBlockEntity::new, Blocks.PAR_LED.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<LaserBlockEntity>> LASER = BLOCK_ENTITIES.register("laser", () -> BlockEntityType.Builder.of(LaserBlockEntity::new, Blocks.LASER_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<BlinderBlockEntity>> BLINDER = BLOCK_ENTITIES.register("blinder", () -> BlockEntityType.Builder.of(BlinderBlockEntity::new, Blocks.BLINDER.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<BlinderwarmBlockEntity>> BLINDER_WARM = BLOCK_ENTITIES.register("blinder_warm", () -> BlockEntityType.Builder.of(BlinderwarmBlockEntity::new, Blocks.BLINDER_WARM_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<StrobeBlockEntity>> STROBE = BLOCK_ENTITIES.register("strobe", () -> BlockEntityType.Builder.of(StrobeBlockEntity::new, Blocks.STROBE.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<AtomicStrobeBlockEntity>> ATOMIC_STROBE = BLOCK_ENTITIES.register("atomic_strobe", () -> BlockEntityType.Builder.of(AtomicStrobeBlockEntity::new, Blocks.ATOMIC_STROBE.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<truss3lightsBlockEntity>> TRUSS_3LIGHTS = BLOCK_ENTITIES.register("truss_3lights", () -> BlockEntityType.Builder.of(truss3lightsBlockEntity::new, Blocks.TRUSS_3LIGHTS.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<Beam7RBlockEntity>> BEAM_7R = BLOCK_ENTITIES.register("beam_7r", () -> BlockEntityType.Builder.of(Beam7RBlockEntity::new, Blocks.BEAM_7R_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<Source4BlockEntity>> SOURCE_FOUR = BLOCK_ENTITIES.register("source_four", () -> BlockEntityType.Builder.of(Source4BlockEntity::new, Blocks.SOURCE_FOUR_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<Source4warmBlockEntity>> SOURCE_FOUR_WARM = BLOCK_ENTITIES.register("source_four_warm", () -> BlockEntityType.Builder.of(Source4warmBlockEntity::new, Blocks.SOURCE_FOUR_WARM_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<MacVipBlockEntity>> MAC_VIP = BLOCK_ENTITIES.register("mac_vip", () -> BlockEntityType.Builder.of(MacVipBlockEntity::new, Blocks.MAC_VIP_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<SharplusBlockEntity>> SHARPLUS = BLOCK_ENTITIES.register("sharplus", () -> BlockEntityType.Builder.of(SharplusBlockEntity::new, Blocks.SHARPLUS_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<Par1000RedBlockEntity>> PAR1000_RED = BLOCK_ENTITIES.register("par1000_red", () -> BlockEntityType.Builder.of(Par1000RedBlockEntity::new, Blocks.PAR1000_RED_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<Par1000BlueBlockEntity>> PAR1000_BLUE = BLOCK_ENTITIES.register("par1000_blue", () -> BlockEntityType.Builder.of(Par1000BlueBlockEntity::new, Blocks.PAR1000_BLUE_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<Par1000GreenBlockEntity>> PAR1000_GREEN = BLOCK_ENTITIES.register("par1000_green", () -> BlockEntityType.Builder.of(Par1000GreenBlockEntity::new, Blocks.PAR1000_GREEN_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<Par1000MagentaBlockEntity>> PAR1000_MAGENTA = BLOCK_ENTITIES.register("par1000_magenta", () -> BlockEntityType.Builder.of(Par1000MagentaBlockEntity::new, Blocks.PAR1000_MAGENTA_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<Par1000AmberBlockEntity>> PAR1000_AMBER = BLOCK_ENTITIES.register("par1000_amber", () -> BlockEntityType.Builder.of(Par1000AmberBlockEntity::new, Blocks.PAR1000_AMBER_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<Par1000BlockEntity>> PAR1000 = BLOCK_ENTITIES.register("par1000", () -> BlockEntityType.Builder.of(Par1000BlockEntity::new, Blocks.PAR1000_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<Par1000PurpleBlockEntity>> PAR1000_PURPLE = BLOCK_ENTITIES.register("par1000_purple", () -> BlockEntityType.Builder.of(Par1000PurpleBlockEntity::new, Blocks.PAR1000_PURPLE_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<Par1000LightblueBlockEntity>> PAR1000_LIGHTBLUE = BLOCK_ENTITIES.register("par1000_lightblue", () -> BlockEntityType.Builder.of(Par1000LightblueBlockEntity::new, Blocks.PAR1000_LIGHTBLUE_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<Par1000WhiteBlockEntity>> PAR1000_WHITE = BLOCK_ENTITIES.register("par1000_white", () -> BlockEntityType.Builder.of(Par1000WhiteBlockEntity::new, Blocks.PAR1000_WHITE_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<Par1000OrangeBlockEntity>> PAR1000_ORANGE = BLOCK_ENTITIES.register("par1000_orange", () -> BlockEntityType.Builder.of(Par1000OrangeBlockEntity::new, Blocks.PAR1000_ORANGE_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<Moving500BlockEntity>> MOVING500 = BLOCK_ENTITIES.register("moving500", () -> BlockEntityType.Builder.of(Moving500BlockEntity::new, Blocks.MOVING500_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<RobitspotBlockEntity>> ROBITSPOT = BLOCK_ENTITIES.register("robitspot", () -> BlockEntityType.Builder.of(RobitspotBlockEntity::new, Blocks.ROBITSPOT_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<VervespotBlockEntity>> VERVESPOT = BLOCK_ENTITIES.register("vervespot", () -> BlockEntityType.Builder.of(VervespotBlockEntity::new, Blocks.VERVESPOT_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<VerticalbarBlockEntity>> VERTICAL_BAR = BLOCK_ENTITIES.register("vertical_bar", () -> BlockEntityType.Builder.of(VerticalbarBlockEntity::new, Blocks.VERTICALBAR_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<SearchlightBlockEntity>> SEARCHLIGHT = BLOCK_ENTITIES.register("searchlight", () -> BlockEntityType.Builder.of(SearchlightBlockEntity::new, Blocks.SEARCHLIGHT_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<WashlightBlockEntity>> WASHLIGHT = BLOCK_ENTITIES.register("washlight", () -> BlockEntityType.Builder.of(WashlightBlockEntity::new, Blocks.WASHLIGHT_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<AtomictiltBlockEntity>> ATOMICTILT = BLOCK_ENTITIES.register("atomictilt", () -> BlockEntityType.Builder.of(AtomictiltBlockEntity::new, Blocks.ATOMICTILT_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<MiniwashBlockEntity>> MINIWASH = BLOCK_ENTITIES.register("miniwash", () -> BlockEntityType.Builder.of(MiniwashBlockEntity::new, Blocks.MINIWASH_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<InvisiblelightBlockEntity>> INVISIBLELIGHT = BLOCK_ENTITIES.register("invisiblelight", () -> BlockEntityType.Builder.of(InvisiblelightBlockEntity::new, Blocks.INVISIBLE_LIGHT_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<x8par_redBlockEntity>> x8PAR_RED = BLOCK_ENTITIES.register("x8par_red", () -> BlockEntityType.Builder.of(x8par_redBlockEntity::new, Blocks.X8PAR_RED_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<x8par_greenBlockEntity>> x8PAR_GREEN = BLOCK_ENTITIES.register("x8par_green", () -> BlockEntityType.Builder.of(x8par_greenBlockEntity::new, Blocks.X8PAR_GREEN_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<x8par_blueBlockEntity>> x8PAR_BLUE = BLOCK_ENTITIES.register("x8par_blue", () -> BlockEntityType.Builder.of(x8par_blueBlockEntity::new, Blocks.X8PAR_BLUE_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<x8par_magentaBlockEntity>> x8PAR_MAGENTA = BLOCK_ENTITIES.register("x8par_magenta", () -> BlockEntityType.Builder.of(x8par_magentaBlockEntity::new, Blocks.X8PAR_MAGENTA_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<x8par_lightblueBlockEntity>> x8PAR_LIGHTBLUE = BLOCK_ENTITIES.register("x8par_lightblue", () -> BlockEntityType.Builder.of(x8par_lightblueBlockEntity::new, Blocks.X8PAR_LIGHTBLUE_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<x8par_yellowBlockEntity>> x8PAR_YELLOW = BLOCK_ENTITIES.register("x8par_yellow", () -> BlockEntityType.Builder.of(x8par_yellowBlockEntity::new, Blocks.X8PAR_YELLOW_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<x8par_whiteBlockEntity>> x8PAR_WHITE = BLOCK_ENTITIES.register("x8par_white", () -> BlockEntityType.Builder.of(x8par_whiteBlockEntity::new, Blocks.X8PAR_WHITE_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<x8par_purpleBlockEntity>> x8PAR_PURPLE = BLOCK_ENTITIES.register("x8par_purple", () -> BlockEntityType.Builder.of(x8par_purpleBlockEntity::new, Blocks.X8PAR_PURPLE_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<x8par_warmBlockEntity>> x8PAR_WARM = BLOCK_ENTITIES.register("x8par_warm", () -> BlockEntityType.Builder.of(x8par_warmBlockEntity::new, Blocks.X8PAR_WARM_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<x8par_orangeBlockEntity>> x8PAR_ORANGE = BLOCK_ENTITIES.register("x8par_orange", () -> BlockEntityType.Builder.of(x8par_orangeBlockEntity::new, Blocks.X8PAR_ORANGE_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<par56_redBlockEntity>> PAR56_RED = BLOCK_ENTITIES.register("par56_red", () -> BlockEntityType.Builder.of(par56_redBlockEntity::new, Blocks.PAR56_RED_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<par56_greenBlockEntity>> PAR56_GREEN = BLOCK_ENTITIES.register("par56_green", () -> BlockEntityType.Builder.of(par56_greenBlockEntity::new, Blocks.PAR56_GREEN_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<par56_blueBlockEntity>> PAR56_BLUE = BLOCK_ENTITIES.register("par56_blue", () -> BlockEntityType.Builder.of(par56_blueBlockEntity::new, Blocks.PAR56_BLUE_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<par56_orangeBlockEntity>> PAR56_ORANGE = BLOCK_ENTITIES.register("par56_orange", () -> BlockEntityType.Builder.of(par56_orangeBlockEntity::new, Blocks.PAR56_ORANGE_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<par56_magentaBlockEntity>> PAR56_MAGENTA = BLOCK_ENTITIES.register("par56_magenta", () -> BlockEntityType.Builder.of(par56_magentaBlockEntity::new, Blocks.PAR56_MAGENTA_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<par56_lightblueBlockEntity>> PAR56_LIGHTBLUE = BLOCK_ENTITIES.register("par56_lightblue", () -> BlockEntityType.Builder.of(par56_lightblueBlockEntity::new, Blocks.PAR56_LIGHTBLUE_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<par56_purpleBlockEntity>> PAR56_PURPLE = BLOCK_ENTITIES.register("par56_purple", () -> BlockEntityType.Builder.of(par56_purpleBlockEntity::new, Blocks.PAR56_PURPLE_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<par56_whiteBlockEntity>> PAR56_WHITE = BLOCK_ENTITIES.register("par56_white", () -> BlockEntityType.Builder.of(par56_whiteBlockEntity::new, Blocks.PAR56_WHITE_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<par56_warmBlockEntity>> PAR56_WARM = BLOCK_ENTITIES.register("par56_warm", () -> BlockEntityType.Builder.of(par56_warmBlockEntity::new, Blocks.PAR56_WARM_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<par56_yellowBlockEntity>> PAR56_YELLOW = BLOCK_ENTITIES.register("par56_yellow", () -> BlockEntityType.Builder.of(par56_yellowBlockEntity::new, Blocks.PAR56_YELLOW_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<VL6000BlockEntity>> VL6000 = BLOCK_ENTITIES.register("vl6000", () -> BlockEntityType.Builder.of(VL6000BlockEntity::new, Blocks.VL6000_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<a2x2par64_redBlockEntity>> A2X2PAR64_RED = BLOCK_ENTITIES.register("a2x2par64_red", () -> BlockEntityType.Builder.of(a2x2par64_redBlockEntity::new, Blocks.A2X2PAR64_RED_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<a2x2par64_greenBlockEntity>> A2X2PAR64_GREEN = BLOCK_ENTITIES.register("a2x2par64_green", () -> BlockEntityType.Builder.of(a2x2par64_greenBlockEntity::new, Blocks.A2X2PAR64_GREEN_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<a2x2par64_blueBlockEntity>> A2X2PAR64_BLUE = BLOCK_ENTITIES.register("a2x2par64_blue", () -> BlockEntityType.Builder.of(a2x2par64_blueBlockEntity::new, Blocks.A2X2PAR64_BLUE_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<a2x2par64_magentaBlockEntity>> A2X2PAR64_MAGENTA = BLOCK_ENTITIES.register("a2x2par64_magenta", () -> BlockEntityType.Builder.of(a2x2par64_magentaBlockEntity::new, Blocks.A2X2PAR64_MAGENTA_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<a2x2par64_lightblueBlockEntity>> A2X2PAR64_LIGHTBLUE = BLOCK_ENTITIES.register("a2x2par64_lightblue", () -> BlockEntityType.Builder.of(a2x2par64_lightblueBlockEntity::new, Blocks.A2X2PAR64_LIGHTBLUE_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<a2x2par64_purpleBlockEntity>> A2X2PAR64_PURPLE = BLOCK_ENTITIES.register("a2x2par64_purple", () -> BlockEntityType.Builder.of(a2x2par64_purpleBlockEntity::new, Blocks.A2X2PAR64_PURPLE_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<a2x2par64_orangeBlockEntity>> A2X2PAR64_ORANGE = BLOCK_ENTITIES.register("a2x2par64_orange", () -> BlockEntityType.Builder.of(a2x2par64_orangeBlockEntity::new, Blocks.A2X2PAR64_ORANGE_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<a2x2par64_yellowBlockEntity>> A2X2PAR64_YELLOW = BLOCK_ENTITIES.register("a2x2par64_yellow", () -> BlockEntityType.Builder.of(a2x2par64_yellowBlockEntity::new, Blocks.A2X2PAR64_YELLOW_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<a2x2par64_warmBlockEntity>> A2X2PAR64_WARM = BLOCK_ENTITIES.register("a2x2par64_warm", () -> BlockEntityType.Builder.of(a2x2par64_warmBlockEntity::new, Blocks.A2X2PAR64_WARM_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<a2x2par64_whiteBlockEntity>> A2X2PAR64_WHITE = BLOCK_ENTITIES.register("a2x2par64_white", () -> BlockEntityType.Builder.of(a2x2par64_whiteBlockEntity::new, Blocks.A2X2PAR64_WHITE_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<DWTPanelBlockEntity>> DWT_PANEL = BLOCK_ENTITIES.register("dwt_panel", () -> BlockEntityType.Builder.of(DWTPanelBlockEntity::new, Blocks.DWT_PANEL_BLOCK.get()).build(null));


    public static final RegistrySupplier<BlockEntityType<FollowspotBlockEntity>> FOLLOWSPOT = BLOCK_ENTITIES.register("followspot", () -> BlockEntityType.Builder.of(FollowspotBlockEntity::new, Blocks.FOLLOWSPOT_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<bigscrollerBlockEntity>> BIGSCROLLER = BLOCK_ENTITIES.register("bigscroller", () -> BlockEntityType.Builder.of(bigscrollerBlockEntity::new, Blocks.BIGSCROLLER_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<horizontalscrollerBlockEntity>> HORIZONTALSCROLLER = BLOCK_ENTITIES.register("horizontalscroller", () -> BlockEntityType.Builder.of(horizontalscrollerBlockEntity::new, Blocks.HORIZONTALSCROLLER_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<verticalscrollerBlockEntity>> VERTICALSCROLLER = BLOCK_ENTITIES.register("verticalcroller", () -> BlockEntityType.Builder.of(verticalscrollerBlockEntity::new, Blocks.VERTICALSCROLLER_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<washledBlockEntity>> WASHLED = BLOCK_ENTITIES.register("washled", () -> BlockEntityType.Builder.of(washledBlockEntity::new, Blocks.WASHLED_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<MovingbarBlockEntity>> MOVING_BAR = BLOCK_ENTITIES.register("moving_bar", () -> BlockEntityType.Builder.of(MovingbarBlockEntity::new, Blocks.MOVING_BAR_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<MovingMiniBarBlockEntity>> MOVING_MINI_BAR = BLOCK_ENTITIES.register("moving_mini_bar", () -> BlockEntityType.Builder.of(MovingMiniBarBlockEntity::new, Blocks.MOVING_MINI_BAR_BLOCK.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<WaterJetBlockEntity>> WATER_JET = BLOCK_ENTITIES.register("water_jet", () -> BlockEntityType.Builder.of(WaterJetBlockEntity::new, Blocks.WATER_JET_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<MovingJetBlockEntity>> MOVING_JET = BLOCK_ENTITIES.register("moving_jet", () -> BlockEntityType.Builder.of(MovingJetBlockEntity::new, Blocks.MOVING_JET_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<WaterJetThinBlockEntity>> WATER_JET_THIN = BLOCK_ENTITIES.register("water_jet_thin", () -> BlockEntityType.Builder.of(WaterJetThinBlockEntity::new, Blocks.WATER_JET_THIN_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<SpinnerBlockEntity>> SPINNER = BLOCK_ENTITIES.register("spinner", () -> BlockEntityType.Builder.of(SpinnerBlockEntity::new, Blocks.SPINNER_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<OrganPipesBlockEntity>> ORGANPIPES = BLOCK_ENTITIES.register("organpipes", () -> BlockEntityType.Builder.of(OrganPipesBlockEntity::new, Blocks.ORGANPIPES_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<OrganPipesInvBlockEntity>> ORGANPIPES_INV = BLOCK_ENTITIES.register("organpipes_inv", () -> BlockEntityType.Builder.of(OrganPipesInvBlockEntity::new, Blocks.ORGANPIPES_INV_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<WaterJetSpreadBlockEntity>> WATER_JET_SPREAD = BLOCK_ENTITIES.register("water_jet_spread", () -> BlockEntityType.Builder.of(WaterJetSpreadBlockEntity::new, Blocks.WATER_JET_SPREAD_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<WaterJetBigBlockEntity>> WATER_JET_BIG = BLOCK_ENTITIES.register("water_jet_big", () -> BlockEntityType.Builder.of(WaterJetBigBlockEntity::new, Blocks.WATER_JET_BIG_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<WaterJetCentralBlockEntity>> WATER_JET_CENTRAL = BLOCK_ENTITIES.register("water_jet_central", () -> BlockEntityType.Builder.of(WaterJetCentralBlockEntity::new, Blocks.WATER_JET_CENTRAL_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<WaterJetConeBlockEntity>> WATER_JET_CONE = BLOCK_ENTITIES.register("water_jet_cone", () -> BlockEntityType.Builder.of(WaterJetConeBlockEntity::new, Blocks.WATER_JET_CONE_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<WaterJetBloomBlockEntity>> WATER_JET_BLOOM = BLOCK_ENTITIES.register("water_jet_bloom", () -> BlockEntityType.Builder.of(WaterJetBloomBlockEntity::new, Blocks.WATER_JET_BLOOM_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<WaterJetFogBlockEntity>> WATER_JET_FOG = BLOCK_ENTITIES.register("water_jet_fog", () -> BlockEntityType.Builder.of(WaterJetFogBlockEntity::new, Blocks.WATER_JET_FOG_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<FanWaterJetBlockEntity>> FAN_WATER_JET = BLOCK_ENTITIES.register("fan_water_jet", () -> BlockEntityType.Builder.of(FanWaterJetBlockEntity::new, Blocks.FAN_WATER_JET_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<CakeWaterJetBlockEntity>> CAKE_WATER_JET = BLOCK_ENTITIES.register("cake_water_jet", () -> BlockEntityType.Builder.of(CakeWaterJetBlockEntity::new, Blocks.CAKE_WATER_JET_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<VaseWaterJetBlockEntity>> VASE_WATER_JET = BLOCK_ENTITIES.register("vase_water_jet", () -> BlockEntityType.Builder.of(VaseWaterJetBlockEntity::new, Blocks.VASE_WATER_JET_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<WaltzesWaterJetBlockEntity>> WALTZES_WATER_JET = BLOCK_ENTITIES.register("waltzes_water_jet", () -> BlockEntityType.Builder.of(WaltzesWaterJetBlockEntity::new, Blocks.WALTZES_WATER_JET_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<WaltzCurtainBlockEntity>> WALTZ_CURTAIN = BLOCK_ENTITIES.register("waltz_curtain", () -> BlockEntityType.Builder.of(WaltzCurtainBlockEntity::new, Blocks.WALTZ_CURTAIN_BLOCK.get()).build(null));


    public static final RegistrySupplier<BlockEntityType<WhiteStrobeBlockEntity>> WHITE_STROBE = BLOCK_ENTITIES.register("white_strobe", () -> BlockEntityType.Builder.of(WhiteStrobeBlockEntity::new, Blocks.WHITE_STROBE_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<FireworkLauncherBlockEntity>> FIREWORK_LAUNCHER = BLOCK_ENTITIES.register("firework_launcher", () ->
            BlockEntityType.Builder.of(
                    FireworkLauncherBlockEntity::new,
                    Blocks.FIREWORK_RED_COMET_BLOCK.get(),
                    Blocks.FIREWORK_BLUE_COMET_BLOCK.get(),
                    Blocks.FIREWORK_GREEN_COMET_BLOCK.get(),
                    Blocks.FIREWORK_GOLD_COMET_BLOCK.get(),
                    Blocks.FIREWORK_GOLD_BELL_COMET_BLOCK.get(),
                    Blocks.FIREWORK_RED_PEONY_BLOCK.get(),
                    Blocks.FIREWORK_BLUE_PEONY_BLOCK.get(),
                    Blocks.FIREWORK_GREEN_PEONY_BLOCK.get(),
                    Blocks.FIREWORK_GOLD_PEONY_BLOCK.get(),
                    Blocks.FIREWORK_WHITE_PEONY_BLOCK.get(),
                    Blocks.FIREWORK_AMBER_PEONY_BLOCK.get(),
                    Blocks.FIREWORK_VIOLET_PEONY_BLOCK.get(),
                    Blocks.FIREWORK_WHITE_STROBE_BURST_BLOCK.get(),
                    Blocks.FIREWORK_WHITE_AERIAL_STROBE_BLOCK.get(),
                    Blocks.FIREWORK_RED_AERIAL_STROBE_BLOCK.get(),
                    Blocks.FIREWORK_BLUE_AERIAL_STROBE_BLOCK.get(),
                    Blocks.FIREWORK_GREEN_AERIAL_STROBE_BLOCK.get(),
                    Blocks.FIREWORK_GOLD_AERIAL_STROBE_BLOCK.get(),
                    Blocks.FIREWORK_AMBER_AERIAL_STROBE_BLOCK.get(),
                    Blocks.FIREWORK_VIOLET_AERIAL_STROBE_BLOCK.get(),
                    Blocks.FIREWORK_SILVER_AERIAL_STROBE_BLOCK.get(),
                    Blocks.FIREWORK_GOLD_WILLOW_BLOCK.get(),
                    Blocks.FIREWORK_RED_WILLOW_BLOCK.get(),
                    Blocks.FIREWORK_BLUE_WILLOW_BLOCK.get(),
                    Blocks.FIREWORK_GREEN_WILLOW_BLOCK.get(),
                    Blocks.FIREWORK_WHITE_WILLOW_BLOCK.get(),
                    Blocks.FIREWORK_AMBER_WILLOW_BLOCK.get(),
                    Blocks.FIREWORK_VIOLET_WILLOW_BLOCK.get(),
                    Blocks.FIREWORK_MULTICOLOR_BURST_BLOCK.get(),
                    Blocks.FIREWORK_PALM_GOLD_BLOCK.get(),
                    Blocks.FIREWORK_CHRYSANTHEMUM_BLUE_BLOCK.get(),
                    Blocks.FIREWORK_CHRYSANTHEMUM_RED_BLOCK.get(),
                    Blocks.FIREWORK_CHRYSANTHEMUM_GREEN_BLOCK.get(),
                    Blocks.FIREWORK_CHRYSANTHEMUM_GOLD_BLOCK.get(),
                    Blocks.FIREWORK_CHRYSANTHEMUM_WHITE_BLOCK.get(),
                    Blocks.FIREWORK_CHRYSANTHEMUM_AMBER_BLOCK.get(),
                    Blocks.FIREWORK_CHRYSANTHEMUM_VIOLET_BLOCK.get(),
                    Blocks.FIREWORK_HORSETAIL_SILVER_BLOCK.get(),
                    Blocks.FIREWORK_RING_RED_BLOCK.get(),
                    Blocks.FIREWORK_SPINNER_GOLD_BLOCK.get(),
                    Blocks.FIREWORK_CROSSETTE_RED_BLOCK.get(),
                    Blocks.FIREWORK_CROSSETTE_BLUE_BLOCK.get(),
                    Blocks.FIREWORK_CROSSETTE_GREEN_BLOCK.get(),
                    Blocks.FIREWORK_CROSSETTE_GOLD_BLOCK.get(),
                    Blocks.FIREWORK_CROSSETTE_WHITE_BLOCK.get(),
                    Blocks.FIREWORK_CROSSETTE_AMBER_BLOCK.get(),
                    Blocks.FIREWORK_CROSSETTE_VIOLET_BLOCK.get(),
                    Blocks.FIREWORK_MINE_BLUE_BLOCK.get(),
                    Blocks.FIREWORK_MINE_RED_BLOCK.get(),
                    Blocks.FIREWORK_MINE_GREEN_BLOCK.get(),
                    Blocks.FIREWORK_MINE_GOLD_BLOCK.get(),
                    Blocks.FIREWORK_MINE_WHITE_BLOCK.get(),
                    Blocks.FIREWORK_MINE_AMBER_BLOCK.get(),
                    Blocks.FIREWORK_MINE_VIOLET_BLOCK.get(),
                    Blocks.FIREWORK_SPIDER_WHITE_BLOCK.get(),
                    Blocks.FIREWORK_DIADEM_BLUE_BLOCK.get(),
                    Blocks.FIREWORK_SALUTE_WHITE_BLOCK.get(),
                    Blocks.FIREWORK_HEART_PINK_BLOCK.get(),
                    Blocks.FIREWORK_DOUBLE_BURST_PURPLE_BLOCK.get(),
                    Blocks.FIREWORK_WHISTLER_SILVER_BLOCK.get(),
                    Blocks.FIREWORK_GOLD_LONG_COMET_BLOCK.get(),
                    Blocks.FIREWORK_RED_LONG_COMET_BLOCK.get(),
                    Blocks.FIREWORK_BLUE_LONG_COMET_BLOCK.get(),
                    Blocks.FIREWORK_GREEN_LONG_COMET_BLOCK.get(),
                    Blocks.FIREWORK_SILVER_LONG_COMET_BLOCK.get(),
                    Blocks.FIREWORK_DAYTIME_POWDER_LIME_BLOCK.get(),
                    Blocks.FIREWORK_DAYTIME_POWDER_MAGENTA_BLOCK.get(),
                    Blocks.FIREWORK_DAYTIME_POWDER_YELLOW_BLOCK.get(),
                    Blocks.FIREWORK_DAYTIME_POWDER_ORANGE_BLOCK.get(),
                    Blocks.FIREWORK_DAYTIME_POWDER_RED_BLOCK.get(),
                    Blocks.FIREWORK_DAYTIME_POWDER_BLUE_BLOCK.get(),
                    Blocks.FIREWORK_DAYTIME_POWDER_RAINBOW_BLOCK.get(),
                    Blocks.FIREWORK_SILVER_JET_BLOCK.get(),
                    Blocks.FIREWORK_MORTAR_HIT_BLOCK.get(),
                    Blocks.FIREWORK_FLAME_PROJECTOR_BLOCK.get()

            ).build(null));
    public static final RegistrySupplier<BlockEntityType<PyroFanBlockEntity>> PYRO_FAN = BLOCK_ENTITIES.register("pyro_fan", () ->
            BlockEntityType.Builder.of(PyroFanBlockEntity::new, Blocks.PYRO_FAN_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<RgbFireworkLauncherBlockEntity>> RGB_FIREWORK_LAUNCHER = BLOCK_ENTITIES.register("firework_rgb_launcher", () ->
            BlockEntityType.Builder.of(RgbFireworkLauncherBlockEntity::new, Blocks.FIREWORK_RGB_LAUNCHER_BLOCK.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<GerbBlockEntity>> GERB_GOLD = BLOCK_ENTITIES.register("gerb_gold", () ->
            BlockEntityType.Builder.of(GerbBlockEntity::new, Blocks.GERB_GOLD_BLOCK.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<FlameProjectorBlockEntity>> FLAME_PROJECTOR = BLOCK_ENTITIES.register("flame_projector", () ->
            BlockEntityType.Builder.of(FlameProjectorBlockEntity::new, Blocks.FLAME_PROJECTOR_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<FlameThrowerBlockEntity>> FLAME_THROWER = BLOCK_ENTITIES.register("flame_thrower", () ->
            BlockEntityType.Builder.of(FlameThrowerBlockEntity::new, Blocks.FLAME_THROWER_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<Flow2JetBlockEntity>> FLOW2JET = BLOCK_ENTITIES.register("flow2jet", () ->
            BlockEntityType.Builder.of(Flow2JetBlockEntity::new, Blocks.FLOW2JET_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<ConfettiCannonBlockEntity>> CONFETTI_CANNON = BLOCK_ENTITIES.register("confetti_cannon", () ->
            BlockEntityType.Builder.of(ConfettiCannonBlockEntity::new, Blocks.CONFETTI_CANNON_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<LaserMirrorBlockEntity>> LASER_MIRROR = BLOCK_ENTITIES.register("laser_mirror", () -> BlockEntityType.Builder.of(LaserMirrorBlockEntity::new, Blocks.LASER_MIRROR_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<ParScrollerBlockEntity>> PARSCROLLER = BLOCK_ENTITIES.register("parscroller", () -> BlockEntityType.Builder.of(ParScrollerBlockEntity::new, Blocks.PARSCROLLER_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<Blinder2x2BlockEntity>> BLINDER2X2 = BLOCK_ENTITIES.register("blinder2x2", () -> BlockEntityType.Builder.of(Blinder2x2BlockEntity::new, Blocks.BLINDER2X2_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<Blinder2x2warmBlockEntity>> BLINDER2X2WARM = BLOCK_ENTITIES.register("blinder2x2warm", () -> BlockEntityType.Builder.of(Blinder2x2warmBlockEntity::new, Blocks.BLINDER2X2WARM_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<Blinder1x1BlockEntity>> BLINDER1X1 = BLOCK_ENTITIES.register("blinder1x1", () -> BlockEntityType.Builder.of(Blinder1x1BlockEntity::new, Blocks.BLINDER1X1_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<MiniBarBlockEntity>> MINI_BAR = BLOCK_ENTITIES.register("mini_bar", () -> BlockEntityType.Builder.of(MiniBarBlockEntity::new, Blocks.MINI_BAR_BLOCK.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<MovingVL2CBeamsBlockEntity>> MOVING_VL2C_BEAMS = BLOCK_ENTITIES.register("moving_vl2c_beams", () -> BlockEntityType.Builder.of(MovingVL2CBeamsBlockEntity::new, Blocks.MOVING_VL2C_BEAMS_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<MovingScanBeamsBlockEntity>> MOVING_SCAN_BEAMS = BLOCK_ENTITIES.register("moving_scan_beams", () -> BlockEntityType.Builder.of(MovingScanBeamsBlockEntity::new, Blocks.MOVING_SCAN_BEAMS_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<SpotXtremeGoboBlockEntity>> SPOT_XTREME_GOBO = BLOCK_ENTITIES.register("spot_xtreme_gobo", () -> BlockEntityType.Builder.of(SpotXtremeGoboBlockEntity::new, Blocks.SPOT_XTREME_GOBO_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<VL6CGoboBlockEntity>> VL6C_GOBO = BLOCK_ENTITIES.register("vl6c_gobo", () -> BlockEntityType.Builder.of(VL6CGoboBlockEntity::new, Blocks.VL6C_GOBO_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<Iris700GoboBlockEntity>> IRIS_700_GOBO = BLOCK_ENTITIES.register("iris_700_gobo", () -> BlockEntityType.Builder.of(Iris700GoboBlockEntity::new, Blocks.IRIS_700_GOBO_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<ProSpotGoboBlockEntity>> PRO_SPOT_GOBO = BLOCK_ENTITIES.register("pro_spot_gobo", () -> BlockEntityType.Builder.of(ProSpotGoboBlockEntity::new, Blocks.PRO_SPOT_GOBO_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<MiniScanGobosBlockEntity>> MINI_SCAN_GOBO = BLOCK_ENTITIES.register("mini_scan_gobo", () -> BlockEntityType.Builder.of(MiniScanGobosBlockEntity::new, Blocks.MINI_SCAN_GOBO_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<MiniSpotGobosBlockEntity>> MINI_SPOT_GOBO = BLOCK_ENTITIES.register("mini_spot_gobo", () -> BlockEntityType.Builder.of(MiniSpotGobosBlockEntity::new, Blocks.MINI_SPOT_GOBO_BLOCK.get()).build(null));


    public static final RegistrySupplier<BlockEntityType<a1x1par64BlockEntity>> A1X1PAR64 = BLOCK_ENTITIES.register("a1x1par64", () -> BlockEntityType.Builder.of(a1x1par64BlockEntity::new, Blocks.A1X1PAR64_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<a2x8par64BlockEntity>> A2X8PAR64 = BLOCK_ENTITIES.register("a2x8par64", () -> BlockEntityType.Builder.of(a2x8par64BlockEntity::new, Blocks.A2X8PAR64_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<a6x3par64_verticalBlockEntity>> A6X3PAR64_VERTICAL = BLOCK_ENTITIES.register("a6x3par64_vertical", () -> BlockEntityType.Builder.of(a6x3par64_verticalBlockEntity::new, Blocks.A6X3PAR64_VERTICAL_BLOCK.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<FollowspotConsoleBlockEntity>> FOLLOWSPOT_CONSOLE =
            BLOCK_ENTITIES.register("followspot_console",
                    () -> BlockEntityType.Builder.of(FollowspotConsoleBlockEntity::new, Blocks.FOLLOWSPOT_CONSOLE_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<LedFacadeBlockEntity>> LED_FACADE =
            BLOCK_ENTITIES.register("led_facade",
                    () -> BlockEntityType.Builder.of(LedFacadeBlockEntity::new, Blocks.LED_FACADE.get()).build(null));
    public static void init(){
        BLOCK_ENTITIES.register();
    }
}
