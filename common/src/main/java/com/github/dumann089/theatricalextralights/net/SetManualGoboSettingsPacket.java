package com.github.dumann089.theatricalextralights.net;

import com.github.dumann089.theatricalextralights.blockentities.ExtraLightsLightBlockEntity;
import com.github.dumann089.theatricalextralights.blockentities.interfaces.ManualGoboSettings;
import dev.architectury.networking.NetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import java.util.function.Supplier;

public final class SetManualGoboSettingsPacket {

    private final BlockPos pos;
    private final int gobo;
    private final int zoom;

    public SetManualGoboSettingsPacket(BlockPos pos, int gobo, int zoom) {
        this.pos = pos;
        this.gobo = gobo;
        this.zoom = zoom;
    }

    public static SetManualGoboSettingsPacket decode(FriendlyByteBuf buf) {
        return new SetManualGoboSettingsPacket(
                buf.readBlockPos(),
                buf.readVarInt(),
                buf.readVarInt()
        );
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeVarInt(gobo);
        buf.writeVarInt(zoom);
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        NetworkManager.PacketContext context = contextSupplier.get();
        context.queue(() -> {
            Player player = context.getPlayer();
            if (player == null
                    || !player.mayBuild()
                    || player.distanceToSqr(Vec3.atCenterOf(pos)) > 64.0D
                    || !player.level().hasChunkAt(pos)) {
                return;
            }

            BlockEntity blockEntity = player.level().getBlockEntity(pos);
            if (blockEntity instanceof ExtraLightsLightBlockEntity
                    && blockEntity instanceof ManualGoboSettings settings) {
                settings.setManualGoboSettings(gobo, zoom);
            }
        });
    }
}
