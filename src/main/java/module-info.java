module com.example.musicfinder {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;
    requires com.opencsv;
    requires com.google.gson;
    requires java.net.http;
    requires java.desktop;    // ← add this line

    opens com.example.musicfinder to javafx.fxml;
    exports com.example.musicfinder;
}