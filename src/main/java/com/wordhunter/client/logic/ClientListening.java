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
            } catch (IOException e) // disconnect
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
        Player winner = PlayerConversion.toPlayer((tokenList[1]));
        //TODO: have an end screen and put the winner here
        System.out.println("Winner is " + winner.getName() + " with score " + winner.getScore());

        Platform.runLater(() -> SceneController.getInstance().closeStage());
    }

    /**
     * Add or replace with a new Word object given the word index extracted from the broadcast message.
     * @param input message from server
     */
    public void processNewWord(String input) {
        // get lock
        if (!getWordsListLock())
        {
            return;
        }

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
        ClientMain.clientWordsListLock.release();
    }

    /**
     * getWordsListLock()
     * try to get clientWordsListLock
     * @return true if success, false if fail
     */
    public boolean getWordsListLock()
    {
        try
        {
            ClientMain.clientWordsListLock.acquire();
            return true;
        }
        catch (InterruptedException e)
        {
            System.out.println("unable to acquire the lock for client's wordsList");
            return false;
        }
    }

    public void handleCompletedWord(String input) {
        // get lock
        if (!getWordsListLock())
        {
            return;
        }

        String[] tokenList = input.split(ServerMain.messageDelimiter);
        int removedWordId = Integer.parseInt(tokenList[1]);
        //Word removedWord = WordConversion.toWord(tokenList[1]);

//        ClientMain.wordsList.remove(removedWord);
        ClientMain.wordsList.set(removedWordId, null);

        Platform.runLater(() -> {
            if (wordHunterController != null) {
                wordHunterController.clearWordPaneText(removedWordId);
                wordHunterController.stopAnimation(removedWordId);
            }
        });

        ClientMain.clientWordsListLock.release();
    }

    public void handleReserveWord(String input) {
        // get lock
        if (!getWordsListLock())
        {
            return;
        }

        // get reserved word
        String[] tokenList = input.split(ServerMain.messageDelimiter);
        int position = Integer.parseInt(tokenList[1]);
        String color = tokenList[2];

        //Word reservedWord = WordConversion.toWord(tokenList[1]);

        Word reserved = ClientMain.wordsList.get(position);
        if (reserved == null) {
            return;
        }
        reserved.setState(WordState.RESERVED);
        reserved.setColor(color);
        ClientMain.wordsList.set(position, reserved);

        Platform.runLater(() -> {
            if (wordHunterController != null) {
                wordHunterController.setWordPaneTextColor(reserved);
                wordHunterController.startAnimation(reserved);
            }
        });

        if (reserved.getColor().equals(ClientMain.colorId))
        {
            Platform.runLater(() -> {
                if (wordHunterController != null) {
                    wordHunterController.reservedWord = reserved;
                }
            });
        }

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
        ClientMain.clientWordsListLock.release();
    }

    public void handleReopenWord(String input) {
        // get lock
        if (!getWordsListLock())
        {
            return;
        }

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
        });

        //Word reopenWord = WordConversion.toWord(tokenList[1]);

//        int index = ClientMain.wordsList.indexOf(reopenWord);
//        if(index != -1 )
//        {
//            Word word = ClientMain.wordsList.get(index);
//            word.setState(WordState.OPEN);
//            word.setColor(reopenWord.getColor());
//
//            Platform.runLater(() -> {
//                if (wordHunterController != null) {
//                    wordHunterController.clearWordPaneColor(word);
//                    wordHunterController.startAnimation(word);
//                }
//            });
//        }
        ClientMain.clientWordsListLock.release();
    }

    public void updateStartTimer(String input) {
        int duration = Integer.parseInt(input.replace("startTimer!", ""));

        Platform.runLater(() -> {
            parent.getServerPageController().updateStartTimer(duration);
        });
        //ClientMain.getInstance().setStartTimer(duration);
        //System.out.println("Game starts in " + duration + " seconds.");
    }

}
