package com.wordhunter.client.ui;

import com.wordhunter.client.logic.ClientMain;
import com.wordhunter.models.Player;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

public class ServerPageController{
    @FXML
    public Label serverIPLabel;

    @FXML
    public Label currentPlayerLabel;

    @FXML
    public Label startTimer;

    @FXML
    public ListView<String> playerView;
    private ObservableList<String> playerList = FXCollections.observableList(new ArrayList<>());

    @FXML
    public void initialize(){
        playerView.setItems(playerList);
        ClientMain.getInstance().setServerPageController(this);
    }

    public void changeServerIPLabel(int serverIP){
        this.serverIPLabel.setText("" + serverIP);
    }

    public void updateStartTimer(int duration){

        // variable used in lambda expression must be final
        // but i need to change this so i did a roundabout way c:
        final int[] durationArray = {duration};

        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                if (durationArray[0] > 0){
                    Platform.runLater(() -> {
                        startTimer.setText("Game will start in " + durationArray[0] + " seconds.");
                        durationArray[0]--;
                    });
                }
                else {
                    timer.cancel();
                }
            }
        }, 0, 1000);
    }

    public void updateUsername(String username){
        currentPlayerLabel.setText("Current Player: " + username);
        currentPlayerLabel.setAlignment(Pos.CENTER);
    }

    public void updateIPAddress(String address) {
        serverIPLabel.setText(address);
        currentPlayerLabel.setAlignment(Pos.CENTER);
    }

    public void updatePlayerList(Vector<Player> updatedPlayerList){
        for (Player player : updatedPlayerList){
            if (!playerList.contains(player.getName())){
                Platform.runLater(() -> {
                    playerList.add(player.getName());
                });
            }

        }
    }
}