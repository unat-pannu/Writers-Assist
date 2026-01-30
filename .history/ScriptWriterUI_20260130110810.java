import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.net.*;

public class ScriptWriterUI extends JFrame {

    private JTextField scriptField;
    private JButton sendButton;

    private InetAddress group;
    private int port = 4446;
    private MulticastSocket socket;

    public ScriptWriterUI() {
        setTitle("🎬 Writers Assist – Script Writer");
        setSize(500, 150);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JLabel title = new JLabel("Enter Script Line:", JLabel.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 14));

        scriptField = new JTextField();
        sendButton = new JButton("Send Line");

        add(title, BorderLayout.NORTH);
        add(scriptField, BorderLayout.CENTER);
        add(sendButton, BorderLayout.SOUTH);

        try {
group = InetAddress.getByName("230.0.0.1");
socket = new MulticastSocket();

// Force multicast over Wi-Fi (en0)
NetworkInterface ni = NetworkInterface.getByName("en0");
socket.setNetworkInterface(ni);

        } catch (Exception e) {
            e.printStackTrace();
        }

        sendButton.addActionListener(e -> sendScript());
        scriptField.addActionListener(e -> sendScript());

        setVisible(true);
    }

    private void sendScript() {
        try {
            String text = scriptField.getText().trim();
            if (text.isEmpty()) return;

            byte[] buffer = text.getBytes();
            DatagramPacket packet =
                    new DatagramPacket(buffer, buffer.length, group, port);

            socket.send(packet);
            scriptField.setText("");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new ScriptWriterUI();
    }
}
