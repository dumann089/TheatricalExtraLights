package com.github.dumann089.theatricalextralights.fixtures;

import ch.bildspur.artnet.rdm.RDMSlotID;
import ch.bildspur.artnet.rdm.RDMSlotType;
import dev.imabad.theatrical.api.dmx.DMXSlot;

/** Canaux des strobes type Atomic : intensite, duree de flash, cadence, effets, aura et plaque RGB. */
public final class AtomicSlots {
    public static final DMXSlot STROBE = new DMXSlot("Strobe", RDMSlotType.ST_PRIMARY, RDMSlotID.SD_BEAM_SIZE_IRIS);
    public static final DMXSlot INTENSITY = new DMXSlot("Intensity", RDMSlotType.ST_PRIMARY, RDMSlotID.SD_INTENSITY);
    public static final DMXSlot DURATION = new DMXSlot("Flash Duration", RDMSlotType.ST_PRIMARY, RDMSlotID.SD_BEAM_SIZE_IRIS);
    public static final DMXSlot RATE = new DMXSlot("Flash Rate", RDMSlotType.ST_PRIMARY, RDMSlotID.SD_FIXTURE_SPEED);
    public static final DMXSlot EFFECTS = new DMXSlot("Effects", RDMSlotType.ST_PRIMARY, RDMSlotID.SD_PAN);
    public static final DMXSlot AURA_INTENSITY = new DMXSlot("Aura Intensity", RDMSlotType.ST_PRIMARY, RDMSlotID.SD_INTENSITY);
    public static final DMXSlot AURA_RED = new DMXSlot("Aura Red", RDMSlotType.ST_PRIMARY, RDMSlotID.SD_COLOR_SUB_CYAN);
    public static final DMXSlot AURA_GREEN = new DMXSlot("Aura Green", RDMSlotType.ST_PRIMARY, RDMSlotID.SD_COLOR_SUB_MAGENTA);
    public static final DMXSlot AURA_BLUE = new DMXSlot("Aura Blue", RDMSlotType.ST_PRIMARY, RDMSlotID.SD_COLOR_SUB_YELLOW);
    public static final DMXSlot PLATE_INTENSITY = new DMXSlot("Plate Intensity", RDMSlotType.ST_PRIMARY, RDMSlotID.SD_INTENSITY);
    public static final DMXSlot PLATE_DURATION = new DMXSlot("Plate Flash Duration", RDMSlotType.ST_PRIMARY, RDMSlotID.SD_BEAM_SIZE_IRIS);
    public static final DMXSlot PLATE_RATE = new DMXSlot("Plate Flash Rate", RDMSlotType.ST_PRIMARY, RDMSlotID.SD_FIXTURE_SPEED);
    public static final DMXSlot PLATE_RED = new DMXSlot("Plate Red", RDMSlotType.ST_PRIMARY, RDMSlotID.SD_COLOR_SUB_CYAN);
    public static final DMXSlot PLATE_GREEN = new DMXSlot("Plate Green", RDMSlotType.ST_PRIMARY, RDMSlotID.SD_COLOR_SUB_MAGENTA);
    public static final DMXSlot PLATE_BLUE = new DMXSlot("Plate Blue", RDMSlotType.ST_PRIMARY, RDMSlotID.SD_COLOR_SUB_YELLOW);

    private AtomicSlots() {
    }

    /** @param index segment 1..9 du tube strobe */
    public static DMXSlot segment(int index) {
        return new DMXSlot("Bar Segment " + index, RDMSlotType.ST_PRIMARY, RDMSlotID.SD_INTENSITY);
    }

    public static DMXSlot pixelDim(int pixel) {
        return new DMXSlot("Pixel " + pixel + " Dim", RDMSlotType.ST_PRIMARY, RDMSlotID.SD_INTENSITY);
    }

    public static DMXSlot pixelRed(int pixel) {
        return new DMXSlot("Pixel " + pixel + " Red", RDMSlotType.ST_PRIMARY, RDMSlotID.SD_COLOR_SUB_CYAN);
    }

    public static DMXSlot pixelGreen(int pixel) {
        return new DMXSlot("Pixel " + pixel + " Green", RDMSlotType.ST_PRIMARY, RDMSlotID.SD_COLOR_SUB_MAGENTA);
    }

    public static DMXSlot pixelBlue(int pixel) {
        return new DMXSlot("Pixel " + pixel + " Blue", RDMSlotType.ST_PRIMARY, RDMSlotID.SD_COLOR_SUB_YELLOW);
    }

    public static DMXSlot zoneRed(int zone) {
        return new DMXSlot("Zone " + zone + " Red", RDMSlotType.ST_PRIMARY, RDMSlotID.SD_COLOR_SUB_CYAN);
    }

    public static DMXSlot zoneGreen(int zone) {
        return new DMXSlot("Zone " + zone + " Green", RDMSlotType.ST_PRIMARY, RDMSlotID.SD_COLOR_SUB_MAGENTA);
    }

    public static DMXSlot zoneBlue(int zone) {
        return new DMXSlot("Zone " + zone + " Blue", RDMSlotType.ST_PRIMARY, RDMSlotID.SD_COLOR_SUB_YELLOW);
    }
}
