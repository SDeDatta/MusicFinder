module com.example.musicfinder {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;

    opens com.example.musicfinder to javafx.fxml;
    exports com.example.musicfinder;
}