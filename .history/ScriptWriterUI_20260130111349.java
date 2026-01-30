import javax.swing.*;
import java.awt.*;
import java.net.*;

public class ScriptWriterUI extends JFrame {

    private JTextField scriptField;
    private JButton sendButton;
    private DatagramSocket socket;
    private InetAddress address;
    private int port = 5000;

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
            socket = new DatagramSocket();
            address = InetAddress.getByName("127.0.0.1");
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
                    new DatagramPacket(buffer, buffer.length, address, port);

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
