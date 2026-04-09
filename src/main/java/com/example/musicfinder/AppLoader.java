package com.example.musicfinder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AppLoader
{
    public static void main(String[] args) {
        // Load all songs from CSV into a list
        List<Song> songList = DataReader.loadSongs("data/dataset.csv");

        // Build a HashMap for O(1) lookup by track ID
        Map<String, Song> songMap = new HashMap<>();
        for (Song song : songList) {
            songMap.put(song.getTrackId(), song);
        }

// Check total count — should be close to 114,000
        System.out.println("Total songs loaded: " + songList.size());

// Search for Clocks by Coldplay by name
        Song seed = null;
        for (Song s : songList) {
            if (s.getTrackName().equalsIgnoreCase("The Scientist")&& s.getArtists().equalsIgnoreCase("Coldplay")) {
                seed = s;
                break;
            }
        }

// If found, run the similarity finder
        if (seed != null) {
            System.out.println("Seed song: " + seed);
            List<Song> similar = SimilarityFinder.findSimilar(seed, songList, 10);
            System.out.println("\nTop 10 similar songs:");
            similar.forEach(System.out::println);
        } else {
            System.out.println("Clocks not found in dataset");
        }
    }
}
