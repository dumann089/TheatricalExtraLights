package com.github.dumann089.theatricalextralights.laser.dac;

/**
 * Virtual laser DAC sink. Ether Dream is the V1 implementation;
 * later protocols (IDN, …) implement the same surface.
 */
public interface LaserDacDevice {

    String id();

    String protocolName();

    LaserFrame currentFrame();

    LaserDacStatus status();

    void start();

    void stop();
}
