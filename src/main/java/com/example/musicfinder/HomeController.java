package com.example.musicfinder;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

import java.net.URL;
import java.util.*;

public class HomeController implements Initializable {

    // Step 1 fields
    @FXML private TextField seedField;
    @FXML private Label seedErrorLabel;
    @FXML private VBox step1Panel;

    // Step 2 fields
    @FXML private TextField modifierField;
    @FXML private ToggleButton languageToggle;
    @FXML private ComboBox<Integer> songCountBox;
    @FXML private Label statusLabel;
    @FXML private VBox step2Panel;
    @FXML private Label confirmedSeedLabel;

    // Step indicators
    @FXML private Circle step1Dot;
    @FXML private Circle step2Dot;
    @FXML private Label step1Label;
    @FXML private Label step2Label;

    // Stores the confirmed seed text between steps
    private String confirmedSeedText = "";

    // Data — static so it survives screen transitions
    static List<Song> dedupedList;
    static SongGraph graph;
    static boolean dataLoaded = false;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        songCountBox.setItems(FXCollections.observableArrayList(5, 10, 15, 20, 25));
        songCountBox.setValue(10);

        if (!dataLoaded) {
            statusLabel.setText("Loading music data...");
            new Thread(() -> {
                try {
                    // loadAllSongs now handles deduplication internally
                    List<Song> songList = DataReader.loadAllSongs();

                    Map<String, Song> songMap = new HashMap<>();
                    for (Song song : songList) {
                        songMap.put(song.getTrackId(), song);
                    }

                    dedupedList = new ArrayList<>(songMap.values());
                    graph = new SongGraph(songMap);
                    javafx.application.Platform.runLater(() -> {
                        dataLoaded = true;
                        statusLabel.setText("Ready — " + dedupedList.size()
                                + " songs loaded.");
                        statusLabel.setStyle("-fx-text-fill: #00d4aa;");
                    });
                } catch (Exception e) {
                    javafx.application.Platform.runLater(() ->
                            statusLabel.setText("Error loading data: " + e.getMessage())
                    );
                }
            }).start();
        } else {
            // Data already loaded from a previous session on this run
            statusLabel.setText("Ready — " + dedupedList.size()
                    + " songs loaded.");
            statusLabel.setStyle("-fx-text-fill: #00d4aa;");
        }
    }
    private static boolean isRealSpotifyId(String id) {
        return id != null
                && !id.isBlank()
                && !id.startsWith("v2_")
                && !id.startsWith("v3_")
                && !id.startsWith("v4_");
    }

    // -----------------------------------------------------------------------
    // STEP 1 — Seed song handling
    // -----------------------------------------------------------------------

    /**
     * Called when user hits Next on step 1.
     * Validates the seed field isn't empty then transitions to step 2.
     */
    @FXML
    private void handleSeedNext() {
        String seedText = seedField.getText().trim();

        if (seedText.isEmpty()) {
            seedErrorLabel.setText(
                    "⚠ A seed song is required. Enter a song name to continue."
            );
            seedField.requestFocus();
            return;
        }

        // Clear any previous error
        seedErrorLabel.setText("");
        confirmedSeedText = seedText;

        // Update the confirmed chip on step 2
        confirmedSeedLabel.setText(seedText);

        // Animate transition to step 2
        transitionToStep2();
    }

    /** Pre-fills seed field with Clocks example */
    @FXML private void handleChipClocks() {
        seedField.setText("Clocks by Coldplay");
        seedField.requestFocus();
    }

    @FXML private void handleChipWeeknd() {
        seedField.setText("Blinding Lights by The Weeknd");
        seedField.requestFocus();
    }

    /** Pre-fills seed field with Queen example */
    @FXML private void handleChipQueen() {
        seedField.setText("Bohemian Rhapsody by Queen");
        seedField.requestFocus();
    }

    // -----------------------------------------------------------------------
    // STEP 2 — Modifier and search handling
    // -----------------------------------------------------------------------

    /**
     * Clears the confirmed seed and goes back to step 1.
     */
    @FXML
    private void handleClearSeed() {
        confirmedSeedText = "";
        transitionToStep1();
    }

    /**
     * Triggered when user hits Search on step 2.
     * Combines seed + modifier into one query and runs the full pipeline.
     */
    @FXML
    private void handleSearch() {
        if (!dataLoaded) {
            statusLabel.setText("Still loading data, please wait...");
            return;
        }

        if (confirmedSeedText.isEmpty()) {
            transitionToStep1();
            return;
        }

        // Combine seed and modifier into one natural language query
        String modifier = modifierField.getText().trim();
        String fullQuery = modifier.isEmpty()
                ? "songs like " + confirmedSeedText
                : "songs like " + confirmedSeedText + " but " + modifier;

        boolean matchLanguage = languageToggle.isSelected();
        int songCount = songCountBox.getValue();


        statusLabel.setStyle("-fx-text-fill: #5a8a9f;");
        statusLabel.setText("Interpreting your query...");

        new Thread(() -> {
            try {
                QueryResult result = QueryUnderstanding.interpretQuery(fullQuery);

                javafx.application.Platform.runLater(() -> {
                    System.out.println("LLM returned: " + result);
                });

                // Find seed song
                Song seed = findBestSeedMatch(
                        result.getSeedSong(), result.getSeedArtist()
                );

                final Song finalSeed = seed;
                final QueryResult finalResult = result;

                javafx.application.Platform.runLater(() -> {
                    if (finalSeed == null) {
                        showSeedNotFoundError(
                                result.getSeedSong() != null ? result.getSeedSong() : confirmedSeedText,
                                result.getSeedArtist()
                        );
                        return;
                    }

                    statusLabel.setText("Getting recommendations...");

                    int fetchCount = songCount * 3;
                    graph.buildForGenre(finalSeed.getGenre(), dedupedList, 0.90);
                    // If different language requested, also build likely target genre graphs
                    if (finalResult.isDifferentLanguage()) {
                        // Build graphs for common non-English genres so we have
                        // candidates to draw from when filtering by language
                        String[] crossGenres = {
                                "latin", "reggaeton", "salsa", "j-pop", "k-pop",
                                "cantopop", "mandopop", "mpb", "sertanejo", "anime",
                                "french", "german", "spanish", "flamenco", "russian"
                        };
                        for (String g : crossGenres) {
                            graph.buildForGenre(g, dedupedList, 0.90);
                        }
                    }

                    List<Song> candidates;

                    if (finalResult.isDifferentLanguage()) {
                        // For language queries, BFS won't help since edges
                        // don't cross genres — search directly by language instead
                        candidates = new ArrayList<>();
                        String seedLang = finalSeed.inferredLanguage();
                        for (Song s : dedupedList) {
                            if (!s.inferredLanguage().equals(seedLang)) {
                                candidates.add(s);
                            }
                        }
                        System.out.println("Language candidates pool: " + candidates.size());
                    } else {
                        // Normal case — use BFS from seed song
                        candidates = graph.bfsTraversal(finalSeed.getTrackId(), 2, 500);
                    }

                    // Deduplicate candidates by track ID before ranking
// BFS can reach the same song through multiple paths
                    List<Song> uniqueCandidates = new ArrayList<>();
                    Set<String> seenCandidateIds = new HashSet<>();
                    for (Song s : candidates) {
                        if (seenCandidateIds.add(s.getTrackId())) {
                            uniqueCandidates.add(s);
                        }
                    }

                    // Use quality-gated finder instead of fixed top-N
                    // Remove weird/non-musical recommendation contexts
                    uniqueCandidates.removeIf(this::isBadContextForGeneralMusic);
                    List<Song> recommendations = SimilarityFinder.findSimilarQualityGated(
                            finalSeed,
                            uniqueCandidates,
                            finalResult.getWeights(),
                            finalResult.getVibeTargets(),
                            finalResult.getMinimumQuality(),
                            songCount  // max results cap — user's requested count
                    );

// If quality gate returned nothing, lower threshold and retry
                    /*if (recommendations.isEmpty()) {
                        System.out.println("Quality gate too strict — retrying with lower threshold");
                        recommendations = SimilarityFinder.findSimilarQualityGated(
                                finalSeed,
                                uniqueCandidates,
                                finalResult.getWeights(),
                                finalResult.getVibeTargets(),
                                finalResult.getMinimumQuality() * 0.85, // 15% looser
                                songCount
                        );
                    }

// If still empty, fall back to standard finder
                    if (recommendations.isEmpty()) {
                        System.out.println("Falling back to standard similarity finder");
                        recommendations = SimilarityFinder.findSimilar(
                                finalSeed, uniqueCandidates, songCount, finalResult.getWeights()
                        );
                    }*/
                    if (recommendations.isEmpty()) {
                        showNoResultsError(finalSeed, fullQuery);
                        return;
                    }

                    // Same artist filter
                    if (finalResult.isSameArtistOnly()) {
                        String artistLower = finalSeed.getArtists().toLowerCase();
                        List<Song> filtered = new ArrayList<>();
                        for (Song s : recommendations) {
                            if (s.getArtists().toLowerCase().contains(artistLower)
                                    || artistLower.contains(
                                    s.getArtists().toLowerCase())) {
                                filtered.add(s);
                                if (filtered.size() >= songCount) break;
                            }
                        }
                        if (filtered.size() >= 2) recommendations = filtered;
                    }

                    // Different language filter
                    // Language filtering — toggle matches seed language, LLM flag finds different language
                            boolean languageFilterActive = matchLanguage || finalResult.isDifferentLanguage();

                            if (languageFilterActive) {
                                // Use LLM-provided seed language instead of inferredLanguage()
                                // The LLM actually knows what language the seed song is in
                                String seedLang   = finalResult.getSeedLanguage();
                                String targetLang = finalResult.getTargetLanguage();

                                List<Song> filtered = new ArrayList<>();
                                for (Song s : recommendations) {
                                    boolean passes;

                                    if (matchLanguage && !finalResult.isDifferentLanguage()) {
                                        // Toggle on — keep only songs matching the seed's language
                                        // Still uses inferredLanguage() for candidates since LLM
                                        // only tells us the seed's language, not every candidate's
                                        passes = s.inferredLanguage().equals(seedLang);
                                    } else if (finalResult.isDifferentLanguage() && targetLang != null) {
                                        // LLM detected specific target language (e.g. "in Spanish")
                                        passes = s.inferredLanguage().equals(targetLang);
                                    } else {
                                        // Different language but no specific target
                                        passes = !s.inferredLanguage().equals(seedLang);
                                    }

                                    if (passes) {
                                        filtered.add(s);
                                        if (filtered.size() >= songCount) break;
                                    }
                                }

                                if (filtered.size() >= 2) {
                                    recommendations = filtered;
                                    System.out.println("Language filter applied: "
                                            + filtered.size() + " results (seedLang=" + seedLang
                                            + ", matchLanguage=" + matchLanguage
                                            + ", targetLang=" + targetLang + ")");
                                } else {
                                    System.out.println("Language filter too aggressive ("
                                            + filtered.size() + " results) — showing unfiltered");
                                }
                            }


                    // Trim to requested count
                    if (recommendations.size() > songCount) {
                        recommendations = recommendations.subList(0, songCount);
                    }

                    // Switch to results screen
                    try {
                        FXMLLoader loader = new FXMLLoader(
                                HelloApplication.class.getResource("ResultsScreen.fxml")
                        );
                        Scene scene = new Scene(loader.load(), 1000, 700);
                        scene.getStylesheets().add(
                                HelloApplication.class.getResource("styles.css")
                                        .toExternalForm()
                        );
                        ResultsController controller = loader.getController();
                        controller.loadResults(recommendations, finalSeed, fullQuery);
                        HelloApplication.primaryStage.setScene(scene);
                    } catch (Exception e) {
                        statusLabel.setText("Error loading results.");
                        e.printStackTrace();
                    }
                });

            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    statusLabel.setText("Search failed: " + e.getMessage());
                    e.printStackTrace();
                });
            }
        }).start();
    }

    // -----------------------------------------------------------------------
    // STEP TRANSITION ANIMATIONS
    // -----------------------------------------------------------------------

    /**
     * Animates from step 1 to step 2.
     * Fades out step 1 panel, updates indicators, fades in step 2 panel.
     */
    private void transitionToStep2() {
        javafx.animation.FadeTransition fadeOut =
                new javafx.animation.FadeTransition(
                        javafx.util.Duration.millis(200), step1Panel
                );
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(e -> {
            step1Panel.setVisible(false);
            step1Panel.setManaged(false);

            step2Panel.setVisible(true);
            step2Panel.setManaged(true);
            step2Panel.setOpacity(0);

            // Update step indicators
            step1Dot.getStyleClass().remove("step-dot-active");
            step1Dot.getStyleClass().add("step-dot-done");
            step2Dot.getStyleClass().remove("step-dot-inactive");
            step2Dot.getStyleClass().add("step-dot-active");
            step1Label.getStyleClass().remove("step-label-active");
            step1Label.getStyleClass().add("step-label-done");
            step2Label.getStyleClass().remove("step-label-inactive");
            step2Label.getStyleClass().add("step-label-active");

            javafx.animation.FadeTransition fadeIn =
                    new javafx.animation.FadeTransition(
                            javafx.util.Duration.millis(250), step2Panel
                    );
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.setOnFinished(ev -> modifierField.requestFocus());
            fadeIn.play();
        });
        fadeOut.play();
    }

    /**
     * Animates back from step 2 to step 1.
     */
    private void transitionToStep1() {
        javafx.animation.FadeTransition fadeOut =
                new javafx.animation.FadeTransition(
                        javafx.util.Duration.millis(200), step2Panel
                );
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(e -> {
            step2Panel.setVisible(false);
            step2Panel.setManaged(false);

            step1Panel.setVisible(true);
            step1Panel.setManaged(true);
            step1Panel.setOpacity(0);

            // Reset step indicators
            step1Dot.getStyleClass().remove("step-dot-done");
            step1Dot.getStyleClass().add("step-dot-active");
            step2Dot.getStyleClass().remove("step-dot-active");
            step2Dot.getStyleClass().add("step-dot-inactive");
            step1Label.getStyleClass().remove("step-label-done");
            step1Label.getStyleClass().add("step-label-active");
            step2Label.getStyleClass().remove("step-label-active");
            step2Label.getStyleClass().add("step-label-inactive");

            javafx.animation.FadeTransition fadeIn =
                    new javafx.animation.FadeTransition(
                            javafx.util.Duration.millis(250), step1Panel
                    );
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.setOnFinished(ev -> seedField.requestFocus());
            fadeIn.play();
        });
        fadeOut.play();
    }

    // -----------------------------------------------------------------------
    // SEED FINDING (same as before)
    // -----------------------------------------------------------------------

    Song findBestSeedMatch(String seedName, String seedArtist) {
        if (seedName == null) return null;
        Song bestMatch = null;
        double bestScore = -1;
        String seedNameLower   = seedName.toLowerCase().trim();
        String seedArtistLower = seedArtist != null
                ? seedArtist.toLowerCase().trim() : null;

        for (Song s : dedupedList) {
            String songNameLower   = s.getTrackName().toLowerCase().trim();
            String songArtistLower = s.getArtists().toLowerCase().trim();
            double score = 0;

            if (songNameLower.equals(seedNameLower))              score += 10;
            else if (songNameLower.contains(seedNameLower)
                    || seedNameLower.contains(songNameLower))       score += 5;
            else continue;

            if (seedArtistLower != null) {
                if (songArtistLower.equals(seedArtistLower))          score += 10;
                else if (songArtistLower.contains(seedArtistLower)
                        || seedArtistLower.contains(songArtistLower))   score += 6;
            }
            score += s.getPopularity() * 0.01;

            if (score > bestScore) { bestScore = score; bestMatch = s; }
        }
        System.out.println("Best seed score: " + bestScore
                + " → " + (bestMatch != null ? bestMatch.getTrackName() : "null"));

        if (seedArtistLower != null && bestScore < 16) return null;
        if (seedArtistLower == null && bestScore < 10) return null;
        return bestMatch;
    }

    // -----------------------------------------------------------------------
    // ERROR SCREEN (same as before — kept here for completeness)
    // -----------------------------------------------------------------------

    void showSeedNotFoundError(String songName, String artistName) {
        javafx.scene.layout.VBox errorScreen = new javafx.scene.layout.VBox(24);
        errorScreen.setAlignment(javafx.geometry.Pos.CENTER);
        errorScreen.setStyle("-fx-background-color: #0a1628;");
        errorScreen.setPadding(new javafx.geometry.Insets(60));

        Label icon = new Label("🔍");
        icon.setStyle("-fx-font-size: 64px;");

        Label title = new Label("Song Not Found");
        title.setStyle("-fx-font-family: 'Georgia'; -fx-font-size: 28px;" +
                "-fx-font-weight: bold; -fx-text-fill: #e0f0ff;");

        String searchedFor = songName != null && !songName.isBlank()
                ? "\"" + songName + "\"" + (artistName != null ? " by " + artistName : "")
                : "\"" + confirmedSeedText + "\"";
        Label searched = new Label("We looked for " + searchedFor);
        searched.setStyle("-fx-font-family: 'Georgia'; -fx-font-size: 14px;" +
                "-fx-text-fill: #ff6b6b; -fx-font-style: italic;");

        Button tryAgain = new Button("← Try Again");
        tryAgain.setStyle("-fx-background-color: #00d4aa; -fx-background-radius: 26;" +
                "-fx-text-fill: #0a1628; -fx-font-family: 'Georgia';" +
                "-fx-font-size: 14px; -fx-font-weight: bold;" +
                "-fx-cursor: hand; -fx-padding: 12 32 12 32;");
        tryAgain.setOnAction(e -> goHomeWithQuery(""));

        errorScreen.getChildren().addAll(icon, title, searched, tryAgain);
        Scene scene = new Scene(errorScreen, 1000, 700);
        scene.getStylesheets().add(
                HelloApplication.class.getResource("styles.css").toExternalForm()
        );
        HelloApplication.primaryStage.setScene(scene);
    }

    private void goHomeWithQuery(String query) {
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
     * Shows a friendly screen when no songs pass the quality threshold.
     * Tells the user their criteria were too specific and suggests
     * either broadening the query or trying a different seed song.
     */
    private void showNoResultsError(Song seed, String query) {
        javafx.scene.layout.VBox errorScreen = new javafx.scene.layout.VBox(24);
        errorScreen.setAlignment(javafx.geometry.Pos.CENTER);
        errorScreen.setStyle("-fx-background-color: #0a1628;");
        errorScreen.setPadding(new javafx.geometry.Insets(60));

        Label icon = new Label("🎵");
        icon.setStyle("-fx-font-size: 64px;");

        Label title = new Label("No Songs Met the Criteria");
        title.setStyle(
                "-fx-font-family: 'Georgia'; -fx-font-size: 26px;" +
                        "-fx-font-weight: bold; -fx-text-fill: #e0f0ff;"
        );

        Label detail = new Label(
                "Your query was too specific for the songs in our dataset.\n" +
                        "No recommendations scored high enough to meet the quality bar."
        );
        detail.setStyle(
                "-fx-font-family: 'Georgia'; -fx-font-size: 13px;" +
                        "-fx-text-fill: #5a8a9f; -fx-font-style: italic;"
        );
        detail.setWrapText(true);
        detail.setMaxWidth(500);
        detail.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        Label suggestions = new Label(
                "Try:\n" +
                        "• Removing specific style descriptors (e.g. drop \"EDM\" or \"groovy\")\n" +
                        "• Using a different seed song in a more common genre\n" +
                        "• Simplifying to just \"songs like " + seed.getTrackName() + "\""
        );
        suggestions.setStyle(
                "-fx-font-family: 'Georgia'; -fx-font-size: 12px;" +
                        "-fx-text-fill: #5a8a9f;"
        );
        suggestions.setWrapText(true);
        suggestions.setMaxWidth(480);

        Button tryAgain = new Button("← Try Again");
        tryAgain.setStyle(
                "-fx-background-color: #00d4aa; -fx-background-radius: 26;" +
                        "-fx-text-fill: #0a1628; -fx-font-family: 'Georgia';" +
                        "-fx-font-size: 14px; -fx-font-weight: bold;" +
                        "-fx-cursor: hand; -fx-padding: 12 32 12 32;"
        );
        tryAgain.setOnAction(e -> goHomeWithQuery(""));

        errorScreen.getChildren().addAll(icon, title, detail, suggestions, tryAgain);

        Scene scene = new Scene(errorScreen, 1000, 700);
        scene.getStylesheets().add(
                HelloApplication.class.getResource("styles.css").toExternalForm()
        );
        HelloApplication.primaryStage.setScene(scene);
    }
    /**
     * Aggressively normalizes a string for deduplication comparison.
     * Removes featured artist tags, punctuation, extra spaces,
     * and common suffixes that vary across datasets.
     */
    private boolean isBadContextForGeneralMusic(Song s) {
        String text = (s.getTrackName() + " "
                + s.getArtists() + " "
                + s.getGenre()).toLowerCase();

        return text.contains("my little pony")
                || text.contains("disney")
                || text.contains("kids")
                || text.contains("children")
                || text.contains("cartoon")
                || text.contains("soundtrack")
                || text.contains("musical")
                || text.contains("karaoke")
                || text.contains("cover")
                || text.contains("tribute")
                || text.contains("comedy");
    }
}