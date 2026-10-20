package com.github.dumann089.theatricalextralights.laser.dac;

import com.github.dumann089.theatricalextralights.TheatricalExtraLights;
import com.github.dumann089.theatricalextralights.config.TheatricalExtraLightsConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Client-side lifecycle for the virtual Ether Dream. CloudLase talks to
 * the machine running Minecraft, so the DAC listens on the client.
 */
@Environment(EnvType.CLIENT)
public final class LaserDacRuntime {

    private static EtherDreamDevice device;
    private static EtherDreamServer server;

    private LaserDacRuntime() {
    }

    public static void ensureStarted() {
        if (server == null || !server.isRunning()) {
            start();
        }
    }

    public static synchronized void start() {
        if (!TheatricalExtraLightsConfig.isEtherDreamEnabled()) {
            TheatricalExtraLights.LOGGER.info("[EtherDream] disabled in config");
            return;
        }
        if (server != null && server.isRunning()) {
            return;
        }
        stop();
        TheatricalExtraLights.LOGGER.info("[EtherDream] starting virtual DAC (TCP {} / UDP {})",
                TheatricalExtraLightsConfig.getEtherDreamTcpPort(),
                TheatricalExtraLightsConfig.getEtherDreamBroadcastPort());
        device = new EtherDreamDevice(
                EtherDreamDevice.DEFAULT_ID,
                TheatricalExtraLightsConfig.getEtherDreamBufferCapacity()
        );
        LaserDacHub.register(device);
        server = new EtherDreamServer(device);
        server.start();
    }

    public static synchronized void stop() {
        if (server != null) {
            server.stop();
            server = null;
        }
        if (device != null) {
            LaserDacHub.unregister(device.id());
            device = null;
        }
    }

    public static EtherDreamDevice device() {
        return device;
    }
}
