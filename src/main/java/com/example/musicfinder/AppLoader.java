package com.example.musicfinder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AppLoader {
    public static void main(String[] args) {

        // Load all songs from CSV into a list
        List<Song> songList = DataReader.loadSongs("data/dataset.csv");
        //System.out.println("Total songs loaded: " + songList.size());

        // Build a HashMap for O(1) lookup by track ID
        Map<String, Song> songMap = new HashMap<>();
        for (Song song : songList) {
            songMap.put(song.getTrackId(), song);
        }

        // Deduplicate using HashMap values
        List<Song> dedupedList = new ArrayList<>(songMap.values());
       // System.out.println("After deduplication: " + dedupedList.size() + " songs");

        // Find seed song (placeholder before natural language)
        Song seed = null;
        for (Song s : dedupedList) {
            if (s.getTrackName().equalsIgnoreCase("The Scientist")
                    && s.getArtists().toLowerCase().contains("coldplay")) {
                seed = s;
                break;
            }
        }
        // Build the graph
        SongGraph graph = new SongGraph(songMap);
        graph.buildGraph(dedupedList, 0.90);
        List<Song> candidates = graph.bfsTraversal(seed.getTrackId(), 2, 500);
        List<Song> graphResults = SimilarityFinder.findSimilar(seed, candidates, 10);
        System.out.println("Top 10 recommendations:");
        graphResults.forEach(System.out::println);

// *///Test BFS from The Scientist
//        System.out.println("\nBFS neighbors of The Scientist:");
//        System.out.println("Direct neighbors: " + graph.getDegree(seed.getTrackId()));
//
//        //List<Song> candidates = graph.bfsTraversal(seed.getTrackId(), 2, 500);
//        System.out.println("BFS candidate pool size: " + candidates.size());
//
//// Rank the BFS candidate pool using SimilarityFinder
//        //List<Song> graphResults = SimilarityFinder.findSimilar(seed, candidates, 10);
//
//        System.out.println("\nTop 10 from graph-based search:");
//        graphResults.forEach(System.out::println);
//        // Run similarity finder on deduped list
//        if (seed != null) {
//            System.out.println("Seed song: " + seed);
//            List<Song> similar = SimilarityFinder.findSimilar(seed, dedupedList, 10);
//            System.out.println("\nTop 10 similar songs:");
//            similar.forEach(System.out::println);
//        } else {
//            System.out.println("Seed song not found in dataset");
//        }
    }
}