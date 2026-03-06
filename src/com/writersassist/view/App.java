package com.writersassist.view;

import com.writersassist.lab5.NetworkingService;

import com.writersassist.lab7.ScriptService;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Map;

public class App extends Application {

    private BorderPane mainShell;
    private StackPane workspace;
    private Label connectionStatus;
    private Label wordCountLabel;

    private ScriptService rmiService;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("WritersAssist | Unified Creative Workspace");

        // Initialize Services
        initServices();

        // UI Components
        mainShell = new BorderPane();
        mainShell.getStyleClass().add("main-shell");

        VBox sidebar = createSidebar();
        mainShell.setLeft(sidebar);

        VBox centerArea = new VBox();
        HBox topBar = createTopBar();

        workspace = new StackPane();
        workspace.getStyleClass().add("workspace");
        VBox.setVgrow(workspace, Priority.ALWAYS);

        centerArea.getChildren().addAll(topBar, workspace);
        mainShell.setCenter(centerArea);

        // Footer / StatusBar
        mainShell.setBottom(createStatusBar());

        Scene scene = new Scene(mainShell, 1400, 900);
        scene.getStylesheets().add(getClass().getResource("/com/writersassist/ui/css/style.css").toExternalForm());

        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(e -> System.exit(0));

        showDashboard();
        primaryStage.show();
    }

    private void initServices() {
        // RMI Connection
        new Thread(() -> {
            try {
                // Wait for potential server startup
                Thread.sleep(1000);
                Registry registry = LocateRegistry.getRegistry("localhost", 1099);
                rmiService = (ScriptService) registry.lookup("ScriptService");

                Platform.runLater(() -> connectionStatus.setText(" Cloud Connected"));
            } catch (Exception e) {
                Platform.runLater(() -> connectionStatus.setText(" Offline Mode"));
            }
        }).start();
    }

    private HBox createTopBar() {
        HBox topBar = new HBox(20);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(15, 30, 15, 30));
        topBar.setStyle(
                "-fx-background-color: -bg-surface; -fx-border-color: -border-color; -fx-border-width: 0 0 1 0;");

        Text title = new Text("UNFOLDING THE UNKNOWN");
        title.getStyleClass().add("h2");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnSave = new Button("Sync to Cloud");
        btnSave.getStyleClass().add("btn-primary");
        btnSave.setOnAction(e -> {
            if (rmiService != null) {
                try {
                    rmiService.saveScript("project_1", "Draft 1 Content");
                } catch (RemoteException ex) {
                    ex.printStackTrace();
                }
            }
        });

        topBar.getChildren().addAll(title, spacer, btnSave);
        return topBar;
    }

    private VBox createSidebar() {
        VBox sidebar = new VBox(10);
        sidebar.getStyleClass().add("sidebar");

        Label logo = new Label("WritersAssist");
        logo.getStyleClass().add("sidebar-logo");

        VBox navItems = new VBox(5);
        Button btnDash = createNavBtn("Dashboard", true);
        Button btnEditor = createNavBtn("Script Editor", false);
        Button btnChars = createNavBtn("Characters", false);
        Button btnBoards = createNavBtn("Inspiration", false);
        Button btnSpotify = createNavBtn("Mood Playlists", false);
        Button btnVoice = createNavBtn("Voice Memos", false);

        btnDash.setOnAction(e -> {
            setActive(btnDash, navItems);
            showDashboard();
        });
        btnEditor.setOnAction(e -> {
            setActive(btnEditor, navItems);
            workspace.getChildren().setAll(new ScreenplayEditor());
        });
        btnChars.setOnAction(e -> {
            setActive(btnChars, navItems);
            workspace.getChildren().setAll(new CharacterManager());
        });
        btnBoards.setOnAction(e -> {
            setActive(btnBoards, navItems);
            workspace.getChildren().setAll(new InspirationBoard());
        });
        btnSpotify.setOnAction(e -> {
            setActive(btnSpotify, navItems);
            workspace.getChildren().setAll(new SpotifyDashboard());
        });
        btnVoice.setOnAction(e -> {
            setActive(btnVoice, navItems);
            workspace.getChildren().setAll(new VoiceMemoRecorder());
        });

        navItems.getChildren().addAll(btnDash, btnEditor, btnChars, btnBoards, btnSpotify, btnVoice);
        sidebar.getChildren().addAll(logo, navItems);
        return sidebar;
    }

    private Button createNavBtn(String text, boolean active) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.getStyleClass().add("nav-button");
        if (active)
            btn.getStyleClass().add("nav-button-active");
        return btn;
    }

    private void setActive(Button activeBtn, VBox container) {
        container.getChildren().forEach(n -> n.getStyleClass().remove("nav-button-active"));
        activeBtn.getStyleClass().add("nav-button-active");
    }

    private HBox createStatusBar() {
        HBox statusBar = new HBox(20);
        statusBar.setPadding(new Insets(8, 20, 8, 20));
        statusBar.setStyle(
                "-fx-background-color: -bg-surface; -fx-border-color: -border-color; -fx-border-width: 1 0 0 0;");
        statusBar.setAlignment(Pos.CENTER_LEFT);

        connectionStatus = new Label("Connecting...");
        connectionStatus.setStyle("-fx-text-fill: -accent; -fx-font-size: 11px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        wordCountLabel = new Label("Studio Mode Active");
        wordCountLabel.setStyle("-fx-text-fill: -text-secondary; -fx-font-size: 11px;");

        statusBar.getChildren().addAll(connectionStatus, spacer, wordCountLabel);
        return statusBar;
    }

    private void showDashboard() {
        VBox dash = new VBox(30);
        dash.setPadding(new Insets(40));

        Label welcome = new Label("Creative Command Center");
        welcome.getStyleClass().add("h1");

        HBox stats = new HBox(20);
        stats.getChildren().addAll(
                createStatCard("Total Scripts", "14"),
                createStatCard("Word Count", "122,405"),
                createStatCard("Collaborators", "4"));

        VBox netPanel = new VBox(15);
        netPanel.getStyleClass().add("modern-card");
        netPanel.setMaxWidth(400);

        Label netTitle = new Label("System Network");
        netTitle.getStyleClass().add("h2");
        netPanel.getChildren().add(netTitle);

        Map<String, String> info = NetworkingService.getHostInfo();
        info.forEach((k, v) -> {
            Label l = new Label(k + ": " + v);
            l.getStyleClass().add("p");
            netPanel.getChildren().add(l);
        });

        dash.getChildren().addAll(welcome, stats, netPanel);
        workspace.getChildren().setAll(dash);
    }

    private VBox createStatCard(String title, String val) {
        VBox card = new VBox(8);
        card.getStyleClass().add("modern-card");
        card.setPrefWidth(200);
        Label t = new Label(title);
        t.getStyleClass().add("p");
        Label v = new Label(val);
        v.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: -accent;");
        card.getChildren().addAll(t, v);
        return card;
    }

    private void showToast(String message) {
        System.out.println("USER NOTIFICATION: " + message);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
