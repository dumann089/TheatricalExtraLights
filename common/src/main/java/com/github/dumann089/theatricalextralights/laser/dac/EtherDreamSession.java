package com.github.dumann089.theatricalextralights.laser.dac;

import com.github.dumann089.theatricalextralights.TheatricalExtraLights;
import com.github.dumann089.theatricalextralights.config.TheatricalExtraLightsConfig;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;

/**
 * One Ether Dream TCP control connection. The DAC accepts a single host;
 * extra connects are refused by {@link EtherDreamServer}.
 */
final class EtherDreamSession implements Runnable {

    private final EtherDreamDevice device;
    private final Socket socket;
    private final byte[] response = new byte[LaserProtocol.RESPONSE_BYTES];
    private final byte[] header = new byte[7];
    private final byte[] pointScratch = new byte[LaserProtocol.POINT_BYTES];

    EtherDreamSession(EtherDreamDevice device, Socket socket) {
        this.device = device;
        this.socket = socket;
    }

    @Override
    public void run() {
        String remote = socket.getRemoteSocketAddress() != null
                ? socket.getRemoteSocketAddress().toString()
                : "?";
        device.onHostConnected(remote);
        TheatricalExtraLights.LOGGER.info("[EtherDream] host connected {}", remote);
        try {
            socket.setTcpNoDelay(true);
            socket.setSoTimeout(0);
            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();
            reply(out, LaserProtocol.ACK, LaserProtocol.CMD_PING);
            while (!socket.isClosed()) {
                int cmd = in.read();
                if (cmd < 0) {
                    break;
                }
                handle((byte) cmd, in, out);
            }
        } catch (SocketException closed) {
            // host hung up
        } catch (EOFException ignored) {
            // clean close
        } catch (IOException e) {
            TheatricalExtraLights.LOGGER.debug("[EtherDream] session ended: {}", e.toString());
        } finally {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
            device.onHostDisconnected();
            TheatricalExtraLights.LOGGER.info("[EtherDream] host disconnected {}", remote);
        }
    }

    private void handle(byte command, InputStream in, OutputStream out) throws IOException {
        if (LaserProtocol.isEstop(command) || isUnknown(command)) {
            if (isUnknown(command) && !LaserProtocol.isEstop(command)) {
                TheatricalExtraLights.LOGGER.info("[EtherDream] unknown command 0x{} ('{}'), treating as e-stop",
                        Integer.toHexString(command & 0xFF), (char) command);
            } else {
                TheatricalExtraLights.LOGGER.info("[EtherDream] e-stop received (0x{})", Integer.toHexString(command & 0xFF));
            }
            device.emergencyStop();
            reply(out, LaserProtocol.ACK, command);
            return;
        }
        switch (command) {
            case LaserProtocol.CMD_PREPARE -> {
                TheatricalExtraLights.LOGGER.info("[EtherDream] prepare");
                reply(out, device.prepare(), command);
            }
            case LaserProtocol.CMD_BEGIN -> {
                readFully(in, header, 0, 6);
                int rate = EtherDreamDevice.readU32(header, 2);
                TheatricalExtraLights.LOGGER.info("[EtherDream] begin rate={} lowWater={}", rate, EtherDreamDevice.readU16(header, 0));
                reply(out, device.begin(rate), command);
            }
            case LaserProtocol.CMD_QUEUE_RATE, LaserProtocol.CMD_QUEUE_RATE_SPEC_TYPO -> {
                readFully(in, header, 0, 4);
                int rate = EtherDreamDevice.readU32(header, 0);
                TheatricalExtraLights.LOGGER.info("[EtherDream] queue rate={}", rate);
                reply(out, device.queueRate(rate), command);
            }
            case LaserProtocol.CMD_WRITE -> {
                readFully(in, header, 0, 2);
                int npoints = EtherDreamDevice.readU16(header, 0);
                reply(out, ingestPoints(in, npoints), command);
            }
            case LaserProtocol.CMD_STOP -> {
                TheatricalExtraLights.LOGGER.info("[EtherDream] stop");
                reply(out, device.stopCommand(), command);
            }
            case LaserProtocol.CMD_CLEAR_ESTOP -> reply(out, device.clearEstop(), command);
            case LaserProtocol.CMD_PING -> reply(out, device.ping(), command);
            default -> {
                device.emergencyStop();
                reply(out, LaserProtocol.ACK, command);
            }
        }
    }

    private byte ingestPoints(InputStream in, int npoints) throws IOException {
        if (npoints < 0) {
            npoints = 0;
        }
        if (npoints == 0) {
            return device.writePoints(new LaserPoint[0]);
        }
        LaserPoint[] points = new LaserPoint[npoints];
        for (int i = 0; i < npoints; i++) {
            readFully(in, pointScratch, 0, LaserProtocol.POINT_BYTES);
            points[i] = decodePoint(pointScratch);
        }
        return device.writePoints(points);
    }

    private static LaserPoint decodePoint(byte[] raw) {
        int control = EtherDreamDevice.readU16(raw, 0);
        short x = (short) EtherDreamDevice.readI16(raw, 2);
        short y = (short) EtherDreamDevice.readI16(raw, 4);
        int r = EtherDreamDevice.readU16(raw, 6);
        int g = EtherDreamDevice.readU16(raw, 8);
        int b = EtherDreamDevice.readU16(raw, 10);
        int i = EtherDreamDevice.readU16(raw, 12);
        return new LaserPoint(control, x, y, r, g, b, i);
    }

    private long lastNakLog;

    private void reply(OutputStream out, byte responseCode, byte command) throws IOException {
        if (responseCode != LaserProtocol.ACK) {
            long now = System.nanoTime();
            if (now - lastNakLog > 1_000_000_000L) {
                lastNakLog = now;
                TheatricalExtraLights.LOGGER.info("[EtherDream] NAK '{}' to command '{}' (0x{})",
                        (char) responseCode, (char) command, Integer.toHexString(command & 0xFF));
            }
        }
        device.writeResponse(response, responseCode, command);
        out.write(response);
        out.flush();
    }

    private static boolean isUnknown(byte command) {
        return command != LaserProtocol.CMD_PREPARE
                && command != LaserProtocol.CMD_BEGIN
                && command != LaserProtocol.CMD_QUEUE_RATE
                && command != LaserProtocol.CMD_QUEUE_RATE_SPEC_TYPO
                && command != LaserProtocol.CMD_WRITE
                && command != LaserProtocol.CMD_STOP
                && command != LaserProtocol.CMD_CLEAR_ESTOP
                && command != LaserProtocol.CMD_PING
                && !LaserProtocol.isEstop(command);
    }

    private static void readFully(InputStream in, byte[] buf, int off, int len) throws IOException {
        int n = 0;
        while (n < len) {
            int r = in.read(buf, off + n, len - n);
            if (r < 0) {
                throw new EOFException();
            }
            n += r;
        }
    }

    private static void discard(InputStream in, long bytes) throws IOException {
        byte[] skip = new byte[1024];
        long left = bytes;
        while (left > 0) {
            int n = in.read(skip, 0, (int) Math.min(skip.length, left));
            if (n < 0) {
                throw new EOFException();
            }
            left -= n;
        }
    }

}
