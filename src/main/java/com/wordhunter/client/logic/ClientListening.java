package com.wordhunter.client.logic;

import com.wordhunter.client.ui.SceneController;
import com.wordhunter.client.ui.ServerPageController;
import com.wordhunter.client.ui.WordHunterController;
import com.wordhunter.conversion.PlayerConversion;
import com.wordhunter.conversion.WordConversion;
import com.wordhunter.models.Player;
import com.wordhunter.models.Word;
import com.wordhunter.server.ServerMain;
import javafx.application.Platform;
import javafx.scene.Scene;

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
        messageToCallback.put("gameStart", ClientListening::displayGameScreen);
        messageToCallback.put("gameOver", ClientListening::endGameScreen);
        messageToCallback.put("addNewWord", ClientListening::processNewWord);
        messageToCallback.put("removeWord", ClientListening::handleCompletedWord);
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
     * TODO: test reconnection state saving
     * disconnect with server (if theres any cleanup or reconnect attempt, start from here)
     */
    public void disconnect() throws IOException {
        sock.close();
        if (ClientMain.reconnectAttempts < ClientMain.reconnectMaxAttempt) {
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

    // message event functions
    // TODO: add other keyword functions (see message_formats.txt)

    /**
     * newPlayerJoin()
     * TODO: add code to extract own colorId from here too
     *
     * @param input message from server
     */
    public void newPlayerJoin(String input) {
        String playerList = input.split("playerList" + ServerMain.messageDelimiter)[1];
        Vector<Player> players = PlayerConversion.toPlayers(playerList);

        // get own color id
        if (parent.colorId.isEmpty()) {
            parent.colorId = players.elementAt(players.size() - 1).getColor();
            System.out.println("got color id:" + parent.colorId);
        }

        ServerPageController.refreshPlayerList(players);
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
        Platform.runLater(() -> {
            this.wordHunterController = SceneController.getInstance().showGamePage();
        });
    }

    public void endGameScreen(String input) {
        Platform.runLater(() -> {
            SceneController.getInstance().closeStage();
        });
    }

    /**
     * Add or replace with a new Word object given the word index extracted from the broadcast message.
     * @param input message from server
     */
    public void processNewWord(String input) {
        String[] tokenList = input.split(ServerMain.messageDelimiter);
        ClientMain.wordsList.add(WordConversion.toWord(tokenList[1]));

        if (wordHunterController != null) {
            Platform.runLater(() -> {
                wordHunterController.setWordPaneText(WordConversion.toWord(tokenList[1]));
            });
        }
    }

    public void handleCompletedWord(String input) {
        String[] tokenList = input.split(ServerMain.messageDelimiter);
        Word removedWord = WordConversion.toWord(tokenList[1]);
        ClientMain.wordsList.remove(removedWord);
    }

}
