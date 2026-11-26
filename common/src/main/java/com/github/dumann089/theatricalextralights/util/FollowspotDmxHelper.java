
package com.github.dumann089.theatricalextralights.util;

import net.minecraft.util.Mth;

public final class FollowspotDmxHelper {

    public static final float PAN_TILT_STEP = 2.0f;
    public static final int MAX_DMX_ADDRESS =
            512 - FollowspotTargetHelper.REQUIRED_CHANNEL_COUNT + 1;

    private FollowspotDmxHelper() {
    }

    public static boolean isValidDmxAddress(int address) {
        return address >= 1 && address <= MAX_DMX_ADDRESS;
    }

    public static int panToDmxByte(int pan) {
        return Math.round(
                (Mth.clamp(pan, -90, 90) + 90f) / 180f * 255f
        );
    }

    public static int tiltToDmxByte(int tilt) {
        return Math.round(
                (Mth.clamp(tilt, -45, 45) + 45f) / 90f * 255f
        );
    }

    public static int dmxByteToPan(int dmx) {
        return (int) ((dmx * 180) / 255f) - 90;
    }

    public static int dmxByteToTilt(int dmx) {
        return (int) ((dmx * 90) / 255f) - 45;
    }

    public static int quantizePan(float pan) {
        return Math.round(pan);
    }

    public static int quantizeTilt(float tilt) {
        return Math.round(tilt);
    }

    public static float quantizePanAngle(float pan) {
        return quantizePan(pan);
    }

    public static float quantizeTiltAngle(float tilt) {
        return quantizeTilt(tilt);
    }
}
