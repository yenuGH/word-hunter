package com.wordhunter.client.ui;
import com.wordhunter.client.logic.ClientMain;
import com.wordhunter.conversion.WordConversion;
import com.wordhunter.models.Word;
import com.wordhunter.models.WordState;
import com.wordhunter.server.ServerMain;
import javafx.animation.Transition;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.Vector;

public class WordHunterController {
    public ProgressBar healthBarBar;
    @FXML
    private GridPane grids;

    @FXML
    private Label healthBar;

    @FXML
    private TextField userInputField;

    // client connection and game variables
    public ClientMain clientMain;
    private Vector<Word> wordsList = new Vector<>();
    private WordPane[][] wordPanes = new WordPane[ServerMain.dimension][ServerMain.dimension];
    public Word reservedWord;


    private static WordHunterController wordHunterController;
    public static WordHunterController getInstance() {
        if (wordHunterController == null) {
            wordHunterController = new WordHunterController();
        }
        return wordHunterController;
    }

    @FXML
    public void initialize() {
        clientMain = ClientMain.getInstance();
        wordsList = clientMain.wordsList;
        System.out.println("Size of wordlist " + clientMain.wordsList.size());
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
            if (word != null) {
                setWordPaneText(word);
                startAnimation(word);
            }
        }

        handleKeyTypedEvent();
    }

    public int[] positionToDimensions(int position) {
        int x = position/5;
        int y = position % 5;
        int[] dimensions = {x, y};
        return dimensions;
    }

    /**
     *
     * @param word
     */
    public void startAnimation(Word word) {
        int[] dimensions = positionToDimensions(word.getWordID());
        wordPanes[dimensions[0]][dimensions[1]].initAnimation(word.getTimeToLiveRemaining());
    }
    public void stopAnimation(int position) {
        int[] dimensions = positionToDimensions(position);
        wordPanes[dimensions[0]][dimensions[1]].closeAnimation();
    }

    public void setWordPaneText(Word word) {
        int[] dimensions = positionToDimensions(word.getWordID());
        wordPanes[dimensions[0]][dimensions[1]].setWord(word.getWord());
    }

    public void clearWordPaneText(int position) {
        int[] dimensions = positionToDimensions(position);
        wordPanes[dimensions[0]][dimensions[1]].setWord("");
        wordPanes[dimensions[0]][dimensions[1]].setColor(ServerMain.defaultColor);
    }

    private void clearUserInput(){
        Platform.runLater(() -> {
            userInputField.clear();
        });
    }

    public void setWordPaneTextColor(Word word) {
        int[] dimensions = positionToDimensions(word.getWordID());
        wordPanes[dimensions[0]][dimensions[1]].setColor(word.getColor());
    }

    public void clearWordPaneColor(Word word) {
        int[] dimensions = positionToDimensions(word.getWordID());
        wordPanes[dimensions[0]][dimensions[1]].setColor(ServerMain.defaultColor);
    }

    //TODO: change word serialization
    private void requestWordStateChanged(Word word, String token) {
        String message = "";
        switch (token) {
            case "reopenWord":
                message = token + ServerMain.messageDelimiter + word.getWordID();
                break;
            case "reserveWord":
                message = token + ServerMain.messageDelimiter + word.getWordID()
                        + ServerMain.messageDelimiter + word.getColor();
                break;
            case "removeWord":
                message = token + ServerMain.messageDelimiter + word.getWordID();
                break;
        }

        //String message = token + ServerMain.messageDelimiter + WordConversion.fromWord(word);
        try {
            if (!message.equals("")) {
                ClientMain.sendMsgToServer(message);
            }
        } catch (IOException e) {
            System.out.println("Unable to signal word state changed to server");
        }
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
                    if (targetWord == null) {
                        // Case 1: word is not found
                        // TODO: add a warning message on the screen
                        System.out.println("Warning: word is not found, or it it reserved");
                        clearUserInput();
                    } else {
                        // Case 2: word is found, reserve the word
                        String requestedWordStr = WordConversion.fromWord(targetWord);
                        Word requestedWord = WordConversion.toWord(requestedWordStr);
                        // if the word color is default (unreserved)
                        if (Objects.equals(requestedWord.getColor(), "#000000")) {
                            requestedWord.setColor(ClientMain.colorId);
                            requestWordStateChanged(requestedWord, "reserveWord");
                        } else {
                            // if the word is already colored and thus reserved
                            clearUserInput();
                        }

                    }

                } else {
                    if (targetWord == null) {
                        // Case 3: player mistypes the word, reopen it again
                        requestWordStateChanged(reservedWord, "reopenWord");
                        clearUserInput();
                    } else if (targetWord.getWord().equals(newInput)) {
                        // Case 4: word is completed
                        requestWordStateChanged(targetWord, "removeWord");
                        clearUserInput();
                    }
                }
            }

            private Word findTarget(String userInput) {
                for (Word word : wordsList){
                    if (word != null) {
                        if (word.getWord().startsWith(userInput)){
                            return word;
                        }
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
