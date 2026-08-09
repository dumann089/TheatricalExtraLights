package com.github.dumann089.theatricalextralights.firework;

import com.github.dumann089.theatricalextralights.config.TheatricalExtraLightsConfig;
import com.github.dumann089.theatricalextralights.entities.FireworkRocketEntity;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Distances pyro liées à la render distance client / view distance serveur.
 * Quand les blocs du plateau sont visibles, les effets restent actifs.
 */
public final class FireworkRenderDistances {

    /** Marge verticale pour les salves hautes au-dessus du plateau. */
    private static final double ALTITUDE_MARGIN = 80.0;
    /** Plancher si les options ne sont pas encore disponibles. */
    private static final double FALLBACK_BLOCKS = 192.0;

    private FireworkRenderDistances() {
    }

    public static boolean useDynamicDistance() {
        return TheatricalExtraLightsConfig.useFireworkDynamicRenderDistance();
    }

    /** Plafond absolu (config). */
    public static double configCapBlocks() {
        return TheatricalExtraLightsConfig.getFireworkRenderDistance();
    }

    /** Render distance client en blocs (chunks × 16) + marge. */
    public static double clientRenderBlocks() {
        if (!useDynamicDistance()) {
            return Math.min(configCapBlocks(), FALLBACK_BLOCKS);
        }
        if (Platform.getEnvironment() == Env.CLIENT) {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.options != null) {
                return mc.options.getEffectiveRenderDistance() * 16.0 + ALTITUDE_MARGIN;
            }
        }
        return FALLBACK_BLOCKS;
    }

    /** Distance effective côté client pour rendu / particules. */
    public static double effectiveClientRenderBlocks() {
        return Math.min(configCapBlocks(), Math.max(clientRenderBlocks(), FALLBACK_BLOCKS));
    }

    /** View distance serveur (chunks × 16) + marge — garde les fusées tant qu'un joueur voit le plateau. */
    public static double serverKeepBlocks(ServerLevel level) {
        if (!useDynamicDistance()) {
            return Math.min(configCapBlocks(), FALLBACK_BLOCKS);
        }
        int viewChunks = level.getServer() != null
                ? level.getServer().getPlayerList().getViewDistance()
                : 10;
        return Math.min(configCapBlocks(), Math.max(viewChunks * 16.0 + ALTITUDE_MARGIN, FALLBACK_BLOCKS));
    }

    public static double serverReconcileBlocks(ServerLevel level) {
        return serverKeepBlocks(level) + 48.0;
    }

    /** Dérive horizontale max depuis le lanceur avant nettoyage forcé. */
    public static double maxHorizontalDriftBlocks(ServerLevel level) {
        return serverKeepBlocks(level);
    }

    public static boolean isPlayerNearFirework(ServerLevel level, FireworkRocketEntity rocket) {
        double range = serverKeepBlocks(level);
        AABB rocketBox = rocket.getBoundingBox().inflate(range);
        BlockPos launcher = rocket.getLauncherPos();
        AABB launcherBox = new AABB(launcher).inflate(range, range + ALTITUDE_MARGIN, range);

        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || player.isDeadOrDying()) {
                continue;
            }
            Vec3 pos = player.position();
            if (rocketBox.contains(pos) || launcherBox.contains(pos)) {
                return true;
            }
        }
        return false;
    }

    public static double clientSmokeRange(double fractionOfRender) {
        return effectiveClientRenderBlocks() * fractionOfRender;
    }

    public static double clientSparkRangeSq() {
        double range = effectiveClientRenderBlocks();
        return range * range;
    }

    /** Portée client effets flamme (lance-flammes, etc.) — alignée sur la config pyro. */
    public static double clientFlameRangeBlocks() {
        return effectiveClientRenderBlocks();
    }

    public static double clientFlameRangeSq() {
        double range = clientFlameRangeBlocks();
        return range * range;
    }

    public static boolean isWithinClientFlameRange(double x, double y, double z) {
        if (Platform.getEnvironment() != Env.CLIENT) {
            return true;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return false;
        }
        return mc.player.distanceToSqr(x, y, z) <= clientFlameRangeSq();
    }

    /**
     * Agrandit légèrement les particules flamme avec la distance pour rester lisibles
     * quand la render distance config est élevée.
     */
    public static float flameParticleSizeScale(double x, double y, double z) {
        if (Platform.getEnvironment() != Env.CLIENT) {
            return 1.0f;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return 1.0f;
        }
        double dist = Math.sqrt(mc.player.distanceToSqr(x, y, z));
        return (float) Math.min(2.75, Math.max(1.0, 1.0 + dist * 0.012));
    }
}
