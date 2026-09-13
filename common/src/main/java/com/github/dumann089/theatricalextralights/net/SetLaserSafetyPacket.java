package com.github.dumann089.theatricalextralights.net;

import com.github.dumann089.theatricalextralights.blockentities.LaserBlockEntity;
import dev.architectury.networking.NetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.function.Supplier;

/** Client → server: engage or release the emergency stop of a laser. */
public class SetLaserSafetyPacket {

    private final BlockPos pos;
    private final boolean emergencyStop;

    public SetLaserSafetyPacket(BlockPos pos, boolean emergencyStop) {
        this.pos = pos;
        this.emergencyStop = emergencyStop;
    }

    public static SetLaserSafetyPacket decode(FriendlyByteBuf buf) {
        return new SetLaserSafetyPacket(buf.readBlockPos(), buf.readBoolean());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeBoolean(emergencyStop);
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
            if (be instanceof LaserBlockEntity laser) {
                laser.setEmergencyStop(emergencyStop);
            }
        });
    }
}
