package com.wordhunter.client.ui;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

public class WordHunterController {
    @FXML
    public GridPane grids;

    @FXML
    public Label healthBar;

    @FXML
    public TextField userInput;

    @FXML
    public void initialize() throws InterruptedException {
        // Make a border
        grids.setStyle(WordPane.BORDER);
        System.out.println(grids);
        // Make WordPane to each cell
        for (int row = 0; row < grids.getRowCount(); row++) {
            for (int column = 0; column < grids.getColumnCount(); column++) {
                WordPane pane = new WordPane("A");
                grids.add(pane, column, row);
            }
        }
    }

    public void setHealthBar(int health){
        this.healthBar.setText("" + health);
    }
}
