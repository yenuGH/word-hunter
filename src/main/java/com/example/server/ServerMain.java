package com.example.server;

/*
  ServerMain.java

  main tcp server program:
    - broadcast to clients
    - send message to specific client
    - listen/accept new connections for interval before game starts
    - listen for messages from connected clients
    - handle client disconnect
    - broadcast on new client join
 */


import com.example.server.conversion.PlayerConversion;
import com.example.server.models.Player;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;


/**
 * ServerAcceptClients
 * thread to continue listening for new connections
 */
class ServerAcceptClients extends Thread
{
    public ServerSocket sock = null;                        // server socket

    private final Vector<String> colorIds = new Vector<>();       // colors assigned to players

    private final Semaphore playerColorListLock = new Semaphore(1);

    private final Vector<Player> disconnectedPlayerList = new Vector<>();

    /**
     * ServerAcceptClients()
     * set up colorIds vector
     */
    public ServerAcceptClients()
    {
        // setup color vector
        colorIds.add("#F00");
        colorIds.add("#0F0");
        colorIds.add("#00F");
        colorIds.add("#F0F");
        colorIds.add("#FF0");
        colorIds.add("#0FF");
    }


    /**
     * removePlayer()
     * on client disconnect, update username/colorId lists
     * @param index index in playerList and colorId
     */
    public void removePlayer(int index)
    {
        try
        {
            playerColorListLock.acquire();

            // remove from server lists
            Player removedPlayer = ServerMain.playerList.remove(index);
            colorIds.add(removedPlayer.getColor());
            // store for allowing reconnect attempt
            disconnectedPlayerList.add(removedPlayer);

            // broadcast updated player list and color ids
            ServerMain.broadcast("playerDisconnect" + ServerMain.messageDelimiter
                                +"playerList" + ServerMain.messageDelimiter
                                + ","
                                + PlayerConversion.fromPlayers(ServerMain.playerList)
                                + ServerMain.messageDelimiter);
            playerColorListLock.release();
        }
        catch (InterruptedException ignored) {}
    }

    /**
     * run()
     * start listening for new connections
     */
    public void run()
    {
        System.out.println("starting accepting clients");

        // create server
        try
        {
            sock = new ServerSocket(ServerMain.serverPort);
        }
        catch (IOException e)
        {
            System.out.println("Failed to start server");
            System.exit(1);
        }

        // start listening for client connection requests
        try
        {
            ServerMain.serverState = ServerState.LISTENING_CONNECTIONS;
            while (true)
            {
                // on request -> create new thread with socket
                Socket client = sock.accept();

                // setup read stream
                InputStream is = client.getInputStream();
                BufferedReader in = new BufferedReader(new InputStreamReader(is));

                // read and process message from client
                String[] msg = in.readLine().split(ServerMain.messageDelimiter);

                // new client join
                if (Objects.equals(msg[0], "username"))
                {
                    // game started
                    if(ServerMain.serverState != ServerState.LISTENING_CONNECTIONS)
                    {
                        ServerMain.sendMessageToClient(client, "error" + ServerMain.messageDelimiter
                                + "game already started");
                        in.close();
                        client.close();
                    }
                    // max clients reached
                    else if(ServerMain.clientLimit <= ServerMain.playerList.size())
                    {
                        ServerMain.sendMessageToClient(client, "error" + ServerMain.messageDelimiter
                                                                + "maximum clients reached");
                        in.close();
                        client.close();
                    }
                    else
                    {
                        // create player obj and store socket in there
                        newPlayerJoinHandle(msg[1], client);
                    }
                }
                // user reconnect (doesn't work if entire application closed)
                else if(ServerMain.serverState != ServerState.GAME_END
                        && Objects.equals(msg[0], "reconnect")
                        && Objects.equals(msg[1], "username")
                        && Objects.equals(msg[3], "colorId"))
                {
                    try
                    {
                        playerColorListLock.acquire();

                        // username + colorId pair match? (change later?)
                        // kind of jank player matching; find a matching username and then see if the color matches
                        // we should change this to use a UUID later
                        Optional<Player> foundPlayer = disconnectedPlayerList.stream()
                                .filter(player -> player.getName().equals(msg[2]))
                                .findFirst();
                        if (foundPlayer.isPresent() && foundPlayer.get().getColor().equals(msg[4]))
                        {
                            disconnectedPlayerList.remove(foundPlayer);
                            reconnectHandle(msg[2], client);
                        }
                        playerColorListLock.release();
                    }
                    catch (InterruptedException ignored) {}
                }
            }
        }
        catch (IOException e) // called when server socket closed from outside thread
        {
            System.out.println("Server listening closed");
        }
    }

    /**
     * newPlayerJoinHandle()
     * @param username new player username
     * @param client new player socket
     */
    private void newPlayerJoinHandle(String username, Socket client)
    {
        try
        {
            playerColorListLock.acquire();

            int playerCount = ServerMain.playerList.size() + 1;
            System.out.println("creating player " + playerCount + ": " + username);

            // add player to list and create listening thread
            ServerMain.playerList.add(
                    new Player(username, colorIds.remove(0), client)
            );


            PlayerThread newPlayerThread = new PlayerThread(this, client, playerCount - 1);
            newPlayerThread.start();

            // broadcast new player list and color ids
            ServerMain.broadcast("newPlayerJoin" + ServerMain.messageDelimiter
                                +"playerList" + ServerMain.messageDelimiter
                                + PlayerConversion.fromPlayers(ServerMain.playerList));

            // send to new client how long until game starts (change to broadcast to make sure all timers are right?)
            long elapsedTimeNs = System.nanoTime() - ServerMain.timerStartTime;
            long remainingSeconds = (ServerMain.startGameTimeMin * 60)
                                    - TimeUnit.SECONDS.convert(elapsedTimeNs, TimeUnit.NANOSECONDS);
            ServerMain.sendMessageToClient(client, "startTimer" + ServerMain.messageDelimiter
                                            + remainingSeconds);

            playerColorListLock.release();
        }
        catch (InterruptedException ignored) {}
    }

    /**
     * handle disconnected players reconnecting. send updated data if game started
     * TODO: test
     * @param username username
     * @param client socket
     */
    private void reconnectHandle(String username, Socket client)
    {
        int playerCount = ServerMain.playerList.size() + 1;
        System.out.println("re-adding " + username + " as player " + playerCount);

        // add player to list and create listening thread
        ServerMain.playerList.add(
                new Player(username, colorIds.remove(0), client)
        );
        PlayerThread newPlayerThread = new PlayerThread(this, client, playerCount - 1);
        newPlayerThread.start();

        // broadcast reconnected player username + colorId
        ServerMain.broadcast("reconnect" + ServerMain.messageDelimiter
                +"username" + ServerMain.messageDelimiter
                + username + ServerMain.messageDelimiter);

        // if game already started
        if(ServerMain.serverState == ServerState.GAME_IN_PROGRESS)
        {
            // send to reconnected client updated health + current words + word timers + game timer + player list + color ids
            String msg = "updated data here";
            ServerMain.sendMessageToClient(client, msg);
        }
    }

}


/**
 * Used to map keywords to functions
 */
interface ServerMessageMethod
{
    void method(PlayerThread parent, String input);
}

/**
 * PlayerThread
 * listens from messages from client and forwards them to main server thread
 * handles client disconnect
 * can call removePlayer() from ServerAcceptClients
 */
class PlayerThread extends Thread
{
    // change these to player class when implemented
    private final int index;
    private final Socket sock;

    private final ServerAcceptClients parent;

    private final Map<String, ServerMessageMethod> messageToCallback = new HashMap<>();

    // keep alive
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> heartBeatHandle;
    private final Runnable disconnectRunnable = () -> {
        try
        {
            disconnect();
        }
        catch (IOException ignored) {}
    };


    /**
     * PlayerThread()
     * constructor
     * @param aParent parent thread
     * @param aSock client socket
     * @param anIndex index in clientSocks
     */
    public PlayerThread(ServerAcceptClients aParent, Socket aSock, int anIndex)
    {
        parent = aParent;
        sock = aSock;
        index = anIndex;

        // setup message callbacks
        messageToCallback.put("", PlayerThread::handleHeartBeat);

        heartBeatHandle = scheduler.scheduleAtFixedRate(disconnectRunnable,
                2*ServerMain.heartBeatInterval,
                2*ServerMain.heartBeatInterval,
                TimeUnit.SECONDS);
    }

    /**
     * run()
     * listens for messages from client. (identical to code in client, move later to new class?)
     */
    public void run()
    {
        // set up input stream
        InputStream is;
        try
        {
            sock.setSoTimeout(2 * ServerMain.heartBeatInterval);
            is = sock.getInputStream();
        }
        catch (IOException e)
        {
            System.out.println("failed to get input stream");
            return;
        }
        BufferedReader in = new BufferedReader(new InputStreamReader((is)));

        // continue reading
        while(true)
        {
            try
            {
                String input = in.readLine();

                // reset disconnect timer
                heartBeatHandle.cancel(true);
                heartBeatHandle = scheduler.scheduleAtFixedRate(disconnectRunnable,
                        2*ServerMain.heartBeatInterval,
                        2*ServerMain.heartBeatInterval,
                        TimeUnit.SECONDS);

                if (input != null)
                {
                    if(!Objects.equals("", input))
                        System.out.println("client" + index + " says: " + input);
                    handleClientMessage(input);
                }
            }
            catch (IOException e) // disconnect
            {
                try
                {
                    disconnect();
                    System.out.println("Player" + (index + 1) + " disconnected");
                }
                catch (IOException ex)
                {
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
    public void disconnect() throws IOException
    {
        parent.removePlayer(index); // remove username + reset colorId + broadcast to remaining clients
        sock.close();
    }

    /**
     * handleClientMessage()
     * call function depending on first keyword
     * @param msg message from client (identical to client code -> move to separate class?)
     */
    public void handleClientMessage(String msg)
    {
        // call function mapped to message
        String[] tokenList = msg.split(ServerMain.messageDelimiter);
        ServerMessageMethod a = messageToCallback.get(tokenList[0]);
        if(a != null)
        {
            a.method(this, msg);
        }
    }

    /**
     * handleHeartBeat()
     * send back heartbeat ack
     * @param input message from client
     */
    public void handleHeartBeat(String input)
    {
        ServerMain.sendMessageToClient(sock, "");
    }
}


/**
 * program main entry point
 */
public class ServerMain extends Thread
{
    // const variables used in client too
    public final static int serverPort = 6666;
    public final static String messageDelimiter = "!";
    public final static int clientLimit = 6; // update ServerAcceptClients.colorIds if you change
    public final static int startGameTimeMin = 1; // change later
    public final static int gameMaxTimeMin = 1; // change later
    public final static int heartBeatInterval = 1000;


    // server exclusive variables
    public static final Vector<Player> playerList = new Vector<>();
    public static ServerState serverState = ServerState.STARTED;
    public static long timerStartTime;

    // game variables
    public final static int wordLimit = 10;

    /**
     * main()
     * main entry point. creates server, waits x minutes before starting game
     */
    public void run()
    {
        System.out.println("starting server");

        // start thread to start accepting clients
        ServerAcceptClients thread = new ServerAcceptClients();
        thread.start();

        // start game start timer
        try
        {
            timerStartTime = System.nanoTime();
            Thread.sleep(startGameTimeMin * 15000);
            ServerMain.serverState = ServerState.GAME_IN_PROGRESS;
        }
        catch (InterruptedException e)
        {
            System.out.println("server timer interrupted");
        }

        System.out.println("server game start");

        broadcast("gameStart" + messageDelimiter
                + "gameTimer" + messageDelimiter
                + gameMaxTimeMin * 60);

        // start game timer
        try
        {
            timerStartTime = System.nanoTime();
            Thread.sleep(gameMaxTimeMin * 60000);
            thread.sock.close(); // stop server from accepting new connections
            ServerMain.serverState = ServerState.GAME_END;
        }
        catch (InterruptedException | IOException e)
        {
            System.out.println("server timer interrupted");
        }

        // TODO: add scores to message
        broadcast("gameOver");
        // TODO: clean up sockets (from client side? add option to start new game?)
    }

    /**
     * broadcast()
     * send message to all players
     * @param message message to send to all connected clients
     */
    public static void broadcast(String message)
    {
        // write message to each client socket
        for(Player player: playerList)
        {
            sendMessageToClient(player.getSocket(), message);
        }
    }

    /**
     * sendMessageToClient()
     * send message to only one specific client
     * @param sock socket
     * @param message message
     */
    public static void sendMessageToClient(Socket sock, String message)
    {
        try
        {
            OutputStream os = sock.getOutputStream();
            if(os == null)
            {
                System.out.println("Failed to get client output stream");
                return;
            }
            PrintWriter out = new PrintWriter(os, true);
            out.println(message);
        }
        catch (IOException e)
        {
            System.out.println("Failed to get client output stream");
        }
    }
}

