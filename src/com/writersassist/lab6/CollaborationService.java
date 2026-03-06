package com.writersassist.lab6;

import java.io.*;
import java.net.*;
import java.util.function.Consumer;

/**
 * Lab 6: Socket Programming and UDP Multicast
 */
public class CollaborationService {
    private static final String MULTICAST_GROUP = "230.0.0.8";
    private static final int MULTICAST_PORT = 8888;
    private static final int TCP_PORT = 9999;

    private boolean running = false;

    public void startCollaborationServer(Consumer<String> onSyncReceived) {
        running = true;
        new Thread(() -> {
            try (ServerSocket server = new ServerSocket(0)) {
                while (running) {
                    try (Socket socket = server.accept();
                            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                        String content = in.readLine(); // Simplified for MVP
                        onSyncReceived.accept(content);
                    }
                }
            } catch (IOException e) {
                if (running)
                    e.printStackTrace();
            }
        }).start();
    }

    public static void syncScript(String host, String content) {
        new Thread(() -> {
            try (Socket socket = new Socket(host, TCP_PORT);
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
                out.println(content);
            } catch (IOException e) {
                System.err.println("Sync failed: " + e.getMessage());
            }
        }).start();
    }

    /**
     * UDP Multicast to broadcast events.
     */
    public static void broadcastEvent(String event) {
        new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket()) {
                byte[] buffer = event.getBytes();
                InetAddress group = InetAddress.getByName(MULTICAST_GROUP);
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, MULTICAST_PORT);
                socket.send(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void startNotificationListener(Consumer<String> onNotification) {
        new Thread(() -> {
            try (MulticastSocket socket = new MulticastSocket(MULTICAST_PORT)) {
                InetAddress group = InetAddress.getByName(MULTICAST_GROUP);
                NetworkInterface ni = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
                socket.joinGroup(new InetSocketAddress(group, MULTICAST_PORT), ni);

                while (running) {
                    byte[] buffer = new byte[256];
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    String msg = new String(packet.getData(), 0, packet.getLength());
                    onNotification.accept(msg);
                }
            } catch (IOException e) {
                if (running)
                    e.printStackTrace();
            }
        }).start();
    }

    public void stop() {
        running = false;
    }
}
