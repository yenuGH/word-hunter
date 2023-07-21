module com.example.word_hunter {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.word_hunter to javafx.fxml;
    exports com.example.word_hunter;
}