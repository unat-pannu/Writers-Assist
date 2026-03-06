package com.writersassist.view;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.web.WebView;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Spotify-inspired Mood Playlist Dashboard.
 */
public class SpotifyDashboard extends VBox {

    private FlowPane grid;

    public SpotifyDashboard() {
        setSpacing(30);
        setPadding(new Insets(40));

        Label title = new Label("Mood Playlists");
        title.getStyleClass().add("h1");

        HBox inputBar = new HBox(15);
        TextField playlistUrl = new TextField();
        playlistUrl.setPromptText("Enter Spotify URL (e.g. track, album, playlist)...");
        playlistUrl.setPrefWidth(400);

        Button btnAdd = new Button("Add Music");
        btnAdd.getStyleClass().add("btn-primary");

        inputBar.getChildren().addAll(playlistUrl, btnAdd);

        grid = new FlowPane(20, 20);
        grid.setPadding(new Insets(0, 0, 40, 0));

        btnAdd.setOnAction(e -> {
            if (!playlistUrl.getText().isEmpty()) {
                addSpotifyWidget(playlistUrl.getText());
                playlistUrl.clear();
            }
        });

        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        getChildren().addAll(title, inputBar, scroll);

        // Initial Demo
        addSpotifyWidget("https://open.spotify.com/playlist/37i9dQZF1DWWQRwui0ExPn");
    }

    private void addSpotifyWidget(String url) {
        String embedUrl = getEmbedUrl(url);
        if (embedUrl == null) {
            System.err.println("Invalid Spotify URL: " + url);
            return;
        }

        VBox card = new VBox(10);
        card.getStyleClass().add("modern-card");
        card.setPrefWidth(320);
        card.setPrefHeight(380);
        card.setAlignment(javafx.geometry.Pos.CENTER);

        WebView webView = new WebView();
        webView.setPrefSize(300, 380);

        // Use an iframe to embed Spotify
        String html = "<html><body style='margin:0;padding:0;background-color:#121212;'>" +
                "<iframe style='border-radius:12px' src='" + embedUrl + "' width='100%' height='352' frameBorder='0' " +
                "allowfullscreen='' allow='autoplay; clipboard-write; encrypted-media; fullscreen; picture-in-picture' loading='lazy'>"
                +
                "</iframe></body></html>";

        webView.getEngine().loadContent(html);

        card.getChildren().add(webView);
        grid.getChildren().add(card);
    }

    private String getEmbedUrl(String url) {
        // e.g. https://open.spotify.com/playlist/37i9dQZF1DWWQRwui0ExPn
        // turns to https://open.spotify.com/embed/playlist/37i9dQZF1DWWQRwui0ExPn
        try {
            if (url.contains("spotify.com/")) {
                String[] parts = url.split("spotify\\.com/");
                if (parts.length > 1) {
                    String[] typeAndId = parts[1].split("\\?"); // remove query params if any
                    return "https://open.spotify.com/embed/" + typeAndId[0] + "?utm_source=generator&theme=0";
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
