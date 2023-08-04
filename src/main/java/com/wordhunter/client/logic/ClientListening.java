package com.wordhunter.client.logic;

import com.wordhunter.client.ui.SceneController;
import com.wordhunter.client.ui.WinnerPageController;
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
import java.net.SocketException;
import java.util.*;

/**
 * ClientListening
 * <p>
 * thread listening for server messages. handles server disconnect. handles server messages
 */
class ClientListening extends Thread {
    public Socket sock;
    public ClientMain parent;

    public Timer heartBeatTimer;    // send heartbeat every x seconds
    boolean gotColorId = false;     // used to get colorId from first connection message

    private final Map<String, MessageMethod> messageToCallback = new HashMap<>();

    private WordHunterController wordHunterController;

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
        messageToCallback.put("", ClientListening::heartBeatAck);

        messageToCallback.put("startTimer", ClientListening::updateStartTimer);
        messageToCallback.put("gameStart", ClientListening::displayGameScreen);
        messageToCallback.put("gameOver", ClientListening::endGameScreen);

        messageToCallback.put("addNewWord", ClientListening::processNewWord);
        messageToCallback.put("removeWord", ClientListening::handleCompletedWord);
        messageToCallback.put("reserveWord", ClientListening::handleReserveWord);
        messageToCallback.put("reopenWord", ClientListening::handleReopenWord);

        SceneController.getInstance().toggleReconnectionOverlay(false); // TODO: move to reconnect handle when implemented
    }

    /**
     * run()
     * main listening thread
     */
    public void run() {
        // set up input stream
        InputStream is;
        try {
            sock.setSoTimeout(2 * ServerMain.heartBeatInterval);
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
                if (input != null) {
                    if (!Objects.equals("", input))
                        System.out.println("server says: " + input);
                    handleServerMessage(input);
                }
            } catch (IOException e) // disconnect throws SocketException
            {
                System.out.println("failed to read from socket. disconnecting...");
                try {
                    e.printStackTrace();
                    disconnect();
                    System.out.println("disconnected");
                } catch (IOException ex) {
                    System.out.println("server down");
                    System.exit(0);
                }
                break;
            }
        }
    }

    /**
     * disconnect()
     * disconnect with server and attempt reconnection if max retries not reached
     */
    public void disconnect() throws IOException {
        sock.close();
        if (ClientMain.reconnectAttempts < ClientMain.reconnectMaxAttempt) {
            SceneController.getInstance().toggleReconnectionOverlay(true);
            System.out.println("reconnection attempt " + ClientMain.reconnectAttempts);
            parent.connectServer(true);
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
            // setup keep alive
            heartBeatTimer = new Timer();
            heartBeatTimer.schedule(new HeartBeat(), ServerMain.heartBeatInterval);
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

    public void heartBeatAck(String input) {
        heartBeatTimer.schedule(new HeartBeat(), ServerMain.heartBeatInterval);
    }

    public void displayGameScreen(String input) {
        Platform.runLater(() -> this.wordHunterController = SceneController.getInstance().showGamePage());
    }

    public void endGameScreen(String input) {
        String[] tokenList = input.split(ServerMain.messageDelimiter);
        Vector<Player> players = PlayerConversion.toPlayers(tokenList[1]);

        Platform.runLater(() -> {
            SceneController.getInstance().showWinnerPage();
        });

        //TODO: have an end screen and put the winner here
        Player winner = findWinner(players);
        System.out.println("Winner is " + winner.getName() + " with score " + winner.getScore());
        Platform.runLater(() -> {
            parent.getWinnerPageController().updateScoreList(players);
            parent.getWinnerPageController().updateWinner(winner);
        });

        try {
            Thread.sleep(15000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Platform.runLater(() -> SceneController.getInstance().closeStage());
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

//        ClientMain.wordsList.add(newWord);
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
        //Word removedWord = WordConversion.toWord(tokenList[1]);

        Word removed = ClientMain.wordsList.get(removedWordId); // locks
        if (removed == null) {
            return;
        }
        Platform.runLater(() -> {
            if (removed.getColor().equals(ClientMain.colorId)) {
                if (wordHunterController != null) {
                    wordHunterController.setPlayerScore(score);
                }
            }
        });

        ClientMain.wordsList.set(removedWordId, null);

        Platform.runLater(() -> {
            if (wordHunterController != null) {
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

        //Word reservedWord = WordConversion.toWord(tokenList[1]);

        Word reserved = ClientMain.wordsList.get(position); // locks
        if (reserved == null) {
            return;
        }
        reserved.setState(WordState.RESERVED);
        reserved.setColor(color);
//        ClientMain.wordsList.set(position, reserved);

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



//        // if word in list
//        int index = ClientMain.wordsList.indexOf(reservedWord);
//        if (index != -1 )
//        {
//            Word word = ClientMain.wordsList.get(index);
//            word.setState(WordState.RESERVED);
//            word.setColor(reservedWord.getColor());
//
//            // set to player color
//            Platform.runLater(() -> {
//                if (wordHunterController != null) {
//                    wordHunterController.setWordPaneTextColor(word);
//                    wordHunterController.startAnimation(word);
//                }
//            });
//
//            // is current player
//            if (reservedWord.getColor().equals(ClientMain.colorId))
//            {
//                Platform.runLater(() -> {
//                    if (wordHunterController != null) {
//                        wordHunterController.reservedWord = word;
//                    }
//                });
//            }
//        }
    }

    public void handleReopenWord(String input) {
        String[] tokenList = input.split(ServerMain.messageDelimiter);
        int position = Integer.parseInt(tokenList[1]);

        Word reopened = ClientMain.wordsList.get(position);
        if (reopened == null) {
            ClientMain.wordsList.release(position);
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
