package com.github.dumann089.theatricalextralights.net;

import com.github.dumann089.theatricalextralights.blockentities.ExtraLightsLightBlockEntity;
import dev.architectury.networking.NetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.function.Supplier;

public class SetMountTransformPacket {

    private final BlockPos pos;
    private final float offsetX;
    private final float offsetY;
    private final float offsetZ;
    private final float yaw;
    private final float pitch;
    private final float roll;

    public SetMountTransformPacket(BlockPos pos, float offsetX, float offsetY, float offsetZ,
                                   float yaw, float pitch, float roll) {
        this.pos = pos;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.yaw = yaw;
        this.pitch = pitch;
        this.roll = roll;
    }

    public static SetMountTransformPacket decode(FriendlyByteBuf buf) {
        return new SetMountTransformPacket(
                buf.readBlockPos(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readFloat()
        );
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeFloat(offsetX);
        buf.writeFloat(offsetY);
        buf.writeFloat(offsetZ);
        buf.writeFloat(yaw);
        buf.writeFloat(pitch);
        buf.writeFloat(roll);
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        contextSupplier.get().queue(() -> {
            BlockEntity blockEntity = contextSupplier.get().getPlayer().level().getBlockEntity(pos);
            if (!(blockEntity instanceof ExtraLightsLightBlockEntity lightBlockEntity)) {
                return;
            }

            lightBlockEntity.setMountTransform(offsetX, offsetY, offsetZ, yaw, pitch, roll);
            lightBlockEntity.syncMountTransformToClients();
        });
    }
}
