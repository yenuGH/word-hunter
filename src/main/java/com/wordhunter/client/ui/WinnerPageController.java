package com.wordhunter.client.ui;

import com.wordhunter.client.logic.ClientMain;
import com.wordhunter.models.Player;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;

/**
 * PlayerListCell
 * used to display usernames with their assigned colors on waiting page
 */
class ScoreListCell extends Label
{
    public ScoreListCell(Player player)
    {
        this.setText(player.getName() + ": " + player.getScore());
        this.setStyle("-fx-text-fill: " + player.getColor() + ";");
    }
}

public class WinnerPageController {
    @FXML
    public Label winnerText;

    @FXML
    public ListView<ScoreListCell> scoreView;
    private ObservableList<ScoreListCell> scoreViewList = FXCollections.observableList(new ArrayList<>());

    @FXML
    public void initialize() {
        scoreView.setItems(scoreViewList);
        ClientMain.getInstance().setWinnerPageController(this);
    }

    public void updateScoreList(Vector<Player> playersList) {

        playersList.sort(new Comparator<Player>() {
            @Override
            public int compare(Player o1, Player o2) {
                if (o1.getScore() > o2.getScore()) {
                    return -1;
                }
                if (o1.getScore() < o2.getScore()) {
                    return 1;
                }
                return 0;
            }
        });

        for (Player player : playersList)
        {
            Platform.runLater(() -> scoreViewList.add(new ScoreListCell(player)));
        }
    }

    public void updateWinner(Player winner) {
        winnerText.setText(winner.getName());
        winnerText.setStyle("-fx-text-fill: " + winner.getColor());
    }
}
