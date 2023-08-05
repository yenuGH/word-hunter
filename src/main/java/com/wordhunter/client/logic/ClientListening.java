package com.wordhunter.client.logic;

import com.wordhunter.client.ui.SceneController;
import com.wordhunter.client.ui.WordHunterController;
import com.wordhunter.conversion.PlayerConversion;
import com.wordhunter.conversion.WordConversion;
import com.wordhunter.models.Player;
import com.wordhunter.models.Word;
import com.wordhunter.models.WordState;
import com.wordhunter.server.ServerMain;
import javafx.application.Platform;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.*;

/**
 * ClientListening
 * <p>
 * thread listening for server messages. handles server disconnect. handles server messages
 */
class ClientListening extends Thread {
    public Socket sock;
    public ClientMain parent;

    boolean gotColorId = false;     // used to get colorId from first connection message

    private final Map<String, MessageMethod> messageToCallback = new HashMap<>();

    private WordHunterController wordHunterController;
    private boolean heartBeatSent = false;
    private boolean gameOver = false;

    /**
     * ClientListening()
     * constructor
     *
     * @param aSock socket connected to server
     */
    public ClientListening(Socket aSock, ClientMain aParent) {
        sock = aSock;
        parent = aParent;

        // set up callback map
        messageToCallback.put("newPlayerJoin", ClientListening::newPlayerJoin);
        messageToCallback.put("playerDisconnect", ClientListening::playerDisconnect);
        messageToCallback.put("error", ClientListening::error);

        messageToCallback.put("startTimer", ClientListening::updateStartTimer);
        messageToCallback.put("gameStart", ClientListening::displayGameScreen);
        messageToCallback.put("gameOver", ClientListening::endGameScreen);

        messageToCallback.put("addNewWord", ClientListening::processNewWord);
        messageToCallback.put("removeWord", ClientListening::handleCompletedWord);
        messageToCallback.put("reserveWord", ClientListening::handleReserveWord);
        messageToCallback.put("reopenWord", ClientListening::handleReopenWord);
    }

    /**
     * run()
     * main listening thread
     */
    public void run() {
        // set up input stream
        InputStream is;
        try {
            sock.setSoTimeout(ServerMain.heartBeatInterval/2);
            is = sock.getInputStream();
        } catch (IOException e) {
            System.out.println("failed to get input stream");
            return;
        }
        BufferedReader in = new BufferedReader(new InputStreamReader((is)));

        // continue reading
        while (true) {
            try {
                String input = in.readLine();
                if (input != null)
                {
                    heartBeatSent = false;
                    if (!Objects.equals("", input)) {
                        System.out.println("server says: " + input);
                        handleServerMessage(input);
                    }
                }
            } catch (IOException e) // disconnect
            {
                try {
                    if(!heartBeatSent)
                    {
                        heartBeatSent = true;
                        ClientMain.sendMsgToServer("");
                    }
                    else
                    {
                        System.out.println("failed to read from socket. disconnecting...");
                        sock.close();
                        System.exit(0);
                    }
                } catch (IOException ex) {
                    if(!gameOver) {
                        System.out.println("server down");
                        System.exit(0);
                    }
                    return;
                }
            }
        }
    }

    /**
     * handleServerMessage()
     * call function depending on first keyword
     *
     * @param msg message from server
     */
    public void handleServerMessage(String msg) {
        if (!gotColorId) {
            gotColorId = true;
        }

        // call function mapped to message
        String[] tokenList = msg.split(ServerMain.messageDelimiter);
        MessageMethod a = messageToCallback.get(tokenList[0]);
        if (a != null) {
            a.method(this, msg);
        }
    }

    /**
     * newPlayerJoin()
     * extracts own colorId if needed, updates player list
     * @param input message from server
     */
    public void newPlayerJoin(String input) {
        String playerList = input.split("playerList" + ServerMain.messageDelimiter)[1];
        Vector<Player> players = PlayerConversion.toPlayers(playerList);

        // get own color id
        if (parent.colorId.isEmpty()) {
            parent.colorId = players.elementAt(players.size() - 1).getColor();
            Platform.runLater(() -> {
                SceneController.getInstance().setMainColor(parent.colorId);
            });
        }

        Platform.runLater(() -> {
            parent.serverPageController.updatePlayerList(players);
            parent.serverPageController.updateUsername(parent.getUsername());
            parent.serverPageController.updateIPAddress(parent.getServerIP());
        });
    }

    /**
     * playerDisconnect()
     * TODO:
     *
     * @param input message from server
     */
    public void playerDisconnect(String input) {
        System.out.println("playerDisconnect running:" + input);
    }

    /**
     * error()
     * TODO: if other error messages, add implementations
     *
     * @param input message from server
     */
    public void error(String input) {
        try {
            sock.close();
        } catch (IOException e) {
            System.out.println("failed to close socket");
        }
        System.exit(0);
    }

    public void displayGameScreen(String input) {
        Platform.runLater(() -> {
            this.wordHunterController = SceneController.getInstance().showGamePage();
            SceneController.getInstance().setMainColor(parent.colorId);
        });
    }

    /**
     * endGameScreen()
     * close socket, display winner and everyones scores, exit after x seconds
     * @param input playerlist
     */
    public void endGameScreen(String input)
    {
        // close socket and set flag to prevent immediate exit
        gameOver = true;
        try {
            sock.close();
        }
        catch (IOException ignored) { }

        // get players + scores
        String[] tokenList = input.split(ServerMain.messageDelimiter);
        Vector<Player> players = PlayerConversion.toPlayers(tokenList[1]);

        // show winner page
        Platform.runLater(() -> {
            SceneController.getInstance().showWinnerPage();
            SceneController.getInstance().setMainColor(parent.colorId);
        });

        // find and display the winner's username
        Player winner = findWinner(players);
        System.out.println("Winner is " + winner.getName() + " with score " + winner.getScore());
        Platform.runLater(() -> {
            parent.getWinnerPageController().updateScoreList(players);
            parent.getWinnerPageController().updateWinner(winner);
        });

        // close program after 30s
        try {
            Thread.sleep(30000);
        }
        catch (InterruptedException ignored) { }

        System.exit(0);
    }

    public static Player findWinner(Vector<Player> playerList) {
        if (playerList.isEmpty()) {
            return null; // Return null if the playerList is empty
        }

        Player winner = playerList.get(0);

        for (Player player : playerList) {
            if (player.getScore() > winner.getScore()) {
                winner = player;
            }
        }
        return winner;
    }

    /**
     * Add or replace with a new Word object given the word index extracted from the broadcast message.
     * @param input message from server
     */
    public void processNewWord(String input) {
        String[] tokenList = input.split(ServerMain.messageDelimiter);
        Word newWord = WordConversion.toWord(tokenList[1]);

        ClientMain.wordsList.set(newWord.getWordID(), newWord);
        if (wordHunterController != null) {
            Platform.runLater(() -> {
                wordHunterController.setWordPaneText(newWord);
                wordHunterController.startAnimation(newWord);
            });
        }
    }


    public void handleCompletedWord(String input) {
        String[] tokenList = input.split(ServerMain.messageDelimiter);
        int removedWordId = Integer.parseInt(tokenList[1]);
        int score = Integer.parseInt(tokenList[2]);

        Word removed = ClientMain.wordsList.get(removedWordId); // locks
        if (removed == null)
        {
            return;
        }
        Platform.runLater(() -> {
            if (removed.getColor().equals(ClientMain.colorId)) {
                if (wordHunterController != null) {
                    wordHunterController.setPlayerScore(score);
                }
            }
        });
        ClientMain.wordsList.release(removedWordId);
        ClientMain.wordsList.set(removedWordId, null);

        Platform.runLater(() -> {
            if (wordHunterController != null)
            {
                wordHunterController.clearWordPaneText(removedWordId);
                wordHunterController.stopAnimation(removedWordId);
            }
        });
    }

    public void handleReserveWord(String input) {
        // get reserved word
        String[] tokenList = input.split(ServerMain.messageDelimiter);
        int position = Integer.parseInt(tokenList[1]);
        String color = tokenList[2];

        Word reserved = ClientMain.wordsList.get(position); // locks
        if (reserved == null)
        {
            return;
        }
        reserved.setState(WordState.RESERVED);
        reserved.setColor(color);

        Platform.runLater(() -> {
            if (wordHunterController != null) {
                wordHunterController.setWordPaneTextColor(reserved);
                wordHunterController.startAnimation(reserved);
            }
            if (reserved.getColor().equals(ClientMain.colorId)) {
                if (wordHunterController != null) {
                    wordHunterController.reservedWord = reserved; // TODO: look at later
                }
            }
            ClientMain.wordsList.release(position);
        });
    }

    public void handleReopenWord(String input) {
        String[] tokenList = input.split(ServerMain.messageDelimiter);
        int position = Integer.parseInt(tokenList[1]);

        Word reopened = ClientMain.wordsList.get(position);
        if (reopened == null) {
            return;
        }
        reopened.setState(WordState.OPEN);
        reopened.setColor(ServerMain.defaultColor);

        Platform.runLater(() -> {
            if (wordHunterController != null) {
                wordHunterController.clearWordPaneColor(reopened);
                wordHunterController.startAnimation(reopened);
            }
            ClientMain.wordsList.release(position);
        });
    }

    public void updateStartTimer(String input) {
        int duration = Integer.parseInt(input.replace("startTimer!", ""));

        Platform.runLater(() -> {
            parent.getServerPageController().updateStartTimer(duration);
        });
    }
}
