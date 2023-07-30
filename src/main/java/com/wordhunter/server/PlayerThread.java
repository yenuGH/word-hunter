package com.wordhunter.server;

import com.wordhunter.client.logic.ClientMain;
import com.wordhunter.conversion.WordConversion;
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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * PlayerThread
 * listens from messages from client and forwards them to main server thread
 * handles client disconnect
 * can call removePlayer() from ServerAcceptClients
 */
class PlayerThread extends Thread {
    // change these to player class when implemented
    private final int index;
    private final Socket sock;

    private final ServerAcceptClients parent;

    private final Map<String, ServerMessageMethod> messageToCallback = new HashMap<>();

    // keep alive
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> heartBeatHandle;
    private final Runnable disconnectRunnable = () -> {
        try {
            disconnect();
        } catch (IOException ignored) {
        }
    };


    /**
     * PlayerThread()
     * constructor
     *
     * @param aParent parent thread
     * @param aSock   client socket
     * @param anIndex index in clientSocks
     */
    public PlayerThread(ServerAcceptClients aParent, Socket aSock, int anIndex) {
        parent = aParent;
        sock = aSock;
        index = anIndex;

        // setup message callbacks
        messageToCallback.put("", PlayerThread::handleHeartBeat);
        messageToCallback.put("removeWord", PlayerThread::handleCompletedWord);
        messageToCallback.put("reserveWord", PlayerThread::handleReserveWord);
        messageToCallback.put("reopenWord", PlayerThread::handleReopenWord);

        heartBeatHandle = scheduler.scheduleAtFixedRate(disconnectRunnable,
                2 * ServerMain.heartBeatInterval,
                2 * ServerMain.heartBeatInterval,
                TimeUnit.SECONDS);
    }

    /**
     * run()
     * listens for messages from client. (identical to code in client, move later to new class?)
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

                // reset disconnect timer
                heartBeatHandle.cancel(true);
                heartBeatHandle = scheduler.scheduleAtFixedRate(disconnectRunnable,
                        2 * ServerMain.heartBeatInterval,
                        2 * ServerMain.heartBeatInterval,
                        TimeUnit.SECONDS);

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
        ServerMain.sendMessageToClient(sock, "");
    }

    public void handleCompletedWord(String input) {
        String[] tokenList = input.split(ServerMain.messageDelimiter);
        Word target = WordConversion.toWord(tokenList[1]);

        ServerMain.wordsList.remove(target);
        System.out.println("Size of wordlist " + ServerMain.wordsList.size());

        String removeWordMsg = "removingCompletedWord" + ServerMain.messageDelimiter
                                + WordConversion.fromWord(target);
        ServerMain.broadcast(removeWordMsg);

        // Add new word
        Word newWord = WordGenerator.generateNewWord();
        ServerMain.wordsList.add(newWord);
        ServerMain.broadcast("addNewWord" + ServerMain.messageDelimiter + WordConversion.fromWord(newWord));
    }

    public void handleReserveWord(String input) {
        String[] tokenList = input.split(ServerMain.messageDelimiter);
        Word target = WordConversion.toWord(tokenList[1]);

        for (Word word : ServerMain.wordsList) {
            if (target.equals(word) && word.getState() == WordState.OPEN) {
                word.setState(WordState.RESERVED);
                ServerMain.broadcast("reserveWord" + ServerMain.messageDelimiter + WordConversion.fromWord(word));
                return;
            }
        }
    }

    public void handleReopenWord(String input) {
        String[] tokenList = input.split(ServerMain.messageDelimiter);
        Word target = WordConversion.toWord(tokenList[1]);

        for (Word word : ServerMain.wordsList) {
            if (target.equals(word) && word.getState() == WordState.RESERVED) {
                word.setState(WordState.OPEN);
                ServerMain.broadcast("reopenWord" + ServerMain.messageDelimiter + WordConversion.fromWord(word));
                return;
            }
        }
    }

}
