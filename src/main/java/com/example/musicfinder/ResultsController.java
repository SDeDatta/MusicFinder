package com.example.musicfinder;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.geometry.Insets;
import javafx.geometry.Pos;

import java.awt.Desktop;
import java.net.URI;
import java.util.List;

public class ResultsController {

    @FXML private FlowPane resultsPane;
    @FXML private Label queryLabel;
    @FXML private Label seedLabel;

    /**
     * Called by HomeController after search completes.
     * Receives the list of recommended songs and builds the card UI.
     */
    public void loadResults(List<Song> songs, Song seed, String query) {
        queryLabel.setText("\"" + query + "\"");
        seedLabel.setText("Based on: " + seed.getTrackName()
                + " by " + seed.getArtists());

        resultsPane.getChildren().clear();

        for (Song song : songs) {
            resultsPane.getChildren().add(buildSongCard(song));
        }
    }

    /**
     * Builds a single song card VBox for a given Song.
     * Card contains: album art placeholder, title, artist,
     * genre, energy bar, valence bar, and Spotify link button.
     */
    private VBox buildSongCard(Song song) {
        VBox card = new VBox(12);
        card.getStyleClass().add("song-card");
        card.setPrefWidth(280);
        card.setPadding(new Insets(16));
        card.setAlignment(Pos.TOP_LEFT);

        // Album art placeholder — teal rectangle with song initial
        StackPane albumArt = new StackPane();
        albumArt.setPrefSize(248, 248);
        albumArt.getStyleClass().add("album-art");

        Label initial = new Label(
                song.getTrackName().substring(0, 1).toUpperCase()
        );
        initial.getStyleClass().add("album-initial");
        albumArt.getChildren().add(initial);

        // Song title
        Label title = new Label(song.getTrackName());
        title.getStyleClass().add("card-title");
        title.setWrapText(true);
        title.setMaxWidth(248);

        // Artist name
        Label artist = new Label(song.getArtists());
        artist.getStyleClass().add("card-artist");
        artist.setWrapText(true);
        artist.setMaxWidth(248);

        // Genre tag
        Label genre = new Label(song.getGenre().toUpperCase());
        genre.getStyleClass().add("genre-tag");

        // Energy bar
        VBox energyRow = buildFeatureBar(
                "ENERGY", song.getEnergy(), "#00d4aa"
        );

        // Valence bar
        VBox valenceRow = buildFeatureBar(
                "VIBE", song.getValence(), "#4a9eff"
        );

        // Spotify link button
        Button spotifyBtn = new Button("Open in Spotify ↗");
        spotifyBtn.getStyleClass().add("spotify-button");
        spotifyBtn.setOnAction(e -> openSpotify(song.getTrackId()));

        card.getChildren().addAll(
                albumArt, title, artist, genre,
                energyRow, valenceRow, spotifyBtn
        );

        return card;
    }

    /**
     * Builds a labeled progress bar showing a feature value.
     * Used for energy and valence on each card.
     */
    private VBox buildFeatureBar(String label, double value, String color) {
        VBox container = new VBox(4);

        HBox labelRow = new HBox();
        labelRow.setAlignment(Pos.CENTER_LEFT);

        Label featureLabel = new Label(label);
        featureLabel.getStyleClass().add("feature-label");

        Label valueLabel = new Label(String.format("%.0f%%",value * 100));
        valueLabel.getStyleClass().add("feature-value");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        labelRow.getChildren().addAll(featureLabel, spacer, valueLabel);

        // Background track
        StackPane barContainer = new StackPane();
        barContainer.setAlignment(Pos.CENTER_LEFT);

        Rectangle track = new Rectangle(248, 4);
        track.setFill(Color.web("#1e3a5f"));
        track.setArcWidth(4);
        track.setArcHeight(4);

        // Filled portion
        Rectangle fill = new Rectangle(248 * value, 4);
        fill.setFill(Color.web(color));
        fill.setArcWidth(4);
        fill.setArcHeight(4);

        barContainer.getChildren().addAll(track, fill);
        StackPane.setAlignment(fill, javafx.geometry.Pos.CENTER_LEFT);

        container.getChildren().addAll(labelRow, barContainer);
        return container;
    }

    /**
     * Opens the song's Spotify page in the system browser.
     * Spotify track URLs follow the pattern:
     * open.spotify.com/track/{trackId}
     */
    private void openSpotify(String trackId) {
        try {
            Desktop.getDesktop().browse(
                    new URI("https://open.spotify.com/track/" + trackId)
            );
        } catch (Exception e) {
            System.out.println("Could not open Spotify link: " + e.getMessage());
        }
    }

    /**
     * Goes back to the home screen when Back button is clicked.
     */
    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    HelloApplication.class.getResource("HomeScreen.fxml")
            );
            Scene scene = new Scene(loader.load(), 1000, 700);
            scene.getStylesheets().add(
                    HelloApplication.class.getResource("styles.css").toExternalForm()
            );
            HelloApplication.primaryStage.setScene(scene);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}