package com.example.word_hunter;

import javafx.animation.FadeTransition;
import javafx.animation.Transition;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.util.Duration;

public class WordPane extends StackPane {
    // Constant
    // Word font
    private static final Font FONT = new Font("Arial", 50);
    public static final String BORDER = "-fx-border-color: #000000; -fx-border-width: 2px;";
    public static final Color DEFAULT_COLOR = Color.WHITE;

    // Member variables
    // Represents character it has
    private String word;
    // Current color a pane has
    private Color currentColor = DEFAULT_COLOR;
    // Label
    private final Label label;

    public WordPane(String word){
        this.word = word;

        // Set a border
        super.setStyle(BORDER);

        // Create a label and add it to pane
        this.label = new Label(this.word);
        this.label.setFont(FONT);
        this.label.setAlignment(Pos.CENTER);

        // Add it to stack pane
        this.getChildren().add(label);
    }

    public String getWord() {
        return word;
    }

    // Set the word of the label
    public void setWord(String word){
        this.label.setText(word);
    }

    // Give the color flush animation
    // The background color oscillate in given interval in millisecond
    public Transition getAnimation(int interval){
        FadeTransition transition = new FadeTransition(Duration.seconds(interval), this.label);
        transition.setFromValue(1);
        transition.setToValue(0);
        transition.setCycleCount(1);
        return transition;
    }
}
