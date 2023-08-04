package com.wordhunter.client.ui;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class SceneController {
    // components
    private Stage stage;
    private Scene scene;
    private ReconnectionOverlay reconnectionOverlay;

    // device screen size
    private double maxHeight;
    private double maxWidth;

    // min size
    private double minHeight = 400;
    private double minWidth = 600;

    // controllers
    private static SceneController sceneController;

    public static SceneController getInstance(){
        if (sceneController == null){
            sceneController = new SceneController();
        }
        return sceneController;
    }

    /**
     * SceneController()
     * constructor. initializes reconnection overlay and max width/height
     */
    private SceneController()
    {
        reconnectionOverlay = new ReconnectionOverlay();

        maxWidth = Screen.getPrimary().getVisualBounds().getWidth();
        maxHeight = Screen.getPrimary().getVisualBounds().getHeight();
    }

    public void showStartPage() {
        Parent root = null;
        try {
            root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("StartPage.fxml")));
        } catch (IOException e) {
            e.printStackTrace();
        }
        Scene scene = new Scene(Objects.requireNonNull(root));
        this.scene = scene;

        stage.setTitle("WordHunter");
        stage.setScene(scene);
        stage.show();
    }

    public void showWaitingPage() {
        Parent root = null;
        try {
            root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("ServerPage.fxml")));
        } catch (IOException e) {
            e.printStackTrace();
        }
        Scene scene = new Scene(Objects.requireNonNull(root));
        this.scene = scene;

        stage.setScene(scene);
        stage.show();
    }

    public WordHunterController showGamePage() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("WordHunter.fxml"));

        Parent root = null;
        try {
            root = fxmlLoader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Scene scene = new Scene(Objects.requireNonNull(root));
        this.scene = scene;
        stage.close();

        stage.setScene(scene);
        stage.show();
        WordHunterController.getInstance();

        return fxmlLoader.getController();
    }

    public void showWinnerPage() {
        Parent root = null;
        try {
            root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("WinnerPage.fxml")));
        } catch (IOException e) {
            e.printStackTrace();
        }
        Scene scene = new Scene(Objects.requireNonNull(root));
        this.scene = scene;

        stage.setScene(scene);
        stage.show();
    }

    /**
     * setStage()
     * set stage, resize window to fit screen, bind close button to close entire app
     * @param stage stage
     */
    public void setStage(Stage stage)
    {
        this.stage = stage;

        // set min/max sizes
        this.stage.setMinWidth(minWidth);
        this.stage.setMinHeight(minHeight);

        // fit to screen
        this.stage.setWidth(maxWidth*.7);
        this.stage.setHeight((maxWidth*.7)/1.5);

        // make sure program closes on window close
        stage.setOnCloseRequest(t -> {
            Platform.exit();
            System.exit(0);
        });
    }

    public void closeStage() {
        stage.close();
    }

    /**
     * toggleReconnectionOverlay()
     * show/hide reconnection overlay
     * @param show boolean
     */
    public void toggleReconnectionOverlay(boolean show)
    {
        Platform.runLater(() -> {
            try
            {
                StackPane container = (StackPane) scene.getRoot();
                if (show) {
                    container.getChildren().add(reconnectionOverlay);
                    // move focus away from all components (mainly text boxes)
                    reconnectionOverlay.requestFocus();
                } else {
                    container.getChildren().remove(reconnectionOverlay);
                }
            }
            catch (ClassCastException ignored){}
        });
    }
}
