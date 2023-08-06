package com.wordhunter.conversion;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.wordhunter.models.Player;

import java.util.Vector;

public class PlayerConversion {
    public static String fromPlayers(Vector<Player> players) {
        Gson gson = new Gson();
        return gson.toJson(players);
    }

    public static Vector<Player> toPlayers(String serializedData) {
        Gson gson = new Gson();
        return gson.fromJson(serializedData, new TypeToken<Vector<Player>>(){}.getType());
    }
}
