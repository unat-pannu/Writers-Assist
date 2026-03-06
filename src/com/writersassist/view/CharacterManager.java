package com.writersassist.view;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;

/**
 * Modern Character Profile Management System.
 */
public class CharacterManager extends VBox {

    private java.util.List<com.writersassist.lab8.CharacterBean> characterBeans = new java.util.ArrayList<>();

    public CharacterManager() {
        setSpacing(30);
        setPadding(new Insets(40));

        HBox header = new HBox(20);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label title = new Label("Character Library");
        title.getStyleClass().add("h1");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnAdd = new Button("+ New Character");
        btnAdd.getStyleClass().add("btn-primary");

        FlowPane charGrid = new FlowPane(20, 20);

        btnAdd.setOnAction(e -> {
            TextInputDialog nameDialog = new TextInputDialog();
            nameDialog.setTitle("New Character");
            nameDialog.setHeaderText("Enter Character Name:");
            nameDialog.showAndWait().ifPresent(name -> {
                TextInputDialog roleDialog = new TextInputDialog();
                roleDialog.setTitle("New Character");
                roleDialog.setHeaderText("Enter Character Role (e.g., Protagonist):");
                roleDialog.showAndWait().ifPresent(role -> {
                    TextInputDialog descDialog = new TextInputDialog();
                    descDialog.setTitle("New Character");
                    descDialog.setHeaderText("Enter Character Description:");
                    descDialog.showAndWait().ifPresent(desc -> {
                        com.writersassist.lab8.CharacterBean bean = new com.writersassist.lab8.CharacterBean();
                        bean.setName(name);
                        bean.setArchetype(role);
                        bean.setDescription(desc);
                        bean.setMotivation("Unknown");
                        bean.setFlaw("Unknown");
                        bean.setGoal("Unknown");
                        characterBeans.add(bean);
                        charGrid.getChildren().add(createCharacterCard(bean));
                    });
                });
            });
        });

        header.getChildren().addAll(title, spacer, btnAdd);

        // Example Cards using CharacterBean
        com.writersassist.lab8.CharacterBean c1 = new com.writersassist.lab8.CharacterBean();
        c1.setName("Elias Thorne");
        c1.setArchetype("The Protagonist");
        c1.setDescription("Quiet, observant, haunted by his past.");

        com.writersassist.lab8.CharacterBean c2 = new com.writersassist.lab8.CharacterBean();
        c2.setName("Dr. Sarah Chen");
        c2.setArchetype("Mentor");
        c2.setDescription("Brilliant neuroscientist with a secret.");

        com.writersassist.lab8.CharacterBean c3 = new com.writersassist.lab8.CharacterBean();
        c3.setName("The Specter");
        c3.setArchetype("Antagonist");
        c3.setDescription("An enigma of code and shadow.");

        characterBeans.add(c1);
        characterBeans.add(c2);
        characterBeans.add(c3);

        for (com.writersassist.lab8.CharacterBean bean : characterBeans) {
            charGrid.getChildren().add(createCharacterCard(bean));
        }

        getChildren().addAll(header, charGrid);
    }

    private VBox createCharacterCard(com.writersassist.lab8.CharacterBean bean) {
        VBox card = new VBox(12);
        card.getStyleClass().add("modern-card");
        card.setPrefWidth(280);

        Label lblName = new Label(bean.getName());
        lblName.getStyleClass().add("h2");

        Label lblRole = new Label(bean.getArchetype());
        lblRole.setStyle("-fx-text-fill: -accent; -fx-font-weight: 600; -fx-font-size: 12px;");

        Text lblDesc = new Text(bean.getDescription());
        lblDesc.getStyleClass().add("p");
        lblDesc.setWrappingWidth(240);

        Button btnView = new Button("View Profile");
        btnView.getStyleClass().add("nav-button");
        btnView.setMaxWidth(Double.MAX_VALUE);
        btnView.setOnAction(e -> showCharacterProfile(bean));

        card.getChildren().addAll(lblName, lblRole, lblDesc, btnView);
        return card;
    }

    private void showCharacterProfile(com.writersassist.lab8.CharacterBean bean) {
        getChildren().clear(); // Clear the grid

        Button btnBack = new Button("<- Back to Library");
        btnBack.getStyleClass().add("btn-secondary");
        btnBack.setOnAction(e -> {
            getChildren().clear();
            // Re-initialize original view roughly
            CharacterManager fresh = new CharacterManager();
            getChildren().addAll(fresh.getChildren());
        });

        Label title = new Label(bean.getName() + " - " + bean.getArchetype());
        title.getStyleClass().add("h1");
        Text descText = new Text(bean.getDescription());
        descText.getStyleClass().add("p");

        VBox header = new VBox(10, btnBack, title, descText);
        header.setPadding(new Insets(0, 0, 20, 0));

        // Embed Sub-Views
        HBox splits = new HBox(30);
        splits.setAlignment(javafx.geometry.Pos.TOP_LEFT);

        VBox leftCol = new VBox(20);
        leftCol.getChildren().add(new Label("Character Aesthetic"));
        InspirationBoard board = new InspirationBoard();
        board.setPadding(new Insets(0)); // Remove default padding for embedding
        leftCol.getChildren().add(board);

        VBox rightCol = new VBox(20);
        rightCol.getChildren().add(new Label("Character Playlist"));
        SpotifyDashboard playlist = new SpotifyDashboard();
        playlist.setPadding(new Insets(0));
        rightCol.getChildren().add(playlist);

        splits.getChildren().addAll(leftCol, rightCol);

        getChildren().addAll(header, splits);
    }
}
