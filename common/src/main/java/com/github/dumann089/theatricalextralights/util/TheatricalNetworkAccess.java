package com.github.dumann089.theatricalextralights.util;

import com.github.dumann089.theatricalextralights.TheatricalExtraLights;
import dev.imabad.theatrical.api.dmx.DMXConsumer;
import dev.imabad.theatrical.util.UUIDUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Accès runtime aux réseaux Theatrical (package non exposé sur le classpath common).
 * Theatrical alpha.28.9999 a remplacé {@code dmx.DMXNetworkData} par
 * {@code networks.TheatricalNetworkData} + {@code TheatricalNetwork.dmx()}.
 */
public final class TheatricalNetworkAccess {

    private static final String[] DATA_CLASSES = {
            "dev.imabad.theatrical.networks.TheatricalNetworkData",
            "dev.imabad.theatrical.dmx.DMXNetworkData"
    };

    private static volatile boolean loggedMissingApi;

    private TheatricalNetworkAccess() {
    }

    public static boolean canPlayerConfigure(Player player, Level level, UUID networkId) {
        if (networkId.equals(UUIDUtil.NULL) || !(level instanceof ServerLevel)) {
            return true;
        }
        Object network = getNetwork(level, networkId);
        if (network == null) {
            return true;
        }
        try {
            Object members = network.getClass().getMethod("members").invoke(network);
            return (boolean) members.getClass().getMethod("isMember", UUID.class).invoke(members, player.getUUID());
        } catch (ReflectiveOperationException ignored) {
            return true;
        }
    }

    public static String getNetworkName(Level level, UUID networkId) {
        if (networkId.equals(UUIDUtil.NULL)) {
            return "—";
        }
        if (!(level instanceof ServerLevel)) {
            return networkId.toString();
        }
        Object network = getNetwork(level, networkId);
        if (network == null) {
            return "Unknown";
        }
        try {
            return (String) network.getClass().getMethod("name").invoke(network);
        } catch (ReflectiveOperationException ignored) {
            return networkId.toString();
        }
    }

    public static void addConsumer(Level level, UUID networkId, BlockPos pos, DMXConsumer consumer) {
        Object dmx = resolveDmxManager(level, networkId);
        if (dmx == null) {
            return;
        }
        try {
            dmx.getClass().getMethod("addConsumer", BlockPos.class, DMXConsumer.class).invoke(dmx, pos, consumer);
        } catch (ReflectiveOperationException e) {
            TheatricalExtraLights.LOGGER.warn("[theatricalextralights] Unable to register LED facade DMX consumer", e);
        }
    }

    public static void removeConsumer(Level level, UUID networkId, BlockPos pos, DMXConsumer consumer) {
        Object dmx = resolveDmxManager(level, networkId);
        if (dmx == null) {
            return;
        }
        try {
            dmx.getClass().getMethod("removeConsumer", DMXConsumer.class, BlockPos.class).invoke(dmx, consumer, pos);
        } catch (ReflectiveOperationException e) {
            TheatricalExtraLights.LOGGER.warn("[theatricalextralights] Unable to unregister LED facade DMX consumer", e);
        }
    }

    private static Object resolveDmxManager(Level level, UUID networkId) {
        Object network = getNetwork(level, networkId);
        if (network == null) {
            return null;
        }
        try {
            Method dmx = network.getClass().getMethod("dmx");
            return dmx.invoke(network);
        } catch (ReflectiveOperationException ignored) {
            return network;
        }
    }

    private static Object getNetwork(Level level, UUID networkId) {
        Object data = getNetworkData(level);
        if (data == null) {
            return null;
        }
        try {
            return data.getClass().getMethod("getNetwork", UUID.class).invoke(data, networkId);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static Object getNetworkData(Level level) {
        if (level == null || level.getServer() == null) {
            return null;
        }
        Level overworld = level.getServer().overworld();
        for (String className : DATA_CLASSES) {
            Object data = invokeGetInstance(className, overworld);
            if (data != null) {
                return data;
            }
        }
        if (!loggedMissingApi) {
            loggedMissingApi = true;
            TheatricalExtraLights.LOGGER.warn(
                    "[theatricalextralights] Theatrical DMX network API not found (neither TheatricalNetworkData nor DMXNetworkData)");
        }
        return null;
    }

    private static Object invokeGetInstance(String className, Level overworld) {
        try {
            Class<?> clazz = Class.forName(className);
            try {
                return clazz.getMethod("getInstance", Level.class).invoke(null, overworld);
            } catch (NoSuchMethodException ignored) {
                return clazz.getMethod("getInstance", ServerLevel.class).invoke(null, overworld);
            }
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }
}
