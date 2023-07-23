package com.example.client.ui;

import com.example.client.logic.ClientMain;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {
    private final static String TITLE = "Word Hunter";
    @Override
    public void start(Stage stage) throws IOException, InterruptedException {
        Parent root = FXMLLoader.load(getClass().getResource("ServerPage.fxml"));
        Scene scene = new Scene(root);
        stage.setTitle(TITLE);
        stage.setScene(scene);
        stage.show();

        // Make not resizable
        stage.setResizable(false);

        ClientMain clientMain = new ClientMain();

    }

    public static void main(String[] args) {
        launch();
    }
}


