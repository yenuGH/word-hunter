package com.wordhunter.server;

import com.wordhunter.conversion.WordConversion;
import com.wordhunter.models.Player;
import com.wordhunter.models.Word;
import com.wordhunter.models.WordGenerator;
import com.wordhunter.models.WordState;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * PlayerThread
 * listens from messages from client and forwards them to main server thread
 * handles client disconnect
 * can call removePlayer() from ServerAcceptClients
 */
class PlayerThread extends Thread {
    private final Player player;
    private final int index;
    private final Socket sock;

    private final ServerAcceptClients parent;

    private final Map<String, ServerMessageMethod> messageToCallback = new HashMap<>();

    /**
     * PlayerThread()
     * constructor
     *
     * @param aParent parent thread
     * @param aSock   client socket
     * @param anIndex index in clientSocks
     */
    public PlayerThread(ServerAcceptClients aParent, Socket aSock, int anIndex, Player aPlayer) {
        parent = aParent;
        sock = aSock;
        index = anIndex;
        player = aPlayer;

        // setup message callbacks
        messageToCallback.put("", PlayerThread::handleHeartBeat);
        messageToCallback.put("removeWord", PlayerThread::handleCompletedWord);
        messageToCallback.put("reserveWord", PlayerThread::handleReserveWord);
        messageToCallback.put("reopenWord", PlayerThread::handleReopenWord);
    }

    /**
     * run()
     * listens for messages from client. (identical to code in client, move later to new class?)
     */
    public void run() {
        // set up input stream
        InputStream is;
        try {
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
                        System.out.println("client" + index + " says: " + input);
                    handleClientMessage(input);
                }
            } catch (IOException e) // disconnect
            {
                try {
                    disconnect();
                    System.out.println("Player" + (index + 1) + " disconnected");
                } catch (IOException ex) {
                    System.out.println("failed to close socket");
                }
                break;
            }
        }
    }

    /**
     * disconnect()
     * disconnect with client (identical to code in client, move later to new class?)
     */
    public void disconnect() throws IOException {
        parent.removePlayer(index); // remove username + reset colorId + broadcast to remaining clients
        sock.close();
    }

    /**
     * handleClientMessage()
     * call function depending on first keyword
     *
     * @param msg message from client (identical to client code -> move to separate class?)
     */
    public void handleClientMessage(String msg) {
        // call function mapped to message
        String[] tokenList = msg.split(ServerMain.messageDelimiter);
        ServerMessageMethod a = messageToCallback.get(tokenList[0]);
        if (a != null) {
            a.method(this, msg);
        }
    }

    /**
     * handleHeartBeat()
     * send back heartbeat ack
     *
     * @param input message from client
     */
    public void handleHeartBeat(String input) {
        System.out.println("client " + index + " heartbeat");
        ServerMain.sendMessageToClient(sock, "");
    }

    public void handleCompletedWord(String input)
    {
        String[] tokenList = input.split(ServerMain.messageDelimiter);
        int position = Integer.parseInt(tokenList[1]);
        Word target = ServerMain.wordsList.get(position); // lock
        if (target == null) {
            return;
        }

        this.player.addScore(target.getWord().length());
        System.out.println("Player " + player.getName() + "'s score is " + this.player.getScore());

        ServerMain.wordsList.release(position);
        ServerMain.wordsList.set(target.getWordID(), null);
        System.out.println("Size of wordlist " + ServerMain.wordsList.getSize());

        // Remove word once it is done
        String removeWordMsg = "removeWord" + ServerMain.messageDelimiter
                                + target.getWordID() + ServerMain.messageDelimiter
                                + this.player.getScore();
        ServerMain.broadcast(removeWordMsg);

        // Add new word
        Word newWord = WordGenerator.generateNewWord();
        ServerMain.wordsList.set(newWord.getWordID(), newWord);
        ServerMain.broadcast("addNewWord" + ServerMain.messageDelimiter + WordConversion.fromWord(newWord));
    }

    public void handleReserveWord(String input) {
        String[] tokenList = input.split(ServerMain.messageDelimiter);
        int position = Integer.parseInt(tokenList[1]);
        String color = tokenList[2];

        Word target = ServerMain.wordsList.get(position); // lock
        if (target == null) {
            return;
        }
        if (target.getState() == WordState.OPEN) {
            target.setState(WordState.RESERVED);
            target.setColor(color);
            ServerMain.broadcast("reserveWord" + ServerMain.messageDelimiter
                    + target.getWordID() + ServerMain.messageDelimiter
                    + target.getColor());
        }
        ServerMain.wordsList.release(position);
    }

    public void handleReopenWord(String input) {
        String[] tokenList = input.split(ServerMain.messageDelimiter);
        int position = Integer.parseInt(tokenList[1]);

        Word target = ServerMain.wordsList.get(position); // lock
        if (target == null) {
            return;
        }

        if (target.getState() == WordState.RESERVED) {
            target.setState(WordState.OPEN);
            target.setColor(ServerMain.defaultColor);
            ServerMain.broadcast("reopenWord" + ServerMain.messageDelimiter + target.getWordID());
        }
        ServerMain.wordsList.release(position);
    }
}
