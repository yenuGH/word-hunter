package com.wordhunter.client.ui;

import com.wordhunter.client.logic.ClientMain;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;

import java.io.IOException;

public class StartPageController {
    @FXML
    public TextField textField;

    @FXML
    public void createButtonClicked(ActionEvent e){
        ClientMain clientMain = ClientMain.getInstance("localhost");
        try {
            clientMain.createServer();
            clientMain.connectServer(false);

            SceneController.getInstance().showWaitingPage();
        } catch (InterruptedException | IOException ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    public void joinButtonClicked(ActionEvent e){
        ClientMain clientMain = ClientMain.getInstance(textField.getText());
        try {
            clientMain.connectServer(false);
            SceneController.getInstance().showWaitingPage();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void switchWaitingPage(){

    }
}
