package com.github.dumann089.theatricalextralights.laser.dac;

/**
 * Snapshot safe to read from the game thread / GUI.
 */
public final class LaserDacStatus {

    public static final LaserDacStatus DOWN = new LaserDacStatus(
            false, false, LaserProtocol.LIGHT_READY, LaserProtocol.PLAYBACK_IDLE,
            0, LaserProtocol.DEFAULT_BUFFER, 0, 0L, ""
    );

    public final boolean listening;
    public final boolean connected;
    public final int lightEngineState;
    public final int playbackState;
    public final int bufferFullness;
    public final int bufferCapacity;
    public final int pointRate;
    public final long pointCount;
    public final String remoteHost;

    public LaserDacStatus(boolean listening, boolean connected, int lightEngineState, int playbackState,
                          int bufferFullness, int bufferCapacity, int pointRate, long pointCount,
                          String remoteHost) {
        this.listening = listening;
        this.connected = connected;
        this.lightEngineState = lightEngineState;
        this.playbackState = playbackState;
        this.bufferFullness = bufferFullness;
        this.bufferCapacity = bufferCapacity;
        this.pointRate = pointRate;
        this.pointCount = pointCount;
        this.remoteHost = remoteHost == null ? "" : remoteHost;
    }

    public boolean playing() {
        return playbackState == LaserProtocol.PLAYBACK_PLAYING;
    }
}
