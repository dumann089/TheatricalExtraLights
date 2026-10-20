package com.github.dumann089.theatricalextralights.laser.dac;

/**
 * Ether Dream / j4cDAC wire format.
 * <p>
 * Spec: https://ether-dream.com/protocol.html
 * Packed little-endian structs, no padding. TCP 7765, UDP broadcast 7654.
 */
public final class LaserProtocol {

    public static final int TCP_PORT = 7765;
    public static final int BROADCAST_PORT = 7654;

    public static final int POINT_BYTES = 18;
    public static final int STATUS_BYTES = 20;
    public static final int RESPONSE_BYTES = 22;
    public static final int BROADCAST_BYTES = 36;

    public static final byte CMD_PREPARE = 0x70;      // 'p'
    public static final byte CMD_BEGIN = 0x62;        // 'b'
    /** Spec HTML lists 0x74 by mistake; firmware and libetherdream send ASCII 'q' (0x71). */
    public static final byte CMD_QUEUE_RATE = 0x71;   // 'q'
    public static final byte CMD_QUEUE_RATE_SPEC_TYPO = 0x74;
    public static final byte CMD_WRITE = 0x64;        // 'd'
    public static final byte CMD_STOP = 0x73;         // 's'
    public static final byte CMD_CLEAR_ESTOP = 0x63;  // 'c'
    public static final byte CMD_PING = 0x3F;         // '?'
    public static final byte CMD_ESTOP_0 = 0x00;
    public static final byte CMD_ESTOP_FF = (byte) 0xFF;

    public static final byte ACK = 0x61;              // 'a'
    public static final byte NAK_FULL = 0x46;         // 'F'
    public static final byte NAK_INVALID = 0x49;      // 'I'
    public static final byte NAK_STOP = 0x21;         // '!'

    public static final int LIGHT_READY = 0;
    public static final int LIGHT_WARMUP = 1;
    public static final int LIGHT_COOLDOWN = 2;
    public static final int LIGHT_ESTOP = 3;

    public static final int PLAYBACK_IDLE = 0;
    public static final int PLAYBACK_PREPARED = 1;
    public static final int PLAYBACK_PLAYING = 2;

    public static final int SOURCE_NETWORK = 0;

    public static final int LE_FLAG_ESTOP_PACKET = 1;
    public static final int PLAY_FLAG_SHUTTER = 1;
    public static final int PLAY_FLAG_UNDERFLOW = 2;
    public static final int PLAY_FLAG_ESTOP = 4;

    public static final int CTRL_CHANGE_RATE = 0x8000;

    public static final int DEFAULT_BUFFER = 1800;
    public static final int DEFAULT_MAX_PPS = 100_000;
    public static final int RATE_QUEUE_SIZE = 16;

    /** Locally-administered unicast OUI nibble so we never collide with a real NIC. */
    public static final byte[] DEFAULT_MAC = new byte[]{0x02, 0x00, 0x00, (byte) 0xED, 0x01, 0x00};

    public static final int HW_REVISION = 2;
    public static final int SW_REVISION = 2;

    private LaserProtocol() {
    }

    public static boolean isEstop(byte command) {
        return command == CMD_ESTOP_0 || command == CMD_ESTOP_FF;
    }
}
