package com.writersassist.view;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Text;

/**
 * Pinterest-style Inspiration Board.
 */
public class InspirationBoard extends VBox {

    private TilePane grid;

    public InspirationBoard() {
        setSpacing(30);
        setPadding(new Insets(40));

        Label title = new Label("Inspiration Boards");
        title.getStyleClass().add("h1");

        HBox controls = new HBox(15);
        TextField urlInput = new TextField();
        urlInput.setPromptText("Paste Image or Pinterest URL...");
        urlInput.setPrefWidth(400);

        Button btnAdd = new Button("Add to Board");
        btnAdd.getStyleClass().add("btn-primary");

        controls.getChildren().addAll(urlInput, btnAdd);

        grid = new TilePane();
        grid.setHgap(20);
        grid.setVgap(20);
        grid.setPrefColumns(3);

        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        btnAdd.setOnAction(e -> {
            String url = urlInput.getText();
            if (!url.isEmpty())
                fetchAndAddImage(url);
            urlInput.clear();
        });

        getChildren().addAll(title, controls, scroll);

        // Initial Demo Items
        fetchAndAddImage("https://picsum.photos/400/300?1");
        fetchAndAddImage("https://picsum.photos/400/500?2");
    }

    private void fetchAndAddImage(String url) {
        new Thread(() -> {
            // Simulated network fetch
            String metadata = "Fetched metadata for " + url;

            Platform.runLater(() -> {
                VBox card = new VBox(10);
                card.getStyleClass().add("modern-card");
                card.setPrefWidth(280);

                ImageView iv = new ImageView(new Image(url, 280, 0, true, true));
                iv.setPreserveRatio(true);
                iv.getStyleClass().add("radius-md");

                Label caption = new Label("Source: " + url.substring(0, Math.min(url.length(), 30)) + "...");
                caption.getStyleClass().add("p");

                card.getChildren().addAll(iv, caption);
                grid.getChildren().add(card);
            });
        }).start();
    }
}
