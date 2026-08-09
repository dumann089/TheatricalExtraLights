package com.github.dumann089.theatricalextralights.firework;

import java.util.Arrays;
import java.util.function.Supplier;

public enum FireworkPreset {
    RED_COMET("firework_red_comet", "Red Comet", BurstPatterns.Comet::new, 0xFF4A4A, 0xFFB3B3),
    BLUE_COMET("firework_blue_comet", "Blue Comet", BurstPatterns.Comet::new, 0x4D8DFF, 0xB8D3FF),
    GREEN_COMET("firework_green_comet", "Green Comet", BurstPatterns.Comet::new, 0x55FF7A, 0xC6FFD3),
    GOLD_COMET("firework_gold_comet", "Gold Comet", BurstPatterns.Comet::new, 0xFFC451, 0xFFE9AE),
    PYRO_FAN_COMET("pyro_fan_comet", "Pyro Fan Comet", BurstPatterns.PyroFanComet::new, 0xFFC451, 0xFFE9AE),
    GOLD_BELL_COMET("firework_gold_bell_comet", "Gold Bell Comet", BurstPatterns.BellComet::new, 0xFFD36E, 0xFFF3C2),
    RED_PEONY("firework_red_peony", "Red Peony", BurstPatterns.Peony::new, 0xFF5B5B, 0xFFD1D1),
    BLUE_PEONY("firework_blue_peony", "Blue Peony", BurstPatterns.Peony::new, 0x4D8DFF, 0xB8D3FF),
    GREEN_PEONY("firework_green_peony", "Green Peony", BurstPatterns.Peony::new, 0x55FF7A, 0xC6FFD3),
    GOLD_PEONY("firework_gold_peony", "Gold Peony", BurstPatterns.Peony::new, 0xFFC451, 0xFFE9AE),
    WHITE_PEONY("firework_white_peony", "White Peony", BurstPatterns.Peony::new, 0xFFFFFF, 0xFFF5C9),
    AMBER_PEONY("firework_amber_peony", "Amber Peony", BurstPatterns.Peony::new, 0xFFA040, 0xFFD9A0),
    VIOLET_PEONY("firework_violet_peony", "Violet Peony", BurstPatterns.Peony::new, 0xC77BFF, 0xE0BBFF),
    WHITE_STROBE_BURST("firework_white_strobe_burst", "White Strobe Burst", BurstPatterns.Strobe::new, 0xFFFFFF, 0xFFF5C9),
    WHITE_AERIAL_STROBE("firework_white_aerial_strobe", "White Aerial Strobe", BurstPatterns.AerialStrobe::new, 0xFFFFFF, 0xFFF5C9),
    RED_AERIAL_STROBE("firework_red_aerial_strobe", "Red Aerial Strobe", BurstPatterns.AerialStrobe::new, 0xFF4A4A, 0xFFB3B3),
    BLUE_AERIAL_STROBE("firework_blue_aerial_strobe", "Blue Aerial Strobe", BurstPatterns.AerialStrobe::new, 0x4D8DFF, 0xB8D3FF),
    GREEN_AERIAL_STROBE("firework_green_aerial_strobe", "Green Aerial Strobe", BurstPatterns.AerialStrobe::new, 0x55FF7A, 0xC6FFD3),
    GOLD_AERIAL_STROBE("firework_gold_aerial_strobe", "Gold Aerial Strobe", BurstPatterns.AerialStrobe::new, 0xFFC451, 0xFFE9AE),
    AMBER_AERIAL_STROBE("firework_amber_aerial_strobe", "Amber Aerial Strobe", BurstPatterns.AerialStrobe::new, 0xFFA040, 0xFFD9A0),
    VIOLET_AERIAL_STROBE("firework_violet_aerial_strobe", "Violet Aerial Strobe", BurstPatterns.AerialStrobe::new, 0xC77BFF, 0xE0BBFF),
    SILVER_AERIAL_STROBE("firework_silver_aerial_strobe", "Silver Aerial Strobe", BurstPatterns.AerialStrobe::new, 0xCFD8E5, 0xE6EFFF, 0xFFFFFF),
    GOLD_WILLOW("firework_gold_willow", "Gold Willow", BurstPatterns.Willow::new, 0xFFCD63, 0xFFF1B8),
    RED_WILLOW("firework_red_willow", "Red Willow", BurstPatterns.Willow::new, 0xFF5B5B, 0xFFD1D1),
    BLUE_WILLOW("firework_blue_willow", "Blue Willow", BurstPatterns.Willow::new, 0x4D8DFF, 0xB8D3FF),
    GREEN_WILLOW("firework_green_willow", "Green Willow", BurstPatterns.Willow::new, 0x55FF7A, 0xC6FFD3),
    WHITE_WILLOW("firework_white_willow", "White Willow", BurstPatterns.Willow::new, 0xFFFFFF, 0xFFF5C9),
    AMBER_WILLOW("firework_amber_willow", "Amber Willow", BurstPatterns.Willow::new, 0xFFA040, 0xFFD9A0),
    VIOLET_WILLOW("firework_violet_willow", "Violet Willow", BurstPatterns.Willow::new, 0xC77BFF, 0xE0BBFF),
    MULTICOLOR_BURST("firework_multicolor_burst", "Multicolor Burst", BurstPatterns.Multicolor::new, 0xFF6BE1, 0xFFE96D, 0x6B8CFF, 0x7BFF84, 0xFF6B6B),
    PALM_GOLD("firework_palm_gold", "Gold Palm", BurstPatterns.Palm::new, 0xFFC451, 0xFFE9AE),
    CHRYSANTHEMUM_BLUE("firework_chrysanthemum_blue", "Blue Chrysanthemum", BurstPatterns.Chrysanthemum::new, 0x4D8DFF, 0xB8D3FF, 0xE0EEFF),
    CHRYSANTHEMUM_RED("firework_chrysanthemum_red", "Red Chrysanthemum", BurstPatterns.Chrysanthemum::new, 0xFF5B5B, 0xFFD1D1, 0xFFE9E9),
    CHRYSANTHEMUM_GREEN("firework_chrysanthemum_green", "Green Chrysanthemum", BurstPatterns.Chrysanthemum::new, 0x55FF7A, 0xC6FFD3, 0xE9FFEE),
    CHRYSANTHEMUM_GOLD("firework_chrysanthemum_gold", "Gold Chrysanthemum", BurstPatterns.Chrysanthemum::new, 0xFFC451, 0xFFE9AE, 0xFFF6D9),
    CHRYSANTHEMUM_WHITE("firework_chrysanthemum_white", "White Chrysanthemum", BurstPatterns.Chrysanthemum::new, 0xFFFFFF, 0xFFF5C9, 0xFFFFFF),
    CHRYSANTHEMUM_AMBER("firework_chrysanthemum_amber", "Amber Chrysanthemum", BurstPatterns.Chrysanthemum::new, 0xFFA040, 0xFFD9A0, 0xFFEAC9),
    CHRYSANTHEMUM_VIOLET("firework_chrysanthemum_violet", "Violet Chrysanthemum", BurstPatterns.Chrysanthemum::new, 0xC77BFF, 0xE0BBFF, 0xF1DCFF),
    HORSETAIL_SILVER("firework_horsetail_silver", "Silver Horsetail", BurstPatterns.Horsetail::new, 0xE6EFFF, 0xCFD8E5, 0xFFFFFF),
    RING_RED("firework_ring_red", "Red Ring", BurstPatterns.Ring::new, 0xFF5B5B, 0xFFB3B3),
    SPINNER_GOLD("firework_spinner_gold", "Gold Spinner", BurstPatterns.Spinner::new, 0xFFC451, 0xFFE9AE),
    CROSSETTE_RED("firework_crossette_red", "Red Crossette", BurstPatterns.Crossette::new, 0xFF5B5B, 0xFFD1D1),
    CROSSETTE_BLUE("firework_crossette_blue", "Blue Crossette", BurstPatterns.Crossette::new, 0x4D8DFF, 0xB8D3FF),
    CROSSETTE_GREEN("firework_crossette_green", "Green Crossette", BurstPatterns.Crossette::new, 0x55FF7A, 0xC6FFD3),
    CROSSETTE_GOLD("firework_crossette_gold", "Gold Crossette", BurstPatterns.Crossette::new, 0xFFC451, 0xFFE9AE),
    CROSSETTE_WHITE("firework_crossette_white", "White Crossette", BurstPatterns.Crossette::new, 0xFFFFFF, 0xFFF5C9),
    CROSSETTE_AMBER("firework_crossette_amber", "Amber Crossette", BurstPatterns.Crossette::new, 0xFFA040, 0xFFD9A0),
    CROSSETTE_VIOLET("firework_crossette_violet", "Violet Crossette", BurstPatterns.Crossette::new, 0xC77BFF, 0xE0BBFF),
    SILVER_JET(
            "firework_silver_jet",
            "Silver Jet",
            BurstPatterns.SilverJet::new,
            0xFFE39A,
            0xFFF0C2
    ),
    MINE_BLUE(
            "firework_mine_blue",
            "Blue Mine",
            BurstPatterns.Mine::new,
            0x1E6BFF,
            0x3E8CFF,
            0x66A8FF
    ),
    MINE_RED(
            "firework_mine_red",
            "Red Mine",
            BurstPatterns.Mine::new,
            0xFF2A2A,
            0xFF4A4A,
            0xFF7070
    ),
    MINE_GREEN(
            "firework_mine_green",
            "Green Mine",
            BurstPatterns.Mine::new,
            0x00D84A,
            0x33E66A,
            0x66F08D
    ),
    MINE_GOLD(
            "firework_mine_gold",
            "Gold Mine",
            BurstPatterns.Mine::new,
            0xFFB000,
            0xFFC533,
            0xFFD866
    ),
    MINE_WHITE(
            "firework_mine_white",
            "White Mine",
            BurstPatterns.Mine::new,
            0xF8F8F8,
            0xFFFFFF,
            0xFFF2D0
    ),
    MINE_AMBER(
            "firework_mine_amber",
            "Amber Mine",
            BurstPatterns.Mine::new,
            0xFF7A00,
            0xFF9A26,
            0xFFB24A
    ),
    MINE_VIOLET(
            "firework_mine_violet",
            "Violet Mine",
            BurstPatterns.Mine::new,
            0x8A2EFF,
            0xA54DFF,
            0xC070FF
    ),
    SPIDER_WHITE("firework_spider_white", "White Spider", BurstPatterns.Spider::new, 0xFFFFFF, 0xFFF5C9),
    DIADEM_BLUE("firework_diadem_blue", "Blue Diadem", BurstPatterns.Diadem::new, 0x4D8DFF, 0xB8D3FF),
    SALUTE_WHITE("firework_salute_white", "White Salute", BurstPatterns.Salute::new, 0xFFFFFF, 0xFFF5C9),
    HEART_PINK("firework_heart_pink", "Pink Heart", BurstPatterns.Heart::new, 0xFF66AA, 0xFFCCDD),
    DOUBLE_BURST_PURPLE("firework_double_burst_purple", "Purple Double Burst", BurstPatterns.DoubleBurst::new, 0xC77BFF, 0xFF66AA, 0xE0BBFF),
    WHISTLER_SILVER("firework_whistler_silver", "Silver Whistler", BurstPatterns.Whistler::new, 0xCFD8E5, 0xE6EFFF, 0xFFFFFF),
    GOLD_LONG_COMET("firework_gold_long_comet", "Gold Long Comet", BurstPatterns.LongTrailComet::new, 0xFFC451, 0xFFE9AE, 0xFF8C20),
    RED_LONG_COMET("firework_red_long_comet", "Red Long Comet", BurstPatterns.LongTrailComet::new, 0xFF4A4A, 0xFFB3B3, 0xFF3030),
    BLUE_LONG_COMET("firework_blue_long_comet", "Blue Long Comet", BurstPatterns.LongTrailComet::new, 0x4D8DFF, 0xB8D3FF, 0x2A5FD9),
    GREEN_LONG_COMET("firework_green_long_comet", "Green Long Comet", BurstPatterns.LongTrailComet::new, 0x55FF7A, 0xC6FFD3, 0x2ECC55),
    SILVER_LONG_COMET("firework_silver_long_comet", "Silver Long Comet", BurstPatterns.LongTrailComet::new, 0xCFD8E5, 0xE6EFFF, 0xFFFFFF),
    LIME_DAYTIME_POWDER("firework_daytime_powder_lime", "Lime Daytime Powder", BurstPatterns.DaytimePowder::new, 0xB8FF00, 0xD4FF66),
    MAGENTA_DAYTIME_POWDER("firework_daytime_powder_magenta", "Magenta Daytime Powder", BurstPatterns.DaytimePowder::new, 0xFF33CC, 0xFF99E6),
    YELLOW_DAYTIME_POWDER("firework_daytime_powder_yellow", "Yellow Daytime Powder", BurstPatterns.DaytimePowder::new, 0xFFE600, 0xFFFF66),
    ORANGE_DAYTIME_POWDER("firework_daytime_powder_orange", "Orange Daytime Powder", BurstPatterns.DaytimePowder::new, 0xFF6600, 0xFFAA44),
    RED_DAYTIME_POWDER("firework_daytime_powder_red", "Red Daytime Powder", BurstPatterns.DaytimePowder::new, 0xFF2244, 0xFF6688),
    BLUE_DAYTIME_POWDER("firework_daytime_powder_blue", "Blue Daytime Powder", BurstPatterns.DaytimePowder::new, 0x0066FF, 0x66AAFF),
    RAINBOW_DAYTIME_POWDER_FAN("firework_daytime_powder_rainbow", "Rainbow Daytime Powder Fan", BurstPatterns.DaytimePowderFan::new, 0xB8FF00, 0xFFE600, 0xFF6600, 0xFF2244, 0xAA44FF, 0x0066FF, 0x00DDFF, 0x55FF7A),
    FIREWORK_MORTAR_HIT(
            "firework_mortar_hit",
            "Mortar Hit Effect",
            BurstPatterns.MortarHit::new,
            0xFFFFFF,
            0xFFF8D6,
            0xFFE88A,
            0xFFD04A,
            0xFFB000,
            0xFF9800,
            0xFF6A00,
            0xFF4500
    ),
    FIREWORK_FLAME_PROJECTOR(
            "firework_flame_projector",
            "Flame Projector",
            BurstPatterns.FlameProjector::new,
            0xFF8A00,
            0xFF5A00
    );

    private final String blockId;
    private final String displayName;
    private final BurstPattern pattern;
    private final int[] colors;

    FireworkPreset(String blockId, String displayName, Supplier<? extends BurstPattern> patternSupplier, int... colors) {
        this.blockId = blockId;
        this.displayName = displayName;
        this.pattern = patternSupplier.get();
        this.colors = colors;
    }

    public String getBlockId() {
        return blockId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public BurstPattern getPattern() {
        return pattern;
    }

    public int getLaunchColor() {
        return colors[0];
    }

    public int[] getColors() {
        return Arrays.copyOf(colors, colors.length);
    }

    public static FireworkPreset byBlockId(String id) {
        for (FireworkPreset preset : values()) {
            if (preset.blockId.equals(id)) {
                return preset;
            }
        }
        return RED_COMET;
    }

    /** Sélection DMX canal Effet (0–255) → preset parmi tous les types d'explosion. */
    public static FireworkPreset byDmxIndex(int dmx) {
        FireworkPreset[] all = values();
        if (all.length == 0) {
            return RED_COMET;
        }
        int idx = (int) (dmx / 255.0f * all.length);
        if (idx >= all.length) {
            idx = all.length - 1;
        }
        return all[idx];
    }
}
