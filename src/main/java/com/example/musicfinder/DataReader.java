package com.example.musicfinder;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DataReader {

    /**
     * Reads the Kaggle Spotify dataset CSV and returns a list of com.example.musicfinder.Song objects.
     * @param filePath - the path to dataset.csv relative to the project root
     * @return a List of fully initialized com.example.musicfinder.Song objects
     */
    public static List<Song> loadSongs(String filePath) {

        // This will hold all the com.example.musicfinder.Song objects we build from the CSV rows
        List<Song> songs = new ArrayList<>();

        // Try-with-resources ensures the file is automatically closed after reading,
        // even if an exception occurs
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {

            // Skip the header row (the first row contains column names, not data)
            reader.readNext();

            String[] row;

            // readNext() returns the next row as a String array, or null when the file ends
            while ((row = reader.readNext()) != null) {

                // Skip any rows that are malformed or don't have enough columns
                // The dataset has 20 columns (indices 0-19), so anything shorter is bad data
                if (row.length < 20) continue;

                try {
                    // --- Parse each column by its index in the CSV ---
                    // Open dataset.csv in a spreadsheet to verify these indices match your file

                    String trackId            = row[0];   // Unique Spotify track ID
                    String artists            = row[1];   // Artist name(s)
                    String albumName          = row[2];   // Album name
                    String trackName          = row[3];   // com.example.musicfinder.Song title
                    int popularity            = Integer.parseInt(row[4].trim());    // 0-100
                    boolean explicit          = row[5].trim().equals("1") ||
                            row[5].trim().equalsIgnoreCase("true"); // true/false
                    int durationMs            = Integer.parseInt(row[6].trim());    // in milliseconds
                    int key                   = Integer.parseInt(row[7].trim());    // musical key (0-11)
                    double loudness           = Double.parseDouble(row[8].trim());  // in decibels
                    int timeSignature         = Integer.parseInt(row[9].trim());    // beats per measure
                    double acousticness       = Double.parseDouble(row[10].trim()); // 0.0 - 1.0
                    double danceability       = Double.parseDouble(row[11].trim()); // 0.0 - 1.0
                    double energy             = Double.parseDouble(row[12].trim()); // 0.0 - 1.0
                    double instrumentalness   = Double.parseDouble(row[13].trim()); // 0.0 - 1.0
                    double liveness           = Double.parseDouble(row[14].trim()); // 0.0 - 1.0
                    double speechiness        = Double.parseDouble(row[15].trim()); // 0.0 - 1.0
                    double tempo              = Double.parseDouble(row[16].trim()); // in BPM
                    double valence            = Double.parseDouble(row[17].trim()); // 0.0 - 1.0
                    String genre              = row[18];  // music genre string

                    // Construct a fully initialized com.example.musicfinder.Song object with all parsed fields
                    Song song = new Song(
                            trackId, trackName, artists, albumName, genre,
                            popularity, energy, valence, danceability,
                            acousticness, instrumentalness, liveness,
                            speechiness, loudness, tempo,
                            durationMs, explicit, key, timeSignature
                    );

                    // Add the completed com.example.musicfinder.Song to our list
                    songs.add(song);

                } catch (NumberFormatException e) {
                    // If a numeric field can't be parsed (e.g. empty or corrupted cell),
                    // skip that row and continue — we don't want one bad row to crash the whole load
                    System.out.println("Skipping malformed row: " + e.getMessage());
                }
            }

        } catch (IOException e) {
            // Thrown if the file path is wrong or the file can't be opened
            System.out.println("Could not find or open file: " + filePath);
            e.printStackTrace();
        } catch (CsvValidationException e) {
            // Thrown if OpenCSV encounters a structurally invalid CSV format
            System.out.println("CSV formatting error: " + e.getMessage());
            e.printStackTrace();
        }

        // Return however many songs successfully loaded, even if some rows were skipped
        return songs;
    }
}