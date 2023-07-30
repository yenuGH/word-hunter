package com.wordhunter.conversion;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.wordhunter.models.Word;

public class WordConversion {

    public static String fromWord(Word word) {
        Gson gson = new Gson();
        return gson.toJson(word);
    }

    public static Word toWord(String serializedList) {
        Gson gson = new Gson();
        return gson.fromJson(serializedList, new TypeToken<Word>(){}.getType());
    }
}

