package com.wordhunter.models;
import java.io.Serializable;
import java.net.Socket;
import java.util.Objects;
import java.util.UUID;

public class Player implements Serializable {
    private UUID id;
    private String name;
    private String color;
    private int score;

    // socket is not serializable, and the word transient is needed to ensure that Gson doesn't try
    private transient Socket socket;

    public Player(String name, String color, Socket socket) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.color = color;
        this.score = 0;
        this.socket = socket;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String username) {
        this.name = username;
    }

    public String getColor() {
        return color;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
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
                "id=" + id +
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
        return Objects.equals(id, player.id);
    }
}
