package com.wordhunter.client.ui;
import com.wordhunter.client.logic.ClientMain;
import com.wordhunter.conversion.WordConversion;
import com.wordhunter.models.Word;
import com.wordhunter.models.WordState;
import com.wordhunter.server.ServerMain;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import java.io.IOException;
import java.util.Vector;

public class WordHunterController {
    @FXML
    private GridPane grids;

    @FXML
    private Label healthBar;

    @FXML
    private TextField userInputField;

    // client connection and game variables
    private ClientMain clientMain;
    private Vector<Word> wordsList = new Vector<>();
    private WordPane[][] wordPanes = new WordPane[ServerMain.dimension][ServerMain.dimension];

    @FXML
    public void initialize() {
        clientMain = ClientMain.getInstance();
        wordsList = clientMain.wordsList;
        // Make a border
        grids.setStyle(WordPane.BORDER);
        //System.out.println(grids);

        // TODO: start the timer of each word and animation of each word
        for (int row = 0; row < grids.getRowCount(); row++) {
            for (int column = 0; column < grids.getColumnCount(); column++) {
                WordPane pane = new WordPane("");
                grids.add(pane, row, column);
                wordPanes[row][column] = pane;
            }
        }

        for (Word word : wordsList) {
            setWordPaneText(word);
        }

        handleKeyTypedEvent();
    }

    /**
     *
     * @param word
     */
    private void setWordPaneText(Word word) {
        wordPanes[word.getPosX()][word.getPosY()].setWord(word.getWord());
    }

    private void clearUserInput(){
        Platform.runLater(() -> {
            userInputField.clear();
        });
    }

    private void handleKeyTypedEvent() {
        userInputField.setText("");
        userInputField.textProperty().addListener(new ChangeListener<>() {

            @Override
            public void changed(ObservableValue<? extends String> observableValue, String oldInput, String newInput) {
                // TODO: disable backspace and delete key
                if (oldInput.length() > newInput.length()) {
                    return;
                }

                Word targetWord = findTarget(newInput);
                if (newInput.length() == 1) {
                    if (targetWord == null || targetWord.getState() != WordState.OPEN) {
                        // Case 1: word is not found, or word has been reserved

                        clearUserInput();
                    } else {
                        // Case 2: word is found, reserve the word
                    }

                } else {
                    if (targetWord == null) {
                        // Case 3: player mistypes the word, reopwn it again
                        clearUserInput();
                    } else if (targetWord.getWord().equals(newInput)) {
                        // Case 4: word is completed
                        clearUserInput();
                    }
                }
            }

            private Word findTarget(String userInput) {
                for (Word word : wordsList){
                    if (word.getWord().startsWith(userInput)){
                        return word;
                    }
                }
                return null;
            }
        });
    }

    /**
     * Check if every character typed matches the current word that is reserved by the player.
     * @param ch character entered by the player
     * @return index of word
     */
    private int findMatchingWord(char ch, int currentWordIdx, int charIdx) {
        if (charIdx > 0) {
            // Check the reserved word only
            if (ch == wordsList.elementAt(currentWordIdx).getWord().charAt(charIdx))
                return currentWordIdx;
        } else {
            // No word is currently reserved by the player, then check if any word has character match.
            for (int i = 0; i < wordsList.size(); i++) {
                Word word = wordsList.elementAt(i);
                if (ch == word.getWord().charAt(0) && word.getState() == WordState.OPEN)
                    return i;
            }
        }
        return -1;
    }

    public void setHealthBar(int health){
        this.healthBar.setText("" + health);
    }
}
