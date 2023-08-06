package com.wordhunter.client.ui;
import com.wordhunter.client.logic.ClientMain;
import com.wordhunter.conversion.WordConversion;
import com.wordhunter.models.Word;
import com.wordhunter.models.WordGenerator;
import com.wordhunter.models.WordList;
import com.wordhunter.server.ServerMain;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import java.io.IOException;
import java.util.Objects;

public class WordHunterController {
    @FXML
    private GridPane grids;

    @FXML
    private Label playerScore;

    @FXML
    private TextField userInputField;

    // client connection and game variables
    public ClientMain clientMain;
    private WordList wordsList;
    private final WordPane[][] wordPanes = new WordPane[ServerMain.dimension][ServerMain.dimension];
    public Word reservedWord;


    private static WordHunterController wordHunterController;

    public WordHunterController()
    {
        wordsList = new WordList(WordGenerator.dimension);
    }

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
        System.out.println("Size of wordlist " + clientMain.wordsList.getSize());
        // Make a border
        grids.setStyle(WordPane.BORDER);

        for (int row = 0; row < grids.getRowCount(); row++) {
            for (int column = 0; column < grids.getColumnCount(); column++) {
                WordPane pane = new WordPane("");
                grids.add(pane, row, column);
                wordPanes[row][column] = pane;
            }
        }

        for (int i=0; i< wordsList.getSize(); i++) {
            Word word = wordsList.get(i); // lock
            if (word != null) {
                setWordPaneText(word);
                startAnimation(word);
            }
            wordsList.release(i);
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
     */
    public void setPlayerScore(int score) {
        playerScore.setText(Integer.toString(score));
    }

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
        wordPanes[dimensions[0]][dimensions[1]].setOpacity(1);
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
                if (oldInput.length() > newInput.length()) {
                    return;
                }

                Word targetWord = findTarget(newInput); // locks
                if (newInput.length() == 1) {
                    if (targetWord == null)
                    {
                        // Case 1: word is not found
                        clearUserInput();
                        return;
                    } else
                    {
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
                        return;
                    } else if (targetWord.getWord().equals(newInput)) {
                        // Case 4: word is completed
                        requestWordStateChanged(targetWord, "removeWord");
                        clearUserInput();
                    }
                }
                ClientMain.wordsList.release(targetWord.getWordID());
            }

            private Word findTarget(String userInput)
            {
                for (int i=0; i< wordsList.getSize(); i++)
                {
                    Word word = wordsList.get(i);
                    if (word != null && word.getWord().startsWith(userInput))
                    {
                        return word;
                        }
                        wordsList.release(i);
                    }
                return null;
            }
        });
    }
}
