package com.example.client.logic;

/*
 * ClientMain.java
 *
 * main tcp client program:
 * - creates tcp server in thread
 * - connects to server as a client
 * - uses thread to listen for server messages
 * - handles server disconnect by exiting
 */

import com.example.server.ServerMain;
import com.example.server.ServerState;
import com.example.server.conversion.PlayerConversion;
import com.example.server.models.Player;

import java.io.*;
import java.net.Socket;
import java.util.*;


/**
 * Used to map keywords to functions
 */
interface MessageMethod
{
    void method(ClientListening parent, String input);
}


/**
 * send empty message as heartbeat
 */
class HeartBeat extends TimerTask {
    public void run() {
        try
        {
            ClientMain.sendMsgToServer("");
        }
        catch (IOException e)
        {
            System.out.println("failed to send heartbeat");
        }
    }
}

/**
 * ClientListening
 *
 * thread listening for server messages. handles server disconnect. handles server messages
 */
class ClientListening extends Thread
{
    public Socket sock;
    public ClientMain parent;

    public Timer heartBeatTimer;    // send heartbeat every x seconds
    boolean gotColorId = false;     // used to get colorId from first connection message

    private final Map<String, MessageMethod> messageToCallback = new HashMap<>();

    /**
     * ClientListening()
     * constructor
     * @param aSock socket connected to server
     */
    public ClientListening(Socket aSock, ClientMain aParent)
    {
        sock = aSock;
        parent = aParent;

        // set up callback map
        messageToCallback.put("newPlayerJoin", ClientListening::newPlayerJoin);
        messageToCallback.put("playerDisconnect", ClientListening::playerDisconnect);
        messageToCallback.put("error", ClientListening::error);
        messageToCallback.put("", ClientListening::heartBeatAck);
    }

    /**
     * run()
     * main listening thread
     */
    public void run()
    {
        // set up input stream
        InputStream is;
        try
        {
            sock.setSoTimeout(2 * 10);
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
                if(input != null)
                {
                    if(!Objects.equals("", input))
                        System.out.println("server says: " + input);
                    handleServerMessage(input);
                }
            }
            catch (IOException e) // disconnect
            {
                System.out.println("failed to read from socket. disconnecting...");
                try
                {
                    disconnect();
                    System.out.println("disconnected");
                }
                catch (IOException ex)
                {
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
    public void disconnect() throws IOException
    {
        sock.close();
        if(ClientMain.reconnectAttempts < ClientMain.reconnectMaxAttempt)
        {
            System.out.println("reconnection attempt " + ClientMain.reconnectAttempts);
            parent.connectServer(true);
        }
    }

    /**
     * handleServerMessage()
     * call function depending on first keyword
     * @param msg message from server
     */
    public void handleServerMessage(String msg)
    {
        if(!gotColorId)
        {
            gotColorId = true;
            // setup keep alive
            heartBeatTimer = new Timer();
            heartBeatTimer.schedule(new HeartBeat(), 10);
        }

        // call function mapped to message
        String[] tokenList = msg.split(ServerMain.messageDelimiter);
        MessageMethod a = messageToCallback.get(tokenList[0]);
        if(a != null)
        {
            a.method(this, msg);
        }
    }

    // message event functions
    // TODO: add other keyword functions (see message_formats.txt)
    /**
     * newPlayerJoin()
     * TODO: add code to extract own colorId from here too
     * @param input message from server
     */
    public void newPlayerJoin(String input)
    {
        // get own color id
        if(parent.colorId.isEmpty())
        {
            String playerList = input.split("playerList"+ServerMain.messageDelimiter)[1];
            Vector<Player> players = PlayerConversion.toPlayers(playerList);
            parent.colorId = players.elementAt(players.size()-1).getColor();
            System.out.println("got color id:" + parent.colorId);
        }
    }
    /**
     * playerDisconnect()
     * TODO:
     * @param input message from server
     */
    public void playerDisconnect(String input)
    {
        System.out.println("playerDisconnect running:" + input);
    }
    /**
     * error()
     * TODO: if other error messages, add implementations
     * @param input message from server
     */
    public void error(String input)
    {
        try
        {
            sock.close();
        }
        catch (IOException e)
        {
            System.out.println("failed to close socket");
        }
        System.exit(0);
    }

    public void heartBeatAck(String input)
    {
        heartBeatTimer.schedule(new HeartBeat(), 10);
    }

}


/**
 * ClientMain
 *
 * main class handling opening server/connection
 */
public class ClientMain
{
    // server/connection variables
    public static final int reconnectMaxAttempt = 5;
    public static int reconnectAttempts = 0;

    public String serverIP;
    public static Socket sock;              // socket connected to server

    // game variables
    public String username = "test";        // temporary: entered by client later. move to player class?
    public String colorId = "";

    /**
     * ClientMain()
     * entry point -> choose start server or join as client
     */
    public ClientMain() throws IOException, InterruptedException
    {
        // temp for testing. replace with UI prompt?
        System.out.println("enter serverip: (enter nothing for localhost)");
        Scanner myObj = new Scanner(System.in);
        serverIP = myObj.nextLine();
        if(Objects.equals(serverIP, ""))
            serverIP = "localhost";

        System.out.println("1 to start server: (enter anything else to connect as client)");
        String input = myObj.nextLine();

        // for testing -> change later
        if(Objects.equals(input, "1"))
        {
            createServer();
        }
        connectServer(false);
    }

    /**
     * createServer()
     * creates server that other clients will join to.
     */
    public void createServer() throws InterruptedException
    {
        ServerMain server = new ServerMain();
        server.start();
        System.out.println("server thread started");

        // wait server up
        while(server.serverState != ServerState.LISTENING_CONNECTIONS)
        {
            Thread.sleep(100);
        }
    }

    /**
     * connectServer()
     * establishes connection to server with entered ip. sends message with username.
     *        server responds with time left until game start and playerList + colorId list
     * @param reconnect true/false
     */
    public void connectServer(boolean reconnect) throws IOException
    {
        sock = new Socket(serverIP, ServerMain.serverPort);
        sock.setSoTimeout(5000);

        String message = "username" + ServerMain.messageDelimiter
                        + username;
        if(reconnect && !colorId.isEmpty())
        {
            reconnectAttempts++;
            message = "reconnect" + ServerMain.messageDelimiter
                    + message + ServerMain.messageDelimiter
                    + "colorId" + ServerMain.messageDelimiter
                    + colorId;
        }
        else
        {
            reconnectAttempts = 0;
        }

        sendMsgToServer(message);
        ClientListening listenThread = new ClientListening(sock, this);
        listenThread.start();
    }

    /**
     * sendMsgToServer()
     * sends message to server
     * @param msg message to send to server
     */
    public static void sendMsgToServer(String msg) throws IOException
    {
        OutputStream os = sock.getOutputStream();
        PrintWriter out = new PrintWriter(os, true);
        out.println(msg);
    }

}
