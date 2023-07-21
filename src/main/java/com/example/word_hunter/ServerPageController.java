package com.example.word_hunter;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

import java.util.ArrayList;
import java.util.List;

public class ServerPageController {
    @FXML
    Label serverIPLabel;

    @FXML
    ListView<String> playerView;

    ObservableList<String> playerList = FXCollections.observableList(new ArrayList<>());

    @FXML
    public void initialize(){
        playerView.setItems(playerList);
        for (int i = 0; i < 10000; i++){
            playerList.add("Naoto");
            playerList.add("Kevin");
            playerList.add("Jonnacan");
            playerList.add("Lyyn");
            playerList.add("Joel");
        }
    }

    public void changeServerIPLabel(int serverIP){
        this.serverIPLabel.setText("" + serverIP);
    }
}
