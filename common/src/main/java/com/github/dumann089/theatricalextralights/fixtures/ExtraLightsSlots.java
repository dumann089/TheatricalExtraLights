package com.github.dumann089.theatricalextralights.fixtures;

import ch.bildspur.artnet.rdm.RDMSlotID;
import ch.bildspur.artnet.rdm.RDMSlotType;
import dev.imabad.theatrical.api.dmx.DMXSlot;

public final class ExtraLightsSlots {

    public static final DMXSlot STROBE = new DMXSlot("Strobe", RDMSlotType.ST_PRIMARY, RDMSlotID.SD_BEAM_SIZE_IRIS);

    private ExtraLightsSlots() {
    }
}
