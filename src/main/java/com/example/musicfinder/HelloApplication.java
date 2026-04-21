package com.example.musicfinder;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class HelloApplication extends Application {

    public static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;

        FXMLLoader loader = new FXMLLoader(
                HelloApplication.class.getResource("HomeScreen.fxml")
        );
        Scene scene = new Scene(loader.load(), 1000, 700);

        scene.getStylesheets().add(
                HelloApplication.class.getResource("styles.css").toExternalForm()
        );

        stage.setTitle("Music Finder");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}