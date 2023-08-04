package com.wordhunter.models;

import com.wordhunter.conversion.WordConversion;
import com.wordhunter.server.ServerMain;

import java.io.File;
import java.util.Random;
import java.util.Scanner;
import java.util.Vector;

public class WordGenerator {

    public final static int dimension = 25;
    public static final Vector<String> dictionary = new Vector<>();

    /**
     * Load all words in dictionary when server starts
     */
    public static void readDictionary()
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
     */
    public static Word generateNewWord()
    {
        Random rand = new Random(System.currentTimeMillis());
        String newWord = dictionary.get(rand.nextInt(dictionary.size()));
        while (checkDuplicateChar(newWord, ServerMain.wordsList)) {
            newWord = dictionary.get(rand.nextInt(dictionary.size()));
        }

        int wordID = rand.nextInt(dimension);
        while (checkOccupiedSpot(wordID, ServerMain.wordsList)) {
            wordID = rand.nextInt(dimension);
        }

        // Broadcast new word to all clients
        Word word = new Word(newWord, wordID);
//        System.out.println("GENERATING NEW WORD " + word.getWord());
        return word;
    }

    private static boolean checkDuplicateChar(String word, Vector<Word> wordsList)
    {
        for (int i = 0; i < wordsList.size(); i++) {
            if (wordsList.get(i) != null) {
                if (word.charAt(0) == wordsList.get(i).getWord().charAt(0)) {
                    return true;
                }
            }
        }
        return false;
    }
    private static boolean checkOccupiedSpot(int id, Vector<Word> wordsList)
    {
//        for (int i = 0; i < wordsList.size(); i++) {
//            Word word = wordsList.get(i);
//            if (x == word.getWordID()) {
//                return true;
//            }
//        }
//        return false;
        return (wordsList.get(id) != null);
    }
}
