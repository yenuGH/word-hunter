package com.wordhunter.models;

import java.io.Serializable;

public class Word implements Serializable {
    private String word;
    private WordState state;
    private String color;
    private int posX;
    private int posY;
    // TODO: add timer for each word, relevant to health bar

    public Word(String word, int x, int y) {
        this.word = word;
        this.state = WordState.OPEN;
        this.color = "#F00";
        this.posX = x;
        this.posY = y;
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
}

