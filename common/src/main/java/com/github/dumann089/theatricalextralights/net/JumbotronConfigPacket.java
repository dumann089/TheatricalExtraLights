package com.github.dumann089.theatricalextralights.net;

import com.github.dumann089.theatricalextralights.blockentities.JumbotronBlockEntity;
import dev.architectury.networking.NetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import java.util.function.Supplier;

/** Cliente → servidor: tamanho, qualidade e mesa de corte do jumbotron. Tudo é saneado no servidor. */
public class JumbotronConfigPacket {

    private final BlockPos pos;
    private final int width;
    private final int height;
    private final int quality;
    private final BlockPos link; // null = automático

    public JumbotronConfigPacket(BlockPos pos, int width, int height, int quality, BlockPos link) {
        this.pos = pos;
        this.width = width;
        this.height = height;
        this.quality = quality;
        this.link = link;
    }

    public static JumbotronConfigPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        int width = buf.readVarInt();
        int height = buf.readVarInt();
        int quality = buf.readVarInt();
        BlockPos link = buf.readBoolean() ? buf.readBlockPos() : null;
        return new JumbotronConfigPacket(pos, width, height, quality, link);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeVarInt(width);
        buf.writeVarInt(height);
        buf.writeVarInt(quality);
        buf.writeBoolean(link != null);
        if (link != null) {
            buf.writeBlockPos(link);
        }
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        contextSupplier.get().queue(() -> {
            if (!(contextSupplier.get().getPlayer() instanceof ServerPlayer player)
                    || player.gameMode.getGameModeForPlayer() == GameType.SPECTATOR
                    || !player.level().isLoaded(pos)
                    || player.distanceToSqr(Vec3.atCenterOf(pos)) > 16.0 * 16.0) {
                return;
            }
            if (!(player.level().getBlockEntity(pos) instanceof JumbotronBlockEntity jumbotron)) {
                return;
            }
            // O vínculo só vale se estiver dentro do alcance de procura (evita apontar para qualquer lugar).
            BlockPos safeLink = link != null
                    && link.distSqr(pos) <= JumbotronBlockEntity.AUTO_LINK_RANGE * JumbotronBlockEntity.AUTO_LINK_RANGE
                    ? link : null;
            jumbotron.configure(width, height, quality, safeLink);
        });
    }
}
