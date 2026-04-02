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

// Print the first 5 songs to verify fields are parsing correctly
        for (int i = 0; i < 5; i++) {
            System.out.println(songList.get(i));
        }

// Spot check a specific field to make sure values are sensible
// Energy should be between 0.0 and 1.0
        System.out.println("First song energy: " + songList.get(0).getEnergy());
        System.out.println("First song valence: " + songList.get(0).getValence());
    }
}
