package com.github.dumann089.theatricalextralights.net;

import com.github.dumann089.theatricalextralights.blockentities.LedFacadeBlockEntity;
import dev.architectury.networking.NetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * C2S : applique la configuration d'une façade LED (résolution, univers/adresse de base, réseau,
 * niveau de lissage). Changer la résolution efface le dessin côté serveur — l'éditeur renvoie
 * ensuite les pixels via {@link SetLedFacadePixelsPacket}.
 */
public class SetLedFacadeConfigPacket {

    private final BlockPos pos;
    private final int resolution;
    private final int universe;
    private final int address;
    private final UUID networkId;
    private final int smoothing;

    public SetLedFacadeConfigPacket(BlockPos pos, int resolution, int universe, int address, UUID networkId, int smoothing) {
        this.pos = pos;
        this.resolution = resolution;
        this.universe = universe;
        this.address = address;
        this.networkId = networkId;
        this.smoothing = smoothing;
    }

    public static SetLedFacadeConfigPacket decode(FriendlyByteBuf buf) {
        return new SetLedFacadeConfigPacket(buf.readBlockPos(), buf.readInt(), buf.readInt(), buf.readInt(),
                buf.readUUID(), buf.readInt());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeInt(resolution);
        buf.writeInt(universe);
        buf.writeInt(address);
        buf.writeUUID(networkId);
        buf.writeInt(smoothing);
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        contextSupplier.get().queue(() -> {
            BlockEntity be = contextSupplier.get().getPlayer().level().getBlockEntity(pos);
            if (!(be instanceof LedFacadeBlockEntity facade)) {
                return;
            }
            facade.setNetworkId(networkId);
            facade.setUniverse(Math.max(0, universe));
            facade.setChannelStartPoint(Math.max(1, address));
            facade.setResolution(resolution);
            facade.setSmoothing(smoothing);
        });
    }
}
