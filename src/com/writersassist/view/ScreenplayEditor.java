package com.writersassist.view;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.*;
import javafx.scene.text.Text;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.HashMap;
import java.util.Map;

import com.writersassist.lab7.ScriptService;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.io.IOException;

/**
 * High-fidelity Screenplay Editor with Industry Formatting logic.
 */
public class ScreenplayEditor extends VBox {

    private TextArea editor;
    private ComboBox<FormatType> formatSelector;
    private Label wordCountLabel;

    // RMI Services (for saving to cloud)
    private ScriptService scriptService;

    // UDP Multicast Services
    private static final String GROUP_ADDRESS = "230.0.0.1";
    private static final int MULTICAST_PORT = 4446;
    private MulticastSocket multicastSocket;
    private InetAddress multicastGroup;
    private String collaborationUsername;

    // Notification Panel
    private Label notificationLabel;

    // Real-time synchronization flag
    private boolean isUpdatingFromServer = false;

    public enum FormatType {
        SCENE_HEADING("Scene Heading"),
        ACTION("Action"),
        CHARACTER("Character"),
        DIALOGUE("Dialogue"),
        PARENTHETICAL("Parenthetical");

        private String label;

        FormatType(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public ScreenplayEditor() {
        setSpacing(20);
        getStyleClass().add("editor-container");

        // Toolbar
        HBox toolbar = createToolbar();

        // The "Paper" container
        VBox paper = new VBox();
        paper.getStyleClass().add("screenplay-paper");

        editor = new TextArea();
        editor.setWrapText(true);
        editor.getStyleClass().add("screenplay-textarea");
        editor.setPrefHeight(2000); // Massive for page effect

        VBox.setVgrow(editor, Priority.ALWAYS);
        paper.getChildren().add(editor);

        ScrollPane scroll = new ScrollPane(paper);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        VBox.setVgrow(scroll, Priority.ALWAYS);

        // Notification Panel
        HBox notificationPanel = new HBox();
        notificationPanel.setAlignment(Pos.CENTER_LEFT);
        notificationPanel.setPadding(new Insets(10, 15, 10, 15));
        notificationPanel.setStyle(
                "-fx-background-color: -bg-surface; -fx-border-color: -border-color; -fx-border-width: 1 0 0 0;");
        notificationLabel = new Label("Ready");
        notificationLabel.setStyle("-fx-text-fill: -accent; -fx-font-size: 12px;");
        notificationPanel.getChildren().add(notificationLabel);

        this.getChildren().addAll(toolbar, scroll, notificationPanel);

        setupFormattingLogic();
        setupNetworking();
    }

    private HBox createToolbar() {
        HBox toolbar = new HBox(15);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(0, 0, 10, 0));

        formatSelector = new ComboBox<>();
        formatSelector.getItems().addAll(FormatType.values());
        formatSelector.setValue(FormatType.ACTION);
        formatSelector.getStyleClass().add("btn-secondary");

        Button btnSaveCloud = new Button("Save Script to Cloud");
        btnSaveCloud.getStyleClass().add("btn-primary");
        btnSaveCloud.setOnAction(e -> saveToCloud());

        Button btnLoadCloud = new Button("Load Script from Cloud");
        btnLoadCloud.getStyleClass().add("btn-secondary");
        btnLoadCloud.setOnAction(e -> loadFromCloud());

        toolbar.getChildren().addAll(new Text("Format:"), formatSelector, btnSaveCloud, btnLoadCloud);
        return toolbar;
    }

    private void setupFormattingLogic() {
        // Shortcut logic
        editor.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case TAB:
                    cycleFormat();
                    e.consume();
                    break;
                case ENTER:
                    handleEnter();
                    break;
            }
        });

        editor.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!isUpdatingFromServer && multicastSocket != null && !multicastSocket.isClosed()) {
                broadcastMessage("SYNC", newVal);
            }
        });
    }

    private void setupNetworking() {
        // RMI Services
        try {
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            scriptService = (ScriptService) registry.lookup("ScriptService");
        } catch (Exception e) {
            System.out.println("Could not connect to RMI ScriptService: " + e.getMessage());
        }

        // UDP Multicast Collaboration
        try {
            multicastGroup = InetAddress.getByName(GROUP_ADDRESS);
            multicastSocket = new MulticastSocket(MULTICAST_PORT);
            multicastSocket.joinGroup(multicastGroup);

            collaborationUsername = "Writer_" + (int) (Math.random() * 1000);

            // Start UDP Listener Thread
            Thread listenerThread = new Thread(() -> {
                while (!multicastSocket.isClosed()) {
                    try {
                        byte[] buffer = new byte[65535];
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        multicastSocket.receive(packet);

                        String message = new String(packet.getData(), 0, packet.getLength());

                        // Parse Format: USERNAME|TYPE|PAYLOAD
                        // Fallback to plain text if pipes are not found (for compatibility with
                        // standalone Sender tools)
                        int firstPipe = message.indexOf('|');
                        int secondPipe = message.indexOf('|', firstPipe + 1);

                        String sender;
                        String type;
                        String payload;

                        if (firstPipe != -1 && secondPipe != -1) {
                            sender = message.substring(0, firstPipe);
                            type = message.substring(firstPipe + 1, secondPipe);
                            payload = message.substring(secondPipe + 1);
                        } else {
                            sender = "Anonymous";
                            type = "SYNC";
                            payload = message;
                        }

                        if (!sender.equals(collaborationUsername)) {
                            Platform.runLater(() -> {
                                if (type.equals("SYNC")) {
                                    isUpdatingFromServer = true;
                                    int caretPosition = editor.getCaretPosition();
                                    editor.setText(payload);
                                    editor.positionCaret(Math.min(caretPosition, payload.length()));
                                    isUpdatingFromServer = false;
                                } else if (type.equals("EVENT")) {
                                    showNotification("Collaboration Update", payload);
                                }
                            });
                        }

                    } catch (IOException e) {
                        if (!multicastSocket.isClosed()) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            listenerThread.setDaemon(true);
            listenerThread.start();

            System.out.println("Connected to UDP Multicast as " + collaborationUsername);
            broadcastMessage("EVENT", "Joined the session");

        } catch (IOException e) {
            System.out.println("Could not connect to Multicast Group: " + e.getMessage());
            showNotification("Error", "Failed to connect to collaboration network.");
        }
    }

    private void broadcastMessage(String type, String payload) {
        if (multicastSocket != null && !multicastSocket.isClosed()) {
            try {
                String message = collaborationUsername + "|" + type + "|" + payload;
                byte[] buffer = message.getBytes();
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, multicastGroup, MULTICAST_PORT);
                multicastSocket.send(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void loadFromCloud() {
        if (scriptService == null) {
            showNotification("Error", "Cloud Service is not connected.");
            return;
        }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Load from Cloud");
        dialog.setHeaderText("Enter Script Title to load:");
        dialog.showAndWait().ifPresent(title -> {
            try {
                String content = scriptService.loadScript(title);
                editor.setText(content);
                showNotification("Success", "Loaded script: " + title);
            } catch (Exception e) {
                showNotification("Error", "Failed to load script: " + e.getMessage());
            }
        });
    }

    private void saveToCloud() {
        if (scriptService == null) {
            showNotification("Error", "Cloud Service is not connected.");
            return;
        }

        TextInputDialog dialog = new TextInputDialog("Untitled");
        dialog.setTitle("Save to Cloud");
        dialog.setHeaderText("Enter Script Title to save:");
        dialog.showAndWait().ifPresent(title -> {
            try {
                scriptService.saveScript(title, editor.getText());
                showNotification("Success", "Saved script to cloud: " + title);

                // Broadcast state update
                broadcastMessage("EVENT", "A user updated and saved the script: " + title);
            } catch (Exception e) {
                showNotification("Error", "Failed to save script: " + e.getMessage());
            }
        });
    }

    private void showNotification(String title, String message) {
        notificationLabel.setText(title + " | " + message);

        new Thread(() -> {
            try {
                Thread.sleep(5000);
                Platform.runLater(() -> {
                    if (notificationLabel.getText().equals(title + " | " + message)) {
                        notificationLabel.setText("Ready");
                    }
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void cycleFormat() {
        FormatType current = formatSelector.getValue();
        FormatType next;
        switch (current) {
            case SCENE_HEADING:
                next = FormatType.ACTION;
                break;
            case ACTION:
                next = FormatType.CHARACTER;
                break;
            case CHARACTER:
                next = FormatType.DIALOGUE;
                break;
            case DIALOGUE:
                next = FormatType.CHARACTER;
                break;
            default:
                next = FormatType.ACTION;
        }
        formatSelector.setValue(next);
        applyStyle(next);
    }

    private void handleEnter() {
        // Professional logic: If in Character, next is Dialogue. If in Dialogue, next
        // is Character.
        FormatType current = formatSelector.getValue();
        if (current == FormatType.CHARACTER) {
            formatSelector.setValue(FormatType.DIALOGUE);
        } else if (current == FormatType.SCENE_HEADING) {
            formatSelector.setValue(FormatType.ACTION);
        }
        applyStyle(formatSelector.getValue());

        // Broadcast on edit Event
        broadcastMessage("EVENT", "A user is writing new " + formatSelector.getValue() + " lines");
    }

    private void applyStyle(FormatType type) {
        // Industry-standard formatting logic
        // Scene Heading: ALL CAPS
        // Character: CENTERED, ALL CAPS
        // Dialogue: INDENTED
        System.out.println("Applying Formatting: " + type);
    }
}