package com.github.dumann089.theatricalextralights.net;

import com.github.dumann089.theatricalextralights.blockentities.FollowspotConsoleBlockEntity;
import com.github.dumann089.theatricalextralights.util.FollowspotConsoleAccess;
import com.github.dumann089.theatricalextralights.util.FollowspotDmxHelper;
import dev.architectury.networking.NetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.UUID;
import java.util.function.Supplier;

public class FollowspotConsolePatchPacket {

    private final BlockPos consolePos;
    private final UUID networkId;
    private final int universe;
    private final int dmxAddress;

    public FollowspotConsolePatchPacket(BlockPos consolePos, UUID networkId, int universe, int dmxAddress) {
        this.consolePos = consolePos;
        this.networkId = networkId;
        this.universe = universe;
        this.dmxAddress = dmxAddress;
    }

    public static FollowspotConsolePatchPacket decode(FriendlyByteBuf buf) {
        return new FollowspotConsolePatchPacket(
                buf.readBlockPos(),
                buf.readUUID(),
                buf.readVarInt(),
                buf.readVarInt()
        );
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(consolePos);
        buf.writeUUID(networkId);
        buf.writeVarInt(universe);
        buf.writeVarInt(dmxAddress);
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        contextSupplier.get().queue(() -> {
            if (!(contextSupplier.get().getPlayer() instanceof ServerPlayer player)) {
                return;
            }
            if (!FollowspotConsoleAccess.canPlayerUse(player, consolePos)) {
                return;
            }
            if (!FollowspotDmxHelper.isValidDmxAddress(dmxAddress)) {
                return;
            }
            BlockEntity blockEntity = player.level().getBlockEntity(consolePos);
            if (!(blockEntity instanceof FollowspotConsoleBlockEntity console)) {
                return;
            }
            console.setNetworkId(networkId);
            console.setUniverse(universe);
            console.setDmxAddress(dmxAddress);
            console.syncFromLinkedFixture(player.level());
            console.applyToLinkedFixture(player.level());
            console.syncToClients();
        });
    }
}
