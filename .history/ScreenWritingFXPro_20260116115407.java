import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

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
    private TreeView<NodeData> treeView;
    private TextField titleField;
    private TextArea contentArea;
    private VBox boardPane;
    private Label statusLabel;

    // ---------- Current selection ----------
    private int currentProjectId = -1;
    private int currentNoteId = -1;

    // ---------- Tree node data ----------
    private static class NodeData {
        enum Type { ROOT, PROJECT, NOTE }
        final Type type;
        final int id;
        final String label;

        NodeData(Type type, int id, String label) {
            this.type = type;
            this.id = id;
            this.label = label;
        }
        @Override public String toString() { return label; }
    }

    @Override
    public void start(Stage stage) {
        // Left: Tree
        treeView = new TreeView<>();
        treeView.setPrefWidth(320);

        // Right: Editor
        Label editorTitle = new Label("Editor");
        editorTitle.getStyleClass().add("h2");

        titleField = new TextField();
        titleField.setPromptText("Note Title");

        contentArea = new TextArea();
        contentArea.setPromptText("Write your screenplay here...");
        contentArea.setWrapText(true);

        Button newProjectBtn = new Button("New Project");
        Button newNoteBtn = new Button("New Note");
        Button saveBtn = new Button("Save");
        Button deleteBtn = new Button("Delete Note");

        newProjectBtn.getStyleClass().add("btn-secondary");
        newNoteBtn.getStyleClass().add("btn-secondary");
        saveBtn.getStyleClass().add("btn-primary");
        deleteBtn.getStyleClass().add("btn-danger");

        newProjectBtn.setOnAction(e -> createProject());
        newNoteBtn.setOnAction(e -> createNote());
        saveBtn.setOnAction(e -> saveNote());
        deleteBtn.setOnAction(e -> deleteNote());

        HBox actionBar = new HBox(10, newProjectBtn, newNoteBtn, saveBtn, deleteBtn);
        actionBar.setAlignment(Pos.CENTER_LEFT);

        statusLabel = new Label("Ready");
        statusLabel.getStyleClass().add("muted");

        VBox editorPane = new VBox(10, editorTitle, titleField, contentArea, actionBar, statusLabel);
        editorPane.setPadding(new Insets(14));
        VBox.setVgrow(contentArea, Priority.ALWAYS);

        // Board
        Label boardTitle = new Label("Notes Board");
        boardTitle.getStyleClass().add("h2");

        boardPane = new VBox(10);
        boardPane.setPadding(new Insets(10));

        ScrollPane boardScroll = new ScrollPane(boardPane);
        boardScroll.setFitToWidth(true);
        boardScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        VBox rightPane = new VBox(10, editorPane, boardTitle, boardScroll);
        rightPane.setPadding(new Insets(10));
        VBox.setVgrow(boardScroll, Priority.SOMETIMES);

        SplitPane split = new SplitPane(treeView, rightPane);
        split.setDividerPositions(0.32);

        Scene scene = new Scene(split, 1100, 700);
        scene.getStylesheets().add("data:text/css," + css().replace("\n", "%0A"));

        stage.setTitle("ScreenWritingFXPro — JavaFX + MySQL");
        stage.setScene(scene);
        stage.show();

        setupTreeClick();
        reloadAll();
        clearEditor();
    }

    // ---------- Tree ----------
    private void setupTreeClick() {
        treeView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.getValue() == null) return;
            NodeData data = newVal.getValue();

            if (data.type == NodeData.Type.PROJECT) {
                currentProjectId = data.id;
                currentNoteId = -1;
                clearEditor();
                status("Selected project: " + data.label);
            } else if (data.type == NodeData.Type.NOTE) {
                loadNoteById(data.id);
            }
        });
    }

    private void reloadAll() {
        loadProjectsTree();
        updateBoard();
    }

    private void loadProjectsTree() {
        TreeItem<NodeData> root = new TreeItem<>(new NodeData(NodeData.Type.ROOT, -1, "Projects"));
        root.setExpanded(true);

        try (Connection con = getConnection()) {

            // Project table: Project_id, title
            Statement st = con.createStatement();
            ResultSet projRs = st.executeQuery("SELECT Project_id, title FROM Project ORDER BY title");

            while (projRs.next()) {
                int pid = projRs.getInt("Project_id");
                String ptitle = projRs.getString("title");

                TreeItem<NodeData> projNode =
                        new TreeItem<>(new NodeData(NodeData.Type.PROJECT, pid, ptitle));
                projNode.setExpanded(true);
                root.getChildren().add(projNode);

                // Notes table: Note_id, Project_id, Title, content
                PreparedStatement ps = con.prepareStatement(
                        "SELECT Note_id, Title FROM Notes WHERE Project_id=? ORDER BY Title"
                );
                ps.setInt(1, pid);

                ResultSet noteRs = ps.executeQuery();
                while (noteRs.next()) {
                    int nid = noteRs.getInt("Note_id");
                    String ntitle = noteRs.getString("Title");
                    if (ntitle == null || ntitle.trim().isEmpty()) ntitle = "Untitled";

                    projNode.getChildren().add(
                            new TreeItem<>(new NodeData(NodeData.Type.NOTE, nid, ntitle))
                    );
                }
            }

        } catch (Exception e) {
            status("Error loading tree: " + e.getMessage());
            e.printStackTrace();
        }

        treeView.setRoot(root);
    }

    // ---------- Notes ----------
    private void loadNoteById(int noteId) {
        try (Connection con = getConnection()) {
            PreparedStatement ps = con.prepareStatement(
                    "SELECT Note_id, Project_id, Title, content FROM Notes WHERE Note_id=?"
            );
            ps.setInt(1, noteId);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                currentNoteId = rs.getInt("Note_id");
                currentProjectId = rs.getInt("Project_id");

                String title = rs.getString("Title");
                String content = rs.getString("content");

                titleField.setText(title == null ? "" : title);
                contentArea.setText(content == null ? "" : content);

                status("Loaded note #" + currentNoteId);
            }
        } catch (Exception e) {
            status("Error loading note: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createProject() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("New Project");
        dialog.setHeaderText("Create a new project");
        dialog.setContentText("Project title:");

        dialog.showAndWait().ifPresent(name -> {
            String title = name.trim();
            if (title.isEmpty()) { status("Project title cannot be empty."); return; }

            try (Connection con = getConnection()) {
                PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO Project(title) VALUES(?)",
                        Statement.RETURN_GENERATED_KEYS
                );
                ps.setString(1, title);
                ps.executeUpdate();

                ResultSet keys = ps.getGeneratedKeys();
                if (keys.next()) currentProjectId = keys.getInt(1);

                status("Created project: " + title);
                reloadAll();

            } catch (Exception e) {
                status("Error creating project: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private void createNote() {
        if (currentProjectId == -1) {
            status("Select a project first (click a project in the tree).");
            return;
        }

        String title = titleField.getText() == null ? "" : titleField.getText().trim();
        if (title.isEmpty()) title = "Untitled";
        String content = contentArea.getText() == null ? "" : contentArea.getText();

        try (Connection con = getConnection()) {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO Notes(Project_id, Title, content) VALUES(?,?,?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setInt(1, currentProjectId);
            ps.setString(2, title);
            ps.setString(3, content);
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) currentNoteId = keys.getInt(1);

            status("Created note #" + currentNoteId);
            reloadAll();

        } catch (Exception e) {
            status("Error creating note: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void saveNote() {
        if (currentNoteId == -1) {
            status("No note selected. Click a note or use New Note.");
            return;
        }

        try (Connection con = getConnection()) {
            PreparedStatement ps = con.prepareStatement(
                    "UPDATE Notes SET Title=?, content=? WHERE Note_id=?"
            );
            ps.setString(1, titleField.getText());
            ps.setString(2, contentArea.getText());
            ps.setInt(3, currentNoteId);
            ps.executeUpdate();

            status("Saved note #" + currentNoteId);
            reloadAll();

        } catch (Exception e) {
            status("Error saving note: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void deleteNote() {
        if (currentNoteId == -1) {
            status("No note selected.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Note");
        confirm.setHeaderText("Delete this note?");
        confirm.setContentText("This cannot be undone.");

        confirm.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.OK) return;

            try (Connection con = getConnection()) {
                PreparedStatement ps = con.prepareStatement(
                        "DELETE FROM Notes WHERE Note_id=?"
                );
                ps.setInt(1, currentNoteId);
                ps.executeUpdate();

                currentNoteId = -1;
                clearEditor();

                status("Deleted note.");
                reloadAll();

            } catch (Exception e) {
                status("Error deleting note: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    // ---------- Board ----------
    private void updateBoard() {
        boardPane.getChildren().clear();

        try (Connection con = getConnection()) {
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(
                    "SELECT n.Note_id, n.Title, p.title AS projectTitle " +
                    "FROM Notes n JOIN Project p ON n.Project_id=p.Project_id " +
                    "ORDER BY p.title, n.Title"
            );

            while (rs.next()) {
                int noteId = rs.getInt("Note_id");
                String noteTitle = rs.getString("Title");
                String projectTitle = rs.getString("projectTitle");

                if (noteTitle == null || noteTitle.trim().isEmpty()) noteTitle = "Untitled";

                VBox card = new VBox(6);
                card.getStyleClass().add("card");

                Label t = new Label(noteTitle);
                t.getStyleClass().add("card-title");

                Label p = new Label("Project: " + projectTitle);
                p.getStyleClass().add("muted");

                card.getChildren().addAll(t, p);

                card.setOnMouseClicked(e -> loadNoteById(noteId));
                boardPane.getChildren().add(card);
            }

        } catch (Exception e) {
            status("Error updating board: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ---------- Helpers ----------
    private void clearEditor() {
        titleField.clear();
        contentArea.clear();
    }

    private void status(String msg) {
        statusLabel.setText(msg);
    }

    private String css() {
        return """
            .root {
                -fx-font-family: "Inter", "Segoe UI", "Arial";
                -fx-base: #15171c;
                -fx-background: #15171c;
            }
            .tree-view {
                -fx-background-color: #111318;
                -fx-control-inner-background: #111318;
                -fx-padding: 8;
                -fx-border-color: #232733;
                -fx-border-width: 0 1 0 0;
            }
            .text-field, .text-area {
                -fx-background-color: #0f1218;
                -fx-text-fill: #e8eaf0;
                -fx-prompt-text-fill: #7f8798;
                -fx-border-color: #2a2f3b;
                -fx-border-radius: 10;
                -fx-background-radius: 10;
                -fx-padding: 10;
            }
            .text-area .content { -fx-background-color: #0f1218; }
            .label { -fx-text-fill: #e8eaf0; }
            .h2 { -fx-font-size: 16px; -fx-font-weight: 700; }
            .muted { -fx-text-fill: #a6adbd; }

            .button {
                -fx-background-radius: 10;
                -fx-padding: 10 12;
                -fx-font-weight: 600;
                -fx-cursor: hand;
            }
            .btn-primary { -fx-background-color: #3b82f6; -fx-text-fill: white; }
            .btn-secondary {
                -fx-background-color: #222839;
                -fx-text-fill: #e8eaf0;
                -fx-border-color: #2f3750;
                -fx-border-radius: 10;
            }
            .btn-danger { -fx-background-color: #ef4444; -fx-text-fill: white; }

            .card {
                -fx-background-color: #0f1218;
                -fx-border-color: #2a2f3b;
                -fx-border-radius: 14;
                -fx-background-radius: 14;
                -fx-padding: 12;
            }
            .card:hover { -fx-border-color: #3b82f6; }
            .card-title { -fx-font-size: 14px; -fx-font-weight: 700; }

            .scroll-pane { -fx-background: #15171c; -fx-background-color: transparent; }
            .scroll-pane .viewport { -fx-background-color: transparent; }
        """;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
