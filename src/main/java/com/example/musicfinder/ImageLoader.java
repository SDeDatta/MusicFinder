package com.example.musicfinder;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.scene.image.Image;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.ConcurrentHashMap;

public class ImageLoader {

    // Cache so we don't re-fetch the same album art repeatedly
    // ConcurrentHashMap is thread-safe since image loading runs on background threads
    private static final ConcurrentHashMap<String, Image> imageCache
            = new ConcurrentHashMap<>();

    // Placeholder shown while real image loads or if fetch fails
    private static final String PLACEHOLDER_COLOR = "#0d2a45";

    /**
     * Fetches album art for a given Spotify track ID.
     * Uses Spotify's oEmbed API which requires no authentication.
     * Returns null if the fetch fails — caller should show placeholder.
     *
     * This method is blocking — always call it from a background thread.
     */
    public static Image fetchAlbumArt(String trackId) {

        // Return cached image if we already fetched this one
        if (imageCache.containsKey(trackId)) {
            return imageCache.get(trackId);
        }

        // Skip synthetic IDs from dataset2/dataset3 — they won't have
        // real Spotify pages so the oEmbed call would fail
        if (trackId.startsWith("v2_") || trackId.startsWith("v3_")) {
            return null;
        }

        try {
            // Build the oEmbed request URL
            String oEmbedUrl = "https://open.spotify.com/oembed?url="
                    + "https://open.spotify.com/track/" + trackId;

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(oEmbedUrl))
                    .header("User-Agent", "MusicFinder/1.0")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(
                    request, HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() != 200) return null;

            // Parse the thumbnail URL from the JSON response
            JsonObject json = JsonParser.parseString(response.body())
                    .getAsJsonObject();

            if (!json.has("thumbnail_url")) return null;

            String imageUrl = json.get("thumbnail_url").getAsString();

            // Load the image — background=true means non-blocking load
            Image image = new Image(imageUrl, 248, 248, false, true, false);
            if (image.isError()) {
                return null;
            }
            imageCache.put(trackId, image);
            return image;

        } catch (Exception e) {
            System.out.println("Could not fetch album art for: " + trackId);
            return null;
        }
    }

    /**
     * Clears the image cache — call this if memory becomes a concern
     */
    public static void clearCache() {
        imageCache.clear();
    }
}
