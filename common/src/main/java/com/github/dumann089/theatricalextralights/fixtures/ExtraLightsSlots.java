package com.github.dumann089.theatricalextralights.fixtures;

import ch.bildspur.artnet.rdm.RDMSlotID;
import ch.bildspur.artnet.rdm.RDMSlotType;
import dev.imabad.theatrical.api.dmx.DMXSlot;

public final class ExtraLightsSlots {

    public static final DMXSlot STROBE = new DMXSlot("Strobe", RDMSlotType.ST_PRIMARY, RDMSlotID.SD_BEAM_SIZE_IRIS);

    // Module de couteaux (E1.20 RDM : SD_FRAMING_SHUTTER / SD_SHUTTER_ROTATE)
    public static final DMXSlot BLADE_1_INSERTION = new DMXSlot("Blade 1 Insertion", RDMSlotType.ST_PRIMARY, RDMSlotID.SD_FRAMING_SHUTTER);
    public static final DMXSlot BLADE_1_ANGLE     = new DMXSlot("Blade 1 Angle",     RDMSlotType.ST_PRIMARY, RDMSlotID.SD_SHUTTER_ROTATE);
    public static final DMXSlot BLADE_2_INSERTION = new DMXSlot("Blade 2 Insertion", RDMSlotType.ST_PRIMARY, RDMSlotID.SD_FRAMING_SHUTTER);
    public static final DMXSlot BLADE_2_ANGLE     = new DMXSlot("Blade 2 Angle",     RDMSlotType.ST_PRIMARY, RDMSlotID.SD_SHUTTER_ROTATE);
    public static final DMXSlot BLADE_3_INSERTION = new DMXSlot("Blade 3 Insertion", RDMSlotType.ST_PRIMARY, RDMSlotID.SD_FRAMING_SHUTTER);
    public static final DMXSlot BLADE_3_ANGLE     = new DMXSlot("Blade 3 Angle",     RDMSlotType.ST_PRIMARY, RDMSlotID.SD_SHUTTER_ROTATE);
    public static final DMXSlot BLADE_4_INSERTION = new DMXSlot("Blade 4 Insertion", RDMSlotType.ST_PRIMARY, RDMSlotID.SD_FRAMING_SHUTTER);
    public static final DMXSlot BLADE_4_ANGLE     = new DMXSlot("Blade 4 Angle",     RDMSlotType.ST_PRIMARY, RDMSlotID.SD_SHUTTER_ROTATE);
    public static final DMXSlot FRAMING_ROTATION  = new DMXSlot("Framing Rotation",  RDMSlotType.ST_PRIMARY, RDMSlotID.SD_SHUTTER_ROTATE);

    private ExtraLightsSlots() {
    }
}
