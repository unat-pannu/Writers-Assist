import java.net.*;
import java.io.*;

public class ScriptWriterReceiver {

    public static void main(String[] args) throws IOException {

        InetAddress group = InetAddress.getByName("230.0.0.1");
        int port = 4446;

        MulticastSocket socket = new MulticastSocket(port);
        socket.joinGroup(group);

        System.out.println("📝 Script Writer Receiver");
        System.out.println("Waiting for script updates...\n");

        while (true) {
            byte[] buffer = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            socket.receive(packet);

            String received =
                    new String(packet.getData(), 0, packet.getLength());

            System.out.println("📜 New Script Line: " + received);
        }
    }
}
