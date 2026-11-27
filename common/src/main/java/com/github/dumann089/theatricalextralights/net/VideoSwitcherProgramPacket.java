package com.github.dumann089.theatricalextralights.net;

import com.github.dumann089.theatricalextralights.JumbotronDebug;
import com.github.dumann089.theatricalextralights.blockentities.VideoSwitcherBlockEntity;
import dev.architectury.networking.NetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Cliente → servidor: o operador cortou/dissolveu para uma câmera. O servidor guarda como "câmera no ar"
 * da mesa e avisa os jumbotrons vinculados (todos os jogadores passam a ver o mesmo programa).
 */
public class VideoSwitcherProgramPacket {

    private final BlockPos pos;
    private final UUID camera;
    private final boolean fade;

    public VideoSwitcherProgramPacket(BlockPos pos, UUID camera, boolean fade) {
        this.pos = pos;
        this.camera = camera;
        this.fade = fade;
    }

    public static VideoSwitcherProgramPacket decode(FriendlyByteBuf buf) {
        return new VideoSwitcherProgramPacket(buf.readBlockPos(), buf.readUUID(), buf.readBoolean());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeUUID(camera);
        buf.writeBoolean(fade);
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        contextSupplier.get().queue(() -> {
            if (!(contextSupplier.get().getPlayer() instanceof ServerPlayer player)
                    || !player.level().isLoaded(pos)
                    || player.distanceToSqr(Vec3.atCenterOf(pos)) > 12.0 * 12.0) {
                JumbotronDebug.log("pacote-rejeitado", "jogador longe da mesa (>12 blocos) ou chunk descarregado");
                return;
            }
            JumbotronDebug.log("pacote-recebido", "mesa=" + pos + " camera=" + camera + " fade=" + fade);
            if (player.level().getBlockEntity(pos) instanceof VideoSwitcherBlockEntity desk) {
                // só câmeras que estão vinculadas a ESTA mesa
                if (desk.getConfig().bindingForCamera(camera) != null) {
                    desk.setProgram(camera, fade, player.level().getGameTime());
                } else {
                    JumbotronDebug.log("pacote-rejeitado", "camera " + camera + " nao esta vinculada a esta mesa");
                }
            }
        });
    }
}
