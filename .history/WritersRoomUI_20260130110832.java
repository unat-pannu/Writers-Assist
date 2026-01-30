import javax.swing.*;
import java.awt.*;
import java.net.*;

public class WritersRoomUI extends JFrame {

    private JTextArea scriptArea;
    private MulticastSocket socket;
    private InetAddress group;
    private int port = 4446;

    public WritersRoomUI() {
        setTitle("📝 Writers Assist – Writers Room");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        scriptArea = new JTextArea();
        scriptArea.setEditable(false);
        scriptArea.setFont(new Font("Monospaced", Font.PLAIN, 14));

        JScrollPane scrollPane = new JScrollPane(scriptArea);
        add(scrollPane);

        setVisible(true);

        startReceiving();
    }

    private void startReceiving() {
        new Thread(() -> {
            try {
                group = InetAddress.getByName("230.0.0.1");
                socket = new MulticastSocket(port);

NetworkInterface ni = NetworkInterface.getByName("en0");
socket.joinGroup(new InetSocketAddress(group, port), ni);


                while (true) {
                    byte[] buffer = new byte[1024];
                    DatagramPacket packet =
                            new DatagramPacket(buffer, buffer.length);

                    socket.receive(packet);

                    String msg = new String(
                            packet.getData(), 0, packet.getLength());

                    scriptArea.append("📜 " + msg + "\n");
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public static void main(String[] args) {
        new WritersRoomUI();
    }
}
