package com.wordhunter.client.ui;

import com.wordhunter.client.logic.ClientMain;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class SceneController {
    // components
    private Stage stage;
    public Parent root;
    private final double maxWidth;

    // min size
    private final double minHeight = 400;
    private final double minWidth = 600;

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
     * constructor. initializes max width
     */
    private SceneController()
    {
        // device screen size
        maxWidth = Screen.getPrimary().getVisualBounds().getWidth();
    }

    public void showStartPage() {
        root = null;
        try {
            root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("StartPage.fxml")));
        } catch (IOException e) {
            e.printStackTrace();
        }
        Scene scene = new Scene(Objects.requireNonNull(root));

        stage.setTitle("WordHunter");
        stage.setScene(scene);
        stage.show();
    }

    public void setMainColor(String color)
    {
        // get lighter version
        String newHex = color;
        if(color.length() >= 7 && color.startsWith("#")) {
            // make lighter by increasing value by 55
            int r = Integer.parseInt(color.substring(1, 3), 16) + 55;
            int g = Integer.parseInt(color.substring(3, 5), 16) + 55;
            int b = Integer.parseInt(color.substring(5, 7), 16) + 55;
            newHex = "#"
                    + Integer.toHexString(r)
                    + Integer.toHexString(g)
                    + Integer.toHexString(b);
        }
        root.setStyle("-bg-main: " + newHex + ";-bg-main-dark: " + color + ";");
    }

    public void showWaitingPage() {
        root = null;
        try {
            root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("ServerPage.fxml")));
        } catch (IOException e) {
            e.printStackTrace();
        }
        Scene scene = new Scene(Objects.requireNonNull(root));

        stage.setScene(scene);
        stage.show();
    }

    public WordHunterController showGamePage() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("WordHunter.fxml"));

        root = null;
        try {
            root = fxmlLoader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Scene scene = new Scene(Objects.requireNonNull(root));
        stage.close();

        stage.setScene(scene);
        stage.show();
        WordHunterController.getInstance();

        return fxmlLoader.getController();
    }

    public void showWinnerPage() {
        root = null;
        try {
            root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("WinnerPage.fxml")));
        } catch (IOException e) {
            e.printStackTrace();
        }
        Scene scene = new Scene(Objects.requireNonNull(root));

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
}
