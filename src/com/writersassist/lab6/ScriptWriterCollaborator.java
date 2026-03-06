package com.writersassist.lab6;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.io.*;

public class ScriptWriterCollaborator {
    private static final String GROUP_ADDRESS = "230.0.0.1";
    private static final int PORT = 4446;

    private MulticastSocket socket;
    private InetAddress group;

    private JFrame frame;
    private JTextArea scriptArea;
    private JTextField inputField;

    public void init() {
        try {
            group = InetAddress.getByName(GROUP_ADDRESS);
            socket = new MulticastSocket(PORT);
            socket.joinGroup(group);

            setupUI();
            startListener();

        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error connecting to network: " + e.getMessage());
            System.exit(1);
        }
    }

    private void setupUI() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        frame = new JFrame("Script Writer Collaborator");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 450);
        scriptArea = new JTextArea();
        scriptArea.setEditable(false);
        scriptArea.setFont(new Font("Monospaced", Font.PLAIN, 15));
        scriptArea.setLineWrap(true);
        scriptArea.setWrapStyleWord(true);
        scriptArea.setMargin(new Insets(10, 10, 10, 10));
        JScrollPane scrollPane = new JScrollPane(scriptArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Shared Script Document"));
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        inputField = new JTextField();
        inputField.setFont(new Font("SansSerif", Font.PLAIN, 16));
        inputField.setPreferredSize(new Dimension(0, 40));
        JButton sendButton = new JButton("Send");
        sendButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        sendButton.setPreferredSize(new Dimension(80, 40));

        ActionListener sendAction = e -> sendMessage();
        inputField.addActionListener(sendAction);
        sendButton.addActionListener(sendAction);
        bottomPanel.add(inputField, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(bottomPanel, BorderLayout.SOUTH);

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (socket != null && !socket.isClosed()) {
                    try {
                        socket.leaveGroup(group);
                        socket.close();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        inputField.requestFocusInWindow();
    }

    private void sendMessage() {
        String message = inputField.getText().trim();
        if (!message.isEmpty()) {
            try {
                byte[] buffer = message.getBytes();
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, PORT);
                socket.send(packet);
                inputField.setText("");
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(frame, "Failed to send message: " + e.getMessage());
            }
        }
    }

    private void startListener() {
        Thread listenerThread = new Thread(() -> {
            while (!socket.isClosed()) {
                try {
                    byte[] buffer = new byte[1024];
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);

                    String received = new String(packet.getData(), 0, packet.getLength());

                    SwingUtilities.invokeLater(() -> {
                        scriptArea.append(received + "\n");
                        scriptArea.setCaretPosition(scriptArea.getDocument().getLength());
                    });
                } catch (IOException e) {
                    if (!socket.isClosed()) {
                        e.printStackTrace();
                    }
                }
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new ScriptWriterCollaborator().init();
        });
    }
}
