
package com.github.dumann089.theatricalextralights.net;

import com.github.dumann089.theatricalextralights.blockentities.ExtraLightsLightBlockEntity;
import com.github.dumann089.theatricalextralights.util.GoboFieldAngleSupport;
import dev.architectury.networking.NetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import java.util.function.Supplier;

public class SetGoboFieldAnglePacket {

    private final BlockPos pos;
    private final float angle;

    public SetGoboFieldAnglePacket(BlockPos pos, float angle) {
        this.pos = pos;
        this.angle = angle;
    }

    public static SetGoboFieldAnglePacket decode(FriendlyByteBuf buf) {
        return new SetGoboFieldAnglePacket(
                buf.readBlockPos(),
                buf.readFloat()
        );
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeFloat(angle);
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

            if (!(blockEntity instanceof ExtraLightsLightBlockEntity light)) {
                return;
            }

            if (!GoboFieldAngleSupport.supports(light)) {
                return;
            }

            light.setGoboFieldAngle(angle);
        });
    }
}
