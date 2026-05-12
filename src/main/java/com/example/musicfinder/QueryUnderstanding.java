package com.example.musicfinder;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

public class QueryUnderstanding {

    private static final String API_URL = "https://api.openai.com/v1/chat/completions";

    /**
     * The system prompt explains to the LLM exactly what your audio features mean
     * and tells it to return only JSON with weights for each one.
     * This is the key to the whole approach — the better your system prompt,
     * the better the weights you get back.
     */
    private static final String SYSTEM_PROMPT = """
    You are a music analysis assistant for a song recommendation engine.
    
    When given a natural language music query, return ONLY a JSON object.
    No explanation, no markdown, no extra text — pure JSON only.
    
    Fields and their meanings:
    - energy: intensity (0.0=calm, 1.0=intense), weight 0.1-3.0
    - valence: positivity (0.0=sad, 1.0=happy), weight 0.1-3.0
    - danceability: (0.0=not danceable, 1.0=very), weight 0.1-3.0
    - acousticness: (0.0=electronic, 1.0=acoustic), weight 0.1-3.0
    - instrumentalness: (0.0=vocals, 1.0=instrumental), weight 0.1-3.0
    - liveness: (0.0=studio, 1.0=live), weight 0.1-3.0
    - speechiness: (0.0=no speech, 1.0=all speech), weight 0.1-3.0
    - loudness: (0.0=quiet, 1.0=loud), weight 0.1-3.0
    - tempo: (0.0=slow, 1.0=fast), weight 0.1-3.0
    - popularityBias: -100 to +100
    - seedSong: exact song title or null
    - seedArtist: exact artist name or null
    - sameArtistOnly: true if user wants more songs by same artist
    - differentLanguage: true if user wants different language
    - targetLanguage: ISO code if differentLanguage is true, else null
    - seedLanguage: the ISO language code of the seed song itself. Use your knowledge of the song to determine what language it is sung in. Examples: en for English, es for Spanish, tl for Tagalog, ko for Korean, ja for Japanese, pt for Portuguese, fr for French. Always provide this, never null.
    - minimumQuality: a threshold between 0.0 and 1.0 representing
      how strictly candidates must match the described vibe.
      Use 0.5 for vague queries with no strong descriptors.
      Use 0.65-0.75 for queries with clear mood words like
      dreamy, energetic, melancholic.
      Use 0.75-0.85 for very specific genre or style requests
      like EDM, groovy, classical piano, lo-fi hip hop.
      Higher = fewer but more precise results.
    - vibeTargets: a JSON object describing the IDEAL audio feature
      values for the requested vibe, independent of the seed song.
      These are absolute target values (0.0-1.0) not weights.
      Only include features that the query specifically implies.
      Example for groovy EDM:
      {"danceability": 0.85, "energy": 0.80, "tempo": 0.75,
       "acousticness": 0.05, "valence": 0.70}
      Example for dreamy acoustic:
      {"acousticness": 0.85, "energy": 0.25, "valence": 0.50}
      Leave as {} if no strong vibe descriptors in query.
    
    Example input: "songs like Bohemian Rhapsody but more groovy and EDM"
    Example output:
    {
      "energy": 2.0,
      "valence": 1.2,
      "danceability": 2.5,
      "acousticness": 0.2,
      "instrumentalness": 1.0,
      "liveness": 0.5,
      "speechiness": 0.5,
      "loudness": 1.5,
      "tempo": 1.8,
      "popularityBias": 0,
      "seedSong": "Bohemian Rhapsody",
      "seedArtist": "Queen",
      "sameArtistOnly": false,
      "differentLanguage": false,
      "targetLanguage": null,
      "minimumQuality": 0.78,
      "seedLanguage": "en",
      "vibeTargets": {
        "danceability": 0.85,
        "energy": 0.82,
        "tempo": 0.78,
        "acousticness": 0.05,
        "valence": 0.72
      }
    }
    """;
    /**
     * Main method — takes a raw user query and returns a fully populated
     * QueryResult containing both the WeightVector and the seed song info.
     */
    public static QueryResult interpretQuery(String userQuery) {
        try {
            // Read API key from environment variable
            String apiKey = System.getenv("OPENAI_API_KEY");
            if (apiKey == null || apiKey.isEmpty()) {
                System.out.println("No API key found — using default weights");
                return defaultResult(userQuery);
            }

            // Build the JSON request body
            // We send two messages: the system prompt and the user's query
            String requestBody = """
                {
                  "model": "gpt-3.5-turbo",
                  "messages": [
                    {"role": "system", "content": %s},
                    {"role": "user",   "content": %s}
                  ],
                  "temperature": 0.3
                }
                """.formatted(
                    toJsonString(SYSTEM_PROMPT),
                    toJsonString(userQuery)
            );

            // Build and send the HTTP request
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            // Parse the response
            return parseResponse(response.body());

        } catch (Exception e) {
            System.out.println("API call failed: " + e.getMessage());
            return defaultResult(userQuery);
        }
    }

    /**
     * Parses the raw JSON response from OpenAI and extracts
     * the assistant's message content, then maps it to a QueryResult.
     */
    private static QueryResult parseResponse(String responseBody) {
        JsonObject response = JsonParser.parseString(responseBody).getAsJsonObject();
        JsonObject message  = response
                .getAsJsonArray("choices")
                .get(0).getAsJsonObject()
                .getAsJsonObject("message");

        String content = message.get("content").getAsString().trim();
        JsonObject json = JsonParser.parseString(content).getAsJsonObject();

        WeightVector wv = new WeightVector();
        wv.energy           = getDouble(json, "energy",           1.0);
        wv.valence          = getDouble(json, "valence",          1.0);
        wv.danceability     = getDouble(json, "danceability",     1.0);
        wv.acousticness     = getDouble(json, "acousticness",     1.0);
        wv.instrumentalness = getDouble(json, "instrumentalness", 1.0);
        wv.liveness         = getDouble(json, "liveness",         1.0);
        wv.speechiness      = getDouble(json, "speechiness",      1.0);
        wv.loudness         = getDouble(json, "loudness",         1.0);
        wv.tempo            = getDouble(json, "tempo",            1.0);
        wv.popularityBias   = getDouble(json, "popularityBias",   0.0);

        String seedSong   = json.has("seedSong") &&
                !json.get("seedSong").isJsonNull()
                ? json.get("seedSong").getAsString() : null;
        String seedArtist = json.has("seedArtist") &&
                !json.get("seedArtist").isJsonNull()
                ? json.get("seedArtist").getAsString() : null;

        boolean sameArtistOnly   = json.has("sameArtistOnly") &&
                json.get("sameArtistOnly").getAsBoolean();
        boolean differentLanguage = json.has("differentLanguage") &&
                json.get("differentLanguage").getAsBoolean();
        String targetLanguage = json.has("targetLanguage") &&
                !json.get("targetLanguage").isJsonNull()
                ? json.get("targetLanguage").getAsString() : null;
        String seedLanguage = (json.has("seedLanguage")
                && !json.get("seedLanguage").isJsonNull())
                ? json.get("seedLanguage").getAsString().toLowerCase().trim()
                : "en";

        // Parse minimumQuality — default 0.88 if missing
        double minimumQuality = getDouble(json, "minimumQuality", 0.88);

        // Parse vibeTargets — a nested JSON object of feature → target value
        Map<String, Double> vibeTargets = new java.util.HashMap<>();
        if (json.has("vibeTargets") && json.get("vibeTargets").isJsonObject()) {
            JsonObject vt = json.getAsJsonObject("vibeTargets");
            for (String key : vt.keySet()) {
                try {
                    vibeTargets.put(key, vt.get(key).getAsDouble());
                } catch (Exception ignored) {}
            }
        }

        return new QueryResult(wv, seedSong, seedArtist,
                sameArtistOnly, differentLanguage, targetLanguage,
                minimumQuality, vibeTargets, seedLanguage);
    }
    /**
     * Detects the likely language of a query string.
     * Uses the LLM to identify language rather than a separate library.
     * Returns a language code like "en", "es", "pt", "fr" etc.
     * Returns "en" as default if detection fails.
     */
    public static String detectLanguage(String query) {
        try {
            String apiKey = System.getenv("OPENAI_API_KEY");
            if (apiKey == null || apiKey.isEmpty()) return "en";

            String requestBody = """
            {
              "model": "gpt-3.5-turbo",
              "messages": [
                {"role": "system", "content": "Detect the language of the music query. Return ONLY a two-letter ISO language code like 'en', 'es', 'pt', 'fr', 'de', 'ja', 'ko'. Nothing else."},
                {"role": "user", "content": %s}
              ],
              "temperature": 0.0
            }
            """.formatted(toJsonString(query));

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    // where to send query
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    // authentication
                    .header("Authorization", "Bearer " + apiKey)
                    // POST = sending data
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = client.send(
                    request, HttpResponse.BodyHandlers.ofString()
            );

            // Turns into structured JSON object
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            // Choices represents multiple possible AI responses so I just grab first
            return json.getAsJsonArray("choices")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content").getAsString().trim().toLowerCase();

        } catch (Exception e) {
            return "en";
        }
    }

    /**
     * Safely reads a double from a JSON object, returning a default
     * value if the field is missing or malformed
     */
    private static double getDouble(JsonObject obj, String key, double defaultVal) {
        try {
            return obj.has(key) ? obj.get(key).getAsDouble() : defaultVal;
        } catch (Exception e) {
            return defaultVal;
        }
    }

    /**
     * Fallback result used when the API call fails or no key is set.
     * Returns neutral weights so the program still runs.
     */
    private static QueryResult defaultResult(String query) {
        return new QueryResult(new WeightVector(), null, null,
                false, false, null, 0.5,
                new java.util.HashMap<>(), "en");
    }

    /**
     * Escapes a string for safe embedding inside a JSON value.
     * Handles quotes and newlines that would break the JSON structure.
     */
    private static String toJsonString(String text) {
        return "\"" + text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r") + "\"";
    }
}