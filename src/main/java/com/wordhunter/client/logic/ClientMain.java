package com.wordhunter.client.logic;

import com.wordhunter.client.ui.ServerPageController;
import com.wordhunter.client.ui.WinnerPageController;
import com.wordhunter.models.WordGenerator;
import com.wordhunter.models.WordList;
import com.wordhunter.server.ServerMain;
import com.wordhunter.server.ServerState;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Objects;


/**
 * Used to map keywords to functions
 */
interface MessageMethod
{
    void method(ClientListening parent, String input);
}

/**
 * ClientMain
 * main class handling opening server/connection
 */
public class ClientMain
{
    private static ClientMain clientMainInstance = null;

    // server/connection variables
    public ServerPageController serverPageController;
    public WinnerPageController winnerPageController;
    public String serverIP;
    public static Socket sock;              // socket connected to server
    private static final String DEFAULT_HOST = "localhost";

    // game variables
    public String username;
    public static String colorId = "";
    public static WordList wordsList;

    // Singleton; each client should only have one ClientMain
    public static ClientMain getInstance() {
        if (clientMainInstance == null) {
            try
            {
                clientMainInstance = new ClientMain();
            }
            catch (IOException | InterruptedException ignored) {}
        }
        return clientMainInstance;
    }

    /**
     * ClientMain()
     * entry point -> choose start server or join as client
     */
    public ClientMain() throws IOException, InterruptedException {
        wordsList = new WordList(WordGenerator.dimension);
    }

    public void setAddress(String address) {
        if (Objects.equals(address, ""))
            serverIP = DEFAULT_HOST;
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
     */
    public void connectServer() throws IOException
    {
        sock = new Socket(serverIP, ServerMain.serverPort);
        sock.setSoTimeout(5000);

        String message = "username" + ServerMain.messageDelimiter
                + username;

        sendMsgToServer(message);
        ClientListening listenThread = new ClientListening(sock, this);
        listenThread.start();
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

    public WinnerPageController getWinnerPageController() {
        return winnerPageController;
    }

    public void setWinnerPageController(WinnerPageController winnerPageController) {
        this.winnerPageController = winnerPageController;
    }
}
