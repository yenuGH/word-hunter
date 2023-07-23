module com.example.word_hunter {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires com.google.gson;

    opens com.example.client.ui to javafx.fxml;
    opens com.example.server.models to com.google.gson;
    exports com.example.client.ui;
}