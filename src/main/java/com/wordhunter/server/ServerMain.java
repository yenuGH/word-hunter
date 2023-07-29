package com.wordhunter.server;

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


import com.wordhunter.conversion.WordConversion;
import com.wordhunter.models.Player;
import com.wordhunter.models.Word;

import java.io.*;
import java.net.Socket;
import java.util.*;


/**
 * Used to map keywords to functions
 */
interface ServerMessageMethod
{
    void method(PlayerThread parent, String input);
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
    public final static int wordLimit = 15;
    public final static int dimension = 5;
    public static final Vector<String> dictionary = new Vector<>();
    public static final Vector<Word> wordsList = new Vector<>();

    /**
     * main()
     * main entry point. creates server, waits x minutes before starting game
     */
    public void run()
    {
        System.out.println("starting server");
        readDictionary();

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
        // generating words and broadcast each word to all client
        for (int i = 0; i < wordLimit; i++) {
            generateNewWord(i);
        }

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

    // TODO: put methods into different class
    /**
     * Load all words in dictionary when server starts
     */
    private void readDictionary()
    {
        try {
            File file = new File("src/main/java/com/wordhunter/server/dictionary.txt");
            Scanner scanner = new Scanner(file);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                dictionary.add(line);
            }
            scanner.close();
        } catch (Exception e) {
            System.out.println("failed to open dictionary.txt file");
        }
    }

    /**
     * Generate a new Word object and broadcast it to all clients with the associated index
     * @param index index of Word in Vector<Word> wordsList
     */
    public void generateNewWord(int index)
    {
        Random rand = new Random(System.currentTimeMillis());
        String newWord = dictionary.get(rand.nextInt(dictionary.size()));
        while (checkDuplicateChar(newWord, wordsList)) {
            newWord = dictionary.get(rand.nextInt(dictionary.size()));
        }

        int x = rand.nextInt(dimension);
        int y = rand.nextInt(dimension);
        while (checkOccupiedSpot(x, y, wordsList)) {
            x = rand.nextInt(dimension);
            y = rand.nextInt(dimension);
        }

        // Broadcast new word to all clients
        Word word = new Word(newWord, x, y);
        wordsList.add(word);
        broadcast("addNewWord" + messageDelimiter + index + messageDelimiter + WordConversion.fromWord(word));
    }

    private boolean checkDuplicateChar(String word, Vector<Word> wordsList)
    {
        for (int i = 0; i < wordsList.size(); i++) {
            if (word.charAt(0) == wordsList.get(i).getWord().charAt(0)) {
                return true;
            }
        }
        return false;
    }
    private boolean checkOccupiedSpot(int x, int y, Vector<Word> wordsList)
    {
        for (int i = 0; i < wordsList.size(); i++) {
            Word word = wordsList.get(i);
            if (x == word.getPosX() && y == word.getPosY()) {
                return true;
            }
        }
        return false;
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

