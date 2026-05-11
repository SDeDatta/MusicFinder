package com.example.musicfinder;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;

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
     * Loads the third Spotify dataset (Spotify Song Attributes format).
     * Key columns: track_id, track_name, track_artist, track_popularity,
     * track_album_name, playlist_genre, energy, tempo, danceability,
     * loudness, liveness, valence, speechiness, acousticness,
     * instrumentalness, key, mode, duration_ms, time_signature
     *
     * NOTE: Verify these indices by opening dataset3.csv in a spreadsheet first.
     * Column order in CSV may differ from the list you were given.
     */
    public static List<Song> loadSongsV3(String filePath) {
        List<Song> songs = new ArrayList<>();

        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {

            // Read header row and map column names to indices dynamically
            // This is safer than hardcoding indices since column order varies
            String[] headers = reader.readNext();
            Map<String, Integer> colIndex = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                colIndex.put(headers[i].trim().toLowerCase(), i);
            }

            // Verify required columns exist
            String[] required = {
                    "track_id", "track_name", "track_artist", "track_popularity",
                    "energy", "valence", "danceability", "acousticness",
                    "instrumentalness", "liveness", "speechiness",
                    "loudness", "tempo", "playlist_genre", "duration_ms"
            };
            for (String col : required) {
                if (!colIndex.containsKey(col)) {
                    System.out.println("WARNING: Missing column in dataset3: " + col);
                }
            }

            String[] row;
            while ((row = reader.readNext()) != null) {
                if (row.length < 10) continue;

                try {
                    // Use dynamic column lookup instead of hardcoded indices
                    String trackId   = getCol(row, colIndex, "track_id", "");
                    String trackName = getCol(row, colIndex, "track_name", "");
                    String artist    = getCol(row, colIndex, "track_artist", "");
                    String albumName = getCol(row, colIndex, "track_album_name", "Unknown Album");
                    String genre     = getCol(row, colIndex, "playlist_genre", "unknown");

                    // If track_id is empty, generate one like dataset2
                    if (trackId.isEmpty()) {
                        trackId = "v3_" + (artist + trackName)
                                .toLowerCase()
                                .replaceAll("\\s+", "_")
                                .replaceAll("[^a-z0-9_]", "");
                    }

                    int popularity          = parseIntCol(row, colIndex, "track_popularity", 0);
                    int durationMs          = parseIntCol(row, colIndex, "duration_ms", 0);
                    int key                 = parseIntCol(row, colIndex, "key", 0);
                    int mode                = parseIntCol(row, colIndex, "mode", 1);
                    int timeSignature       = parseIntCol(row, colIndex, "time_signature", 4);
                    double energy           = parseDoubleCol(row, colIndex, "energy", 0.5);
                    double valence          = parseDoubleCol(row, colIndex, "valence", 0.5);
                    double danceability     = parseDoubleCol(row, colIndex, "danceability", 0.5);
                    double acousticness     = parseDoubleCol(row, colIndex, "acousticness", 0.5);
                    double instrumentalness = parseDoubleCol(row, colIndex, "instrumentalness", 0.0);
                    double liveness         = parseDoubleCol(row, colIndex, "liveness", 0.5);
                    double speechiness      = parseDoubleCol(row, colIndex, "speechiness", 0.5);
                    double loudness         = parseDoubleCol(row, colIndex, "loudness", -10.0);
                    double tempo            = parseDoubleCol(row, colIndex, "tempo", 120.0);
                    boolean explicit        = false; // not in this dataset

                    Song song = new Song(
                            trackId, trackName, artist, albumName, genre,
                            popularity, energy, valence, danceability,
                            acousticness, instrumentalness, liveness,
                            speechiness, loudness, tempo,
                            durationMs, explicit, key, timeSignature, mode
                    );

                    songs.add(song);

                } catch (Exception e) {
                    System.out.println("Skipping malformed row in dataset3: "
                            + e.getMessage());
                }
            }

        } catch (IOException e) {
            System.out.println("Could not open file: " + filePath);
            e.printStackTrace();
        } catch (CsvValidationException e) {
            System.out.println("CSV error in dataset3: " + e.getMessage());
            e.printStackTrace();
        }

        return songs;
    }
    /**
     * Loads the fourth Spotify dataset.
     * Key columns: id (track_id), song_name, artist, genre,
     * danceability, energy, key, loudness, mode, speechiness,
     * acousticness, instrumentalness, liveness, valence, tempo,
     * duration_ms, time_signature.
     * No popularity column — defaults to neutral value of 50.
     * Uses dynamic column lookup for safety.
     */
    public static List<Song> loadSongsV4(String filePath) {
        List<Song> songs = new ArrayList<>();

        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {

            // Read header and build column index map
            String[] headers = reader.readNext();
            Map<String, Integer> colIndex = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                colIndex.put(headers[i].trim().toLowerCase(), i);
            }

            // Warn about any missing columns we expect
            String[] required = {
                    "id", "song_name", "artist", "genre",
                    "danceability", "energy", "loudness", "speechiness",
                    "acousticness", "instrumentalness", "liveness",
                    "valence", "tempo", "duration_ms"
            };
            for (String col : required) {
                if (!colIndex.containsKey(col)) {
                    System.out.println("WARNING: Missing column in dataset4: " + col);
                }
            }

            String[] row;
            while ((row = reader.readNext()) != null) {
                if (row.length < 10) continue;

                try {
                    // Use Spotify's id column as track ID
                    String trackId = getCol(row, colIndex, "id", "");
                    if (trackId.isEmpty()) {
                        // Fallback synthetic ID if id column is empty
                        String artist    = getCol(row, colIndex, "artist", "unknown");
                        String trackName = getCol(row, colIndex, "song_name", "unknown");
                        trackId = "v4_" + (artist + trackName)
                                .toLowerCase()
                                .replaceAll("\\s+", "_")
                                .replaceAll("[^a-z0-9_]", "");
                    }

                    String trackName = getCol(row, colIndex, "song_name",       "Unknown");
                    String artist    = getCol(row, colIndex, "artist",          "Unknown");
                    String genre     = getCol(row, colIndex, "genre",           "unknown");

                    // Fields not present in this dataset
                    String albumName  = "Unknown Album";
                    boolean explicit  = false;

                    // No popularity column — default to neutral 50
                    // This means popularity-based queries (less mainstream,
                    // more popular) cannot distinguish songs within this
                    // dataset from each other, only relative to other datasets
                    int popularity = 50;

                    double danceability     = parseDoubleCol(row, colIndex, "danceability",     0.5);
                    double energy           = parseDoubleCol(row, colIndex, "energy",           0.5);
                    double loudness         = parseDoubleCol(row, colIndex, "loudness",         -10.0);
                    double speechiness      = parseDoubleCol(row, colIndex, "speechiness",      0.5);
                    double acousticness     = parseDoubleCol(row, colIndex, "acousticness",     0.5);
                    double instrumentalness = parseDoubleCol(row, colIndex, "instrumentalness", 0.0);
                    double liveness         = parseDoubleCol(row, colIndex, "liveness",         0.5);
                    double valence          = parseDoubleCol(row, colIndex, "valence",          0.5);
                    double tempo            = parseDoubleCol(row, colIndex, "tempo",            120.0);
                    int key                 = parseIntCol(row, colIndex, "key",                 0);
                    int mode                = parseIntCol(row, colIndex, "mode",                1);
                    int durationMs          = parseIntCol(row, colIndex, "duration_ms",         0);
                    int timeSignature       = parseIntCol(row, colIndex, "time_signature",      4);

                    Song song = new Song(
                            trackId, trackName, artist, albumName, genre,
                            popularity, energy, valence, danceability,
                            acousticness, instrumentalness, liveness,
                            speechiness, loudness, tempo,
                            durationMs, explicit, key, timeSignature, mode
                    );

                    songs.add(song);

                } catch (Exception e) {
                    System.out.println("Skipping malformed row in dataset4: "
                            + e.getMessage());
                }
            }

        } catch (IOException e) {
            System.out.println("Could not open file: " + filePath);
            e.printStackTrace();
        } catch (CsvValidationException e) {
            System.out.println("CSV error in dataset4: " + e.getMessage());
            e.printStackTrace();
        }

        return songs;
    }
    /**
     * Loads the sixth Spotify dataset.
     * Columns: id, name, artists, popularity, year, release_date,
     * valence, acousticness, danceability, duration_ms, energy,
     * explicit, instrumentalness, key, liveness, loudness,
     * mode, speechiness, tempo
     *
     * Has real Spotify IDs, real popularity, and all audio features
     * in standard 0.0-1.0 decimal format.
     * Missing: genre — defaults to "unknown" and will be enriched
     * by deduplication with other datasets that have genre data.
     */
    /**
     * Loads the enrichment dataset which has no audio features
     * but has rich metadata: real track IDs, artist genres,
     * and popularity scores.
     *
     * Returns a Map keyed by track_id for fast lookup.
     * Used to enrich songs already loaded from other datasets.
     */
    public static Map<String, SongMetadata> loadEnrichmentData(String filePath) {
        Map<String, SongMetadata> metadata = new HashMap<>();

        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {

            String[] headers = reader.readNext();
            Map<String, Integer> colIndex = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                colIndex.put(headers[i].trim().toLowerCase(), i);
            }

            String[] row;
            while ((row = reader.readNext()) != null) {
                if (row.length < 5) continue;

                try {
                    String trackId    = getCol(row, colIndex, "track_id",         "");
                    String genre      = getCol(row, colIndex, "artist_genres",    "");
                    int popularity    = parseIntCol(row, colIndex,
                            "track_popularity", -1);

                    if (trackId.isEmpty()) continue;

                    // artist_genres comes as a string like
                    // "['pop', 'dance pop', 'electropop']"
                    // Clean it into a simple genre string
                    String cleanedGenre = genre
                            .replaceAll("[\\[\\]'\"\\s]", "")
                            .split(",")[0]; // take first genre listed
                    if (cleanedGenre.isEmpty()) cleanedGenre = null;

                    metadata.put(trackId, new SongMetadata(cleanedGenre, popularity));

                } catch (Exception e) {
                    // Skip malformed rows silently
                }
            }

        } catch (IOException | CsvValidationException e) {
            System.out.println("Could not load enrichment data: " + e.getMessage());
        }

        System.out.println("Enrichment data loaded: " + metadata.size() + " entries");
        return metadata;
    }

    /**
     * Safely gets a string value from a row using a column name lookup.
     * Returns defaultVal if column doesn't exist or value is empty.
     */
    private static String getCol(String[] row, Map<String, Integer> colIndex,
                                 String colName, String defaultVal) {
        Integer idx = colIndex.get(colName.toLowerCase());
        if (idx == null || idx >= row.length) return defaultVal;
        String val = row[idx].trim();
        return val.isEmpty() ? defaultVal : val;
    }

    /**
     * Safely parses an integer from a column by name.
     */
    private static int parseIntCol(String[] row, Map<String, Integer> colIndex,
                                   String colName, int defaultVal) {
        try {
            String val = getCol(row, colIndex, colName, String.valueOf(defaultVal));
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    /**
     * Safely parses a double from a column by name.
     */
    private static double parseDoubleCol(String[] row, Map<String, Integer> colIndex,
                                         String colName, double defaultVal) {
        try {
            String val = getCol(row, colIndex, colName, String.valueOf(defaultVal));
            return Double.parseDouble(val);
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }
    /**
     * Loads and combines both datasets into one list.
     * Call this instead of loadSongs() when you want both sources merged.
     */
    public static List<Song> loadAllSongs() {
        List<Song> all = new ArrayList<>();
        all.addAll(loadSongsV2("data/dataset2.csv"));
        all.addAll(loadSongsV3("data/dataset3.csv"));
        all.addAll(loadSongsV4("data/dataset4.csv"));
        all.addAll(loadSongs("data/dataset.csv"));
        System.out.println("Combined total before dedup: " + all.size());

        // --- dedup logic (unchanged) ---
        Map<String, Song> deduped = new LinkedHashMap<>();
        Map<String, String> nameKeyToId = new HashMap<>();
        for (Song song : all) {
            String nameKey = normalizeStatic(song.getTrackName())
                    + "|" + normalizeStatic(song.getArtists());
            if (!deduped.containsKey(nameKey)) {
                deduped.put(nameKey, song);
                nameKeyToId.put(nameKey, song.getTrackId());
            } else {
                String existingId = nameKeyToId.get(nameKey);
                boolean existingIsSynthetic = existingId.startsWith("v2_")
                        || existingId.startsWith("v3_")
                        || existingId.startsWith("v4_");
                boolean newIsReal = !song.getTrackId().startsWith("v2_")
                        && !song.getTrackId().startsWith("v3_")
                        && !song.getTrackId().startsWith("v4_");
                if (existingIsSynthetic && newIsReal) {
                    deduped.put(nameKey, song);
                    nameKeyToId.put(nameKey, song.getTrackId());
                }
            }
        }

        List<Song> result = new ArrayList<>(deduped.values());
        System.out.println("After dedup: " + result.size() + " songs");

        // --- Enrich with metadata dataset ---
       /* Map<String, SongMetadata> enrichment =
                loadEnrichmentData("data/dataset_enrichment.csv");

        int enriched = 0;
        for (Song song : result) {
            SongMetadata meta = enrichment.get(song.getTrackId());
            if (meta == null) continue;

            // Update genre if current genre is unknown or generic
            // and enrichment has a real value
            if (meta.genre != null && !meta.genre.isEmpty()) {
                String currentGenre = song.getGenre().toLowerCase();
                if (currentGenre.equals("unknown") || currentGenre.equals("pop")
                        || currentGenre.isEmpty()) {
                    song.setGenre(meta.genre);
                    enriched++;
                }
            }

            // Update popularity if enrichment has a real value
            // and current popularity is the neutral default
            if (meta.popularity >= 0 && song.getPopularity() == 50) {
                song.setPopularity(meta.popularity);
            }
        }

        System.out.println("Enriched " + enriched + " songs with better metadata");*/
        return result;
    }
    /**
     * Static normalization for deduplication.
     * Strips featured tags, punctuation, and whitespace differences.
     */
    static String normalizeStatic(String input) {
        if (input == null) return "";
        return input.toLowerCase()
                .replaceAll("\\(feat\\..*?\\)", "")
                .replaceAll("\\(ft\\..*?\\)", "")
                .replaceAll("\\(with.*?\\)", "")
                .replaceAll("feat\\..*", "")
                .replaceAll("\\(.*remaster.*?\\)", "")
                .replaceAll("\\(.*version.*?\\)", "")
                .replaceAll("\\(.*edit.*?\\)", "")
                .replaceAll("\\(.*mix.*?\\)", "")
                .replaceAll("\\(.*live.*?\\)", "")
                .replaceAll("[^a-z0-9\\s]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }
}