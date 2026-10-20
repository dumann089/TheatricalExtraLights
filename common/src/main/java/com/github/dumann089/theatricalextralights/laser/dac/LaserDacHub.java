package com.github.dumann089.theatricalextralights.laser.dac;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Maps virtual DAC ids to devices. V1 exposes a single Ether Dream;
 * extra projectors / protocols register here later without touching DMX.
 */
public final class LaserDacHub {

    private static final Map<String, LaserDacDevice> DEVICES = new ConcurrentHashMap<>();

    private LaserDacHub() {
    }

    public static void register(LaserDacDevice device) {
        DEVICES.put(device.id(), device);
    }

    public static void unregister(String id) {
        DEVICES.remove(id);
    }

    public static LaserDacDevice get(String id) {
        if (id == null || id.isBlank()) {
            return DEVICES.get(EtherDreamDevice.DEFAULT_ID);
        }
        LaserDacDevice device = DEVICES.get(id);
        return device != null ? device : DEVICES.get(EtherDreamDevice.DEFAULT_ID);
    }

    public static LaserFrame frame(String dacId) {
        LaserDacDevice device = get(dacId);
        return device == null ? LaserFrame.EMPTY : device.currentFrame();
    }

    public static LaserDacStatus status(String dacId) {
        LaserDacDevice device = get(dacId);
        return device == null ? LaserDacStatus.DOWN : device.status();
    }

    public static boolean hasActiveOutput(String dacId) {
        LaserFrame frame = frame(dacId);
        return frame.isFresh(System.nanoTime(), 250_000_000L);
    }
}
