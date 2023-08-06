package com.wordhunter.models;

import java.io.Serializable;
import java.net.Socket;
import java.util.Objects;

public class Player implements Serializable {
    private final String name;
    private final String color;
    private int score;

    // socket is not serializable, and the word transient is needed to ensure that Gson doesn't try
    private final transient Socket socket;

    public Player(String name, String color, Socket socket) {
        this.name = name;
        this.color = color;
        this.score = 0;
        this.socket = socket;
    }

    public String getName() {
        return name;
    }

    public String getColor() {
        return color;
    }

    public int getScore() {
        return score;
    }

    public void addScore(int points) {
        this.score += points;
    }

    public Socket getSocket() {
        return socket;
    }

    @Override
    public String toString() {
        return "Player{" +
                ", name='" + name + '\'' +
                ", color='" + color + '\'' +
                ", score=" + score +
                ", socket=" + socket +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        Player player = (Player) obj;
        return Objects.equals(color, player.color);
    }
}
