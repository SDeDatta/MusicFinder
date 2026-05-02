package com.example.musicfinder;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

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
    
    Fields and meanings:
    - energy: intensity (0.0=calm, 1.0=intense), weight 0.1-3.0
    - valence: positivity (0.0=sad, 1.0=happy), weight 0.1-3.0
    - danceability: (0.0=not danceable, 1.0=very), weight 0.1-3.0
    - acousticness: (0.0=electronic, 1.0=acoustic), weight 0.1-3.0
    - instrumentalness: (0.0=vocals, 1.0=instrumental), weight 0.1-3.0
    - liveness: (0.0=studio, 1.0=live), weight 0.1-3.0
    - speechiness: (0.0=no speech, 1.0=all speech), weight 0.1-3.0
    - loudness: (0.0=quiet, 1.0=loud), weight 0.1-3.0
    - tempo: (0.0=slow, 1.0=fast), weight 0.1-3.0
    - popularityBias: -100 to +100, negative=obscure, positive=popular
    - seedSong: exact song title from query, or null
    - seedArtist: exact artist name from query, or null
    - sameArtistOnly: true if the query asks for songs by the same artist
      (e.g. "more songs by them", "other songs by this artist",
      "more Coldplay songs"). false otherwise.
    - differentLanguage: true if the query asks for songs in a
      different language than the seed song
      (e.g. "but in Spanish", "in a different language",
      "not in English"). false otherwise.
    
    Example input: "more songs by Coldplay but less mainstream"
    Example output:
    {
      "energy": 1.0,
      "valence": 1.0,
      "danceability": 1.0,
      "acousticness": 1.0,
      "instrumentalness": 1.0,
      "liveness": 1.0,
      "speechiness": 1.0,
      "loudness": 1.0,
      "tempo": 1.0,
      "popularityBias": -60,
      "seedSong": null,
      "seedArtist": "Coldplay",
      "sameArtistOnly": true,
      "differentLanguage": false
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
        JsonObject weights = JsonParser.parseString(content).getAsJsonObject();

        WeightVector wv = new WeightVector();
        wv.energy           = getDouble(weights, "energy",           1.0);
        wv.valence          = getDouble(weights, "valence",          1.0);
        wv.danceability     = getDouble(weights, "danceability",     1.0);
        wv.acousticness     = getDouble(weights, "acousticness",     1.0);
        wv.instrumentalness = getDouble(weights, "instrumentalness", 1.0);
        wv.liveness         = getDouble(weights, "liveness",         1.0);
        wv.speechiness      = getDouble(weights, "speechiness",      1.0);
        wv.loudness         = getDouble(weights, "loudness",         1.0);
        wv.tempo            = getDouble(weights, "tempo",            1.0);
        wv.popularityBias   = getDouble(weights, "popularityBias",   0.0);

        String seedSong   = weights.has("seedSong") &&
                !weights.get("seedSong").isJsonNull()
                ? weights.get("seedSong").getAsString() : null;
        String seedArtist = weights.has("seedArtist") &&
                !weights.get("seedArtist").isJsonNull()
                ? weights.get("seedArtist").getAsString() : null;

        // Parse the two new boolean fields
        boolean sameArtistOnly = weights.has("sameArtistOnly") &&
                weights.get("sameArtistOnly").getAsBoolean();
        boolean differentLanguage = weights.has("differentLanguage") &&
                weights.get("differentLanguage").getAsBoolean();

        return new QueryResult(wv, seedSong, seedArtist,
                sameArtistOnly, differentLanguage);
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
        return new QueryResult(new WeightVector(), null, null, false, false);
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