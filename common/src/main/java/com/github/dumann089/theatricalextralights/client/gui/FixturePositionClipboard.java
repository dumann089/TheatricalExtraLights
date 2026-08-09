package com.github.dumann089.theatricalextralights.client.gui;

/**
 * Client-side clipboard for fixture pan/tilt — copy from one fixture UI and paste into another.
 */
public final class FixturePositionClipboard {

    private static boolean hasValue;
    private static int pan;
    private static int tilt;

    private FixturePositionClipboard() {
    }

    public static void copy(int pan, int tilt) {
        FixturePositionClipboard.pan = pan;
        FixturePositionClipboard.tilt = tilt;
        hasValue = true;
    }

    public static boolean hasValue() {
        return hasValue;
    }

    public static int getPan() {
        return pan;
    }

    public static int getTilt() {
        return tilt;
    }
}
