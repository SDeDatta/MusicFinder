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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.concurrent.Task;

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

// Album art — tries to load real Spotify image, falls back to initial
        // Generate audio fingerprint visual instead of plain initial
        javafx.scene.layout.Pane artPane = buildAudioFingerprint(song, accentColor);

// Only attempt real image for songs with genuine Spotify track IDs
        if (!song.getTrackId().startsWith("v2_")
                && !song.getTrackId().startsWith("v3_")) {

            // Load image on background thread so cards appear instantly
            // and images fill in as they load
            javafx.concurrent.Task<Image> imageTask = new javafx.concurrent.Task<>() {
                @Override
                protected Image call() {
                    return ImageLoader.fetchAlbumArt(song.getTrackId());
                }
            };

            imageTask.setOnSucceeded(e -> {
                Image img = imageTask.getValue();
                if (img != null && !img.isError()) {
                    // Image loaded successfully — replace initial with real art
                    javafx.scene.image.ImageView imageView =
                            new javafx.scene.image.ImageView(img);
                    imageView.setFitWidth(248);
                    imageView.setFitHeight(248);
                    imageView.setPreserveRatio(false);

                    // Clip to rounded corners to match card style
                    javafx.scene.shape.Rectangle clip =
                            new javafx.scene.shape.Rectangle(248, 248);
                    clip.setArcWidth(8);
                    clip.setArcHeight(8);
                    imageView.setClip(clip);
                }
                // If image failed, initial letter stays visible — no action needed
            });

            imageTask.setOnFailed(e ->
                    System.out.println("Image task failed for: " + song.getTrackName())
            );

            // Run on a background thread
            Thread imageThread = new Thread(imageTask);
            imageThread.setDaemon(true); // won't prevent app from closing
            imageThread.start();
        }
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
                artPane, title, artist, genre,
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
        final boolean[] showingBack = {false};

        container.setOnMouseClicked(e -> {
            if (!showingBack[0]) {
                // Flip front → back
                javafx.animation.RotateTransition hideFront =
                        new javafx.animation.RotateTransition(
                                javafx.util.Duration.millis(200), front
                        );
                hideFront.setAxis(javafx.geometry.Point3D.ZERO.add(0, 1, 0));
                hideFront.setFromAngle(0);
                hideFront.setToAngle(90);

                javafx.animation.RotateTransition showBack =
                        new javafx.animation.RotateTransition(
                                javafx.util.Duration.millis(200), back
                        );
                showBack.setAxis(javafx.geometry.Point3D.ZERO.add(0, 1, 0));
                showBack.setFromAngle(90);
                showBack.setToAngle(0);

                hideFront.setOnFinished(evt -> {
                    front.setVisible(false);
                    back.setVisible(true);
                    showBack.play();
                });

                hideFront.play();
                showingBack[0] = true;

            } else {
                // Flip back → front
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
                showingBack[0] = false;
            }
        });

        return container;
    }
    /**
     * Builds a unique visual fingerprint for a song using its audio features.
     * Each song gets a different pattern because the shapes are driven by
     * energy, valence, danceability, acousticness and tempo.
     * High energy = more bars. High valence = warmer shapes. etc.
     */
    private javafx.scene.layout.Pane buildAudioFingerprint(Song song, String accentColor) {
        javafx.scene.layout.Pane pane = new javafx.scene.layout.Pane();
        pane.setPrefSize(248, 180);
        pane.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #0a1628, #0d2040);" +
                        "-fx-background-radius: 8;"
        );

        double w = 248, h = 180;
        double energy       = song.getEnergy();
        double valence      = song.getValence();
        double danceability = song.getDanceability();
        double acousticness = song.getAcousticness();
        double tempo        = song.getTempo() / 200.0;

        // --- Layer 1: Background circles (driven by acousticness) ---
        // Acoustic songs get soft overlapping circles
        int circleCount = (int)(acousticness * 5) + 2;
        for (int i = 0; i < circleCount; i++) {
            double progress = (double) i / circleCount;
            javafx.scene.shape.Circle c = new javafx.scene.shape.Circle();
            c.setCenterX(w * 0.5 + Math.sin(progress * Math.PI * 2) * w * 0.25);
            c.setCenterY(h * 0.5 + Math.cos(progress * Math.PI * 2) * h * 0.25);
            c.setRadius(20 + acousticness * 30 + i * 8);
            c.setFill(javafx.scene.paint.Color.web(accentColor, 0.04 + acousticness * 0.04));
            c.setStroke(javafx.scene.paint.Color.web(accentColor, 0.08 + acousticness * 0.06));
            c.setStrokeWidth(1);
            pane.getChildren().add(c);
        }

        // --- Layer 2: Waveform bars (driven by energy and tempo) ---
        int barCount = (int)(tempo * 20) + 16;
        double barW  = (w - 32) / barCount;
        for (int i = 0; i < barCount; i++) {
            double phase    = (double) i / barCount;
            double wave1    = Math.sin(phase * Math.PI * 4) * 0.4;
            double wave2    = Math.sin(phase * Math.PI * 7 + 1.2) * 0.3;
            double wave3    = Math.sin(phase * Math.PI * 2 + 0.5) * 0.3;
            double combined = (wave1 + wave2 + wave3 + 1.0) / 2.0;
            double barH     = 8 + combined * energy * (h * 0.55);

            javafx.scene.shape.Rectangle bar = new javafx.scene.shape.Rectangle();
            bar.setX(16 + i * barW);
            bar.setY(h / 2.0 - barH / 2.0);
            bar.setWidth(Math.max(1, barW - 1.5));
            bar.setHeight(barH);
            bar.setArcWidth(2);
            bar.setArcHeight(2);

            // Alternate opacity for visual depth
            double opacity = (i % 2 == 0) ? 0.7 + energy * 0.3 : 0.3 + energy * 0.2;
            bar.setFill(javafx.scene.paint.Color.web(accentColor, opacity));
            pane.getChildren().add(bar);
        }

        // --- Layer 3: Valence dots (driven by valence and danceability) ---
        // Happy/danceable songs get more bright dots scattered around
        int dotCount = (int)(valence * 12) + (int)(danceability * 8);
        for (int i = 0; i < dotCount; i++) {
            double angle  = (2 * Math.PI / dotCount) * i + valence;
            double radius = 20 + danceability * 60 + Math.sin(angle * 3) * 20;
            double cx     = w / 2 + Math.cos(angle) * radius;
            double cy     = h / 2 + Math.sin(angle) * radius * 0.6;

            javafx.scene.shape.Circle dot = new javafx.scene.shape.Circle(
                    cx, cy, 1.5 + valence * 2
            );
            dot.setFill(javafx.scene.paint.Color.web(accentColor,
                    0.3 + valence * 0.5));
            pane.getChildren().add(dot);
        }

        // --- Layer 4: Song title overlaid at bottom ---
        Label nameLabel = new Label(song.getTrackName().toUpperCase());
        nameLabel.setStyle(
                "-fx-font-family: 'Georgia';" +
                        "-fx-font-size: 11px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: " + accentColor + ";" +
                        "-fx-background-color: #0a162888;" +
                        "-fx-padding: 4 8 4 8;" +
                        "-fx-background-radius: 4;"
        );
        nameLabel.setLayoutX(12);
        nameLabel.setLayoutY(h - 30);
        nameLabel.setMaxWidth(224);
        pane.getChildren().add(nameLabel);

        return pane;
    }
}