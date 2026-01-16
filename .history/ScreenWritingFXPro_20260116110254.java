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

    // ---------- UI ----------
    private TreeView<NodeData> treeView;
    private TextField titleField;
    private TextArea contentArea;
    private VBox boardPane;

    private Label statusLabel;

    // ---------- Selection state ----------
    private int currentProjectId = -1;
    private int currentNoteId = -1;

    // ---------- DB ----------
    private static final String DB_URL = "jdbc:mysql://localhost:3306/ScreenwritingDB";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "sqlmain2024";

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
    }

    // Store ids in the TreeView instead of just strings
    private static class NodeData {
        enum Type { ROOT, PROJECT, NOTE }
        final Type type;
        final int id;           // project_id or note_id, root uses -1
        final String title;

        NodeData(Type type, int id, String title) {
            this.type = type;
            this.id = id;
            this.title = title;
        }

        @Override
        public String toString() {
            return title;
        }
    }

    @Override
    public void start(Stage stage) {
        // Left: Tree
        treeView = new TreeView<>();
        treeView.setPrefWidth(320);
        treeView.setShowRoot(true);

        // Right: Editor
        Label editorTitle = new Label("Editor");
        editorTitle.getStyleClass().add("h2");

        titleField = new TextField();
        titleField.setPromptText("Note title");

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

        // Bottom board
        Label boardTitle = new Label("Notes Board");
        boardTitle.getStyleClass().add("h2");

        boardPane = new VBox(10);
        boardPane.setPadding(new Insets(10));

        ScrollPane boardScroll = new ScrollPane(boardPane);
        boardScroll.setFitToWidth(true);
        boardScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        boardScroll.getStyleClass().add("board-scroll");

        VBox rightPane = new VBox(10, editorPane, boardTitle, boardScroll);
        rightPane.setPadding(new Insets(10));
        VBox.setVgrow(boardScroll, Priority.SOMETIMES);

        SplitPane splitPane = new SplitPane(treeView, rightPane);
        splitPane.setDividerPositions(0.32);

        Scene scene = new Scene(splitPane, 1100, 700);
        scene.getStylesheets().add("data:text/css," + css().replace("\n", "%0A"));

        stage.setTitle("ScreenWritingFXPro — JavaFX + JDBC");
        stage.setScene(scene);
        stage.show();

        setupTreeClick();
        reloadAll();
        clearEditor();
    }

    private void setupTreeClick() {
        treeView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) return;
            NodeData data = newVal.getValue();
            if (data == null) return;

            if (data.type == NodeData.Type.PROJECT) {
                currentProjectId = data.id;
                currentNoteId = -1;
                status("Selected project: " + data.title);
                // Optional: clear editor when selecting a project
                clearEditor();
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
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery("SELECT project_id, title FROM Project ORDER BY title");

            while (rs.next()) {
                int pid = rs.getInt("project_id");
                String ptitle = rs.getString("title");

                TreeItem<NodeData> projNode =
                        new TreeItem<>(new NodeData(NodeData.Type.PROJECT, pid, ptitle));
                projNode.setExpanded(true);
                root.getChildren().add(projNode);

                PreparedStatement ps = con.prepareStatement(
                        "SELECT note_id, title FROM notes WHERE project_id=? ORDER BY title");
                ps.setInt(1, pid);

                ResultSet notes = ps.executeQuery();
                while (notes.next()) {
                    int nid = notes.getInt("note_id");
                    String ntitle = notes.getString("title");
                    projNode.getChildren().add(
                            new TreeItem<>(new NodeData(NodeData.Type.NOTE, nid, ntitle))
                    );
                }
            }
        } catch (Exception e) {
            status("DB error loading projects: " + e.getMessage());
            e.printStackTrace();
        }

        treeView.setRoot(root);
    }

    private void loadNoteById(int noteId) {
        try (Connection con = getConnection()) {
            PreparedStatement ps = con.prepareStatement(
                    "SELECT note_id, project_id, title, content FROM notes WHERE note_id=?"
            );
            ps.setInt(1, noteId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                currentNoteId = rs.getInt("note_id");
                currentProjectId = rs.getInt("project_id");

                titleField.setText(rs.getString("title"));
                contentArea.setText(rs.getString("content"));

                status("Loaded note #" + currentNoteId);
            }
        } catch (Exception e) {
            status("DB error loading note: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createProject() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("New Project");
        dialog.setHeaderText("Create a new project");
        dialog.setContentText("Project title:");

        dialog.showAndWait().ifPresent(title -> {
            String trimmed = title.trim();
            if (trimmed.isEmpty()) {
                status("Project title cannot be empty.");
                return;
            }

            try (Connection con = getConnection()) {
                PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO Project(title) VALUES(?)",
                        Statement.RETURN_GENERATED_KEYS
                );
                ps.setString(1, trimmed);
                ps.executeUpdate();

                ResultSet keys = ps.getGeneratedKeys();
                if (keys.next()) {
                    currentProjectId = keys.getInt(1);
                    status("Created project: " + trimmed);
                }

            } catch (Exception e) {
                status("DB error creating project: " + e.getMessage());
                e.printStackTrace();
            }

            reloadAll();
        });
    }

    private void createNote() {
        if (currentProjectId == -1) {
            status("Select a project first (click a project in the tree).");
            return;
        }

        String title = titleField.getText() == null ? "" : titleField.getText().trim();
        if (title.isEmpty()) title = "Untitled Note";

        String content = contentArea.getText() == null ? "" : contentArea.getText();

        try (Connection con = getConnection()) {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO notes(project_id, title, content) VALUES(?,?,?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setInt(1, currentProjectId);
            ps.setString(2, title);
            ps.setString(3, content);
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                currentNoteId = keys.getInt(1);
                status("Created note #" + currentNoteId);
            }
        } catch (Exception e) {
            status("DB error creating note: " + e.getMessage());
            e.printStackTrace();
        }

        reloadAll();
    }

    private void saveNote() {
        if (currentNoteId == -1) {
            status("No note selected. Use New Note or click a note first.");
            return;
        }

        try (Connection con = getConnection()) {
            PreparedStatement ps = con.prepareStatement(
                    "UPDATE notes SET title=?, content=? WHERE note_id=?"
            );
            ps.setString(1, titleField.getText());
            ps.setString(2, contentArea.getText());
            ps.setInt(3, currentNoteId);
            ps.executeUpdate();
            status("Saved note #" + currentNoteId);
        } catch (Exception e) {
            status("DB error saving note: " + e.getMessage());
            e.printStackTrace();
        }

        reloadAll();
    }

    private void deleteNote() {
        if (currentNoteId == -1) {
            status("No note selected to delete.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Note");
        confirm.setHeaderText("Delete this note?");
        confirm.setContentText("This cannot be undone.");

        confirm.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.OK) return;

            try (Connection con = getConnection()) {
                PreparedStatement ps = con.prepareStatement("DELETE FROM notes WHERE note_id=?");
                ps.setInt(1, currentNoteId);
                ps.executeUpdate();
                status("Deleted note.");
            } catch (Exception e) {
                status("DB error deleting note: " + e.getMessage());
                e.printStackTrace();
            }

            currentNoteId = -1;
            clearEditor();
            reloadAll();
        });
    }

    private void updateBoard() {
        boardPane.getChildren().clear();

        try (Connection con = getConnection()) {
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(
                    "SELECT n.note_id, n.title, p.title AS project " +
                            "FROM notes n JOIN Project p ON n.project_id=p.project_id " +
                            "ORDER BY p.title, n.title"
            );

            while (rs.next()) {
                int noteId = rs.getInt("note_id");
                String noteTitle = rs.getString("title");
                String projectTitle = rs.getString("project");

                VBox card = new VBox(6);
                card.getStyleClass().add("card");

                Label title = new Label(noteTitle);
                title.getStyleClass().add("card-title");

                Label project = new Label("Project: " + projectTitle);
                project.getStyleClass().add("muted");

                card.getChildren().addAll(title, project);

                card.setOnMouseClicked(e -> loadNoteById(noteId));

                boardPane.getChildren().add(card);
            }
        } catch (Exception e) {
            status("DB error updating board: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void clearEditor() {
        titleField.clear();
        contentArea.clear();
    }

    private void status(String msg) {
        statusLabel.setText(msg);
    }

    // Inline CSS so you don't need a separate file
    private String css() {
        return """
            .root {
                -fx-font-family: "Inter", "Segoe UI", "Arial";
                -fx-base: #15171c;
                -fx-background: #15171c;
            }

            .split-pane {
                -fx-background-color: #15171c;
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

            .text-area .content {
                -fx-background-color: #0f1218;
            }

            .label {
                -fx-text-fill: #e8eaf0;
            }

            .h2 {
                -fx-font-size: 16px;
                -fx-font-weight: 700;
            }

            .muted {
                -fx-text-fill: #a6adbd;
            }

            .button {
                -fx-background-radius: 10;
                -fx-padding: 10 12;
                -fx-font-weight: 600;
                -fx-cursor: hand;
            }

            .btn-primary {
                -fx-background-color: #3b82f6;
                -fx-text-fill: white;
            }

            .btn-secondary {
                -fx-background-color: #222839;
                -fx-text-fill: #e8eaf0;
                -fx-border-color: #2f3750;
                -fx-border-radius: 10;
            }

            .btn-danger {
                -fx-background-color: #ef4444;
                -fx-text-fill: white;
            }

            .card {
                -fx-background-color: #0f1218;
                -fx-border-color: #2a2f3b;
                -fx-border-radius: 14;
                -fx-background-radius: 14;
                -fx-padding: 12;
            }

            .card:hover {
                -fx-border-color: #3b82f6;
            }

            .card-title {
                -fx-font-size: 14px;
                -fx-font-weight: 700;
            }

            .scroll-pane, .board-scroll {
                -fx-background: #15171c;
                -fx-background-color: transparent;
            }

            .scroll-pane .viewport {
                -fx-background-color: transparent;
            }
        """;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
