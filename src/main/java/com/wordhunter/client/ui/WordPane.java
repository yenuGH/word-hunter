package com.wordhunter.client.ui;

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
    private static final Font FONT = new Font("Arial", 20);
    public static final String BORDER = "-fx-border-color: -border-color; -fx-border-width: 2;";
    public static final Color DEFAULT_COLOR = Color.WHITE;

    // Member variables
    // Represents character it has
    private String word;
    // Current color a pane has
    private Color currentColor = DEFAULT_COLOR;
    // Label
    private final Label label;

    private FadeTransition animation;

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
        this.word = word;
        this.label.setText(word);
    }

    public void setColor(String color) {
        this.label.setTextFill(Color.web(color));
    }

    // Give the color flush animation
    // The background color oscillate in given interval in millisecond
    public void initAnimation(long interval){
        System.out.println("Interval: " + interval);
        if (animation != null) {
            closeAnimation();
        }
        if (interval > 0) {
            animation = new FadeTransition(Duration.millis(interval), this.label);
            double currentOpacity = this.label.getOpacity();
            animation.setFromValue(currentOpacity);
            animation.setToValue(0);
            animation.setCycleCount(1);
            animation.play();
        }
    }

    public void closeAnimation() {
        try
        {
            animation.stop();
            animation = null;
        }
        catch(NullPointerException ignored){}
    }
}
