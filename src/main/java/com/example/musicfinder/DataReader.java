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
            reader.readNext(); // skip header row

            String[] row;

            // readNext() returns the next row as a String array, or null when the file ends
            while ((row = reader.readNext()) != null) {

                // Skip any rows that are malformed or don't have enough columns
                // The dataset has 20 columns (indices 0-19), so anything shorter is bad data
                if (row.length < 21) continue;

                try {
                    // --- Parse each column by its index in the CSV ---
                    // Open dataset.csv in a spreadsheet to verify these indices match your file

                    String trackId            = row[1];   // Unique Spotify track ID
                    String artists            = row[2];   // Artist name(s)
                    String albumName          = row[3];   // Album name
                    String trackName          = row[4];   // com.example.musicfinder.Song title
                    int popularity            = Integer.parseInt(row[5].trim());    // 0-100
                    int durationMs            = Integer.parseInt(row[6].trim());    // in milliseconds
                    boolean explicit          = row[7].trim().equals("1") ||
                            row[6].trim().equalsIgnoreCase("true"); // true/false
                    double danceability       = Double.parseDouble(row[8].trim()); // 0.0 - 1.0
                    double energy             = Double.parseDouble(row[9].trim()); // 0.0 - 1.0
                    int key                   = Integer.parseInt(row[10].trim());    // musical key (0-11)
                    double loudness           = Double.parseDouble(row[11].trim());  // in decibels
                    int mode                = Integer.parseInt(row[12].trim()); // major=1, minor=0
                    double speechiness        = Double.parseDouble(row[13].trim()); // 0.0 - 1.0
                    double acousticness       = Double.parseDouble(row[14].trim()); // 0.0 - 1.0
                    double instrumentalness   = Double.parseDouble(row[15].trim()); // 0.0 - 1.0
                    double liveness           = Double.parseDouble(row[16].trim()); // 0.0 - 1.0
                    double valence            = Double.parseDouble(row[17].trim()); // 0.0 - 1.0
                    double tempo              = Double.parseDouble(row[18].trim()); // in BPM
                    int timeSignature         = Integer.parseInt(row[19].trim());    // beats per measure
                    String genre              = row[20];  // music genre string

                    // Construct a fully initialized com.example.musicfinder.Song object with all parsed fields
                    Song song = new Song(
                            trackId, trackName, artists, albumName, genre,
                            popularity, energy, valence, danceability,
                            acousticness, instrumentalness, liveness,
                            speechiness, loudness, tempo,
                            durationMs, explicit, key, timeSignature, mode
                    );

                    // Add the completed com.example.musicfinder.Song to our list
                    songs.add(song);

                } catch (NumberFormatException e) {
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
    /**
     * Loads the second Spotify dataset which has a different column structure.
     * Columns: artist, song, duration_ms, year, popularity, danceability,
     *          energy, key, loudness, mode, speechiness, acousticness,
     *          instrumentalness, liveness, valence, tempo, genre
     *
     * Since this dataset has no track_id, we generate one from artist + song name.
     * Missing fields (albumName, explicit, timeSignature) get safe default values.
     */
    public static List<Song> loadSongsV2(String filePath) {
        List<Song> songs = new ArrayList<>();

        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {

            // Skip header row
            reader.readNext();

            String[] row;
            while ((row = reader.readNext()) != null) {

                if (row.length < 17) continue;

                try {
                    // Generate a synthetic track ID from artist + song name
                    // since this dataset has no Spotify track ID column
                    String artist    = row[0].trim();
                    String trackName = row[1].trim();
                    String trackId   = "v2_" + (artist + trackName)
                            .toLowerCase()
                            .replaceAll("\\s+", "_")
                            .replaceAll("[^a-z0-9_]", "");

                    int durationMs          = Integer.parseInt(row[2].trim());
                    // row[3] is year — we don't use it but skip past it
                    int popularity          = Integer.parseInt(row[5].trim());
                    double danceability     = Double.parseDouble(row[6].trim());
                    double energy           = Double.parseDouble(row[7].trim());
                    int key                 = Integer.parseInt(row[8].trim());
                    double loudness         = Double.parseDouble(row[9].trim());
                    int mode                = Integer.parseInt(row[10].trim());
                    double speechiness      = Double.parseDouble(row[11].trim());
                    double acousticness     = Double.parseDouble(row[12].trim());
                    double instrumentalness = Double.parseDouble(row[13].trim());
                    double liveness         = Double.parseDouble(row[14].trim());
                    double valence          = Double.parseDouble(row[15].trim());
                    double tempo            = Double.parseDouble(row[16].trim());
                    String genre            = row[17].trim();

                    // Defaults for fields missing from this dataset
                    String albumName   = "Unknown Album";
                    boolean explicit   = false;
                    int timeSignature  = 4; // most songs are in 4/4

                    Song song = new Song(
                            trackId, trackName, artist, albumName, genre,
                            popularity, energy, valence, danceability,
                            acousticness, instrumentalness, liveness,
                            speechiness, loudness, tempo,
                            durationMs, explicit, key, timeSignature, mode
                    );

                    songs.add(song);

                } catch (NumberFormatException e) {
                    System.out.println("Skipping malformed row in dataset2: "
                            + e.getMessage());
                }
            }

        } catch (IOException e) {
            System.out.println("Could not open file: " + filePath);
            e.printStackTrace();
        } catch (CsvValidationException e) {
            System.out.println("CSV error in dataset2: " + e.getMessage());
            e.printStackTrace();
        }

        return songs;
    }
    /**
     * Loads and combines both datasets into one list.
     * Call this instead of loadSongs() when you want both sources merged.
     */
    public static List<Song> loadAllSongs() {
        List<Song> all = new ArrayList<>();
        // Load dataset2 first — it has more common/mainstream songs
        all.addAll(loadSongsV2("data/dataset2.csv"));
        all.addAll(loadSongs("data/dataset.csv"));
        System.out.println("Combined total before dedup: " + all.size());
        return all;
    }
}