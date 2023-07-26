package com.wordhunter.client.ui;

import com.wordhunter.client.logic.ClientMain;
import com.wordhunter.client.ui.SceneController;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.io.IOException;

public class StartPageController {
    @FXML
    public TextField textField;

    @FXML
    public Label label;

    @FXML
    public void createButtonClicked(ActionEvent e){
        ClientMain clientMain = ClientMain.getInstance();
        clientMain.setAddress("localhost");
        try {
            clientMain.createServer();
            clientMain.connectServer(false);

            SceneController.getInstance().showWaitingPage();
        } catch (InterruptedException | IOException ex) {
            ex.printStackTrace();
        }
    }

    public static boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch(NumberFormatException e){
            return false;
        }
    }

    @FXML
    public void joinButtonClicked(ActionEvent e){
        ClientMain clientMain = ClientMain.getInstance();
        String address = textField.getText();
        if ((address.length() <= 11 && isNumeric(address) || address.length() == 0)) {
            clientMain.setAddress(address);
        } else {
            this.label.setText("Address must be numeric!");
            this.label.setStyle("-fx-text-fill: #ff0000; ");
            return;
        }
        try {
            clientMain.connectServer(false);
            SceneController.getInstance().showWaitingPage();
        } catch (IOException ex) {
            this.label.setText("Invalid IP Address!");
            this.label.setStyle("-fx-text-fill: #ff0000; ");
        }
    }

    private void switchWaitingPage(){

    }
}
