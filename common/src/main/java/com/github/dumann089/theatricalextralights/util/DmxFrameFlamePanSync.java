package com.github.dumann089.theatricalextralights.util;

import com.github.dumann089.theatricalextralights.client.StrobeRenderHelper;
import com.github.dumann089.theatricalextralights.compat.dmx.DmxFrameExtendedFixture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;

/**
 * Sync pan (inclinaison tête) pour fixtures 2 canaux — le batch DMX Theatrical
 * n'inclut pan/tilt que si {@code channelCount >= 5}, donc on réutilise le slot
 * extended STROBE (2 octets) pour pan + prevPan.
 */
public interface DmxFrameFlamePanSync extends DmxFrameExtendedFixture {

    int getSyncPan();

    int getSyncPrevPan();

    void setSyncPan(int value);

    void setSyncPrevPan(int value);

    BlockPos getSyncBlockPos();

    Level getSyncLevel();

    @Override
    default byte dmxFrameExtraType() {
        return EXTRA_TYPE_STROBE;
    }

    @Override
    default void writeDmxFrameExtras(FriendlyByteBuf buf) {
        buf.writeByte(getSyncPan());
        buf.writeByte(getSyncPrevPan());
    }

    @Override
    default void applyDmxFrameExtras(FriendlyByteBuf buf) {
        setSyncPan(buf.readUnsignedByte());
        setSyncPrevPan(buf.readUnsignedByte());
        markFlamePanFrameApplied();
    }

    default void markFlamePanFrameApplied() {
        Level level = getSyncLevel();
        if (level != null && level.isClientSide) {
            StrobeRenderHelper.markSectionDirty(getSyncBlockPos());
        }
    }
}
