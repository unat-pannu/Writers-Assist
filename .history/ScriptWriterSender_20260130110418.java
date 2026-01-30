import java.net.*;
import java.io.*;

public class ScriptWriterSender {

    public static void main(String[] args) throws IOException {

        InetAddress group = InetAddress.getByName("230.0.0.1");
        int port = 4446;

        MulticastSocket socket = new MulticastSocket();

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(System.in));

        System.out.println("🎬 Script Writer Sender");
        System.out.println("Type script lines (type 'exit' to quit):");

        while (true) {
            String scriptLine = reader.readLine();

            if (scriptLine.equalsIgnoreCase("exit")) {
                break;
            }

            byte[] buffer = scriptLine.getBytes();

            DatagramPacket packet =
                    new DatagramPacket(buffer, buffer.length, group, port);

            socket.send(packet);
        }

        socket.close();
        System.out.println("Sender closed.");
    }
}
