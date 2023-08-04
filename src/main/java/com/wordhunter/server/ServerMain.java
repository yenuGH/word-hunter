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


import com.wordhunter.conversion.PlayerConversion;
import com.wordhunter.conversion.WordConversion;
import com.wordhunter.models.Player;
import com.wordhunter.models.Word;
import com.wordhunter.models.WordGenerator;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.Semaphore;


/**
 * Used to map keywords to functions
 */
interface ServerMessageMethod
{
    void method(PlayerThread parent, String input);
}

class WordTimerTask extends TimerTask {

    private Word currentWord;

    public WordTimerTask(Word word) {
        this.currentWord = word;
    }
    public void run() {
        try {
            ServerMain.wordsListLock.acquire();
        } catch (InterruptedException e) {
            System.out.println("unable to lock the words list access");
            return;
        }
        // Remove once the TTL of the word is done
        //ServerMain.wordsList.remove(currentWord);
        ServerMain.wordsList.set(currentWord.getWordID(), null);
        ServerMain.broadcast("removeWord" + ServerMain.messageDelimiter + currentWord.getWordID());

        Word newWord = WordGenerator.generateNewWord();
        //ServerMain.wordsList.add(newWord);
        ServerMain.wordsList.set(newWord.getWordID(), newWord);
        ServerMain.broadcast("addNewWord" + ServerMain.messageDelimiter + WordConversion.fromWord(newWord));

        ServerMain.wordsListLock.release();

        Timer timer = new Timer();
        timer.schedule(new WordTimerTask(newWord), newWord.generateTimeToLive());
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

    public static final Semaphore wordsListLock = new Semaphore(1);

    // game variables
    public final static int wordLimit = 15;
    public final static int dimension = 5;
    public static final Vector<Word> wordsList = new Vector<>();
    public final static String defaultColor = "#000000";

    /**
     * main()
     * main entry point. creates server, waits x minutes before starting game
     */
    public void run()
    {
        for (int i = 0; i < 25; i++) {
            wordsList.add(null);
        }

        System.out.println("starting server");
        WordGenerator.readDictionary();

        // start thread to start accepting clients
        ServerAcceptClients thread = new ServerAcceptClients();
        thread.start();

        // waiting room timer
        try
        {
            timerStartTime = System.nanoTime();
            // TODO: CHANGE THIS TIMER BACK TO 60; it's 15 for testing
            Thread.sleep(startGameTimeMin * 5000);
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

        // generating words and broadcast each word to all client
        for (int i = 0; i < wordLimit; i++) {
            Word newWord = WordGenerator.generateNewWord();
            wordsList.set(newWord.getWordID(), newWord);
            ServerMain.broadcast("addNewWord" + ServerMain.messageDelimiter + WordConversion.fromWord(newWord));

            Timer timer = new Timer();
            timer.schedule(new WordTimerTask(newWord), newWord.generateTimeToLive());
        }

        // game duration timer
        try
        {
            timerStartTime = System.nanoTime();
            Thread.sleep(gameMaxTimeMin * 30000);
            thread.sock.close(); // stop server from accepting new connections
            ServerMain.serverState = ServerState.GAME_END;
        }
        catch (InterruptedException | IOException e)
        {
            System.out.println("server timer interrupted");
        }

        Player winner = findWinner();
        // TODO: add scores to message
        broadcast("gameOver" + ServerMain.messageDelimiter + PlayerConversion.fromPlayer(winner));
        // TODO: clean up sockets (from client side? add option to start new game?)
        System.exit(0);
    }

    public static Player findWinner() {
        if (playerList.isEmpty()) {
            return null; // Return null if the playerList is empty
        }

        Player winner = playerList.get(0);

        for (Player player : playerList) {
            if (player.getScore() > winner.getScore()) {
                winner = player;
            }
        }
        return winner;
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
