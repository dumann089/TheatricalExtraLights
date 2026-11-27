package com.github.dumann089.theatricalextralights.net;

import com.github.dumann089.theatricalextralights.blockentities.VideoSwitcherBlockEntity;
import com.github.dumann089.theatricalextralights.blockentities.VideoSwitcherConfig;
import dev.architectury.networking.NetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.function.Supplier;

/** Cliente → servidor: a nova configuração da mesa de corte. */
public class VideoSwitcherConfigPacket {

    private final BlockPos pos;
    private final CompoundTag data;

    public VideoSwitcherConfigPacket(BlockPos pos, CompoundTag data) {
        this.pos = pos;
        this.data = data;
    }

    public static VideoSwitcherConfigPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        CompoundTag data = buf.readNbt();
        return new VideoSwitcherConfigPacket(pos, data == null ? new CompoundTag() : data);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeNbt(data);
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        contextSupplier.get().queue(() -> {
            if (!(contextSupplier.get().getPlayer() instanceof ServerPlayer player)
                    || player.distanceToSqr(Vec3.atCenterOf(pos)) > 12.0 * 12.0) {
                return;
            }
            if (player.level().getBlockEntity(pos) instanceof VideoSwitcherBlockEntity desk) {
                VideoSwitcherConfig config = new VideoSwitcherConfig();
                config.load(data); // saneia: limites, teclas repetidas, etc.
                desk.setConfig(config);
            }
        });
    }
}
