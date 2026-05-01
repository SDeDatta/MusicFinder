/*
package com.example.musicfinder;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class HomeController implements Initializable {

    @FXML private TextField searchField;
    @FXML private ToggleButton languageToggle;
    @FXML private ComboBox<Integer> songCountBox;
    @FXML private Label statusLabel;

    // These are loaded once and reused across searches
    private static List<Song> dedupedList;
    private static SongGraph graph;
    private static boolean dataLoaded = false;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Populate the song count dropdown with options 5 to 25
        songCountBox.setItems(FXCollections.observableArrayList(
                5, 10, 15, 20, 25
        ));
        songCountBox.setValue(10); // default to 10

        // Load data in background so UI doesn't freeze
        if (!dataLoaded) {
            statusLabel.setText("Loading music data...");
            new Thread(() -> {
                loadData();
                javafx.application.Platform.runLater(() -> {
                    statusLabel.setText("");
                    dataLoaded = true;
                });
            }).start();
        }
    }

    */
/**
     * Loads the dataset and builds the graph.
     * Runs on a background thread so the UI stays responsive.
     *//*

    private void loadData() {
        List<Song> songList = DataReader.loadSongs("data/dataset.csv");

        Map<String, Song> songMap = new HashMap<>();
        for (Song song : songList) {
            songMap.put(song.getTrackId(), song);
        }

        dedupedList = new ArrayList<>(songMap.values());
        graph = new SongGraph(songMap);
        graph.buildGraph(dedupedList, 0.90);
    }

    */
/**
     * Triggered when user hits Enter in search field or clicks the arrow button.
     * Sends query to LLM, finds seed song, gets recommendations, switches screen.
     *//*

    @FXML
    private void handleSearch() {
        String query = searchField.getText().trim();

        if (query.isEmpty()) {
            statusLabel.setText("Please enter a query.");
            return;
        }

        if (!dataLoaded) {
            statusLabel.setText("Still loading data, please wait...");
            return;
        }

        int songCount = songCountBox.getValue();
        boolean matchLanguage = languageToggle.isSelected();

        statusLabel.setText("Searching...");
        searchField.setDisable(true);

        // Run search on background thread to keep UI responsive
        new Thread(() -> {
            try {
                // Send query to LLM
                QueryResult result = QueryUnderstanding.interpretQuery(query);

                // Find seed song
                Song seed = null;
                if (result.getSeedSong() != null) {
                    for (Song s : dedupedList) {
                        boolean nameMatch = s.getTrackName()
                                .equalsIgnoreCase(result.getSeedSong());
                        boolean artistMatch = result.getSeedArtist() == null ||
                                s.getArtists().toLowerCase()
                                        .contains(result.getSeedArtist().toLowerCase());
                        if (nameMatch && artistMatch) {
                            seed = s;
                            break;
                        }
                    }
                }

                final Song finalSeed = seed;
                final QueryResult finalResult = result;

                javafx.application.Platform.runLater(() -> {
                    searchField.setDisable(false);

                    if (finalSeed == null) {
                        statusLabel.setText("Song not found: \""
                                + result.getSeedSong() + "\". Try another query.");
                        return;
                    }

                    // Get recommendations
                    List<Song> candidates = graph.bfsTraversal(
                            finalSeed.getTrackId(), 2, 500
                    );
                    List<Song> recommendations = SimilarityFinder.findSimilar(
                            finalSeed, candidates, songCount, finalResult.getWeights()
                    );

                    // Switch to results screen
                    try {
                        FXMLLoader loader = new FXMLLoader(
                                HelloApplication.class.getResource("ResultsScreen.fxml")
                        );
                        Scene scene = new Scene(loader.load(), 1000, 700);
                        scene.getStylesheets().add(
                                HelloApplication.class.getResource("styles.css").toExternalForm()
                        );
                        // Pass data to results controller
                        ResultsController controller = loader.getController();
                        controller.loadResults(recommendations, finalSeed, query);

                        HelloApplication.primaryStage.setScene(scene);

                    } catch (Exception e) {
                        statusLabel.setText("Error loading results screen.");
                        e.printStackTrace();
                    }
                });

            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    searchField.setDisable(false);
                    statusLabel.setText("Search failed: " + e.getMessage());
                });
            }
        }).start();
    }
}*/
package com.example.musicfinder;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;


import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class HomeController implements Initializable {

    @FXML private TextField searchField;
    @FXML private ToggleButton languageToggle;
    @FXML private ComboBox<Integer> songCountBox;
    @FXML private Label statusLabel;
    @FXML private VBox statusBox;

    private static List<Song> dedupedList;
    private static SongGraph graph;
    private static boolean dataLoaded = false;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        songCountBox.setItems(FXCollections.observableArrayList(5, 10, 15, 20, 25));
        songCountBox.setValue(10);

        if (!dataLoaded) {
            showStatus("Step 1/2: Loading songs...");
            new Thread(() -> {
                try {
                    // Step 1 — load songs
                    List<Song> songList = DataReader.loadAllSongs();

                    javafx.application.Platform.runLater(() ->
                           showStatus("Step 2/2: Building graph (this takes ~1 min)...")
                    );

                    // Step 2 — deduplicate
                    Map<String, Song> songMap = new HashMap<>();
                    for (Song song : songList) {
                        songMap.put(song.getTrackId(), song);
                    }
                    dedupedList = new ArrayList<>(songMap.values());

                    // Step 3 — build graph
                    graph = new SongGraph(songMap);
                    graph.buildGraph(dedupedList, 0.90);

                    // Done
                    javafx.application.Platform.runLater(() -> {
                        dataLoaded = true;
                        showStatus("Ready — " + dedupedList.size() + " songs loaded.");
                        statusLabel.setStyle("-fx-text-fill: #00d4aa;");
                    });

                } catch (Exception e) {
                    javafx.application.Platform.runLater(() -> {
                        showStatus("Error loading data: " + e.getMessage());
                        System.out.println("Data load error: " + e.getMessage());
                        e.printStackTrace();
                    });
                }
            }).start();
        }
    }

    @FXML
    private void handleSearch() {
        String query = searchField.getText().trim();

        if (query.isEmpty()) {
            showStatus("Please enter a query.");
            return;
        }

        if (!dataLoaded) {
            showStatus("Still loading data, please wait...");
            return;
        }

        // Check dedupedList actually has songs
        if (dedupedList == null || dedupedList.isEmpty()) {
            showStatus("Data not loaded correctly. Check data/dataset.csv path.");
            return;
        }

        int songCount = songCountBox.getValue();
        boolean matchLanguage = languageToggle.isSelected();

        statusLabel.setStyle("-fx-text-fill: #5a8a9f;");
        showStatus("Asking AI to interpret query...");
        searchField.setDisable(true);

        new Thread(() -> {
            try {
                // Step 1 — send to LLM
                QueryResult result = QueryUnderstanding.interpretQuery(query);

                javafx.application.Platform.runLater(() -> {
                    System.out.println("LLM returned: " + result);
                    System.out.println("Seed song from LLM: " + result.getSeedSong());
                    System.out.println("Seed artist from LLM: " + result.getSeedArtist());
                });

                // Step 2 — find seed song
                // NEW - This single line:
                Song seed = findBestSeedMatch(result.getSeedSong(), result.getSeedArtist());

                final Song finalSeed = seed;
                final QueryResult finalResult = result;

                javafx.application.Platform.runLater(() -> {
                    searchField.setDisable(false);

                    if (finalSeed == null) {
                        statusLabel.setStyle("-fx-text-fill: #ff6b6b;");
                        showStatus("Song not found: \""
                                + finalResult.getSeedSong()
                                + "\". Try a different song name.");
                        return;
                    }

                    System.out.println("Found seed: " + finalSeed);
                    showStatus("Found song, getting recommendations...");

                    // Step 3 — get recommendations
                    List<Song> candidates = graph.bfsTraversal(
                            finalSeed.getTrackId(), 2, 500
                    );
                    // Get more candidates than needed in case language filtering removes some
                    int fetchCount = matchLanguage ? songCount * 3 : songCount;
                    List<Song> recommendations = SimilarityFinder.findSimilar(
                            finalSeed, candidates, fetchCount, finalResult.getWeights()
                    );

// Apply language filter if toggle is on
                    if (matchLanguage) {
                        String queryLanguage = QueryUnderstanding.detectLanguage(query);
                        System.out.println("Query language detected: " + queryLanguage);

                        List<Song> filtered = new ArrayList<>();
                        for (Song s : recommendations) {
                            if (s.inferredLanguage().equals(queryLanguage)) {
                                filtered.add(s);
                                if (filtered.size() >= songCount) break;
                            }
                        }

                        // If filter was too aggressive and removed too many, fall back to unfiltered
                        recommendations = filtered.size() >= (songCount / 2) ? filtered : recommendations;
                        if (filtered.size() < songCount / 2) {
                            System.out.println("Language filter too aggressive, showing unfiltered results");
                        }
                    }

                    // Step 4 — switch to results screen
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
                        controller.loadResults(recommendations, finalSeed, query);
                        HelloApplication.primaryStage.setScene(scene);

                    } catch (Exception e) {
                        showStatus("Error loading results screen.");
                        e.printStackTrace();
                    }
                });

            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    searchField.setDisable(false);
                    statusLabel.setStyle("-fx-text-fill: #ff6b6b;");
                    showStatus("Search failed: " + e.getMessage());
                    e.printStackTrace();
                });
            }
        }).start();
    }
    /**
     * Finds the best matching seed song using a scoring system
     * rather than stopping at the first name match.
     * Scores each candidate on name match quality and artist match quality,
     * then returns the highest scoring song.
     */
    private Song findBestSeedMatch(String seedName, String seedArtist) {
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

            // --- Name matching ---
            if (songNameLower.equals(seedNameLower)) {
                score += 10; // exact name match is the strongest signal
            } else if (songNameLower.contains(seedNameLower)
                    || seedNameLower.contains(songNameLower)) {
                score += 5;  // partial name match
            } else {
                continue; // name doesn't match at all — skip entirely
            }

            // --- Artist matching ---
            if (seedArtistLower != null) {
                if (songArtistLower.equals(seedArtistLower)) {
                    score += 10; // exact artist match
                } else if (songArtistLower.contains(seedArtistLower)
                        || seedArtistLower.contains(songArtistLower)) {
                    score += 6;  // partial artist match e.g. "Eminem" in "Eminem;Dr Dre"
                } else {
                    // Artist name doesn't match at all
                    // Still keep it as a fallback but score it very low
                    score += 0;
                }
            }

            // --- Popularity tiebreaker ---
            // If two songs have the same name and artist score,
            // prefer the more popular one (likely the original version)
            score += s.getPopularity() * 0.01;

            if (score > bestScore) {
                bestScore = score;
                bestMatch = s;
            }
        }

        System.out.println("Best seed match score: " + bestScore
                + " → " + (bestMatch != null ? bestMatch : "null"));
        if (seedArtistLower != null && bestScore < 16) return null;
        if (seedArtistLower == null && bestScore < 10) return null;
        return bestMatch;

    }
    private void showStatus(String message) {
        statusLabel.setText(message);
        statusBox.setVisible(true);
        statusBox.setManaged(true);
    }
    private void clearStatus() {
        statusLabel.setText("");
        statusBox.setVisible(false);
        statusBox.setManaged(false);
    }
}