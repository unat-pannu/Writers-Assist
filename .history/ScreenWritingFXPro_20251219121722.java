import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.sql.*;

import .history.DBUtil;

public class ScreenWritingFXPro extends Application {

    private TreeView<String> treeView;
    private TextField titleField;
    private TextArea contentArea;
    private FlowPane boardPane;

    private int currentProjectId = -1;
    private int currentNoteId = -1;

    @Override
    public void start(Stage stage) {

        // ===== Left: Project Tree =====
        treeView = new TreeView<>();
        treeView.setPrefWidth(300);
        loadProjects();
        setupTreeClick();
        setupTreeContextMenu();

        VBox treeContainer = new VBox(new Label("Projects & Notes"), treeView);
        treeContainer.setPadding(new Insets(10));
        treeContainer.setSpacing(5);
        treeContainer.setBackground(new Background(new BackgroundFill(Color.web("#2b2b2b"), CornerRadii.EMPTY, Insets.EMPTY)));

        // ===== Center: Editor =====
        titleField = new TextField();
        titleField.setPromptText("Note Title");
        titleField.setFont(Font.font("Arial", 16));

        contentArea = new TextArea();
        contentArea.setPromptText("Write your screenplay here...");
        contentArea.setFont(Font.font("Consolas", 14));
        contentArea.setWrapText(true);

        Button saveBtn = new Button("Save");
        saveBtn.setOnAction(e -> saveNote());

        VBox editorPane = new VBox(10, titleField, contentArea, saveBtn);
        editorPane.setPadding(new Insets(10));

        // ===== Right: Visual Board (Spotify/Pinterest style) =====
        boardPane = new FlowPane();
        boardPane.setHgap(10);
        boardPane.setVgap(10);
        boardPane.setPadding(new Insets(10));
        boardPane.setStyle("-fx-background-color: #1e1e1e;");

        ScrollPane boardScroll = new ScrollPane(boardPane);
        boardScroll.setFitToWidth(true);
        boardScroll.setPrefWidth(400);

        updateBoard();

        // ===== Toolbar =====
        ToolBar toolbar = new ToolBar();
        Button addProjectBtn = new Button("New Project");
        addProjectBtn.setOnAction(e -> createProject());
        Button addNoteBtn = new Button("New Note");
        addNoteBtn.setOnAction(e -> createNote());
        Button deleteBtn = new Button("Delete");
        deleteBtn.setOnAction(e -> deleteSelected());

        toolbar.getItems().addAll(addProjectBtn, addNoteBtn, deleteBtn);

        VBox editorWithToolbar = new VBox(toolbar, editorPane);

        // ===== SplitPane =====
        SplitPane splitPane = new SplitPane(treeContainer, editorWithToolbar, boardScroll);
        Scene scene = new Scene(splitPane, 1200, 700);
        scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());

        stage.setTitle("ScreenWritingFXPro — JavaFX + JDBC");
        stage.setScene(scene);
        stage.show();
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

    private void setupTreeContextMenu() {
        treeView.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.SECONDARY) {
                ContextMenu menu = new ContextMenu();
                MenuItem addNote = new MenuItem("Add Note");
                addNote.setOnAction(e -> createNote());

                MenuItem delete = new MenuItem("Delete");
                delete.setOnAction(e -> deleteSelected());

                menu.getItems().addAll(addNote, delete);
                menu.show(treeView, event.getScreenX(), event.getScreenY());
            }
        });
    }

    private void loadProjects() {
        TreeItem<String> root = new TreeItem<>("Projects");
        root.setExpanded(true);

        try (Connection con = DBUtil.getConnection()) {
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery("SELECT * FROM Project");

            while (rs.next()) {
                TreeItem<String> projNode = new TreeItem<>(rs.getString("title"));
                root.getChildren().add(projNode);

                PreparedStatement ps = con.prepareStatement("SELECT * FROM Note WHERE project_id=?");
                ps.setInt(1, rs.getInt("project_id"));
                ResultSet notes = ps.executeQuery();

                while (notes.next()) {
                    projNode.getChildren().add(new TreeItem<>(notes.getString("title")));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        treeView.setRoot(root);
    }

    private void loadNote(String noteTitle) {
        try (Connection con = DBUtil.getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT * FROM Note WHERE title=?");
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

        try (Connection con = DBUtil.getConnection()) {
            PreparedStatement ps = con.prepareStatement(
                "UPDATE Note SET title=?, content=? WHERE note_id=?");
            ps.setString(1, titleField.getText());
            ps.setString(2, contentArea.getText());
            ps.setInt(3, currentNoteId);
            ps.executeUpdate();
            updateBoard(); // refresh board after saving
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ===== Board for Pinterest/Spotify style =====
    private void updateBoard() {
        boardPane.getChildren().clear();
        try (Connection con = DBUtil.getConnection()) {
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery("SELECT n.note_id, n.title, p.title AS project FROM Note n JOIN Project p ON n.project_id=p.project_id");

        while (rs.next()) {
            String noteTitle = rs.getString("title"); // capture
            String projectTitle = rs.getString("project");

            VBox card = new VBox();
            card.setStyle("-fx-background-color:#333333; -fx-padding:10; -fx-border-radius:5; -fx-background-radius:5;");
            
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

    private void createProject() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setHeaderText("Enter new project name");
        dialog.showAndWait().ifPresent(name -> {
            try (Connection con = DBUtil.getConnection()) {
                PreparedStatement ps = con.prepareStatement("INSERT INTO Project(title) VALUES(?)");
                ps.setString(1, name);
                ps.executeUpdate();
                loadProjects();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void createNote() {
        if (treeView.getSelectionModel().getSelectedItem() == null) return;

        TreeItem<String> selected = treeView.getSelectionModel().getSelectedItem();
        int projectId = getProjectIdFromName(selected.getValue());
        if (projectId == -1) return;

        TextInputDialog dialog = new TextInputDialog();
        dialog.setHeaderText("Enter new note title");
        dialog.showAndWait().ifPresent(title -> {
            try (Connection con = DBUtil.getConnection()) {
                PreparedStatement ps = con.prepareStatement("INSERT INTO Note(title, content, project_id) VALUES(?,?,?)");
                ps.setString(1, title);
                ps.setString(2, "");
                ps.setInt(3, projectId);
                ps.executeUpdate();
                loadProjects();
                updateBoard();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void deleteSelected() {
        TreeItem<String> selected = treeView.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        try (Connection con = DBUtil.getConnection()) {
            if (selected.getParent() == treeView.getRoot()) {
                // Delete project
                int projId = getProjectIdFromName(selected.getValue());
                if (projId != -1) {
                    PreparedStatement ps = con.prepareStatement("DELETE FROM Project WHERE project_id=?");
                    ps.setInt(1, projId);
                    ps.executeUpdate();
                }
            } else {
                // Delete note
                PreparedStatement ps = con.prepareStatement("DELETE FROM Note WHERE title=?");
                ps.setString(1, selected.getValue());
                ps.executeUpdate();
            }
            loadProjects();
            updateBoard();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int getProjectIdFromName(String name) {
        try (Connection con = DBUtil.getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT project_id FROM Project WHERE title=?");
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("project_id");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
