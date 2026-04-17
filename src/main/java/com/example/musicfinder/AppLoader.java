package com.example.musicfinder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AppLoader {
    public static void main(String[] args) {

        // Load all songs from CSV into a list
        List<Song> songList = DataReader.loadSongs("data/dataset.csv");

        // Build a HashMap for O(1) lookup by track ID
        Map<String, Song> songMap = new HashMap<>();
        for (Song song : songList) {
            songMap.put(song.getTrackId(), song);
        }

        // Deduplicate using HashMap values
        List<Song> dedupedList = new ArrayList<>(songMap.values());

        // Build the graph
        SongGraph graph = new SongGraph(songMap);
        graph.buildGraph(dedupedList, 0.90);

        // Test queries
        String[] testQueries = {
                "songs like The Scientist by Coldplay",
                "songs like The Scientist by Coldplay but more energetic",
                "songs like The Scientist by Coldplay but less mainstream",
                "songs like The Scientist by Coldplay but more dreamy and acoustic"
        };

        for (String query : testQueries) {
            System.out.println("\n========================================");
            System.out.println("Query: " + query);

            // Send query to LLM — returns weights AND seed song info
            QueryResult result = QueryUnderstanding.interpretQuery(query);
            System.out.println("LLM interpreted: " + result);

            // Find seed song using what the LLM extracted
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

            if (seed == null) {
                System.out.println("Seed song not found: " + result.getSeedSong());
                continue;
            }

            System.out.println("Seed song found: " + seed);

            // Get recommendations using LLM-generated weights
            List<Song> candidates = graph.bfsTraversal(seed.getTrackId(), 2, 500);
            List<Song> recommendations = SimilarityFinder.findSimilar(
                    seed, candidates, 10, result.getWeights()
            );

            System.out.println("Top 10 recommendations:");
            recommendations.forEach(System.out::println);
        }
    }
}