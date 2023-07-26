package com.wordhunter.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

public class Word implements Serializable {
    private String word;
    private String state;
    private String color;
    private int posX;
    private int posY;
    //add timer for each word

    public Word(String word, String state) {
        this.word = word;
        this.state = state;
        this.color = "#F00";
        this.posX = 0;
        this.posY = 0;
    }

    public String getWord() {
        return word;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
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

    public void setGridPosition(int x, int y) {
        this.posX = x;
        this.posY = y;
    }

    public ArrayList<Integer> getGridPosition() {
        return new ArrayList<>(Arrays.asList(posX, posY));
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

