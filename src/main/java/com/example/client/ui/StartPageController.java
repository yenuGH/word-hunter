package com.example.client.ui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;

public class StartPageController {
    @FXML
    public TextField textField;

    @FXML
    public void createButtonClicked(ActionEvent e){
        System.out.println("User pressed create");
    }

    @FXML
    public void joinButtonClicked(ActionEvent e){
        System.out.println(textField.getText());
    }
}
