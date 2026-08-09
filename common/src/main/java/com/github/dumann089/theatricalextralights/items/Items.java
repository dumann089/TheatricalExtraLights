package com.github.dumann089.theatricalextralights.items;

import com.github.dumann089.theatricalextralights.TheatricalExtraLights;
import com.github.dumann089.theatricalextralights.TheatricalExtraLightsRegistry;
import com.github.dumann089.theatricalextralights.blocks.Blocks;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

public class Items {
    public static final DeferredRegister<Item> ITEMS = TheatricalExtraLightsRegistry.get(Registries.ITEM);
    public static final RegistrySupplier<Item> TRUSS = ITEMS.register(
            "truss",
            () -> new BlockItem(Blocks.TRUSS_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
    public static final RegistrySupplier<Item> TRUSS_JOINT = ITEMS.register(
            "truss_joint",
            () -> new BlockItem(Blocks.TRUSS_JOINT_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
    public static final RegistrySupplier<Item> TRUSS_CORNER = ITEMS.register(
            "truss_corner",
            () -> new BlockItem(Blocks.TRUSS_CORNER_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
    public static final RegistrySupplier<Item> TRUSS_CORNER_T = ITEMS.register(
            "truss_corner_t",
            () -> new BlockItem(Blocks.TRUSS_CORNER_T_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
    public static final RegistrySupplier<Item> MOVING_VL2C = ITEMS.register(
            "moving_vl2c",
            () -> new BlockItem(Blocks.MOVING_VL2C_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
    public static final RegistrySupplier<Item> MOVING_SCAN = ITEMS.register(
            "moving_scan",
            () -> new BlockItem(Blocks.MOVING_SCAN_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
    public static final RegistrySupplier<Item> RGB_BAR = ITEMS.register(
            "rgb_bar",
            () -> new BlockItem(Blocks.RGB_BAR.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
    public static final RegistrySupplier<Item> MINI_BAR = ITEMS.register(
            "mini_bar",
            () -> new BlockItem(Blocks.MINI_BAR_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
    public static final RegistrySupplier<Item> VERTICAL_BAR = ITEMS.register(
        "vertical_bar",
        () -> new BlockItem(Blocks.VERTICALBAR_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
);
    public static final RegistrySupplier<Item> MOVING_BAR = ITEMS.register(
            "moving_bar",
            () -> new BlockItem(Blocks.MOVING_BAR_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
    public static final RegistrySupplier<Item> MOVING_MINI_BAR = ITEMS.register(
            "moving_mini_bar",
            () -> new BlockItem(Blocks.MOVING_MINI_BAR_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
    public static final RegistrySupplier<Item> MOVING_BEAM = ITEMS.register(
            "moving_beam",
            () -> new BlockItem(Blocks.MOVING_BEAM_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
    public static final RegistrySupplier<Item> LED_FOUNTAIN = ITEMS.register(
            "led_fountain",
            () -> new BlockItem(Blocks.LED_FOUNTAIN.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
    public static final RegistrySupplier<Item> LED_PANEL_2 = ITEMS.register(
            "led_panel_2",
            () -> new BlockItem(Blocks.LED_PANEL_2.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
    public static final RegistrySupplier<Item> BIG_PANEL = ITEMS.register(
            "big_panel",
            () -> new BlockItem(Blocks.BIG_PANEL.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
    public static final RegistrySupplier<Item> BIG_PANEL2 = ITEMS.register(
            "big_panel2",
            () -> new BlockItem(Blocks.BIG_PANEL2.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
    public static final RegistrySupplier<Item> MOVING_VL6 = ITEMS.register(
            "moving_vl6",
            () -> new BlockItem(Blocks.MOVING_VL6_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
    public static final RegistrySupplier<Item> PAR_LED = ITEMS.register(
            "par_led",
            () -> new BlockItem(Blocks.PAR_LED.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
    public static final RegistrySupplier<Item> LASER = ITEMS.register(
           "laser",
           () -> new BlockItem(Blocks.LASER_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
    public static final RegistrySupplier<Item> BLINDER = ITEMS.register(
            "blinder",
            () -> new BlockItem(Blocks.BLINDER.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
    public static final RegistrySupplier<Item> BLINDER2X2 = ITEMS.register(
            "blinder2x2",
            () -> new BlockItem(Blocks.BLINDER2X2_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
    public static final RegistrySupplier<Item> BLINDER2X2WARM = ITEMS.register(
            "blinder2x2warm",
            () -> new BlockItem(Blocks.BLINDER2X2WARM_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
    public static final RegistrySupplier<Item> BLINDER1X1 = ITEMS.register(
            "blinder1x1",
            () -> new BlockItem(Blocks.BLINDER1X1_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
    public static final RegistrySupplier<Item> BLINDER_WARM = ITEMS.register(
            "blinder_warm",
            () -> new BlockItem(Blocks.BLINDER_WARM_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
    public static final RegistrySupplier<Item> STROBE = ITEMS.register(
            "strobe",
            () -> new BlockItem(Blocks.STROBE.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
    public static final RegistrySupplier<Item> ATOMIC_STROBE = ITEMS.register(
            "atomic_strobe",
            () -> new BlockItem(Blocks.ATOMIC_STROBE.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
    public static final RegistrySupplier<Item> WHITE_STROBE = ITEMS.register(
            "white_strobe",
            () -> new BlockItem(Blocks.WHITE_STROBE_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
    public static final RegistrySupplier<Item> FIREWORK_RED_COMET = ITEMS.register(
            "firework_red_comet",
            () -> new BlockItem(Blocks.FIREWORK_RED_COMET_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.PYRO_TAB))
    );
    public static final RegistrySupplier<Item> FIREWORK_BLUE_COMET = ITEMS.register(
            "firework_blue_comet",
            () -> new BlockItem(Blocks.FIREWORK_BLUE_COMET_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.PYRO_TAB))
    );
    public static final RegistrySupplier<Item> FIREWORK_GREEN_COMET = ITEMS.register(
            "firework_green_comet",
            () -> new BlockItem(Blocks.FIREWORK_GREEN_COMET_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.PYRO_TAB))
    );
    public static final RegistrySupplier<Item> FIREWORK_GOLD_COMET = ITEMS.register(
            "firework_gold_comet",
            () -> new BlockItem(Blocks.FIREWORK_GOLD_COMET_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.PYRO_TAB))
    );
    public static final RegistrySupplier<Item> FIREWORK_GOLD_BELL_COMET = ITEMS.register(
            "firework_gold_bell_comet",
            () -> new BlockItem(Blocks.FIREWORK_GOLD_BELL_COMET_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.PYRO_TAB))
    );
    public static final RegistrySupplier<Item> FIREWORK_RED_PEONY = ITEMS.register(
            "firework_red_peony",
            () -> new BlockItem(Blocks.FIREWORK_RED_PEONY_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.PYRO_TAB))
    );
    public static final RegistrySupplier<Item> FIREWORK_BLUE_PEONY = ITEMS.register(
            "firework_blue_peony",
            () -> new BlockItem(Blocks.FIREWORK_BLUE_PEONY_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.PYRO_TAB))
    );
    public static final RegistrySupplier<Item> FIREWORK_GREEN_PEONY = ITEMS.register(
            "firework_green_peony",
            () -> new BlockItem(Blocks.FIREWORK_GREEN_PEONY_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.PYRO_TAB))
    );
    public static final RegistrySupplier<Item> FIREWORK_GOLD_PEONY = ITEMS.register(
            "firework_gold_peony",
            () -> new BlockItem(Blocks.FIREWORK_GOLD_PEONY_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.PYRO_TAB))
    );
    public static final RegistrySupplier<Item> FIREWORK_WHITE_PEONY = ITEMS.register(
            "firework_white_peony",
            () -> new BlockItem(Blocks.FIREWORK_WHITE_PEONY_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.PYRO_TAB))
    );
    public static final RegistrySupplier<Item> FIREWORK_AMBER_PEONY = ITEMS.register(
            "firework_amber_peony",
            () -> new BlockItem(Blocks.FIREWORK_AMBER_PEONY_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.PYRO_TAB))
    );
    public static final RegistrySupplier<Item> FIREWORK_VIOLET_PEONY = ITEMS.register(
            "firework_violet_peony",
            () -> new BlockItem(Blocks.FIREWORK_VIOLET_PEONY_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.PYRO_TAB))
    );
    public static final RegistrySupplier<Item> FIREWORK_WHITE_STROBE_BURST = ITEMS.register(
            "firework_white_strobe_burst",
            () -> new BlockItem(Blocks.FIREWORK_WHITE_STROBE_BURST_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.PYRO_TAB))
    );
    public static final RegistrySupplier<Item> FIREWORK_WHITE_AERIAL_STROBE = ITEMS.register(
            "firework_white_aerial_strobe",
            () -> new BlockItem(Blocks.FIREWORK_WHITE_AERIAL_STROBE_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.PYRO_TAB))
    );
    public static final RegistrySupplier<Item> FIREWORK_RED_AERIAL_STROBE = ITEMS.register(
            "firework_red_aerial_strobe",
            () -> new BlockItem(Blocks.FIREWORK_RED_AERIAL_STROBE_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.PYRO_TAB))
    );
    public static final RegistrySupplier<Item> FIREWORK_BLUE_AERIAL_STROBE = ITEMS.register(
            "firework_blue_aerial_strobe",
            () -> new BlockItem(Blocks.FIREWORK_BLUE_AERIAL_STROBE_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.PYRO_TAB))
    );
    public static final RegistrySupplier<Item> FIREWORK_GREEN_AERIAL_STROBE = ITEMS.register(
            "firework_green_aerial_strobe",
            () -> new BlockItem(Blocks.FIREWORK_GREEN_AERIAL_STROBE_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.PYRO_TAB))
    );
    public static final RegistrySupplier<Item> FIREWORK_GOLD_AERIAL_STROBE = ITEMS.register(
            "firework_gold_aerial_strobe",
            () -> new BlockItem(Blocks.FIREWORK_GOLD_AERIAL_STROBE_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.PYRO_TAB))
    );
    public static final RegistrySupplier<Item> FIREWORK_AMBER_AERIAL_STROBE = ITEMS.register(
            "firework_amber_aerial_strobe",
            () -> new BlockItem(Blocks.FIREWORK_AMBER_AERIAL_STROBE_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.PYRO_TAB))
    );
    public static final RegistrySupplier<Item> FIREWORK_VIOLET_AERIAL_STROBE = ITEMS.register(
            "firework_violet_aerial_strobe",
            () -> new BlockItem(Blocks.FIREWORK_VIOLET_AERIAL_STROBE_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.PYRO_TAB))
    );
    public static final RegistrySupplier<Item> FIREWORK_SILVER_AERIAL_STROBE = ITEMS.register(
            "firework_silver_aerial_strobe",
            () -> new BlockItem(Blocks.FIREWORK_SILVER_AERIAL_STROBE_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.PYRO_TAB))
    );
    public static final RegistrySupplier<Item> FIREWORK_GOLD_WILLOW = ITEMS.register(
            "firework_gold_willow",
            () -> new BlockItem(Blocks.FIREWORK_GOLD_WILLOW_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.PYRO_TAB))
    );
    public static final RegistrySupplier<Item> FIREWORK_RED_WILLOW = ITEMS.register(
            "firework_red_willow",
            () -> new BlockItem(Blocks.FIREWORK_RED_WILLOW_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.PYRO_TAB))
    );
    public static final RegistrySupplier<Item> FIREWORK_BLUE_WILLOW = ITEMS.register(
            "firework_blue_willow",
            () -> new BlockItem(Blocks.FIREWORK_BLUE_WILLOW_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.PYRO_TAB))
    );
    public static final RegistrySupplier<Item> FIREWORK_GREEN_WILLOW = ITEMS.register(
            "firework_green_willow",
            () -> new BlockItem(Blocks.FIREWORK_GREEN_WILLOW_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.PYRO_TAB))
    );
    public static final RegistrySupplier<Item> FIREWORK_WHITE_WILLOW = ITEMS.register(
            "firework_white_willow",
            () -> new BlockItem(Blocks.FIREWORK_WHITE_WILLOW_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.PYRO_TAB))
    );
    public static final RegistrySupplier<Item> FIREWORK_AMBER_WILLOW = ITEMS.register(
            "firework_amber_willow",
            () -> new BlockItem(Blocks.FIREWORK_AMBER_WILLOW_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.PYRO_TAB))
    );
    public static final RegistrySupplier<Item> FIREWORK_VIOLET_WILLOW = ITEMS.register(
            "firework_violet_willow",
            () -> new BlockItem(Blocks.FIREWORK_VIOLET_WILLOW_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.PYRO_TAB))
    );
    public static final RegistrySupplier<Item> FIREWORK_MULTICOLOR_BURST = ITEMS.register(
            "firework_multicolor_burst",
            () -> new BlockItem(Blocks.FIREWORK_MULTICOLOR_BURST_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.PYRO_TAB))
    );
    public static final RegistrySupplier<Item> FIREWORK_PALM_GOLD = ITEMS.register(
            "firework_palm_gold",
            () -> new BlockItem(Blocks.FIREWORK_PALM_GOLD_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.PYRO_TAB))
    );
    public static final RegistrySupplier<Item> FIREWORK_CHRYSANTHEMUM_BLUE = ITEMS.register(
            "firework_chrysanthemum_blue",
            () -> new BlockItem(Blocks.FIREWORK_CHRYSANTHEMUM_BLUE_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.PYRO_TAB))
    );
    public static final RegistrySupplier<Item> FIREWORK_CHRYSANTHEMUM_RED = ITEMS.register(
            "firework_chrysanthemum_red",
            () -> new BlockItem(Blocks.FIREWORK_CHRYSANTHEMUM_RED_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.PYRO_TAB))
    );
    public static final RegistrySupplier<Item> FIREWORK_CHRYSANTHEMUM_GREEN = ITEMS.register(
            "firework_chrysanthemum_green",
            () -> new BlockItem(Blocks.FIREWORK_CHRYSANTHEMUM_GREEN_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.PYRO_TAB))
    );
    public static final RegistrySupplier<Item> FIREWORK_CHRYSANTHEMUM_GOLD = ITEMS.register(
            "firework_chrysanthemum_gold",
            () -> new BlockItem(Blocks.FIREWORK_CHRYSANTHEMUM_GOLD_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.PYRO_TAB))
    );
    public static final RegistrySupplier<Item> FIREWORK_CHRYSANTHEMUM_WHITE = ITEMS.register(
            "firework_chrysanthemum_white",
            () -> new BlockItem(Blocks.FIREWORK_CHRYSANTHEMUM_WHITE_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.PYRO_TAB))
    );
    public static final RegistrySupplier<Item> FIREWORK_CHRYSANTHEMUM_AMBER = ITEMS.register(
            "firework_chrysanthemum_amber",
            () -> new BlockItem(Blocks.FIREWORK_CHRYSANTHEMUM_AMBER_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.PYRO_TAB))
    );
    public static final RegistrySupplier<Item> FIREWORK_CHRYSANTHEMUM_VIOLET = ITEMS.register(
            "firework_chrysanthemum_violet",
            () -> new BlockItem(Blocks.FIREWORK_CHRYSANTHEMUM_VIOLET_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.PYRO_TAB))
    );
    public static final RegistrySupplier<Item> FIREWORK_HORSETAIL_SILVER = ITEMS.register(
            "firework_horsetail_silver",
            () -> new BlockItem(Blocks.FIREWORK_HORSETAIL_SILVER_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.PYRO_TAB))
    );
    public static final RegistrySupplier<Item> FIREWORK_RING_RED = ITEMS.register(
            "firework_ring_red",
            () -> new BlockItem(Blocks.FIREWORK_RING_RED_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.PYRO_TAB))
    );
    public static final RegistrySupplier<Item> FIREWORK_SPINNER_GOLD = ITEMS.register(
            "firework_spinner_gold",
            () -> new BlockItem(Blocks.FIREWORK_SPINNER_GOLD_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.PYRO_TAB))
    );
    public static final RegistrySupplier<Item> FIREWORK_CROSSETTE_RED = ITEMS.register(
            "firework_crossette_red",
            () -> new BlockItem(Blocks.FIREWORK_CROSSETTE_RED_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.PYRO_TAB))
    );
    public static final RegistrySupplier<Item> FIREWORK_CROSSETTE_BLUE = ITEMS.register(
            "firework_crossette_blue",
            () -> new BlockItem(Blocks.FIREWORK_CROSSETTE_BLUE_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.PYRO_TAB))
    );
    public static final RegistrySupplier<Item> FIREWORK_CROSSETTE_GREEN = ITEMS.register(
            "firework_crossette_green",
            () -> new BlockItem(Blocks.FIREWORK_CROSSETTE_GREEN_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.PYRO_TAB))
    );
    public static final RegistrySupplier<Item> FIREWORK_CROSSETTE_GOLD = ITEMS.register(
            "firework_crossette_gold",
            () -> new BlockItem(Blocks.FIREWORK_CROSSETTE_GOLD_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.PYRO_TAB))
    );
    public static final RegistrySupplier<Item> FIREWORK_CROSSETTE_WHITE = ITEMS.register(
            "firework_crossette_white",
            () -> new BlockItem(Blocks.FIREWORK_CROSSETTE_WHITE_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.PYRO_TAB))
    );
    public static final RegistrySupplier<Item> FIREWORK_CROSSETTE_AMBER = ITEMS.register(
            "firework_crossette_amber",
            () -> new BlockItem(Blocks.FIREWORK_CROSSETTE_AMBER_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.PYRO_TAB))
    );
    public static final RegistrySupplier<Item> FIREWORK_CROSSETTE_VIOLET = ITEMS.register(
            "firework_crossette_violet",
            () -> new BlockItem(Blocks.FIREWORK_CROSSETTE_VIOLET_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.PYRO_TAB))
    );
    public static final RegistrySupplier<Item> FIREWORK_MINE_BLUE = ITEMS.register(
            "firework_mine_blue",
            () -> new BlockItem(Blocks.FIREWORK_MINE_BLUE_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.PYRO_TAB))
    );
    public static final RegistrySupplier<Item> FIREWORK_MINE_RED = ITEMS.register(
            "firework_mine_red",
            () -> new BlockItem(Blocks.FIREWORK_MINE_RED_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.PYRO_TAB))
    );
    public static final RegistrySupplier<Item> FIREWORK_MINE_GREEN = ITEMS.register(
            "firework_mine_green",
            () -> new BlockItem(Blocks.FIREWORK_MINE_GREEN_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.PYRO_TAB))
    );
    public static final RegistrySupplier<Item> FIREWORK_MINE_GOLD = ITEMS.register(
            "firework_mine_gold",
            () -> new BlockItem(Blocks.FIREWORK_MINE_GOLD_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.PYRO_TAB))
    );
    public static final RegistrySupplier<Item> FIREWORK_MINE_WHITE = ITEMS.register(
            "firework_mine_white",
            () -> new BlockItem(Blocks.FIREWORK_MINE_WHITE_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.PYRO_TAB))
    );
    public static final RegistrySupplier<Item> FIREWORK_MINE_AMBER = ITEMS.register(
            "firework_mine_amber",
            () -> new BlockItem(Blocks.FIREWORK_MINE_AMBER_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.PYRO_TAB))
    );
    public static final RegistrySupplier<Item> FIREWORK_MINE_VIOLET = ITEMS.register(
            "firework_mine_violet",
            () -> new BlockItem(Blocks.FIREWORK_MINE_VIOLET_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.PYRO_TAB))
    );
    public static final RegistrySupplier<Item> FIREWORK_SPIDER_WHITE = ITEMS.register(
            "firework_spider_white",
            () -> new BlockItem(Blocks.FIREWORK_SPIDER_WHITE_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.PYRO_TAB))
    );
    public static final RegistrySupplier<Item> FIREWORK_DIADEM_BLUE = ITEMS.register(
            "firework_diadem_blue",
            () -> new BlockItem(Blocks.FIREWORK_DIADEM_BLUE_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.PYRO_TAB))
    );
    public static final RegistrySupplier<Item> FIREWORK_SALUTE_WHITE = ITEMS.register(
            "firework_salute_white",
            () -> new BlockItem(Blocks.FIREWORK_SALUTE_WHITE_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.PYRO_TAB))
    );
    public static final RegistrySupplier<Item> FIREWORK_HEART_PINK = ITEMS.register(
            "firework_heart_pink",
            () -> new BlockItem(Blocks.FIREWORK_HEART_PINK_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.PYRO_TAB))
    );
    public static final RegistrySupplier<Item> FIREWORK_DOUBLE_BURST_PURPLE = ITEMS.register(
            "firework_double_burst_purple",
            () -> new BlockItem(Blocks.FIREWORK_DOUBLE_BURST_PURPLE_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.PYRO_TAB))
    );
    public static final RegistrySupplier<Item> FIREWORK_WHISTLER_SILVER = ITEMS.register(
            "firework_whistler_silver",
            () -> new BlockItem(Blocks.FIREWORK_WHISTLER_SILVER_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.PYRO_TAB))
    );
    public static final RegistrySupplier<Item> FIREWORK_RGB_LAUNCHER = ITEMS.register(
            "firework_rgb_launcher",
            () -> new BlockItem(Blocks.FIREWORK_RGB_LAUNCHER_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.PYRO_TAB))
    );
    public static final RegistrySupplier<Item> PYRO_FAN = ITEMS.register(
            "pyro_fan",
            () -> new BlockItem(Blocks.PYRO_FAN_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.PYRO_TAB))
    );
    public static final RegistrySupplier<Item> FIREWORK_GOLD_LONG_COMET = ITEMS.register(
            "firework_gold_long_comet",
            () -> new BlockItem(Blocks.FIREWORK_GOLD_LONG_COMET_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.PYRO_TAB))
    );
    public static final RegistrySupplier<Item> FIREWORK_RED_LONG_COMET = ITEMS.register(
            "firework_red_long_comet",
            () -> new BlockItem(Blocks.FIREWORK_RED_LONG_COMET_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.PYRO_TAB))
    );
    public static final RegistrySupplier<Item> FIREWORK_BLUE_LONG_COMET = ITEMS.register(
            "firework_blue_long_comet",
            () -> new BlockItem(Blocks.FIREWORK_BLUE_LONG_COMET_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.PYRO_TAB))
    );
    public static final RegistrySupplier<Item> FIREWORK_GREEN_LONG_COMET = ITEMS.register(
            "firework_green_long_comet",
            () -> new BlockItem(Blocks.FIREWORK_GREEN_LONG_COMET_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.PYRO_TAB))
    );
    public static final RegistrySupplier<Item> FIREWORK_SILVER_LONG_COMET = ITEMS.register(
            "firework_silver_long_comet",
            () -> new BlockItem(Blocks.FIREWORK_SILVER_LONG_COMET_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.PYRO_TAB))
    );
    public static final RegistrySupplier<Item> FIREWORK_DAYTIME_POWDER_LIME = ITEMS.register(
            "firework_daytime_powder_lime",
            () -> new BlockItem(Blocks.FIREWORK_DAYTIME_POWDER_LIME_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.PYRO_TAB))
    );
    public static final RegistrySupplier<Item> FIREWORK_DAYTIME_POWDER_MAGENTA = ITEMS.register(
            "firework_daytime_powder_magenta",
            () -> new BlockItem(Blocks.FIREWORK_DAYTIME_POWDER_MAGENTA_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.PYRO_TAB))
    );
    public static final RegistrySupplier<Item> FIREWORK_DAYTIME_POWDER_YELLOW = ITEMS.register(
            "firework_daytime_powder_yellow",
            () -> new BlockItem(Blocks.FIREWORK_DAYTIME_POWDER_YELLOW_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.PYRO_TAB))
    );
    public static final RegistrySupplier<Item> FIREWORK_DAYTIME_POWDER_ORANGE = ITEMS.register(
            "firework_daytime_powder_orange",
            () -> new BlockItem(Blocks.FIREWORK_DAYTIME_POWDER_ORANGE_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.PYRO_TAB))
    );
    public static final RegistrySupplier<Item> FIREWORK_DAYTIME_POWDER_RED = ITEMS.register(
            "firework_daytime_powder_red",
            () -> new BlockItem(Blocks.FIREWORK_DAYTIME_POWDER_RED_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.PYRO_TAB))
    );
    public static final RegistrySupplier<Item> FIREWORK_DAYTIME_POWDER_BLUE = ITEMS.register(
            "firework_daytime_powder_blue",
            () -> new BlockItem(Blocks.FIREWORK_DAYTIME_POWDER_BLUE_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.PYRO_TAB))
    );
    public static final RegistrySupplier<Item> FIREWORK_DAYTIME_POWDER_RAINBOW = ITEMS.register(
            "firework_daytime_powder_rainbow",
            () -> new BlockItem(Blocks.FIREWORK_DAYTIME_POWDER_RAINBOW_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.PYRO_TAB))
    );
    public static final RegistrySupplier<Item> GERB_GOLD = ITEMS.register(
            "gerb_gold",
            () -> new BlockItem(Blocks.GERB_GOLD_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.PYRO_TAB))
    );


    public static final RegistrySupplier<Item> FIREWORK_MORTAR_HIT = ITEMS.register(
            "firework_mortar_hit",
            () -> new BlockItem(Blocks.FIREWORK_MORTAR_HIT_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.PYRO_TAB))
    );
    public static final RegistrySupplier<Item> FIREWORK_SILVER_JET = ITEMS.register(
            "firework_silver_jet",
            () -> new BlockItem(Blocks.FIREWORK_SILVER_JET_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.PYRO_TAB))
    );
    public static final RegistrySupplier<Item> FLAME_THROWER = ITEMS.register(
            "flame_thrower",
            () -> new BlockItem(Blocks.FLAME_THROWER_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.PYRO_TAB))
    );
    public static final RegistrySupplier<Item> FLOW2JET = ITEMS.register(
            "flow2jet",
            () -> new BlockItem(Blocks.FLOW2JET_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.PYRO_TAB))
    );
    public static final RegistrySupplier<Item> FIREWORK_FLAME_PROJECTOR = ITEMS.register(
            "firework_flame_projector",
            () -> new BlockItem(Blocks.FIREWORK_FLAME_PROJECTOR_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.PYRO_TAB))
    );


    public static final RegistrySupplier<Item> CONFETTI_CANNON = ITEMS.register(
            "confetti_cannon",
            ConfettiCannonItemRegistration::create
    );
    public static final RegistrySupplier<Item> FIXTURE_WRENCH = ITEMS.register(
            "fixture_wrench",
            FixtureWrenchItem::new
    );
    public static final RegistrySupplier<Item> TRUSS_3LIGHTS = ITEMS.register(
        "truss_3lights",
        () -> new BlockItem(Blocks.TRUSS_3LIGHTS.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    ); 
    public static final RegistrySupplier<Item> BEAM_7R = ITEMS.register(
        "beam_7r",
        () -> new BlockItem(Blocks.BEAM_7R_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
    public static final RegistrySupplier<Item> MAC_VIP = ITEMS.register(
        "mac_vip",
        () -> new BlockItem(Blocks.MAC_VIP_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
    public static final RegistrySupplier<Item> SHARPLUS = ITEMS.register(
        "sharplus",
        () -> new BlockItem(Blocks.SHARPLUS_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
    public static final RegistrySupplier<Item> MOVING500 = ITEMS.register(
        "moving500",
        () -> new BlockItem(Blocks.MOVING500_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
    public static final RegistrySupplier<Item> ROBITSPOT = ITEMS.register(
        "robitspot",
        () -> new BlockItem(Blocks.ROBITSPOT_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
    public static final RegistrySupplier<Item> VERVESPOT = ITEMS.register(
        "vervespot",
        () -> new BlockItem(Blocks.VERVESPOT_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
    public static final RegistrySupplier<Item> SEARCHLIGHT = ITEMS.register(
        "searchlight",
        () -> new BlockItem(Blocks.SEARCHLIGHT_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
        public static final RegistrySupplier<Item> WASHLIGHT = ITEMS.register(
        "washlight",
        () -> new BlockItem(Blocks.WASHLIGHT_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
            public static final RegistrySupplier<Item> MINIWASH = ITEMS.register(
        "miniwash",
        () -> new BlockItem(Blocks.MINIWASH_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
            public static final RegistrySupplier<Item> ATOMICTILT = ITEMS.register(
        "atomictilt",
        () -> new BlockItem(Blocks.ATOMICTILT_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
                public static final RegistrySupplier<Item> VL6000 = ITEMS.register(
        "vl6000",
        () -> new BlockItem(Blocks.VL6000_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
    public static final RegistrySupplier<Item> MOVING_VL2C_BEAMS = ITEMS.register(
            "moving_vl2c_beams",
            () -> new BlockItem(Blocks.MOVING_VL2C_BEAMS_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
    public static final RegistrySupplier<Item> MOVING_SCAN_BEAMS = ITEMS.register(
            "moving_scan_beams",
            () -> new BlockItem(Blocks.MOVING_SCAN_BEAMS_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
    public static final RegistrySupplier<Item> SPOT_XTREME_GOBO = ITEMS.register(
            "spot_xtreme_gobo",
            () -> new BlockItem(Blocks.SPOT_XTREME_GOBO_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
    public static final RegistrySupplier<Item> VL6C_GOBO = ITEMS.register(
            "vl6c_gobo",
            () -> new BlockItem(Blocks.VL6C_GOBO_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
    public static final RegistrySupplier<Item> IRIS_700_GOBO = ITEMS.register(
            "iris_700_gobo",
            () -> new BlockItem(Blocks.IRIS_700_GOBO_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
    public static final RegistrySupplier<Item> PRO_SPOT_GOBO = ITEMS.register(
            "pro_spot_gobo",
            () -> new BlockItem(Blocks.PRO_SPOT_GOBO_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
    public static final RegistrySupplier<Item> MINI_SCAN_GOBO = ITEMS.register(
            "mini_scan_gobo",
            () -> new BlockItem(Blocks.MINI_SCAN_GOBO_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
    public static final RegistrySupplier<Item> MINI_SPOT_GOBO = ITEMS.register(
            "mini_spot_gobo",
            () -> new BlockItem(Blocks.MINI_SPOT_GOBO_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
    public static final RegistrySupplier<Item> WASHLED = ITEMS.register(
            "washled",
            () -> new BlockItem(Blocks.WASHLED_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
                public static final RegistrySupplier<Item> INVISIBLELIGHT = ITEMS.register(
        "invisiblelight",
        () -> new BlockItem(Blocks.INVISIBLE_LIGHT_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
    public static final RegistrySupplier<Item> SOURCE_FOUR = ITEMS.register(
        "source_four",
        () -> new BlockItem(Blocks.SOURCE_FOUR_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
    public static final RegistrySupplier<Item> SOURCE_FOUR_warm = ITEMS.register(
            "source_four_warm",
            () -> new BlockItem(Blocks.SOURCE_FOUR_WARM_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
    public static final RegistrySupplier<Item> PARSCROLLER = ITEMS.register(
            "parscroller",
            () -> new BlockItem(Blocks.PARSCROLLER_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
    public static final RegistrySupplier<Item> PAR1000_RED = ITEMS.register(
        "par1000_red",
        () -> new BlockItem(Blocks.PAR1000_RED_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
    public static final RegistrySupplier<Item> PAR1000_BLUE = ITEMS.register(
        "par1000_blue",
        () -> new BlockItem(Blocks.PAR1000_BLUE_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
    public static final RegistrySupplier<Item> PAR1000_GREEN = ITEMS.register(
        "par1000_green",
        () -> new BlockItem(Blocks.PAR1000_GREEN_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
    public static final RegistrySupplier<Item> PAR1000_MAGENTA = ITEMS.register(
        "par1000_magenta",
        () -> new BlockItem(Blocks.PAR1000_MAGENTA_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
    public static final RegistrySupplier<Item> PAR1000_AMBER = ITEMS.register(
        "par1000_amber",
        () -> new BlockItem(Blocks.PAR1000_AMBER_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
        public static final RegistrySupplier<Item> PAR1000_ORANGE = ITEMS.register(
        "par1000_orange",
        () -> new BlockItem(Blocks.PAR1000_ORANGE_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
    public static final RegistrySupplier<Item> PAR1000 = ITEMS.register(
        "par1000",
        () -> new BlockItem(Blocks.PAR1000_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
    public static final RegistrySupplier<Item> PAR1000_PURPLE = ITEMS.register(
        "par1000_purple",
        () -> new BlockItem(Blocks.PAR1000_PURPLE_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
    public static final RegistrySupplier<Item> PAR1000_LIGHTBLUE = ITEMS.register(
        "par1000_lightblue",
        () -> new BlockItem(Blocks.PAR1000_LIGHTBLUE_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
    public static final RegistrySupplier<Item> PAR1000_WHITE = ITEMS.register(
        "par1000_white",
        () -> new BlockItem(Blocks.PAR1000_WHITE_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
    public static final RegistrySupplier<Item> x8PAR_RED = ITEMS.register(
        "x8par_red",
        () -> new BlockItem(Blocks.X8PAR_RED_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
        public static final RegistrySupplier<Item> x8PAR_GREEN = ITEMS.register(
        "x8par_green",
        () -> new BlockItem(Blocks.X8PAR_GREEN_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
            public static final RegistrySupplier<Item> x8PAR_BLUE = ITEMS.register(
        "x8par_blue",
        () -> new BlockItem(Blocks.X8PAR_BLUE_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
                public static final RegistrySupplier<Item> x8PAR_MAGENTA = ITEMS.register(
        "x8par_magenta",
        () -> new BlockItem(Blocks.X8PAR_MAGENTA_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
            public static final RegistrySupplier<Item> x8PAR_LIGHTBLUE = ITEMS.register(
        "x8par_lightblue",
        () -> new BlockItem(Blocks.X8PAR_LIGHTBLUE_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
            public static final RegistrySupplier<Item> x8PAR_YELLOW = ITEMS.register(
        "x8par_yellow",
        () -> new BlockItem(Blocks.X8PAR_YELLOW_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
                    public static final RegistrySupplier<Item> x8PAR_PURPLE = ITEMS.register(
        "x8par_purple",
        () -> new BlockItem(Blocks.X8PAR_PURPLE_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
                public static final RegistrySupplier<Item> x8PAR_WARM = ITEMS.register(
        "x8par_warm",
        () -> new BlockItem(Blocks.X8PAR_WARM_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
                public static final RegistrySupplier<Item> x8PAR_ORANGE = ITEMS.register(
        "x8par_orange",
        () -> new BlockItem(Blocks.X8PAR_ORANGE_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
                    public static final RegistrySupplier<Item> x8PAR_WHITE = ITEMS.register(
        "x8par_white",
        () -> new BlockItem(Blocks.X8PAR_WHITE_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
                        public static final RegistrySupplier<Item> PAR56_RED = ITEMS.register(
        "par56_red",
        () -> new BlockItem(Blocks.PAR56_RED_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
                            public static final RegistrySupplier<Item> PAR56_GREEN = ITEMS.register(
        "par56_green",
        () -> new BlockItem(Blocks.PAR56_GREEN_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
                        public static final RegistrySupplier<Item> PAR56_BLUE = ITEMS.register(
        "par56_blue",
        () -> new BlockItem(Blocks.PAR56_BLUE_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
                        public static final RegistrySupplier<Item> PAR56_ORANGE = ITEMS.register(
        "par56_orange",
        () -> new BlockItem(Blocks.PAR56_ORANGE_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
                            public static final RegistrySupplier<Item> PAR56_MAGENTA = ITEMS.register(
        "par56_magenta",
        () -> new BlockItem(Blocks.PAR56_MAGENTA_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
                                public static final RegistrySupplier<Item> PAR56_LIGHTBLUE = ITEMS.register(
        "par56_lightblue",
        () -> new BlockItem(Blocks.PAR56_LIGHTBLUE_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
                                    public static final RegistrySupplier<Item> PAR56_PURPLE = ITEMS.register(
        "par56_purple",
        () -> new BlockItem(Blocks.PAR56_PURPLE_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
                                        public static final RegistrySupplier<Item> PAR56_YELLOW = ITEMS.register(
        "par56_yellow",
        () -> new BlockItem(Blocks.PAR56_YELLOW_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
                                        public static final RegistrySupplier<Item> PAR56_WHITE = ITEMS.register(
        "par56_white",
        () -> new BlockItem(Blocks.PAR56_WHITE_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
                                            public static final RegistrySupplier<Item> PAR56_WARM = ITEMS.register(
        "par56_warm",
        () -> new BlockItem(Blocks.PAR56_WARM_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
    
        public static final RegistrySupplier<Item> A2X2PAR64_RED = ITEMS.register(
        "a2x2par64_red",
        () -> new BlockItem(Blocks.A2X2PAR64_RED_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
    public static final RegistrySupplier<Item> A2X2PAR64_GREEN = ITEMS.register(
            "a2x2par64_green",
            () -> new BlockItem(Blocks.A2X2PAR64_GREEN_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
    public static final RegistrySupplier<Item> A2X2PAR64_BLUE = ITEMS.register(
            "a2x2par64_blue",
            () -> new BlockItem(Blocks.A2X2PAR64_BLUE_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
    public static final RegistrySupplier<Item> A2X2PAR64_MAGENTA = ITEMS.register(
            "a2x2par64_magenta",
            () -> new BlockItem(Blocks.A2X2PAR64_MAGENTA_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
    public static final RegistrySupplier<Item> A2X2PAR64_LIGHTBLUE = ITEMS.register(
            "a2x2par64_lightblue",
            () -> new BlockItem(Blocks.A2X2PAR64_LIGHTBLUE_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
    public static final RegistrySupplier<Item> A2X2PAR64_PURPLE = ITEMS.register(
            "a2x2par64_purple",
            () -> new BlockItem(Blocks.A2X2PAR64_PURPLE_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
    public static final RegistrySupplier<Item> A2X2PAR64_ORANGE = ITEMS.register(
            "a2x2par64_orange",
            () -> new BlockItem(Blocks.A2X2PAR64_ORANGE_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
    public static final RegistrySupplier<Item> A2X2PAR64_YELLOW = ITEMS.register(
            "a2x2par64_yellow",
            () -> new BlockItem(Blocks.A2X2PAR64_YELLOW_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
    public static final RegistrySupplier<Item> A2X2PAR64_WARM = ITEMS.register(
            "a2x2par64_warm",
            () -> new BlockItem(Blocks.A2X2PAR64_WARM_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
    public static final RegistrySupplier<Item> A2X2PAR64_WHITE = ITEMS.register(
            "a2x2par64_white",
            () -> new BlockItem(Blocks.A2X2PAR64_WHITE_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
    public static final RegistrySupplier<Item> A1X1PAR64 = ITEMS.register(
            "a1x1par64",
            () -> new BlockItem(Blocks.A1X1PAR64_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
    public static final RegistrySupplier<Item> A2X8PAR64 = ITEMS.register(
            "a2x8par64",
            () -> new BlockItem(Blocks.A2X8PAR64_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
    public static final RegistrySupplier<Item> A6X3PAR64_VERTICAL = ITEMS.register(
            "a6x3par64_vertical",
            () -> new BlockItem(Blocks.A6X3PAR64_VERTICAL_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
    public static final RegistrySupplier<Item> FOLLOWSPOT = ITEMS.register(
        "followspot",
        () -> new BlockItem(Blocks.FOLLOWSPOT_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
    public static final RegistrySupplier<Item> FOLLOWSPOT_CONSOLE = ITEMS.register(
        "followspot_console",
        () -> new BlockItem(Blocks.FOLLOWSPOT_CONSOLE_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    ); 
    public static final RegistrySupplier<Item> BIGSCROLLER = ITEMS.register(
        "bigscroller",
        () -> new BlockItem(Blocks.BIGSCROLLER_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
        public static final RegistrySupplier<Item> HORIZONTALSCROLLER = ITEMS.register(
        "horizontalscroller",
        () -> new BlockItem(Blocks.HORIZONTALSCROLLER_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
            public static final RegistrySupplier<Item> VERTICALSCROLLER = ITEMS.register(
        "verticalscroller",
        () -> new BlockItem(Blocks.VERTICALSCROLLER_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
    public static final RegistrySupplier<Item> LASER_MIRROR = ITEMS.register(
            "laser_mirror",
            () -> new BlockItem(Blocks.LASER_MIRROR_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );
    public static final RegistrySupplier<Item> MOVING_JET = ITEMS.register(
            "moving_jet",
            () -> new BlockItem(Blocks.MOVING_JET_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.MISC_TAB))
    );
    public static final RegistrySupplier<Item> WATER_JET_THIN = ITEMS.register(
            "water_jet_thin",
            () -> new BlockItem(Blocks.WATER_JET_THIN_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.MISC_TAB))
    );
    public static final RegistrySupplier<Item> WATER_JET_SPREAD = ITEMS.register(
            "water_jet_spread",
            () -> new BlockItem(Blocks.WATER_JET_SPREAD_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.MISC_TAB))
    );
    public static final RegistrySupplier<Item> WATER_JET_BIG = ITEMS.register(
            "water_jet_big",
            () -> new BlockItem(Blocks.WATER_JET_BIG_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.MISC_TAB))
    );
    public static final RegistrySupplier<Item> WATER_JET_CENTRAL = ITEMS.register(
            "water_jet_central",
            () -> new BlockItem(Blocks.WATER_JET_CENTRAL_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.MISC_TAB))
    );
    public static final RegistrySupplier<Item> SPINNER = ITEMS.register(
            "spinner",
            () -> new BlockItem(Blocks.SPINNER_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.MISC_TAB))
    );
    public static final RegistrySupplier<Item> ORGANPIPES = ITEMS.register(
            "organpipes",
            () -> new BlockItem(Blocks.ORGANPIPES_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.MISC_TAB))
    );
    public static final RegistrySupplier<Item> ORGANPIPES_INV = ITEMS.register(
            "organpipes_inv",
            () -> new BlockItem(Blocks.ORGANPIPES_INV_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.MISC_TAB))
    );
    public static final RegistrySupplier<Item> WATER_JET_CONE = ITEMS.register(
            "water_jet_cone",
            () -> new BlockItem(Blocks.WATER_JET_CONE_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.MISC_TAB))
    );
    public static final RegistrySupplier<Item> WATER_JET_BLOOM = ITEMS.register(
            "water_jet_bloom",
            () -> new BlockItem(Blocks.WATER_JET_BLOOM_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.MISC_TAB))
    );
    public static final RegistrySupplier<Item> WATER_JET_FOG = ITEMS.register(
            "water_jet_fog",
            () -> new BlockItem(Blocks.WATER_JET_FOG_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.MISC_TAB))
    );
    public static final RegistrySupplier<Item> FAN_WATER_JET = ITEMS.register(
            "fan_water_jet",
            () -> new BlockItem(Blocks.FAN_WATER_JET_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.MISC_TAB))
    );
    public static final RegistrySupplier<Item> CAKE_WATER_JET = ITEMS.register(
            "cake_water_jet",
            () -> new BlockItem(Blocks.CAKE_WATER_JET_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.MISC_TAB))
    );
    public static final RegistrySupplier<Item> VASE_WATER_JET = ITEMS.register(
            "vase_water_jet",
            () -> new BlockItem(Blocks.VASE_WATER_JET_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.MISC_TAB))
    );
    public static final RegistrySupplier<Item> WALTZES_WATER_JET = ITEMS.register(
            "waltzes_water_jet",
            () -> new BlockItem(Blocks.WALTZES_WATER_JET_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.MISC_TAB))
    );
    public static final RegistrySupplier<Item> WALTZ_CURTAIN = ITEMS.register(
            "waltz_curtain",
            () -> new BlockItem(Blocks.WALTZ_CURTAIN_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.MISC_TAB))
    );
    public static final RegistrySupplier<Item> LED_FACADE = ITEMS.register(
            "led_facade",
            () -> new BlockItem(Blocks.LED_FACADE.get(), new Item.Properties().arch$tab(TheatricalExtraLights.TAB))
    );

    public static final RegistrySupplier<Item> DWT_PANEL = ITEMS.register(
            "dwt_panel",
            () -> new BlockItem(Blocks.DWT_PANEL_BLOCK.get(), new Item.Properties().arch$tab(TheatricalExtraLights.MISC_TAB))
    );
    public static void init(){
        ITEMS.register();
    }
}
