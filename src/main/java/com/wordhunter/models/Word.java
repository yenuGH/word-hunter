package com.wordhunter.models;

import java.io.Serializable;
import java.util.Objects;
import java.util.Random;

public class Word implements Serializable {
    private final String word;
    private WordState state;
    private String color;
    private final int wordID;
    private final long expiredTime;

    private final int LOWER_BOUND_TTL = 8000;
    private final int UPPER_BOUND_TTL = 16000;

    public Word(String word, int wordID) {
        this.word = word;
        this.state = WordState.OPEN;
        this.color = "#000000";
        this.wordID = wordID;
        this.expiredTime = System.currentTimeMillis() + generateTimeToLive();
    }

    public long generateTimeToLive() {
        Random random = new Random();
        return random.nextInt((UPPER_BOUND_TTL - LOWER_BOUND_TTL) + 1) + LOWER_BOUND_TTL;
    }

    public long getTimeToLiveRemaining() {
        long timeRemaining = expiredTime - System.currentTimeMillis();
        return Math.max(0, timeRemaining);
    }

    public String getWord() {
        return word;
    }

    public WordState getState() {
        return state;
    }

    public void setState(WordState state) {
        this.state = state;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public int getWordID() {
        return wordID;
    }

     @Override
    public String toString() {
        return "Word{" +
                "word='" + word + '\'' +
                ", state='" + state + '\'' +
                ", color=" + color +
                ", wordID=" + wordID +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        Word other = (Word) obj;
        return Objects.equals(word, other.word);
    }
}

