module com.example.word_hunter {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires com.google.gson;

    opens com.wordhunter.client.ui to javafx.fxml;
    opens com.wordhunter.models to com.google.gson;
    exports com.wordhunter.client.ui;
    exports com.wordhunter.client;
    opens com.wordhunter.client to javafx.fxml;
}