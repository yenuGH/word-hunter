package com.wordhunter.client.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class SceneController {

    private Stage stage;
    private Scene scene;

    private static SceneController sceneController;
    public static SceneController getInstance(){
        if (sceneController == null){
            sceneController = new SceneController();
        }
        return sceneController;
    }
    private SceneController(){
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

        // Make not resizable
        stage.setResizable(false);
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

        stage.setResizable(false);
    }

    public void showGamePage() {
        Parent root = null;
        try {
            root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("WordHunter.fxml")));
        } catch (IOException e) {
            e.printStackTrace();
        }
        Scene scene = new Scene(Objects.requireNonNull(root));
        this.scene = scene;
        stage.close();

        stage.setScene(scene);
        stage.show();
        WordHunterController.getInstance();

        stage.setResizable(false);
    }

    public void setStage(Stage stage){
        this.stage = stage;
    }

    public void closeStage() {
        stage.close();
    }

}
