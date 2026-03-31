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
        // Quick sanity check - should print a real song
        System.out.println(songMap.get("5vjLSffimiIP26QG5WcN2K"));
    }
}
