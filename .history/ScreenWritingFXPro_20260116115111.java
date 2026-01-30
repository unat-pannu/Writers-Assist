import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
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
    private TreeView<String> projectTree;
    private FlowPane canvas;
    private Label status;
    private TableView<JoinRow> joinTable;

    // ---------- State ----------
    private int currentProjectId = -1;

    // ---------- JOIN row model ----------
    private static class JoinRow {
        final int widgetId;
        final String projectTitle;
        final String widgetType;
        final String widgetContent;

        JoinRow(int widgetId, String projectTitle, String widgetType, String widgetContent) {
            this.widgetId = widgetId;
            this.projectTitle = projectTitle;
            this.widgetType = widgetType;
            this.widgetContent = widgetContent;
        }
    }

    // ---------- Resizable wrapper (drag handle bottom-right) ----------
    private static class ResizableCard extends StackPane {
        private final Region target;
        private final Region handle = new Region();

        private double pressX, pressY;
        private double startW, startH;

        private static final double MIN_W = 220;
        private static final double MIN_H = 140;
        private static final double MAX_W = 900;
        private static final double MAX_H = 700;

        ResizableCard(Region target) {
            this.target = target;

            getChildren().add(target);

            handle.setPrefSize(14, 14);
            handle.setMinSize(14, 14);
            handle.setMaxSize(14, 14);
            handle.setStyle("""
                -fx-background-color: rgba(255,255,255,0.25);
                -fx-background-radius: 3;
                -fx-border-color: rgba(255,255,255,0.35);
                -fx-border-radius: 3;
            """);
            StackPane.setAlignment(handle, Pos.BOTTOM_RIGHT);
            StackPane.setMargin(handle, new Insets(0, 6, 6, 0));
            getChildren().add(handle);

            handle.setOnMouseEntered(e -> handle.setCursor(javafx.scene.Cursor.SE_RESIZE));
            handle.setOnMouseExited(e -> handle.setCursor(javafx.scene.Cursor.DEFAULT));

            handle.setOnMousePressed(e -> {
                pressX = e.getSceneX();
                pressY = e.getSceneY();
                startW = clamp(getPrefWidth() > 0 ? getPrefWidth() : getWidth(), MIN_W, MAX_W);
                startH = clamp(getPrefHeight() > 0 ? getPrefHeight() : getHeight(), MIN_H, MAX_H);
                e.consume();
            });

            handle.setOnMouseDragged(e -> {
                double dx = e.getSceneX() - pressX;
                double dy = e.getSceneY() - pressY;

                double newW = clamp(startW + dx, MIN_W, MAX_W);
                double newH = clamp(startH + dy, MIN_H, MAX_H);

                setPrefSize(newW, newH);
                target.setPrefSize(newW, newH);
                e.consume();
            });
        }

        private static double clamp(double v, double min, double max) {
            return Math.max(min, Math.min(max, v));
        }
    }

    @Override
    public void start(Stage stage) {

        // Ensure Widgets table supports BLOB cover + sizes
        ensureWidgetsSchema();

        // ---------- LEFT : PROJECT TREE ----------
        projectTree = new TreeView<>();
        projectTree.setPrefWidth(280);
        loadProjects();

        projectTree.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> {
            if (b == null) return;
            Node g = b.getGraphic();
            if (g == null || g.getId() == null) return;
            currentProjectId = Integer.parseInt(g.getId());
            loadWidgets();
            loadJoinTable(); // JOIN refresh
        });

        // ---------- TOP BAR ----------
        Button addProject = new Button("+ Project");
        Button addWidget = new Button("+ Add Widget");

        addProject.setOnAction(e -> createProject());
        addWidget.setOnAction(e -> showAddWidgetDialog(stage));

        HBox topBar = new HBox(10, addProject, addWidget);
        topBar.setPadding(new Insets(10));

        // ---------- CANVAS ----------
        canvas = new FlowPane();
        canvas.setHgap(14);
        canvas.setVgap(14);
        canvas.setPadding(new Insets(14));

        ScrollPane scroll = new ScrollPane(canvas);
        scroll.setFitToWidth(true);

        // ---------- JOIN TABLE (Project JOIN Widgets) ----------
        joinTable = new TableView<>();
        joinTable.setPrefHeight(200);

        TableColumn<JoinRow, String> cProj = new TableColumn<>("Project");
        cProj.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().projectTitle));
        cProj.setPrefWidth(180);

        TableColumn<JoinRow, String> cType = new TableColumn<>("Widget Type");
        cType.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().widgetType));
        cType.setPrefWidth(120);

        TableColumn<JoinRow, String> cContent = new TableColumn<>("Content (or image name)");
        cContent.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().widgetContent));
        cContent.setPrefWidth(340);

        joinTable.getColumns().addAll(cProj, cType, cContent);

        TitledPane joinPane = new TitledPane("JOIN View: Project ⨝ Widgets", joinTable);
        joinPane.setExpanded(true);

        status = new Label("Ready");

        VBox right = new VBox(topBar, scroll, joinPane, status);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        SplitPane root = new SplitPane(projectTree, right);
        root.setDividerPositions(0.25);

        Scene scene = new Scene(root, 1200, 780);
        stage.setTitle("ScreenWritingFXPro — Project Boards (Resizable + BLOB Images + JOIN)");
        stage.setScene(scene);
        stage.show();
    }

    // =========================================================
    //  SCHEMA: Widgets supports image BLOB + sizes
    // =========================================================
    private void ensureWidgetsSchema() {
        try (Connection con = getConnection(); Statement st = con.createStatement()) {

            // Create Widgets if missing (safe)
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS Widgets (
                  Widget_id INT PRIMARY KEY AUTO_INCREMENT,
                  Project_id INT,
                  type VARCHAR(30),
                  content TEXT,
                  position INT,
                  w INT DEFAULT 280,
                  h INT DEFAULT 200,
                  image_blob LONGBLOB,
                  image_name VARCHAR(255),
                  FOREIGN KEY (Project_id) REFERENCES Project(Project_id)
                )
            """);

            // If table existed without columns, add them (ignore errors)
            try { st.executeUpdate("ALTER TABLE Widgets ADD COLUMN w INT DEFAULT 280"); } catch (SQLException ignored) {}
            try { st.executeUpdate("ALTER TABLE Widgets ADD COLUMN h INT DEFAULT 200"); } catch (SQLException ignored) {}
            try { st.executeUpdate("ALTER TABLE Widgets ADD COLUMN image_blob LONGBLOB"); } catch (SQLException ignored) {}
            try { st.executeUpdate("ALTER TABLE Widgets ADD COLUMN image_name VARCHAR(255)"); } catch (SQLException ignored) {}

        } catch (Exception e) {
            e.printStackTrace();
            status("Schema error: " + e.getMessage());
        }
    }

    // =========================================================
    //  PROJECTS
    // =========================================================
    private void loadProjects() {
        TreeItem<String> root = new TreeItem<>("Projects");
        root.setExpanded(true);

        try (Connection con = getConnection()) {
            ResultSet rs = con.createStatement()
                    .executeQuery("SELECT Project_id, title FROM Project ORDER BY title");

            while (rs.next()) {
                TreeItem<String> item = new TreeItem<>(rs.getString("title"));
                Label id = new Label();
                id.setId(String.valueOf(rs.getInt("Project_id"))); // storing id in graphic id
                item.setGraphic(id);
                root.getChildren().add(item);
            }
        } catch (Exception e) {
            e.printStackTrace();
            status("DB error loading projects: " + e.getMessage());
        }

        projectTree.setRoot(root);
    }

    private void createProject() {
        TextInputDialog d = new TextInputDialog();
        d.setHeaderText("New Project");
        d.setContentText("Project name:");
        d.showAndWait().ifPresent(name -> {
            String n = name == null ? "" : name.trim();
            if (n.isEmpty()) return;

            try (Connection con = getConnection()) {
                PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO Project(title) VALUES(?)"
                );
                ps.setString(1, n);
                ps.executeUpdate();
                loadProjects();
                status("Created project: " + n);
            } catch (Exception e) {
                e.printStackTrace();
                status("DB error creating project: " + e.getMessage());
            }
        });
    }

    // =========================================================
    //  WIDGETS
    // =========================================================
    private void showAddWidgetDialog(Stage stage) {
        if (currentProjectId == -1) {
            status("Select a project first.");
            return;
        }

        Dialog<ButtonType> d = new Dialog<>();
        d.setTitle("Add Widget");

        ComboBox<String> type = new ComboBox<>();
        type.getItems().addAll("TEXT", "IMAGE_BLOB", "SPOTIFY", "PINTEREST");
        type.getSelectionModel().selectFirst();

        TextField content = new TextField();
        content.setPromptText("Text or URL (ignored for IMAGE_BLOB)");

        Button pickImageBtn = new Button("Pick Image (for IMAGE_BLOB)");
        Label picked = new Label("No image selected");
        final File[] pickedFile = {null};

        pickImageBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Choose Image");
            fc.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif")
            );
            File f = fc.showOpenDialog(stage);
            if (f != null) {
                pickedFile[0] = f;
                picked.setText("Selected: " + f.getName());
            }
        });

        VBox box = new VBox(10,
                new Label("Type"), type,
                new Label("Content (Text/URL)"), content,
                pickImageBtn, picked
        );
        box.setPadding(new Insets(10));

        d.getDialogPane().setContent(box);
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        d.showAndWait().ifPresent(b -> {
            if (b != ButtonType.OK) return;
            if ("IMAGE_BLOB".equals(type.getValue()) && pickedFile[0] == null) {
                status("Pick an image for IMAGE_BLOB widget.");
                return;
            }
            saveWidget(type.getValue(), content.getText(), pickedFile[0]);
        });
    }

    private void saveWidget(String type, String content, File imageFileOrNull) {
        try (Connection con = getConnection()) {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO Widgets(Project_id,type,content,position,w,h,image_blob,image_name) VALUES(?,?,?,?,?,?,?,?)"
            );
            ps.setInt(1, currentProjectId);
            ps.setString(2, type);
            ps.setString(3, content);
            ps.setInt(4, canvas.getChildren().size());
            ps.setInt(5, 280);
            ps.setInt(6, 200);

            if ("IMAGE_BLOB".equals(type) && imageFileOrNull != null) {
                ps.setBinaryStream(7, new FileInputStream(imageFileOrNull), (int) imageFileOrNull.length());
                ps.setString(8, imageFileOrNull.getName());
            } else {
                ps.setNull(7, Types.BLOB);
                ps.setNull(8, Types.VARCHAR);
            }

            ps.executeUpdate();
            loadWidgets();
            loadJoinTable(); // JOIN refresh
            status("Widget added.");
        } catch (Exception e) {
            e.printStackTrace();
            status("DB error saving widget: " + e.getMessage());
        }
    }

    private void loadWidgets() {
        canvas.getChildren().clear();

        try (Connection con = getConnection()) {
            PreparedStatement ps = con.prepareStatement(
                    "SELECT Widget_id, type, content, w, h, image_blob, image_name FROM Widgets WHERE Project_id=? ORDER BY position"
            );
            ps.setInt(1, currentProjectId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                int id = rs.getInt("Widget_id");
                String type = rs.getString("type");
                String content = rs.getString("content");
                double w = rs.getInt("w");
                double h = rs.getInt("h");

                Blob blob = rs.getBlob("image_blob");
                String imageName = rs.getString("image_name");

                canvas.getChildren().add(buildWidget(id, type, content, w, h, blob, imageName));
            }
            status("Loaded widgets.");
        } catch (Exception e) {
            e.printStackTrace();
            status("DB error loading widgets: " + e.getMessage());
        }
    }

    private Pane buildWidget(int id, String type, String content, double w, double h, Blob blob, String imageName) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(10));
        card.setStyle("-fx-background-color:#1e1e2f;-fx-border-radius:12;-fx-background-radius:12;");
        card.setPrefSize(w, h);

        // Header (delete + optional replace image)
        Button del = new Button("✕");
        del.setOnAction(e -> deleteWidget(id));

        Button replaceImg = new Button("Replace Image");
        replaceImg.setVisible("IMAGE_BLOB".equals(type));
        replaceImg.setManaged("IMAGE_BLOB".equals(type));
        replaceImg.setOnAction(e -> replaceImageBlob(id));

        HBox head = new HBox(8, replaceImg, del);
        head.setAlignment(Pos.TOP_RIGHT);

        Region body = switch (type) {
            case "TEXT" -> {
                TextArea t = new TextArea(content == null ? "" : content);
                t.setWrapText(true);
                VBox.setVgrow(t, Priority.ALWAYS);
                t.textProperty().addListener((o, a, b) -> updateWidgetContent(id, b));
                yield t;
            }
            case "IMAGE_BLOB" -> {
                ImageView img = new ImageView();
                img.setPreserveRatio(true);
                img.setSmooth(true);

                // Load from BLOB
                if (blob != null) {
                    try (InputStream in = blob.getBinaryStream()) {
                        img.setImage(new Image(in));
                    } catch (Exception ignored) {}
                }

                // bind to card size
                img.fitWidthProperty().bind(card.widthProperty().subtract(20));
                img.fitHeightProperty().bind(card.heightProperty().subtract(60));

                Label name = new Label(imageName == null ? "Image" : imageName);
                name.setStyle("-fx-text-fill: #cfcfff;");

                VBox wrap = new VBox(6, name, img);
                VBox.setVgrow(wrap, Priority.ALWAYS);
                yield wrap;
            }
            default -> {
                WebView web = new WebView();
                web.setContextMenuEnabled(false);
                web.getEngine().load(content == null ? "" : content);

                web.prefWidthProperty().bind(card.widthProperty().subtract(20));
                web.prefHeightProperty().bind(card.heightProperty().subtract(60));

                VBox.setVgrow(web, Priority.ALWAYS);
                yield web;
            }
        };

        card.getChildren().addAll(head, body);

        // Resizable wrapper with handle
        ResizableCard wrapper = new ResizableCard(card);
        wrapper.setPrefSize(w, h);

        // Save size after resizing
        wrapper.setOnMouseReleased(e -> saveWidgetSize(id, wrapper.getPrefWidth(), wrapper.getPrefHeight()));

        return wrapper;
    }

    private void updateWidgetContent(int id, String content) {
        try (Connection con = getConnection()) {
            PreparedStatement ps = con.prepareStatement(
                    "UPDATE Widgets SET content=? WHERE Widget_id=?"
            );
            ps.setString(1, content);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (Exception ignored) {}
    }

    private void saveWidgetSize(int widgetId, double w, double h) {
        try (Connection con = getConnection()) {
            PreparedStatement ps = con.prepareStatement(
                    "UPDATE Widgets SET w=?, h=? WHERE Widget_id=?"
            );
            ps.setInt(1, (int) Math.round(w));
            ps.setInt(2, (int) Math.round(h));
            ps.setInt(3, widgetId);
            ps.executeUpdate();
        } catch (Exception ignored) {}
    }

    private void replaceImageBlob(int widgetId) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choose New Image");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );
        File f = fc.showOpenDialog(null);
        if (f == null) return;

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE Widgets SET image_blob=?, image_name=? WHERE Widget_id=?"
             );
             FileInputStream fis = new FileInputStream(f)) {

            ps.setBinaryStream(1, fis, (int) f.length());
            ps.setString(2, f.getName());
            ps.setInt(3, widgetId);
            ps.executeUpdate();

            loadWidgets();
            loadJoinTable();
            status("Image replaced.");

        } catch (Exception e) {
            e.printStackTrace();
            status("Replace image error: " + e.getMessage());
        }
    }

    private void deleteWidget(int id) {
        try (Connection con = getConnection()) {
            PreparedStatement ps = con.prepareStatement(
                    "DELETE FROM Widgets WHERE Widget_id=?"
            );
            ps.setInt(1, id);
            ps.executeUpdate();
            loadWidgets();
            loadJoinTable();
            status("Widget deleted.");
        } catch (Exception e) {
            e.printStackTrace();
            status("DB error deleting widget: " + e.getMessage());
        }
    }

    // =========================================================
    //  JOIN OPERATION (Project JOIN Widgets) — Requirement #6
    // =========================================================
    private void loadJoinTable() {
        joinTable.getItems().clear();

        try (Connection con = getConnection()) {
            PreparedStatement ps;
            if (currentProjectId == -1) {
                ps = con.prepareStatement("""
                    SELECT w.Widget_id, p.title AS projectTitle, w.type, 
                           COALESCE(w.content, w.image_name, '[no content]') AS displayContent
                    FROM Widgets w
                    JOIN Project p ON w.Project_id = p.Project_id
                    ORDER BY p.title, w.position
                """);
            } else {
                ps = con.prepareStatement("""
                    SELECT w.Widget_id, p.title AS projectTitle, w.type, 
                           COALESCE(w.content, w.image_name, '[no content]') AS displayContent
                    FROM Widgets w
                    JOIN Project p ON w.Project_id = p.Project_id
                    WHERE p.Project_id = ?
                    ORDER BY w.position
                """);
                ps.setInt(1, currentProjectId);
            }

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                joinTable.getItems().add(new JoinRow(
                        rs.getInt("Widget_id"),
                        rs.getString("projectTitle"),
                        rs.getString("type"),
                        rs.getString("displayContent")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
            status("JOIN load error: " + e.getMessage());
        }
    }

    private void status(String msg) {
        if (status != null) status.setText(msg);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
