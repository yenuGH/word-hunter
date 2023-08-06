package com.wordhunter.models;

import java.util.Arrays;
import java.util.concurrent.Semaphore;

public class WordList {
    private final int size;
    private final Semaphore[] wordListLock;
    private final Word[] wordList;

    public WordList(int size){
        this.size = size;

        // Initialize lock array
        this.wordListLock = new Semaphore[this.size];
        Arrays.fill(this.wordListLock, new Semaphore(1));

        // Initialize word array
        this.wordList = new Word[this.size];
        Arrays.fill(this.wordList, null);
    }

    private void lock(int index){
        try {
            this.wordListLock[index].acquire();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public void release(int index){
        this.wordListLock[index].release();
    }

    public void set(int index, Word word){
        lock(index);
        this.wordList[index] = word;
        release(index);
    }

    public Word get(int index){
        lock(index);
        if(this.wordList[index] == null)
        {
            release(index);
        }
        return this.wordList[index];
    }

    public boolean isOccupied(int index){
        boolean result = this.get(index) != null;
        this.release(index);
        return result;
    }

    public int getSize(){
        return this.size;
    }
}
