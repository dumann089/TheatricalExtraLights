package com.github.dumann089.theatricalextralights.net;

import com.github.dumann089.theatricalextralights.blockentities.FollowspotConsoleBlockEntity;
import com.github.dumann089.theatricalextralights.util.FollowspotConsoleAccess;
import dev.architectury.networking.NetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.function.Supplier;

/** Client → server: store or clear one followspot console preset. */
public class FollowspotPresetPacket {

    private final BlockPos consolePos;
    private final int slot;
    private final boolean store;
    private final int pan;
    private final int tilt;
    private final int intensity;
    private final int focus;

    public FollowspotPresetPacket(BlockPos consolePos, int slot, boolean store, int pan, int tilt, int intensity, int focus) {
        this.consolePos = consolePos;
        this.slot = slot;
        this.store = store;
        this.pan = pan;
        this.tilt = tilt;
        this.intensity = intensity;
        this.focus = focus;
    }

    public static FollowspotPresetPacket decode(FriendlyByteBuf buf) {
        return new FollowspotPresetPacket(buf.readBlockPos(), buf.readVarInt(), buf.readBoolean(),
                buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(consolePos);
        buf.writeVarInt(slot);
        buf.writeBoolean(store);
        buf.writeVarInt(pan);
        buf.writeVarInt(tilt);
        buf.writeVarInt(intensity);
        buf.writeVarInt(focus);
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        contextSupplier.get().queue(() -> {
            Player player = contextSupplier.get().getPlayer();
            if (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)
                    || !FollowspotConsoleAccess.canPlayerUse(serverPlayer, consolePos)) {
                return;
            }
            BlockEntity be = player.level().getBlockEntity(consolePos);
            if (be instanceof FollowspotConsoleBlockEntity console) {
                console.setPreset(slot, store, pan, tilt, intensity, focus);
                console.syncToClients();
            }
        });
    }
}
