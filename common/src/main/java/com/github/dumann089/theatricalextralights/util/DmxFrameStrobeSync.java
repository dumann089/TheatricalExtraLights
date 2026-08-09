package com.github.dumann089.theatricalextralights.util;

import com.github.dumann089.theatricalextralights.client.StrobeRenderHelper;
import com.github.dumann089.theatricalextralights.compat.dmx.DmxFrameExtendedFixture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;

/**
 * Sync batched DMX frame extras for fixtures with a dedicated strobe / shutter channel.
 */
public interface DmxFrameStrobeSync extends DmxFrameExtendedFixture {

    int getSyncStrobe();

    int getSyncPrevStrobe();

    void setSyncStrobe(int value);

    void setSyncPrevStrobe(int value);

    BlockPos getSyncBlockPos();

    Level getSyncLevel();

    @Override
    default byte dmxFrameExtraType() {
        return EXTRA_TYPE_STROBE;
    }

    @Override
    default void writeDmxFrameExtras(FriendlyByteBuf buf) {
        buf.writeByte(getSyncStrobe());
        buf.writeByte(getSyncPrevStrobe());
    }

    @Override
    default void applyDmxFrameExtras(FriendlyByteBuf buf) {
        setSyncStrobe(buf.readUnsignedByte());
        setSyncPrevStrobe(buf.readUnsignedByte());
        markStrobeFrameApplied();
    }

    default void markStrobeFrameApplied() {
        Level level = getSyncLevel();
        if (level != null && level.isClientSide) {
            StrobeRenderHelper.markSectionDirty(getSyncBlockPos());
        }
    }
}
