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

    private ExtraLightsSlots() {
    }
}
