import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileInputStream;
import java.sql.*;

public class ScreenWritingFXPro extends Application {

    // ---------- DB ----------
    private static final String DB_URL =
            "jdbc:mysql://localhost:3306/ScreenwritingDB?useSSL=false&serverTimezone=UTC";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "sqlmain2024";

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
    }

    // ---------- UI ----------
    private VBox widgetBoard;
    private int currentProjectId = -1;

    @Override
    public void start(Stage stage) {
        // --- Controls ---
        Button newText = new Button("Add Text");
        Button newImage = new Button("Add Image");

        newText.getStyleClass().add("btn-secondary");
        newImage.getStyleClass().add("btn-secondary");

        HBox controls = new HBox(10, newText, newImage);
        controls.setAlignment(Pos.CENTER_LEFT);

        widgetBoard = new VBox(12);
        widgetBoard.setPadding(new Insets(10));

        ScrollPane scroll = new ScrollPane(widgetBoard);
        scroll.setFitToWidth(true);

        VBox root = new VBox(12, controls, scroll);
        root.setPadding(new Insets(12));

        Scene scene = new Scene(root, 900, 650);
        scene.getStylesheets().add("data:text/css," + css().replace("\n", "%0A"));

        stage.setTitle("ScreenWritingFXPro — Widgets");
        stage.setScene(scene);
        stage.show();

        // TEMP project (replace with tree selection later)
        currentProjectId = 1;

        newText.setOnAction(e -> addTextWidget());
        newImage.setOnAction(e -> addImageWidget(stage));

        loadWidgets();
    }

    // ---------- LOAD ----------
    private void loadWidgets() {
        widgetBoard.getChildren().clear();

        String sql = """
            SELECT * FROM Widgets
            WHERE Project_id=?
            ORDER BY position
        """;

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, currentProjectId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String type = rs.getString("type");

                Node widget = switch (type) {
                    case "TEXT" -> loadTextWidget(rs);
                    case "IMAGE" -> loadImageWidget(rs);
                    default -> null;
                };

                if (widget != null)
                    widgetBoard.getChildren().add(widget);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ---------- TEXT ----------
    private Node loadTextWidget(ResultSet rs) throws SQLException {
        TextArea area = new TextArea(rs.getString("content"));
        area.setWrapText(true);

        area.textProperty().addListener((a, b, c) ->
                updateWidgetContent(rs.getInt("Widget_id"), c)
        );

        VBox card = new VBox(area);
        card.getStyleClass().add("card");
        return card;
    }

    private void addTextWidget() {
        String sql = """
            INSERT INTO Widgets (Project_id, type, content, position)
            VALUES (?, 'TEXT', '', ?)
        """;

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, currentProjectId);
            ps.setInt(2, widgetBoard.getChildren().size());
            ps.executeUpdate();

            loadWidgets();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ---------- IMAGE ----------
    private Node loadImageWidget(ResultSet rs) throws SQLException {
        ImageView iv = new ImageView();
        iv.setFitWidth(360);
        iv.setPreserveRatio(true);

        Blob blob = rs.getBlob("content_blob");
        if (blob != null)
            iv.setImage(new Image(blob.getBinaryStream()));

        VBox card = new VBox(iv);
        card.getStyleClass().add("card");
        return card;
    }

    private void addImageWidget(Stage stage) {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(
                        "Images", "*.png", "*.jpg", "*.jpeg"
                )
        );

        File file = fc.showOpenDialog(stage);
        if (file == null) return;

        String sql = """
            INSERT INTO Widgets (Project_id, type, content_blob, position)
            VALUES (?, 'IMAGE', ?, ?)
        """;

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             FileInputStream fis = new FileInputStream(file)) {

            ps.setInt(1, currentProjectId);
            ps.setBinaryStream(2, fis, file.length());
            ps.setInt(3, widgetBoard.getChildren().size());
            ps.executeUpdate();

            loadWidgets();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ---------- UPDATE ----------
    private void updateWidgetContent(int id, String text) {
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE Widgets SET content=? WHERE Widget_id=?"
             )) {
            ps.setString(1, text);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ---------- CSS ----------
    private String css() {
        return """
            .root {
                -fx-font-family: "Inter", "Segoe UI", "Arial";
                -fx-background-color: #15171c;
            }
            .text-area {
                -fx-background-color: #0f1218;
                -fx-text-fill: #e8eaf0;
                -fx-border-color: #2a2f3b;
                -fx-border-radius: 10;
                -fx-background-radius: 10;
            }
            .button {
                -fx-background-radius: 10;
                -fx-padding: 10 12;
                -fx-font-weight: 600;
            }
            .btn-secondary {
                -fx-background-color: #222839;
                -fx-text-fill: #e8eaf0;
            }
            .card {
                -fx-background-color: #0f1218;
                -fx-border-color: #2a2f3b;
                -fx-border-radius: 14;
                -fx-background-radius: 14;
                -fx-padding: 12;
            }
        """;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
