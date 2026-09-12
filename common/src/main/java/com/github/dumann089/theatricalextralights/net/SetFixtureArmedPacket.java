package com.github.dumann089.theatricalextralights.net;

import com.github.dumann089.theatricalextralights.blockentities.interfaces.HasSafetyArm;
import dev.architectury.networking.NetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.function.Supplier;

/** Client → server: arm or disarm a pyro effect (flame thrower, flame projector…). */
public class SetFixtureArmedPacket {

    private final BlockPos pos;
    private final boolean armed;

    public SetFixtureArmedPacket(BlockPos pos, boolean armed) {
        this.pos = pos;
        this.armed = armed;
    }

    public static SetFixtureArmedPacket decode(FriendlyByteBuf buf) {
        return new SetFixtureArmedPacket(buf.readBlockPos(), buf.readBoolean());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeBoolean(armed);
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        contextSupplier.get().queue(() -> {
            Player player = contextSupplier.get().getPlayer();
            if (player == null || player.level() == null) {
                return;
            }
            if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64.0 * 64.0) {
                return;
            }
            BlockEntity be = player.level().getBlockEntity(pos);
            if (be instanceof HasSafetyArm arm) {
                arm.setArmed(armed);
            }
        });
    }
}
