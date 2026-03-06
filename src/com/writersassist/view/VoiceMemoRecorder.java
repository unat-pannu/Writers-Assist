package com.writersassist.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javax.sound.sampled.*;
import java.io.File;

/**
 * Modern Voice Memo Recorder (Lab integration of Java Sound API).
 */
public class VoiceMemoRecorder extends VBox {

    private boolean recording = false;
    private TargetDataLine line;

    public VoiceMemoRecorder() {
        setSpacing(30);
        setPadding(new Insets(40));
        setAlignment(Pos.TOP_CENTER);

        Label title = new Label("Voice Memos");
        title.getStyleClass().add("h1");

        VBox recorderCard = new VBox(20);
        recorderCard.getStyleClass().add("modern-card");
        recorderCard.setMaxWidth(400);
        recorderCard.setAlignment(Pos.CENTER);

        Label micIcon = new Label("🎤");
        micIcon.setStyle("-fx-font-size: 60px; -fx-text-fill: -accent;");

        Button btnRecord = new Button("Start Recording");
        btnRecord.getStyleClass().add("btn-primary");
        btnRecord.setMaxWidth(Double.MAX_VALUE);

        Label status = new Label("Ready to record");
        status.getStyleClass().add("p");

        btnRecord.setOnAction(e -> {
            if (!recording) {
                recording = true;
                btnRecord.setText("🛑 Stop Recording");
                btnRecord.setStyle("-fx-background-color: #ef5350;");
                status.setText("Recording in progress...");
                startRecording();
            } else {
                recording = false;
                btnRecord.setText("Start Recording");
                btnRecord.setStyle("");
                status.setText("Memo saved locally.");
                stopRecording();
            }
        });

        recorderCard.getChildren().addAll(micIcon, btnRecord, status);

        getChildren().addAll(title, recorderCard);
    }

    private void startRecording() {
        new Thread(() -> {
            try {
                AudioFormat format = new AudioFormat(44100, 16, 2, true, true);
                DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
                line = (TargetDataLine) AudioSystem.getLine(info);
                line.open(format);
                line.start();

                AudioInputStream ais = new AudioInputStream(line);
                File dir = new File("assets/audio");
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                File out = new File(dir, "memo_" + System.currentTimeMillis() + ".wav");
                AudioSystem.write(ais, AudioFileFormat.Type.WAVE, out);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();
    }

    private void stopRecording() {
        if (line != null) {
            line.stop();
            line.close();
        }
    }
}
