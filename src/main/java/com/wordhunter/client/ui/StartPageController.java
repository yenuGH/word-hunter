package com.wordhunter.client.ui;

import com.wordhunter.client.logic.ClientMain;
import com.wordhunter.client.ui.SceneController;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.util.Optional;

public class StartPageController {
    @FXML
    public TextField ipAddressField;

    @FXML
    public TextField usernameField;
    private String username;

    @FXML
    public Label label;

    @FXML
    public void createButtonClicked(ActionEvent e){
        if (getUsername() == false){
            return;
        }

        ClientMain clientMain = ClientMain.getInstance();
        clientMain.setAddress("localhost");
        clientMain.setUsername(this.username);

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
        if (getUsername() == false){
            return;
        }

        ClientMain clientMain = ClientMain.getInstance();
        clientMain.setUsername(this.username);

        String address = ipAddressField.getText();
        clientMain.setAddress(address);
        try {
            clientMain.connectServer(false);
            SceneController.getInstance().showWaitingPage();
        } catch (IOException ex) {
            this.label.setText("Invalid IP Address!");
            this.label.setStyle("-fx-text-fill: #ff0000; ");
        }
    }

    private boolean getUsername(){
        String username = usernameField.getText();
        if (username.equals("")){
            showUsernameAlert();
            return false;
        }

        this.username = username;
        return true;
    }

    private void showUsernameAlert(){
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Username not set!");
        alert.setHeaderText("Please enter in a username.");
        alert.setContentText("Your username cannot be left blank.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK){
            System.out.println("User has confirmed a username is needed.");
        }
    }
}
