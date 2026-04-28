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
            // Use flippable card wrapper instead of buildSongCard directly
            resultsPane.getChildren().add(buildFlippableCard(song));
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
        // Get genre color for this card
        String accentColor = getGenreColor(song.getGenre());

// Album art placeholder with genre-colored gradient
        StackPane albumArt = new StackPane();
        albumArt.setPrefSize(248, 248);
        albumArt.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #0d2a45, "
                        + accentColor + "44);" +
                        "-fx-background-radius: 8;"
        );

        Label initial = new Label(song.getTrackName().substring(0, 1).toUpperCase());
        initial.setStyle(
                "-fx-font-family: 'Georgia';" +
                        "-fx-font-size: 72px;" +
                        "-fx-text-fill: " + accentColor + "66;" +
                        "-fx-font-weight: bold;"
        );
        albumArt.getChildren().add(initial);

// Add colored left border accent to card
        card.setStyle(
                "-fx-background-color: #112240;" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: " + accentColor + "66;" +
                        "-fx-border-radius: 12;" +
                        "-fx-border-width: 1;" +
                        "-fx-effect: dropshadow(gaussian, #00000066, 12, 0.1, 0, 4);"
        );

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
        // Change the energy and valence bar calls to pass the genre color
        VBox energyRow  = buildFeatureBar("ENERGY", song.getEnergy(),  accentColor);
        VBox valenceRow = buildFeatureBar("VIBE",   song.getValence(), accentColor);

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
    /**
     * Returns a teal/accent color hex string based on genre.
     * Used for the card's left border and album art gradient.
     */
    private String getGenreColor(String genre) {
        String g = genre.toLowerCase();
        if (g.contains("pop"))          return "#ff6eb4"; // pink
        if (g.contains("rock"))         return "#ff8c42"; // orange
        if (g.contains("hip-hop")
                || g.contains("rap"))          return "#9b59b6"; // purple
        if (g.contains("ambient")
                || g.contains("study")
                || g.contains("sleep"))        return "#4a9eff"; // blue
        if (g.contains("jazz"))         return "#f1c40f"; // gold
        if (g.contains("classical"))    return "#e8d5b7"; // cream
        if (g.contains("metal")
                || g.contains("punk"))         return "#e74c3c"; // red
        if (g.contains("r-n-b")
                || g.contains("soul"))         return "#e67e22"; // warm orange
        if (g.contains("country"))      return "#a0785a"; // brown
        if (g.contains("electronic")
                || g.contains("dance")
                || g.contains("edm"))          return "#00d4aa"; // teal
        if (g.contains("indie"))        return "#7ec8e3"; // light blue
        if (g.contains("folk")
                || g.contains("acoustic"))     return "#95e17d"; // green
        return "#00d4aa"; // default teal
    }
    /**
     * Builds the back face of the card showing detailed audio features.
     * Shown when the user hovers over a card.
     */
    private VBox buildCardBack(Song song, String accentColor) {
        VBox back = new VBox(10);
        back.setPrefWidth(280);
        back.setPrefHeight(470); // match front card height
        back.setPadding(new Insets(20));
        back.setAlignment(Pos.CENTER_LEFT);
        back.setStyle(
                "-fx-background-color: #0d2a45;" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: " + accentColor + ";" +
                        "-fx-border-radius: 12;" +
                        "-fx-border-width: 1.5;"
        );

        // Header
        Label header = new Label("AUDIO FEATURES");
        header.setStyle(
                "-fx-font-family: 'Georgia';" +
                        "-fx-font-size: 11px;" +
                        "-fx-text-fill: " + accentColor + ";" +
                        "-fx-font-weight: bold;"
        );

        Label songTitle = new Label(song.getTrackName());
        songTitle.setStyle(
                "-fx-font-family: 'Georgia';" +
                        "-fx-font-size: 13px;" +
                        "-fx-text-fill: #e0f0ff;" +
                        "-fx-font-weight: bold;"
        );
        songTitle.setWrapText(true);
        songTitle.setMaxWidth(240);

        javafx.scene.control.Separator sep =
                new javafx.scene.control.Separator();
        sep.setStyle("-fx-background-color: " + accentColor + "44;");

        // All feature bars on the back
        VBox features = new VBox(8);
        features.getChildren().addAll(
                buildFeatureBar("ENERGY",          song.getEnergy(),           accentColor),
                buildFeatureBar("VIBE",            song.getValence(),          accentColor),
                buildFeatureBar("DANCEABILITY",    song.getDanceability(),     accentColor),
                buildFeatureBar("ACOUSTICNESS",    song.getAcousticness(),     accentColor),
                buildFeatureBar("INSTRUMENTALNESS",song.getInstrumentalness(), accentColor),
                buildFeatureBar("LIVENESS",        song.getLiveness(),         accentColor),
                buildFeatureBar("SPEECHINESS",     song.getSpeechiness(),      accentColor)
        );

        // Popularity and tempo as text
        Label popLabel = new Label(String.format(
                "POPULARITY  %d / 100", song.getPopularity()
        ));
        popLabel.setStyle(
                "-fx-font-family: 'Georgia'; -fx-font-size: 10px; -fx-text-fill: #5a8a9f;"
        );

        Label tempoLabel = new Label(String.format(
                "TEMPO  %.0f BPM", song.getTempo()
        ));
        tempoLabel.setStyle(
                "-fx-font-family: 'Georgia'; -fx-font-size: 10px; -fx-text-fill: #5a8a9f;"
        );

        back.getChildren().addAll(
                header, songTitle, sep, features, popLabel, tempoLabel
        );
        return back;
    }

    /**
     * Wraps front and back card faces in a StackPane with a flip animation.
     * On mouse enter → flip to back. On mouse exit → flip to front.
     */
    private StackPane buildFlippableCard(Song song) {
        String accentColor = getGenreColor(song.getGenre());

        VBox front = buildSongCard(song);
        VBox back  = buildCardBack(song, accentColor);

        // Back starts invisible and rotated
        back.setRotationAxis(javafx.geometry.Point3D.ZERO
                .add(0, 1, 0)); // Y axis rotation
        back.setRotate(180);
        back.setVisible(false);

        StackPane container = new StackPane(front, back);
        container.setPrefWidth(280);

        // Flip animation — two halves: front rotates to 90 (disappears)
        // then back rotates from 90 to 0 (appears)
        container.setOnMouseEntered(e -> {
            // First half — rotate front to 90 degrees
            javafx.animation.RotateTransition hidefront =
                    new javafx.animation.RotateTransition(
                            javafx.util.Duration.millis(200), front
                    );
            hidefront.setAxis(javafx.geometry.Point3D.ZERO.add(0, 1, 0));
            hidefront.setFromAngle(0);
            hidefront.setToAngle(90);

            // Second half — rotate back from 90 to 0
            javafx.animation.RotateTransition showBack =
                    new javafx.animation.RotateTransition(
                            javafx.util.Duration.millis(200), back
                    );
            showBack.setAxis(javafx.geometry.Point3D.ZERO.add(0, 1, 0));
            showBack.setFromAngle(90);
            showBack.setToAngle(0);

            // Chain them: when front finishes hiding, show back
            hidefront.setOnFinished(evt -> {
                front.setVisible(false);
                back.setVisible(true);
                showBack.play();
            });

            hidefront.play();
        });

        container.setOnMouseExited(e -> {
            // Reverse — hide back, show front
            javafx.animation.RotateTransition hideBack =
                    new javafx.animation.RotateTransition(
                            javafx.util.Duration.millis(200), back
                    );
            hideBack.setAxis(javafx.geometry.Point3D.ZERO.add(0, 1, 0));
            hideBack.setFromAngle(0);
            hideBack.setToAngle(90);

            javafx.animation.RotateTransition showFront =
                    new javafx.animation.RotateTransition(
                            javafx.util.Duration.millis(200), front
                    );
            showFront.setAxis(javafx.geometry.Point3D.ZERO.add(0, 1, 0));
            showFront.setFromAngle(90);
            showFront.setToAngle(0);

            hideBack.setOnFinished(evt -> {
                back.setVisible(false);
                front.setVisible(true);
                showFront.play();
            });

            hideBack.play();
        });

        return container;
    }
}