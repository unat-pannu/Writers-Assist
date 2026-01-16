import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.sql.*;

public class ScreenWritingFXPro extends Application {

    private TreeView<String> treeView;
    private TextField titleField;
    private TextArea contentArea;
    private VBox boardPane;

    private int currentProjectId = -1;
    private int currentNoteId = -1;

    // === Database credentials ===
    private static final String DB_URL = "jdbc:mysql://localhost:3306/Screenwritingdb";
    private static final String DB_USER = "root";         // your MySQL username
    private static final String DB_PASS = "sqlmain2024";     // your MySQL password

    // Get connection
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
    }

    @Override
    public void start(Stage stage) {

        // Left: Project Tree
        treeView = new TreeView<>();
        treeView.setPrefWidth(300);
        loadProjects();
        setupTreeClick();

        // Right: Editor
        titleField = new TextField();
        titleField.setPromptText("Note Title");

        contentArea = new TextArea();
        contentArea.setPromptText("Write your screenplay here...");

        Button saveBtn = new Button("Save");
        saveBtn.setOnAction(e -> saveNote());

        VBox editorPane = new VBox(10, titleField, contentArea, saveBtn);
        editorPane.setPadding(new Insets(10));

        // Bottom: Pinterest-style board
        boardPane = new VBox(10);
        boardPane.setPadding(new Insets(10));
        ScrollPane boardScroll = new ScrollPane(boardPane);
        boardScroll.setFitToWidth(true);
        boardScroll.setPrefHeight(200);

        VBox rightPane = new VBox(10, editorPane, new Label("Notes Board"), boardScroll);
        VBox.setVgrow(editorPane, Priority.ALWAYS);

        SplitPane splitPane = new SplitPane(treeView, rightPane);
        Scene scene = new Scene(splitPane, 1000, 650);

        stage.setTitle("ScreenWritingFXPro — JavaFX + JDBC");
        stage.setScene(scene);
        stage.show();

        updateBoard();
    }

    private void setupTreeClick() {
        treeView.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> {
                if (newVal != null && newVal.getParent() != null) {
                    loadNote(newVal.getValue());
                }
            }
        );
    }

    private void loadProjects() {
        TreeItem<String> root = new TreeItem<>("Projects");
        root.setExpanded(true);

        try (Connection con = getConnection()) {
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery("SELECT * FROM Project");

            while (rs.next()) {
                TreeItem<String> projNode = new TreeItem<>(rs.getString("title"));
                root.getChildren().add(projNode);

                PreparedStatement ps = con.prepareStatement(
                    "SELECT * FROM notes WHERE project_id=?");
                ps.setInt(1, rs.getInt("project_id"));
                ResultSet notes = ps.executeQuery();

                while (notes.next()) {
                    projNode.getChildren().add(
                        new TreeItem<>(notes.getString("title")));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        treeView.setRoot(root);
    }

    private void loadNote(String noteTitle) {
        try (Connection con = getConnection()) {
            PreparedStatement ps = con.prepareStatement(
                "SELECT * FROM notes WHERE title=?");
            ps.setString(1, noteTitle);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                currentNoteId = rs.getInt("note_id");
                titleField.setText(rs.getString("title"));
                contentArea.setText(rs.getString("content"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveNote() {
        if (currentNoteId == -1) return;

        try (Connection con = getConnection()) {
            PreparedStatement ps = con.prepareStatement(
                "UPDATE notes SET title=?, content=? WHERE note_id=?");
            ps.setString(1, titleField.getText());
            ps.setString(2, contentArea.getText());
            ps.setInt(3, currentNoteId);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Refresh board and TreeView
        loadProjects();
        updateBoard();
    }

    private void updateBoard() {
        boardPane.getChildren().clear();

        try (Connection con = getConnection()) {
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(
                "SELECT n.note_id, n.title, p.title AS project " +
                "FROM notes n JOIN Project p ON n.project_id=p.project_id");

            while (rs.next()) {
                String noteTitle = rs.getString("title");
                String projectTitle = rs.getString("project");

                VBox card = new VBox();
                card.setStyle("-fx-background-color:#333333; -fx-padding:10; -fx-border-radius:5; -fx-background-radius:5;");
                card.setSpacing(5);

                Label title = new Label(noteTitle);
                title.setTextFill(Color.WHITE);
                Label project = new Label("Project: " + projectTitle);
                project.setTextFill(Color.LIGHTGRAY);

                card.getChildren().addAll(title, project);

                card.setOnMouseClicked(e -> loadNote(noteTitle));

                boardPane.getChildren().add(card);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
