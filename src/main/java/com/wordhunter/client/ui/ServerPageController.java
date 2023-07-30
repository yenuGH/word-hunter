package com.wordhunter.client.ui;

import com.wordhunter.models.Player;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

import java.util.ArrayList;
import java.util.Vector;

public class ServerPageController{
    @FXML
    public Label serverIPLabel;

    @FXML
    public ListView<String> playerView;

    private static ObservableList<String> playerList = FXCollections.observableList(new ArrayList<>());

    @FXML
    public void initialize(){
        playerView.setItems(playerList);
    }

    public void changeServerIPLabel(int serverIP){
        this.serverIPLabel.setText("" + serverIP);
    }

    public static void refreshPlayerList(Vector<Player> updatedPlayerList){
        for (Player player : updatedPlayerList){
            if (!playerList.contains(player.getName())){
                Platform.runLater(() -> {
                    playerList.add(player.getName());
                });
            }

        }
    }
}