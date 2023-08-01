package com.wordhunter.client.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.layout.VBox;

import java.io.IOException;

public class ReconnectionOverlay extends VBox
{

    public ReconnectionOverlay() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("ReconnectionOverlay.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(ReconnectionOverlay.this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }
}
