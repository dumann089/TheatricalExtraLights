package com.github.dumann089.theatricalextralights.compat.dmx;

import net.minecraft.network.FriendlyByteBuf;

/**
 * Extended DMX frame payload for fixtures whose visual state exceeds standard light fields.
 * Standalone copy of {@code dev.imabad.theatrical.api.dmx.DmxFrameExtendedFixture} so Extra
 * Lights loads on older published Theatrical builds; a conditional mixin re-attaches the
 * official API when that class is present at runtime.
 */
public interface DmxFrameExtendedFixture {

    byte EXTRA_TYPE_LASER = 1;
    /** strobe + prevStrobe (unsigned bytes). */
    byte EXTRA_TYPE_STROBE = 2;

    byte dmxFrameExtraType();

    void writeDmxFrameExtras(FriendlyByteBuf buf);

    void applyDmxFrameExtras(FriendlyByteBuf buf);
}
