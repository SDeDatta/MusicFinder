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

    private static List<Song> dedupedList;
    private static SongGraph graph;
    private static boolean dataLoaded = false;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        songCountBox.setItems(FXCollections.observableArrayList(5, 10, 15, 20, 25));
        songCountBox.setValue(10);

        if (!dataLoaded) {
            statusLabel.setText("Step 1/2: Loading songs...");
            new Thread(() -> {
                try {
                    // Step 1 — load songs
                    List<Song> songList = DataReader.loadSongs("data/dataset.csv");

                    javafx.application.Platform.runLater(() ->
                            statusLabel.setText("Step 2/2: Building graph (this takes ~1 min)...")
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
                        statusLabel.setText("Ready — " + dedupedList.size() + " songs loaded.");
                        statusLabel.setStyle("-fx-text-fill: #00d4aa;");
                    });

                } catch (Exception e) {
                    javafx.application.Platform.runLater(() -> {
                        statusLabel.setText("Error loading data: " + e.getMessage());
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
            statusLabel.setText("Please enter a query.");
            return;
        }

        if (!dataLoaded) {
            statusLabel.setText("Still loading data, please wait...");
            return;
        }

        // Check dedupedList actually has songs
        if (dedupedList == null || dedupedList.isEmpty()) {
            statusLabel.setText("Data not loaded correctly. Check data/dataset.csv path.");
            return;
        }

        int songCount = songCountBox.getValue();

        statusLabel.setStyle("-fx-text-fill: #5a8a9f;");
        statusLabel.setText("Asking AI to interpret query...");
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
                Song seed = null;
                List<Song> titleMatches = new ArrayList<>();

                for (Song s : dedupedList) {
                    if (s.getTrackName().equalsIgnoreCase(result.getSeedSong())) {
                        titleMatches.add(s);
                    }
                }

                if (result.getSeedArtist() != null) {
                    for (Song s : titleMatches) {
                        if (s.getArtists().toLowerCase().contains(result.getSeedArtist().toLowerCase())) {
                            seed = s;
                            break;
                        }
                    }
                } else {
                    // fallback: prefer likely music tracks
                    for (Song s : titleMatches) {
                        String genre = s.getGenre().toLowerCase();
                        if (!genre.contains("comedy") &&
                                !genre.contains("podcast") &&
                                !genre.contains("spoken")) {
                            seed = s;
                            break;
                        }
                    }
                }

                // If exact match fails, try name only without artist
                if (seed == null && result.getSeedSong() != null) {
                    for (Song s : dedupedList) {
                        if (s.getTrackName().equalsIgnoreCase(result.getSeedSong())) {
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
                        statusLabel.setStyle("-fx-text-fill: #ff6b6b;");
                        statusLabel.setText("Song not found: \""
                                + finalResult.getSeedSong()
                                + "\". Try a different song name.");
                        return;
                    }

                    System.out.println("Found seed: " + finalSeed);
                    statusLabel.setText("Found song, getting recommendations...");

                    // Step 3 — get recommendations
                    List<Song> candidates = graph.bfsTraversal(
                            finalSeed.getTrackId(), 2, 500
                    );
                    List<Song> recommendations = SimilarityFinder.findSimilar(
                            finalSeed, candidates, songCount, finalResult.getWeights()
                    );

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
                        statusLabel.setText("Error loading results screen.");
                        e.printStackTrace();
                    }
                });

            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    searchField.setDisable(false);
                    statusLabel.setStyle("-fx-text-fill: #ff6b6b;");
                    statusLabel.setText("Search failed: " + e.getMessage());
                    e.printStackTrace();
                });
            }
        }).start();
    }
}