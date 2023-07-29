package com.wordhunter.models;

import com.wordhunter.server.ServerMain;

import java.io.Serializable;
import java.util.Objects;
import java.util.Random;

public class Word implements Serializable {
    private String word;
    private WordState state;
    private String color;
    private int posX;
    private int posY;
    // TODO: add timer for each word, relevant to health bar
    private long expiredTime;

    public Word(String word, int x, int y) {
        this.word = word;
        this.state = WordState.OPEN;
        this.color = "#F00";
        this.posX = x;
        this.posY = y;
        this.expiredTime = System.currentTimeMillis() + getTimeToLive();
    }

    public long getTimeToLive() {
        Random random = new Random();
        int lowerBound = 5000;
        int upperBound = 8000;
        return random.nextInt((upperBound - lowerBound) + 1) + lowerBound;
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

    public int getPosX() {
        return posX;
    }

    public int getPosY() {
        return posY;
    }

    @Override
    public String toString() {
        return "Word{" +
                "word='" + word + '\'' +
                ", state='" + state + '\'' +
                ", color=" + color +
                ", posX=" + posX +
                ", posY=" + posY +
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

