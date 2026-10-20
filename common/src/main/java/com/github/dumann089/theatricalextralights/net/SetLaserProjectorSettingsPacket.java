package com.github.dumann089.theatricalextralights.net;

import com.github.dumann089.theatricalextralights.blockentities.LaserProjectorBlockEntity;
import com.github.dumann089.theatricalextralights.laser.dac.EtherDreamDevice;
import dev.architectury.networking.NetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.function.Supplier;

public class SetLaserProjectorSettingsPacket {

    private final BlockPos pos;
    private final String dacId;
    private final float scanHalfAngle;
    private final float projectionScale;

    public SetLaserProjectorSettingsPacket(BlockPos pos, String dacId, float scanHalfAngle, float projectionScale) {
        this.pos = pos;
        this.dacId = dacId;
        this.scanHalfAngle = scanHalfAngle;
        this.projectionScale = projectionScale;
    }

    public static SetLaserProjectorSettingsPacket decode(FriendlyByteBuf buf) {
        return new SetLaserProjectorSettingsPacket(
                buf.readBlockPos(),
                buf.readUtf(),
                buf.readFloat(),
                buf.readFloat()
        );
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeUtf(dacId == null ? EtherDreamDevice.DEFAULT_ID : dacId);
        buf.writeFloat(scanHalfAngle);
        buf.writeFloat(projectionScale);
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        contextSupplier.get().queue(() -> {
            BlockEntity be = contextSupplier.get().getPlayer().level().getBlockEntity(pos);
            if (be instanceof LaserProjectorBlockEntity projector) {
                projector.setProjectorSettings(dacId, scanHalfAngle, projectionScale);
            }
        });
    }
}
