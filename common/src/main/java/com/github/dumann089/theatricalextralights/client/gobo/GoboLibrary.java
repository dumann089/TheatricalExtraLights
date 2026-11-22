package com.github.dumann089.theatricalextralights.client.gobo;

import net.minecraft.resources.ResourceLocation;

public enum GoboLibrary {

    VL2C(
            new GoboEntry("generic_2/open", FakeVolumetricBeamPattern.SINGLE),
            new GoboEntry("generic_2/bubbles_2", FakeVolumetricBeamPattern.SCATTER_NATURE),
            new GoboEntry("generic_2/radial_lines", FakeVolumetricBeamPattern.RING_8),
            new GoboEntry("generic_3/gobo56", FakeVolumetricBeamPattern.SCATTER_NATURE),
            new GoboEntry("generic_2/ring_dots", FakeVolumetricBeamPattern.DOUBLE_RING),
            new GoboEntry("generic_3/gobo52", FakeVolumetricBeamPattern.SCATTER_NATURE),
            new GoboEntry("generic_2/mixed_squares", FakeVolumetricBeamPattern.CROSS_5),
            new GoboEntry("generic_3/gobo53", FakeVolumetricBeamPattern.SCATTER_NATURE),
            new GoboEntry("generic_2/lines_vertical", FakeVolumetricBeamPattern.FAN_7),
            new GoboEntry("generic_3/gobo1", FakeVolumetricBeamPattern.RING_8)
    ),

    SCAN(
            new GoboEntry("generic_1/open", FakeVolumetricBeamPattern.SINGLE),
            new GoboEntry("generic_1/gobo7", FakeVolumetricBeamPattern.RING_8),
            new GoboEntry("generic_3/gobo13", FakeVolumetricBeamPattern.SPIRAL_6),
            new GoboEntry("generic_1/gobo40", FakeVolumetricBeamPattern.DOUBLE_RING),
            new GoboEntry("generic_1/gobo25", FakeVolumetricBeamPattern.CROSS_5),
            new GoboEntry("generic_1/gobo15", FakeVolumetricBeamPattern.FAN_7),
            new GoboEntry("generic_3/gobo6", FakeVolumetricBeamPattern.SCATTER_NATURE),
            new GoboEntry("generic_3/gobo40", FakeVolumetricBeamPattern.RING_8),
            new GoboEntry("generic_1/gobo41", FakeVolumetricBeamPattern.SPIRAL_6),
            new GoboEntry("generic_1/gobo19", FakeVolumetricBeamPattern.CROSS_5)
    ),

    MINISCAN(
            new GoboEntry("generic_1/open", FakeVolumetricBeamPattern.SINGLE),
            new GoboEntry("generic_1/gobo9", FakeVolumetricBeamPattern.RING_8),
            new GoboEntry("generic_1/gobo16", FakeVolumetricBeamPattern.SPIRAL_6),
            new GoboEntry("generic_1/gobo17", FakeVolumetricBeamPattern.DOUBLE_RING),
            new GoboEntry("generic_1/gobo21", FakeVolumetricBeamPattern.CROSS_5),
            new GoboEntry("generic_1/gobo12", FakeVolumetricBeamPattern.FAN_7),
            new GoboEntry("generic_1/gobo26", FakeVolumetricBeamPattern.SCATTER_NATURE),
            new GoboEntry("generic_1/gobo40", FakeVolumetricBeamPattern.RING_8),
            new GoboEntry("generic_1/gobo39", FakeVolumetricBeamPattern.SPIRAL_6),
            new GoboEntry("generic_1/gobo30", FakeVolumetricBeamPattern.CROSS_5)
    ),

    MINISPOT(
            new GoboEntry("generic_2/open", FakeVolumetricBeamPattern.SINGLE),
            new GoboEntry("generic_2/ring", FakeVolumetricBeamPattern.RING_8),
            new GoboEntry("generic_2/breakups", FakeVolumetricBeamPattern.SPIRAL_6),
            new GoboEntry("generic_2/dots_radial", FakeVolumetricBeamPattern.DOUBLE_RING),
            new GoboEntry("generic_2/flowers", FakeVolumetricBeamPattern.CROSS_5),
            new GoboEntry("generic_2/stains", FakeVolumetricBeamPattern.FAN_7),
            new GoboEntry("generic_2/bubbles", FakeVolumetricBeamPattern.SCATTER_NATURE),
            new GoboEntry("generic_2/spiral", FakeVolumetricBeamPattern.RING_8)
    ),

    VL6C(
            new GoboEntry("generic_3/open", FakeVolumetricBeamPattern.SINGLE),
            new GoboEntry("generic_3/gobo9", FakeVolumetricBeamPattern.SCATTER_NATURE),
            new GoboEntry("generic_3/gobo19", FakeVolumetricBeamPattern.RING_8),
            new GoboEntry("generic_3/gobo14", FakeVolumetricBeamPattern.SCATTER_NATURE),
            new GoboEntry("generic_3/gobo46", FakeVolumetricBeamPattern.DOUBLE_RING),
            new GoboEntry("generic_3/gobo56", FakeVolumetricBeamPattern.SCATTER_NATURE),
            new GoboEntry("generic_3/gobo11", FakeVolumetricBeamPattern.CROSS_5),
            new GoboEntry("generic_3/gobo1", FakeVolumetricBeamPattern.SCATTER_NATURE),
            new GoboEntry("generic_3/gobo26", FakeVolumetricBeamPattern.FAN_7),
            new GoboEntry("generic_3/gobo38", FakeVolumetricBeamPattern.RING_8)
    ),

    IRIS700(
            new GoboEntry("generic_3/open", FakeVolumetricBeamPattern.SINGLE),
            new GoboEntry("generic_3/gobo9", FakeVolumetricBeamPattern.SCATTER_NATURE),
            new GoboEntry("generic_3/gobo21", FakeVolumetricBeamPattern.RING_8),
            new GoboEntry("generic_3/gobo32", FakeVolumetricBeamPattern.SCATTER_NATURE),
            new GoboEntry("generic_3/gobo27", FakeVolumetricBeamPattern.DOUBLE_RING),
            new GoboEntry("generic_3/gobo36", FakeVolumetricBeamPattern.SCATTER_NATURE),
            new GoboEntry("generic_3/gobo18", FakeVolumetricBeamPattern.CROSS_5),
            new GoboEntry("generic_3/gobo3", FakeVolumetricBeamPattern.SCATTER_NATURE),
            new GoboEntry("generic_3/gobo51", FakeVolumetricBeamPattern.FAN_7),
            new GoboEntry("generic_3/gobo50", FakeVolumetricBeamPattern.RING_8),
            new GoboEntry("generic_3/gobo36", FakeVolumetricBeamPattern.SCATTER_NATURE),
            new GoboEntry("generic_3/gobo34", FakeVolumetricBeamPattern.CROSS_5),
            new GoboEntry("generic_3/gobo43", FakeVolumetricBeamPattern.SCATTER_NATURE),
            new GoboEntry("generic_3/gobo31", FakeVolumetricBeamPattern.DOUBLE_RING),
            new GoboEntry("generic_3/gobo7", FakeVolumetricBeamPattern.RING_8)
    ),

    PROFILE3(
            new GoboEntry("generic_3/open", FakeVolumetricBeamPattern.SINGLE),
            new GoboEntry("generic_1/gobo15", FakeVolumetricBeamPattern.SCATTER_NATURE),
            new GoboEntry("generic_1/gobo13", FakeVolumetricBeamPattern.RING_8),
            new GoboEntry("generic_1/gobo21", FakeVolumetricBeamPattern.SCATTER_NATURE),
            new GoboEntry("generic_1/gobo29", FakeVolumetricBeamPattern.DOUBLE_RING),
            new GoboEntry("generic_1/gobo37", FakeVolumetricBeamPattern.SCATTER_NATURE),
            new GoboEntry("generic_1/gobo18", FakeVolumetricBeamPattern.CROSS_5),
            new GoboEntry("generic_1/gobo14", FakeVolumetricBeamPattern.SCATTER_NATURE),
            new GoboEntry("generic_1/gobo10", FakeVolumetricBeamPattern.FAN_7),
            new GoboEntry("generic_1/gobo22", FakeVolumetricBeamPattern.RING_8),
            new GoboEntry("generic_1/gobo6", FakeVolumetricBeamPattern.SCATTER_NATURE),
            new GoboEntry("generic_1/gobo31", FakeVolumetricBeamPattern.CROSS_5),
            new GoboEntry("generic_1/gobo7", FakeVolumetricBeamPattern.SCATTER_NATURE),
            new GoboEntry("generic_1/gobo27", FakeVolumetricBeamPattern.DOUBLE_RING),
            new GoboEntry("generic_1/gobo19", FakeVolumetricBeamPattern.RING_8)
    ),

    PROSPOT(
            new GoboEntry("generic_3/open", FakeVolumetricBeamPattern.SINGLE),
            new GoboEntry("generic_3/gobo3", FakeVolumetricBeamPattern.SCATTER_NATURE),
            new GoboEntry("generic_3/gobo12", FakeVolumetricBeamPattern.RING_8),
            new GoboEntry("generic_3/gobo20", FakeVolumetricBeamPattern.SCATTER_NATURE),
            new GoboEntry("generic_3/gobo21", FakeVolumetricBeamPattern.DOUBLE_RING),
            new GoboEntry("generic_3/gobo15", FakeVolumetricBeamPattern.SCATTER_NATURE),
            new GoboEntry("generic_3/gobo23", FakeVolumetricBeamPattern.CROSS_5),
            new GoboEntry("generic_3/gobo39", FakeVolumetricBeamPattern.SCATTER_NATURE),
            new GoboEntry("generic_3/gobo33", FakeVolumetricBeamPattern.FAN_7),
            new GoboEntry("generic_3/gobo38", FakeVolumetricBeamPattern.RING_8),
            new GoboEntry("generic_3/gobo45", FakeVolumetricBeamPattern.SCATTER_NATURE),
            new GoboEntry("generic_3/gobo42", FakeVolumetricBeamPattern.CROSS_5),
            new GoboEntry("generic_3/gobo10", FakeVolumetricBeamPattern.SCATTER_NATURE),
            new GoboEntry("generic_3/gobo32", FakeVolumetricBeamPattern.DOUBLE_RING),
            new GoboEntry("generic_3/gobo57", FakeVolumetricBeamPattern.RING_8)
    ),

    SpotXtreme(
            new GoboEntry("generic_1/open", FakeVolumetricBeamPattern.SINGLE),
            new GoboEntry("generic_1/gobo35", FakeVolumetricBeamPattern.RING_8),
            new GoboEntry("generic_1/gobo39", FakeVolumetricBeamPattern.SPIRAL_6),
            new GoboEntry("generic_1/gobo3", FakeVolumetricBeamPattern.DOUBLE_RING),
            new GoboEntry("generic_1/gobo2", FakeVolumetricBeamPattern.CROSS_5),
            new GoboEntry("generic_1/gobo28", FakeVolumetricBeamPattern.FAN_7),
            new GoboEntry("generic_1/gobo32", FakeVolumetricBeamPattern.SCATTER_NATURE),
            new GoboEntry("generic_1/gobo23", FakeVolumetricBeamPattern.RING_8),
            new GoboEntry("generic_1/gobo24", FakeVolumetricBeamPattern.DOUBLE_RING),
            new GoboEntry("generic_1/gobo18", FakeVolumetricBeamPattern.SPIRAL_6)
    ),

    MACVIP(
            new GoboEntry("generic_2/open", FakeVolumetricBeamPattern.SINGLE)
    ),

    WASH(
            new GoboEntry("generic_1/wash", FakeVolumetricBeamPattern.SINGLE)
    ),

    MOVINGBAR(
            new GoboEntry("generic_2/open", FakeVolumetricBeamPattern.SINGLE)
    ),

    // Linear color-gel wheel used only by the PARScroller.
    // Each texture is a solid RGBA color tile; replace these PNGs to customize the wheel.
    PARSCROLLER_COLORWHEEL(
            new GoboEntry("parscroller/colorwheel/00_white", FakeVolumetricBeamPattern.SINGLE, 0xFFFFFF),
            new GoboEntry("parscroller/colorwheel/01_red", FakeVolumetricBeamPattern.SINGLE, 0xFF0000),
            new GoboEntry("parscroller/colorwheel/02_orange", FakeVolumetricBeamPattern.SINGLE, 0xFF7000),
            new GoboEntry("parscroller/colorwheel/03_yellow", FakeVolumetricBeamPattern.SINGLE, 0xFFEE00),
            new GoboEntry("parscroller/colorwheel/04_green", FakeVolumetricBeamPattern.SINGLE, 0x00FF28),
            new GoboEntry("parscroller/colorwheel/05_cyan", FakeVolumetricBeamPattern.SINGLE, 0x00E6FF),
            new GoboEntry("parscroller/colorwheel/06_blue", FakeVolumetricBeamPattern.SINGLE, 0x0040FF),
            new GoboEntry("parscroller/colorwheel/07_magenta", FakeVolumetricBeamPattern.SINGLE, 0xFF00DC)
    ),

    PAR1000(
            new GoboEntry("generic_2/ring", FakeVolumetricBeamPattern.RING_8)
    );

    public record GoboEntry(ResourceLocation texture, FakeVolumetricBeamPattern pattern, int color) {
        public GoboEntry(ResourceLocation texture, FakeVolumetricBeamPattern pattern) {
            this(texture, pattern, 0xFFFFFF);
        }

        public GoboEntry(String path, FakeVolumetricBeamPattern pattern) {
            this(path, pattern, 0xFFFFFF);
        }

        public GoboEntry(String path, FakeVolumetricBeamPattern pattern, int color) {
            this(
                    new ResourceLocation("theatricalextralights", "textures/gobos/" + path + ".png"),
                    pattern,
                    color & 0xFFFFFF
            );
        }
    }

    private final GoboEntry[] entries;

    GoboLibrary(GoboEntry... entries) {
        this.entries = entries;
    }

    public ResourceLocation getTexture(int slot) {
        if (slot < 0 || slot >= entries.length) return entries[0].texture();
        return entries[slot].texture();
    }

    public FakeVolumetricBeamPattern getPattern(int slot) {
        if (slot < 0 || slot >= entries.length) return FakeVolumetricBeamPattern.SINGLE;
        return entries[slot].pattern();
    }

    public int getColor(int slot) {
        if (slot < 0 || slot >= entries.length) return 0xFFFFFF;
        return entries[slot].color() & 0xFFFFFF;
    }

    public int getSlotCount() {
        return entries.length;
    }
}
