import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.scene.Node;


import java.sql.*;

public class ScreenWritingFXPro extends Application {
    private static final String DB_URL =
            "jdbc:mysql://localhost:3306/ScreenwritingDB?useSSL=false&serverTimezone=UTC";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "sqlmain2024";

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
    }
    private TreeView<String> projectTree;
    private FlowPane canvas;
    private Label status;
    private int currentProjectId = -1;
    @Override
    public void start(Stage stage) {
        projectTree = new TreeView<>();
        projectTree.setPrefWidth(280);
        loadProjects();
        projectTree.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> {
            if (b == null) return;
            currentProjectId = Integer.parseInt(b.getGraphic().getId());
            loadWidgets();
        });
        Button addProject = new Button("+ Project");
        Button addWidget = new Button("+ Add Widget");
        addProject.setOnAction(e -> createProject());
        addWidget.setOnAction(e -> showAddWidgetDialog());
        HBox topBar = new HBox(10, addProject, addWidget);
        topBar.setPadding(new Insets(10));
        canvas = new FlowPane();
        canvas.setHgap(14);
        canvas.setVgap(14);
        canvas.setPadding(new Insets(14));
        ScrollPane scroll = new ScrollPane(canvas);
        scroll.setFitToWidth(true);
        status = new Label("Ready");
        VBox right = new VBox(topBar, scroll, status);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        SplitPane root = new SplitPane(projectTree, right);
        root.setDividerPositions(0.25);
        Scene scene = new Scene(root, 1200, 750);
        stage.setTitle("ScreenWritingFXPro — Project Boards");
        stage.setScene(scene);
        stage.show();
    }
    private void loadProjects() {
        TreeItem<String> root = new TreeItem<>("Projects");
        root.setExpanded(true);
        try (Connection con = getConnection()) {
            ResultSet rs = con.createStatement()
                    .executeQuery("SELECT Project_id, title FROM Project");
            while (rs.next()) {
                TreeItem<String> item = new TreeItem<>(rs.getString("title"));
                Label id = new Label();
                id.setId(String.valueOf(rs.getInt("Project_id")));
                item.setGraphic(id);
                root.getChildren().add(item);
            }
        } catch (Exception e) { e.printStackTrace(); }
        projectTree.setRoot(root);
    }
    private void createProject() {
        TextInputDialog d = new TextInputDialog();
        d.setHeaderText("New Project");
        d.showAndWait().ifPresent(name -> {
            try (Connection con = getConnection()) {
                PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO Project(title) VALUES(?)"
                );
                ps.setString(1, name);
                ps.executeUpdate();
                loadProjects();
            } catch (Exception e) { e.printStackTrace(); }
        });
    }
    private void showAddWidgetDialog() {
        if (currentProjectId == -1) return;
        Dialog<ButtonType> d = new Dialog<>();
        d.setTitle("Add Widget");
        ComboBox<String> type = new ComboBox<>();
        type.getItems().addAll("TEXT", "IMAGE", "SPOTIFY", "PINTEREST");
        type.getSelectionModel().selectFirst();
        TextField content = new TextField();
        content.setPromptText("Text / Image path / Embed URL");
        VBox box = new VBox(10, new Label("Type"), type, content);
        box.setPadding(new Insets(10));
        d.getDialogPane().setContent(box);
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        d.showAndWait().ifPresent(b -> {
            if (b == ButtonType.OK) saveWidget(type.getValue(), content.getText());
        });
    }

    private void saveWidget(String type, String content) {
        try (Connection con = getConnection()) {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO Widgets(Project_id,type,content,position) VALUES(?,?,?,?)"
            );
            ps.setInt(1, currentProjectId);
            ps.setString(2, type);
            ps.setString(3, content);
            ps.setInt(4, canvas.getChildren().size());
            ps.executeUpdate();
            loadWidgets();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void loadWidgets() {
        canvas.getChildren().clear();
        try (Connection con = getConnection()) {
            PreparedStatement ps = con.prepareStatement(
                    "SELECT * FROM Widgets WHERE Project_id=? ORDER BY position"
            );
            ps.setInt(1, currentProjectId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                int id = rs.getInt("Widget_id");
                String type = rs.getString("type");
                String content = rs.getString("content");

                canvas.getChildren().add(buildWidget(id, type, content));
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private Pane buildWidget(int id, String type, String content) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(16));
        card.setMinWidth(420);
        card.setPrefWidth(420);
        card.setMaxWidth(420);
        card.setStyle("-fx-background-color:#1e1e2f;-fx-border-radius:12;-fx-background-radius:12;");

        Node body = switch (type) {
            case "TEXT" -> {
                TextArea t = new TextArea(content);
                t.textProperty().addListener((o,a,b)->updateWidget(id,b));
                yield t;
            }
            case "IMAGE" -> {
                ImageView img = new ImageView(new Image("file:" + content));
                img.setFitWidth(260);
                img.setPreserveRatio(true);
                yield img;
            }
            default -> {
                WebView web = new WebView();
                web.setPrefSize(400,500);
                web.setZoom(0.6);
                web.getEngine().load(content);
                yield web;
            }
        };

        Button del = new Button("✕");
        del.setOnAction(e -> deleteWidget(id));

        HBox head = new HBox(del);
        head.setAlignment(Pos.TOP_RIGHT);

        card.getChildren().addAll(head, body);
        return card;
    }

    private void updateWidget(int id, String content) {
        try (Connection con = getConnection()) {
            PreparedStatement ps = con.prepareStatement(
                    "UPDATE Widgets SET content=? WHERE Widget_id=?"
            );
            ps.setString(1, content);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (Exception ignored) {}
    }

    private void deleteWidget(int id) {
        try (Connection con = getConnection()) {
            PreparedStatement ps = con.prepareStatement(
                    "DELETE FROM Widgets WHERE Widget_id=?"
            );
            ps.setInt(1, id);
            ps.executeUpdate();
            loadWidgets();
        } catch (Exception e) { e.printStackTrace(); }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
