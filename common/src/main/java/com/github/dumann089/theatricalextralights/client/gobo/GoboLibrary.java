package com.github.dumann089.theatricalextralights.client.gobo;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public enum GoboLibrary {

    VL2C(
            new GoboEntry("generic_2/open",          FakeVolumetricBeamPattern.SINGLE),
            new GoboEntry("generic_2/bubbles_2",     FakeVolumetricBeamPattern.SCATTER_NATURE),
            new GoboEntry("generic_2/radial_lines",  FakeVolumetricBeamPattern.RING_8),
            new GoboEntry("generic_3/gobo56",        FakeVolumetricBeamPattern.SCATTER_NATURE),
            new GoboEntry("generic_2/ring_dots",     FakeVolumetricBeamPattern.DOUBLE_RING),
            new GoboEntry("generic_3/gobo52",        FakeVolumetricBeamPattern.SCATTER_NATURE),
            new GoboEntry("generic_2/mixed_squares", FakeVolumetricBeamPattern.CROSS_5),
            new GoboEntry("generic_3/gobo53",        FakeVolumetricBeamPattern.SCATTER_NATURE),
            new GoboEntry("generic_2/lines_vertical",FakeVolumetricBeamPattern.FAN_7),
            new GoboEntry("generic_3/gobo1",         FakeVolumetricBeamPattern.RING_8)
    ),

    SCAN(
            new GoboEntry("generic_1/open",   FakeVolumetricBeamPattern.SINGLE),
            new GoboEntry("generic_1/gobo7",  FakeVolumetricBeamPattern.RING_8),
            new GoboEntry("generic_3/gobo13", FakeVolumetricBeamPattern.SPIRAL_6),
            new GoboEntry("generic_1/gobo40", FakeVolumetricBeamPattern.DOUBLE_RING),
            new GoboEntry("generic_1/gobo25", FakeVolumetricBeamPattern.CROSS_5),
            new GoboEntry("generic_1/gobo15", FakeVolumetricBeamPattern.FAN_7),
            new GoboEntry("generic_3/gobo6",  FakeVolumetricBeamPattern.SCATTER_NATURE),
            new GoboEntry("generic_3/gobo40", FakeVolumetricBeamPattern.RING_8),
            new GoboEntry("generic_1/gobo41", FakeVolumetricBeamPattern.SPIRAL_6),
            new GoboEntry("generic_1/gobo19", FakeVolumetricBeamPattern.CROSS_5)
    ),

    MINISCAN(
            new GoboEntry("generic_1/open",   FakeVolumetricBeamPattern.SINGLE),
            new GoboEntry("generic_1/gobo9",  FakeVolumetricBeamPattern.RING_8),
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
            new GoboEntry("generic_2/open",        FakeVolumetricBeamPattern.SINGLE),
            new GoboEntry("generic_2/ring",        FakeVolumetricBeamPattern.RING_8),
            new GoboEntry("generic_2/breakups",    FakeVolumetricBeamPattern.SPIRAL_6),
            new GoboEntry("generic_2/dots_radial", FakeVolumetricBeamPattern.DOUBLE_RING),
            new GoboEntry("generic_2/flowers",     FakeVolumetricBeamPattern.CROSS_5),
            new GoboEntry("generic_2/stains",      FakeVolumetricBeamPattern.FAN_7),
            new GoboEntry("generic_2/bubbles",     FakeVolumetricBeamPattern.SCATTER_NATURE),
            new GoboEntry("generic_2/spiral",      FakeVolumetricBeamPattern.RING_8)
    ),

    VL6C(
            new GoboEntry("generic_3/open",   FakeVolumetricBeamPattern.SINGLE),
            new GoboEntry("generic_3/gobo9",  FakeVolumetricBeamPattern.SCATTER_NATURE),
            new GoboEntry("generic_3/gobo19", FakeVolumetricBeamPattern.RING_8),
            new GoboEntry("generic_3/gobo14", FakeVolumetricBeamPattern.SCATTER_NATURE),
            new GoboEntry("generic_3/gobo46", FakeVolumetricBeamPattern.DOUBLE_RING),
            new GoboEntry("generic_3/gobo56", FakeVolumetricBeamPattern.SCATTER_NATURE),
            new GoboEntry("generic_3/gobo11", FakeVolumetricBeamPattern.CROSS_5),
            new GoboEntry("generic_3/gobo1",  FakeVolumetricBeamPattern.SCATTER_NATURE),
            new GoboEntry("generic_3/gobo26", FakeVolumetricBeamPattern.FAN_7),
            new GoboEntry("generic_3/gobo38", FakeVolumetricBeamPattern.RING_8)
    ),

    IRIS700(
            new GoboEntry("generic_3/open",   FakeVolumetricBeamPattern.SINGLE),
            new GoboEntry("generic_3/gobo9",  FakeVolumetricBeamPattern.SCATTER_NATURE),
            new GoboEntry("generic_3/gobo21", FakeVolumetricBeamPattern.RING_8),
            new GoboEntry("generic_3/gobo32", FakeVolumetricBeamPattern.SCATTER_NATURE),
            new GoboEntry("generic_3/gobo27", FakeVolumetricBeamPattern.DOUBLE_RING),
            new GoboEntry("generic_3/gobo36", FakeVolumetricBeamPattern.SCATTER_NATURE),
            new GoboEntry("generic_3/gobo18", FakeVolumetricBeamPattern.CROSS_5),
            new GoboEntry("generic_3/gobo3",  FakeVolumetricBeamPattern.SCATTER_NATURE),
            new GoboEntry("generic_3/gobo51", FakeVolumetricBeamPattern.FAN_7),
            new GoboEntry("generic_3/gobo50", FakeVolumetricBeamPattern.RING_8),
            new GoboEntry("generic_3/gobo36", FakeVolumetricBeamPattern.SCATTER_NATURE),
            new GoboEntry("generic_3/gobo34", FakeVolumetricBeamPattern.CROSS_5),
            new GoboEntry("generic_3/gobo43", FakeVolumetricBeamPattern.SCATTER_NATURE),
            new GoboEntry("generic_3/gobo31", FakeVolumetricBeamPattern.DOUBLE_RING),
            new GoboEntry("generic_3/gobo7",  FakeVolumetricBeamPattern.RING_8)
    ),

    PROSPOT(
            new GoboEntry("generic_3/open",   FakeVolumetricBeamPattern.SINGLE),
            new GoboEntry("generic_3/gobo3",  FakeVolumetricBeamPattern.SCATTER_NATURE),
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
            new GoboEntry("generic_1/open",   FakeVolumetricBeamPattern.SINGLE),
            new GoboEntry("generic_1/gobo35", FakeVolumetricBeamPattern.RING_8),
            new GoboEntry("generic_1/gobo39", FakeVolumetricBeamPattern.SPIRAL_6),
            new GoboEntry("generic_1/gobo3",  FakeVolumetricBeamPattern.DOUBLE_RING),
            new GoboEntry("generic_1/gobo2",  FakeVolumetricBeamPattern.CROSS_5),
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
    );

    public record GoboEntry(ResourceLocation texture, FakeVolumetricBeamPattern pattern) {
        public GoboEntry(String path, FakeVolumetricBeamPattern pattern) {
            this(
                    new ResourceLocation("theatricalextralights", "textures/gobos/" + path + ".png"),
                    pattern
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

    public int getSlotCount() {
        return entries.length;
    }

    /**
     * Utilidad maestra: Extrae TODAS las texturas únicas de tu mod.
     * Esto se usa para registrar las imágenes en el motor de IRLights de un solo golpe.
     */
    public static List<ResourceLocation> getAllUniqueGobos() {
        List<ResourceLocation> list = new ArrayList<>();
        for (GoboLibrary library : values()) {
            for (GoboEntry entry : library.entries) {
                if (!list.contains(entry.texture())) {
                    list.add(entry.texture());
                }
            }
        }
        return list;
    }
}