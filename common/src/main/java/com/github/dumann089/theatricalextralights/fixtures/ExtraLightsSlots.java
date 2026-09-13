package com.github.dumann089.theatricalextralights.fixtures;

import ch.bildspur.artnet.rdm.RDMSlotID;
import ch.bildspur.artnet.rdm.RDMSlotType;
import dev.imabad.theatrical.api.dmx.DMXSlot;

public final class ExtraLightsSlots {

    public static final DMXSlot STROBE = new DMXSlot("Strobe", RDMSlotType.ST_PRIMARY, RDMSlotID.SD_BEAM_SIZE_IRIS);

    // Module de couteaux (E1.20 RDM : SD_FRAMING_SHUTTER / SD_SHUTTER_ROTATE)
    public static final DMXSlot BLADE_1_A = new DMXSlot("Blade 1 A", RDMSlotType.ST_PRIMARY, RDMSlotID.SD_FRAMING_SHUTTER);
    public static final DMXSlot BLADE_1_B     = new DMXSlot("Blade 1 B",     RDMSlotType.ST_PRIMARY, RDMSlotID.SD_FRAMING_SHUTTER);
    public static final DMXSlot BLADE_2_A = new DMXSlot("Blade 2 A", RDMSlotType.ST_PRIMARY, RDMSlotID.SD_FRAMING_SHUTTER);
    public static final DMXSlot BLADE_2_B     = new DMXSlot("Blade 2 B",     RDMSlotType.ST_PRIMARY, RDMSlotID.SD_FRAMING_SHUTTER);
    public static final DMXSlot BLADE_3_A = new DMXSlot("Blade 3 A", RDMSlotType.ST_PRIMARY, RDMSlotID.SD_FRAMING_SHUTTER);
    public static final DMXSlot BLADE_3_B     = new DMXSlot("Blade 3 B",     RDMSlotType.ST_PRIMARY, RDMSlotID.SD_FRAMING_SHUTTER);
    public static final DMXSlot BLADE_4_A = new DMXSlot("Blade 4 A", RDMSlotType.ST_PRIMARY, RDMSlotID.SD_FRAMING_SHUTTER);
    public static final DMXSlot BLADE_4_B     = new DMXSlot("Blade 4 B",     RDMSlotType.ST_PRIMARY, RDMSlotID.SD_FRAMING_SHUTTER);
    public static final DMXSlot FRAMING_ROTATION  = new DMXSlot("Framing Rotation",  RDMSlotType.ST_PRIMARY, RDMSlotID.SD_SHUTTER_ROTATE);

    // Personnalite Profile 16 bit
    public static final DMXSlot SHUTTER_STROBE = new DMXSlot("Shutter / Strobe", RDMSlotType.ST_PRIMARY, RDMSlotID.SD_STROBE);
    public static final DMXSlot DIMMER_FINE    = new DMXSlot("Dimmer Fine",      RDMSlotType.ST_PRIMARY, RDMSlotID.SD_INTENSITY);
    public static final DMXSlot CYAN           = new DMXSlot("Cyan",             RDMSlotType.ST_PRIMARY, RDMSlotID.SD_COLOR_SUB_CYAN);
    public static final DMXSlot MAGENTA        = new DMXSlot("Magenta",          RDMSlotType.ST_PRIMARY, RDMSlotID.SD_COLOR_SUB_MAGENTA);
    public static final DMXSlot YELLOW         = new DMXSlot("Yellow",           RDMSlotType.ST_PRIMARY, RDMSlotID.SD_COLOR_SUB_YELLOW);
    public static final DMXSlot COLOR_WHEEL    = new DMXSlot("Color Wheel",      RDMSlotType.ST_PRIMARY, RDMSlotID.SD_COLOR_WHEEL);
    public static final DMXSlot GOBO_WHEEL     = new DMXSlot("Gobo Wheel",       RDMSlotType.ST_PRIMARY, RDMSlotID.SD_ROTO_GOBO_WHEEL);
    public static final DMXSlot GOBO_ROTATION  = new DMXSlot("Gobo Rotation",    RDMSlotType.ST_PRIMARY, RDMSlotID.SD_ROTO_GOBO_WHEEL);
    public static final DMXSlot PRISM          = new DMXSlot("Prism",            RDMSlotType.ST_PRIMARY, RDMSlotID.SD_PRISM_WHEEL);
    public static final DMXSlot PRISM_ROTATION = new DMXSlot("Prism Rotation",   RDMSlotType.ST_PRIMARY, RDMSlotID.SD_PRISM_WHEEL);
    public static final DMXSlot FROST          = new DMXSlot("Frost",            RDMSlotType.ST_PRIMARY, RDMSlotID.SD_FROST);
    public static final DMXSlot ZOOM           = new DMXSlot("Zoom",             RDMSlotType.ST_PRIMARY, RDMSlotID.SD_ZOOM);
    public static final DMXSlot PAN_FINE       = new DMXSlot("Pan Fine",         RDMSlotType.ST_PRIMARY, RDMSlotID.SD_PAN);
    public static final DMXSlot TILT_FINE      = new DMXSlot("Tilt Fine",        RDMSlotType.ST_PRIMARY, RDMSlotID.SD_TILT);
    public static final DMXSlot PAN_TILT_SPEED = new DMXSlot("Pan/Tilt Speed",   RDMSlotType.ST_PRIMARY, RDMSlotID.SD_FIXTURE_SPEED);

    private ExtraLightsSlots() {
    }
}
