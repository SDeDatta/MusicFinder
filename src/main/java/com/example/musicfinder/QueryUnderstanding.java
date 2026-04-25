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
        
        When given a natural language music query, you must return ONLY a JSON object
        with weights and values for the following fields. No explanation, no markdown,
        no extra text — pure JSON only.
        
        The fields and their meanings:
        - energy: intensity and activity (0.0=very calm, 1.0=very intense)
        - valence: musical positivity (0.0=sad/dark, 1.0=happy/euphoric)
        - danceability: how suitable for dancing (0.0=not danceable, 1.0=very danceable)
        - acousticness: acoustic vs electronic (0.0=electronic, 1.0=fully acoustic)
        - instrumentalness: vocals vs instrumental (0.0=has vocals, 1.0=fully instrumental)
        - liveness: studio vs live feel (0.0=studio, 1.0=live performance)
        - speechiness: spoken words (0.0=no speech, 1.0=all speech)
        - loudness: perceived loudness (0.0=very quiet, 1.0=very loud)
        - tempo: speed (0.0=very slow, 1.0=very fast)
        - popularityBias: how mainstream (negative=obscure, 0=neutral, positive=popular)
        - "- seedSong: the EXACT song title mentioned in the query, or null if none\\n" +
        - "- seedArtist: the EXACT artist name mentioned in the query, or null if none. " +
        - "  Always extract the artist if one is mentioned — never leave this null " +
        - "  if the query contains 'by [artist name]'.\\n"
        
        For weights (energy through tempo): use values between 0.1 and 3.0.
        1.0 means normal importance. Higher means more important for this query.
        Lower means less important.
        
        For popularityBias: use -100 to +100. Negative means prefer obscure songs.
        Positive means prefer popular songs. 0 means popularity doesn't matter.
        
        Example input: "songs like Clocks by Coldplay but less mainstream and more energetic"
        Example output:
        {
          "energy": 2.0,
          "valence": 1.0,
          "danceability": 1.0,
          "acousticness": 0.8,
          "instrumentalness": 1.0,
          "liveness": 1.0,
          "speechiness": 1.0,
          "loudness": 1.0,
          "tempo": 1.5,
          "popularityBias": -60,
          "seedSong": "Clocks",
          "seedArtist": "Coldplay"
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
        // The OpenAI response wraps the actual content in a nested structure:
        // response → choices[0] → message → content → our JSON
        JsonObject response  = JsonParser.parseString(responseBody).getAsJsonObject();
        JsonObject message   = response
                .getAsJsonArray("choices")
                .get(0).getAsJsonObject()
                .getAsJsonObject("message");

        String content = message.get("content").getAsString().trim();

        // Now parse the inner JSON that the LLM returned
        JsonObject weights = JsonParser.parseString(content).getAsJsonObject();

        // Build WeightVector from the parsed JSON
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

        // Extract seed song and artist
        String seedSong   = weights.has("seedSong")   &&
                !weights.get("seedSong").isJsonNull()
                ? weights.get("seedSong").getAsString() : null;
        String seedArtist = weights.has("seedArtist") &&
                !weights.get("seedArtist").isJsonNull()
                ? weights.get("seedArtist").getAsString() : null;

        return new QueryResult(wv, seedSong, seedArtist);
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
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = client.send(
                    request, HttpResponse.BodyHandlers.ofString()
            );

            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
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
        return new QueryResult(new WeightVector(), null, null);
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