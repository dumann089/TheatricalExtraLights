package com.github.dumann089.theatricalextralights.net;

import com.github.dumann089.theatricalextralights.blockentities.FollowspotConsoleBlockEntity;
import com.github.dumann089.theatricalextralights.util.FollowspotConsoleAccess;
import dev.architectury.networking.NetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.function.Supplier;

public class FollowspotConsoleControlPacket {

    private final BlockPos consolePos;
    private final int intensity;
    private final int red;
    private final int green;
    private final int blue;
    private final int focus;
    private final int pan;
    private final int tilt;

    public FollowspotConsoleControlPacket(
            BlockPos consolePos,
            int intensity,
            int red,
            int green,
            int blue,
            int focus,
            int pan,
            int tilt
    ) {
        this.consolePos = consolePos;
        this.intensity = intensity;
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.focus = focus;
        this.pan = pan;
        this.tilt = tilt;
    }

    public static FollowspotConsoleControlPacket decode(FriendlyByteBuf buf) {
        return new FollowspotConsoleControlPacket(
                buf.readBlockPos(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt()
        );
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(consolePos);
        buf.writeVarInt(intensity);
        buf.writeVarInt(red);
        buf.writeVarInt(green);
        buf.writeVarInt(blue);
        buf.writeVarInt(focus);
        buf.writeVarInt(pan);
        buf.writeVarInt(tilt);
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        contextSupplier.get().queue(() -> {
            if (!(contextSupplier.get().getPlayer() instanceof ServerPlayer player)) {
                return;
            }
            if (!FollowspotConsoleAccess.canPlayerUse(player, consolePos)) {
                return;
            }
            BlockEntity blockEntity = player.level().getBlockEntity(consolePos);
            if (!(blockEntity instanceof FollowspotConsoleBlockEntity console)) {
                return;
            }
            console.setControlState(intensity, red, green, blue, focus, pan, tilt);
            console.applyToLinkedFixture(player.level());
            console.syncToClients();
        });
    }
}
