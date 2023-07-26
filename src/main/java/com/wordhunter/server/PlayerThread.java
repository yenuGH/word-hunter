package com.wordhunter.server;

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
        messageToCallback.put("reserveWordByIndex", PlayerThread::handleReserveWord);

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

    public void handleReserveWord(String input) {
        int index = Integer.parseInt(input.split(ServerMain.messageDelimiter)[1]);
        String word = input.split(ServerMain.messageDelimiter)[2];
        System.out.println("Reserved word at " + index + ": " + word);

        ServerMain.broadcast("addNewWord" + ServerMain.messageDelimiter + index + ServerMain.messageDelimiter + word);
    }
}
