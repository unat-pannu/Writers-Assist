import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.sql.*;

public class ScreenWritingFXApp extends Application {

    private TreeView<String> treeView;
    private TextField titleField;
    private TextArea contentArea;

    private int currentProjectId = -1;
    private int currentNoteId = -1;

    @Override
    public void start(Stage stage) {

        // Left: Project Tree
        treeView = new TreeView<>();
        treeView.setPrefWidth(300);
        loadProjects();

        // Right: Editor
        titleField = new TextField();
        titleField.setPromptText("Note Title");

        contentArea = new TextArea();
        contentArea.setPromptText("Write your screenplay here...");

        Button saveBtn = new Button("Save");
        saveBtn.setOnAction(e -> saveNote());

        VBox editorPane = new VBox(10, titleField, contentArea, saveBtn);

        SplitPane splitPane = new SplitPane(treeView, editorPane);
        Scene scene = new Scene(splitPane, 1000, 650);

        stage.setTitle("ScreenWritingApp — JavaFX + JDBC");
        stage.setScene(scene);
        stage.show();

        setupTreeClick();
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

        try (Connection con = DBUtil.getConnection()) {
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery("SELECT * FROM Project");

            while (rs.next()) {
                TreeItem<String> projNode =
                    new TreeItem<>(rs.getString("title"));
                root.getChildren().add(projNode);

                PreparedStatement ps =
                    con.prepareStatement(
                        "SELECT * FROM Note WHERE project_id=?");
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
        try (Connection con = DBUtil.getConnection()) {
            PreparedStatement ps =
                con.prepareStatement(
                    "SELECT * FROM Note WHERE title=?");
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
            PreparedStatement ps =
                con.prepareStatement(
                    "UPDATE Note SET title=?, content=? WHERE note_id=?");
            ps.setString(1, titleField.getText());
            ps.setString(2, contentArea.getText());
            ps.setInt(3, currentNoteId);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
