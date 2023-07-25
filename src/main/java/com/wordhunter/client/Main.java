package com.wordhunter.client;

import com.wordhunter.client.ui.SceneController;
import javafx.application.Application;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {
    private final static String TITLE = "Word Hunter";
    @Override
    public void start(Stage stage) throws IOException, InterruptedException {
        SceneController sceneController = SceneController.getInstance();
        sceneController.setStage(stage);
        sceneController.showStartPage();
    }

    public static void main(String[] args) {
        launch();
    }
}


