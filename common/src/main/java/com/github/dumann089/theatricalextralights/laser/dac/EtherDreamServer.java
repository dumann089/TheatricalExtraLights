package com.github.dumann089.theatricalextralights.laser.dac;

import com.github.dumann089.theatricalextralights.TheatricalExtraLights;
import com.github.dumann089.theatricalextralights.config.TheatricalExtraLightsConfig;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Ether Dream network face: UDP identity broadcast on 7654 (1 Hz) and a
 * single-host TCP control socket on 7765.
 */
public final class EtherDreamServer {

    private final EtherDreamDevice device;
    private final AtomicBoolean running = new AtomicBoolean();
    private final AtomicReference<Socket> active = new AtomicReference<>();

    private Thread acceptThread;
    private Thread broadcastThread;
    private ServerSocket serverSocket;

    public EtherDreamServer(EtherDreamDevice device) {
        this.device = device;
    }

    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        device.setListening(false);
        acceptThread = new Thread(this::acceptLoop, "tel-etherdream-tcp");
        acceptThread.setDaemon(true);
        acceptThread.start();
        broadcastThread = new Thread(this::broadcastLoop, "tel-etherdream-udp");
        broadcastThread.setDaemon(true);
        broadcastThread.start();
    }

    public void stop() {
        running.set(false);
        device.setListening(false);
        ServerSocket server = serverSocket;
        if (server != null) {
            try {
                server.close();
            } catch (IOException ignored) {
            }
        }
        Socket socket = active.getAndSet(null);
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
        if (acceptThread != null) {
            acceptThread.interrupt();
        }
        if (broadcastThread != null) {
            broadcastThread.interrupt();
        }
        device.stop();
    }

    public boolean isRunning() {
        return running.get();
    }

    private void acceptLoop() {
        String bind = TheatricalExtraLightsConfig.getEtherDreamBindAddress();
        int port = TheatricalExtraLightsConfig.getEtherDreamTcpPort();
        try (ServerSocket server = new ServerSocket()) {
            server.setReuseAddress(true);
            InetAddress address = bind == null || bind.isBlank() || "0.0.0.0".equals(bind)
                    ? null
                    : InetAddress.getByName(bind.trim());
            server.bind(new InetSocketAddress(address, port));
            serverSocket = server;
            device.setListening(true);
            device.start();
            TheatricalExtraLights.LOGGER.info("[EtherDream] listening on {}:{}", bind, port);
            while (running.get()) {
                Socket incoming = server.accept();
                Socket previous = active.get();
                if (previous != null && !previous.isClosed()) {
                    TheatricalExtraLights.LOGGER.debug("[EtherDream] rejecting extra connection from {}",
                            incoming.getRemoteSocketAddress());
                    incoming.close();
                    continue;
                }
                incoming.setTcpNoDelay(true);
                active.set(incoming);
                Thread session = new Thread(new EtherDreamSession(device, incoming), "tel-etherdream-session");
                session.setDaemon(true);
                session.start();
            }
        } catch (SocketException closed) {
            if (running.get()) {
                TheatricalExtraLights.LOGGER.error("[EtherDream] TCP bind failed on {}:{} — {}", bind, port, closed.toString());
            }
        } catch (IOException e) {
            if (running.get()) {
                TheatricalExtraLights.LOGGER.error("[EtherDream] TCP server failed", e);
            }
        } finally {
            device.setListening(false);
        }
    }

    private void broadcastLoop() {
        byte[] payload = new byte[LaserProtocol.BROADCAST_BYTES];
        byte[] mac = TheatricalExtraLightsConfig.getEtherDreamMac();
        int udpPort = TheatricalExtraLightsConfig.getEtherDreamBroadcastPort();
        boolean logged = false;
        while (running.get()) {
            device.writeBroadcast(payload, mac);
            int sent = advertise(payload, udpPort);
            if (!logged) {
                TheatricalExtraLights.LOGGER.info("[EtherDream] advertising on UDP {} ({} packets/s)", udpPort, sent);
                logged = true;
            }
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    /**
     * CloudLase listens on UDP 7654. Same-machine Windows often drops
     * 255.255.255.255, so we also unicast from each local IPv4 (source IP =
     * the address CloudLase will TCP-connect on 7765).
     */
    private static int advertise(byte[] payload, int udpPort) {
        int sent = 0;
        for (InetAddress local : localIpv4()) {
            sent += sendFrom(local, payload, udpPort, true);
        }
        try {
            sent += sendFrom(InetAddress.getByName("127.0.0.1"), payload, udpPort, false);
        } catch (IOException ignored) {
        }
        return sent;
    }

    private static int sendFrom(InetAddress local, byte[] payload, int udpPort, boolean includeBroadcast) {
        int sent = 0;
        try (DatagramSocket udp = new DatagramSocket(new InetSocketAddress(local, 0))) {
            udp.setBroadcast(true);
            udp.setReuseAddress(true);
            List<InetAddress> targets = new ArrayList<>();
            targets.add(local);
            if (includeBroadcast) {
                targets.add(InetAddress.getByName("255.255.255.255"));
                NetworkInterface nic = NetworkInterface.getByInetAddress(local);
                if (nic != null) {
                    nic.getInterfaceAddresses().forEach(addr -> {
                        if (addr.getAddress().equals(local) && addr.getBroadcast() != null) {
                            targets.add(addr.getBroadcast());
                        }
                    });
                }
            }
            for (InetAddress target : targets) {
                try {
                    udp.send(new DatagramPacket(payload, payload.length, target, udpPort));
                    sent++;
                } catch (IOException ignored) {
                }
            }
        } catch (IOException ignored) {
        }
        return sent;
    }

    private static List<InetAddress> localIpv4() {
        List<InetAddress> locals = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> nics = NetworkInterface.getNetworkInterfaces();
            while (nics != null && nics.hasMoreElements()) {
                NetworkInterface nic = nics.nextElement();
                if (!nic.isUp() || nic.isLoopback() || nic.isVirtual()) {
                    continue;
                }
                nic.getInterfaceAddresses().forEach(addr -> {
                    InetAddress ip = addr.getAddress();
                    if (ip != null && !ip.isLoopbackAddress() && ip.getAddress().length == 4) {
                        locals.add(ip);
                    }
                });
            }
        } catch (SocketException ignored) {
        }
        return locals;
    }
}
