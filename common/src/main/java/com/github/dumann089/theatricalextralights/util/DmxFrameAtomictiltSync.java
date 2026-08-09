package com.github.dumann089.theatricalextralights.util;

import com.github.dumann089.theatricalextralights.client.StrobeRenderHelper;
import com.github.dumann089.theatricalextralights.compat.dmx.DmxFrameExtendedFixture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;

/**
 * Sync extended DMX pour Atomic Tilt : strobe (7ch) ou tilt brut 0–255 (6ch).
 * Le batch pan/tilt/focus ne suffit pas toujours côté client (Sodium) — ce slot
 * garantit que le tilt arrive même si les shorts pan/tilt sont ignorés.
 */
public interface DmxFrameAtomictiltSync extends DmxFrameExtendedFixture {

    boolean usesStrobeExtras();

    int getSyncStrobe();

    int getSyncPrevStrobe();

    void setSyncStrobe(int value);

    void setSyncPrevStrobe(int value);

    int getSyncTiltDmx();

    int getSyncPrevTiltDmx();

    void applySyncTiltDmx(int raw, int prevRaw);

    BlockPos getSyncBlockPos();

    Level getSyncLevel();

    @Override
    default byte dmxFrameExtraType() {
        return EXTRA_TYPE_STROBE;
    }

    @Override
    default void writeDmxFrameExtras(FriendlyByteBuf buf) {
        if (usesStrobeExtras()) {
            buf.writeByte(getSyncStrobe());
            buf.writeByte(getSyncPrevStrobe());
        } else {
            buf.writeByte(getSyncTiltDmx());
            buf.writeByte(getSyncPrevTiltDmx());
        }
    }

    @Override
    default void applyDmxFrameExtras(FriendlyByteBuf buf) {
        if (usesStrobeExtras()) {
            setSyncStrobe(buf.readUnsignedByte());
            setSyncPrevStrobe(buf.readUnsignedByte());
        } else {
            applySyncTiltDmx(buf.readUnsignedByte(), buf.readUnsignedByte());
        }
        markAtomicTiltFrameApplied();
    }

    default void markAtomicTiltFrameApplied() {
        Level level = getSyncLevel();
        if (level != null && level.isClientSide) {
            StrobeRenderHelper.markSectionDirty(getSyncBlockPos());
        }
    }
}
