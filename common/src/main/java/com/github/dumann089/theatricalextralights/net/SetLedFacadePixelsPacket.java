package com.github.dumann089.theatricalextralights.net;

import com.github.dumann089.theatricalextralights.blockentities.LedFacadeBlockEntity;
import dev.architectury.networking.NetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.BitSet;
import java.util.function.Supplier;

/**
 * C2S : remplace l'ensemble des pixels actifs d'une façade LED (bitmask row-major).
 * Envoyé au relâchement de la souris depuis l'éditeur. ≤ 8 Ko (256² bits).
 */
public class SetLedFacadePixelsPacket {

    private final BlockPos pos;
    private final byte[] mask;

    public SetLedFacadePixelsPacket(BlockPos pos, byte[] mask) {
        this.pos = pos;
        this.mask = mask;
    }

    public static SetLedFacadePixelsPacket decode(FriendlyByteBuf buf) {
        return new SetLedFacadePixelsPacket(buf.readBlockPos(), buf.readByteArray());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeByteArray(mask);
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        contextSupplier.get().queue(() -> {
            BlockEntity be = contextSupplier.get().getPlayer().level().getBlockEntity(pos);
            if (!(be instanceof LedFacadeBlockEntity facade)) {
                return;
            }
            facade.setActivePixels(BitSet.valueOf(mask));
        });
    }
}
