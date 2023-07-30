package com.wordhunter.client.logic;

/*
 * ClientMain.java
 *
 * main tcp client program:
 * - creates tcp server in thread
 * - connects to server as a client
 * - uses thread to listen for server messages
 * - handles server disconnect by exiting
 */

import com.wordhunter.client.ui.ServerPageController;
import com.wordhunter.models.Word;
import com.wordhunter.models.Player;
import com.wordhunter.server.ServerMain;
import com.wordhunter.server.ServerState;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.Semaphore;


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
 * ClientMain
 *
 * main class handling opening server/connection
 */
public class ClientMain
{
    private static ClientMain clientMainInstance = null;
    // server/connection variables
    public static final int reconnectMaxAttempt = 5;
    public static int reconnectAttempts = 0;

    public ServerPageController serverPageController;

    public String serverIP;
    public static Socket sock;              // socket connected to server

    // game variables
    public String username;        // temporary: entered by client later. move to player class?
    public static String colorId = "#DC143C";
    public static final Vector<Word> wordsList = new Vector<>();
    public Vector<Player> players;

    public static final Semaphore clientWordsListLock = new Semaphore(1);

    // Singleton
    public static ClientMain getInstance() {
        if (clientMainInstance == null) {
            try {
                clientMainInstance = new ClientMain();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return clientMainInstance;
    }

    /**
     * ClientMain()
     * entry point -> choose start server or join as client
     */
    public ClientMain() throws IOException, InterruptedException {}

    public void setAddress(String address) {
        if (Objects.equals(address, ""))
            serverIP = "localhost";
        else
            serverIP = address;
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

    public Vector<Player> getPlayerList(){
        return players;
    }

    public String getServerIP(){
        return this.serverIP;
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

    public void setUsername(String username){
        this.username = username;
    }

    public String getUsername(){
        return this.username;
    }

    public void setServerPageController(ServerPageController serverPageController){
        this.serverPageController = serverPageController;
    }

    public ServerPageController getServerPageController(){
        return this.serverPageController;
    }
}
