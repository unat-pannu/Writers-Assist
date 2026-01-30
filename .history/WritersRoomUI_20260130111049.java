import javax.swing.*;
import java.awt.*;
import java.net.*;

public class WritersRoomUI extends JFrame {

    private JTextArea scriptArea;
    private DatagramSocket socket;
    private int port = 5000;

    public WritersRoomUI() {
        setTitle("📝 Writers Assist – Writers Room");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        scriptArea = new JTextArea();
        scriptArea.setEditable(false);
        scriptArea.setFont(new Font("Monospaced", Font.PLAIN, 14));

        add(new JScrollPane(scriptArea));
        setVisible(true);

        startReceiving();
    }

    private void startReceiving() {
        new Thread(() -> {
            try {
                socket = new DatagramSocket(port);

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
