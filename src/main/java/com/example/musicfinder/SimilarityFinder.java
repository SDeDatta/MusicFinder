package com.example.musicfinder;

import java.util.*;

public class SimilarityFinder {

    /**
     * Finds the top N songs most similar to the seed song
     * using cosine similarity on normalized audio feature vectors
     */
    // How much genre influences the final score vs audio features
// 0.7 audio + 0.3 genre = genre matters but audio features dominate
    private static final double AUDIO_WEIGHT = 0.7;
    private static final double GENRE_WEIGHT = 0.3;

    public static List<Song> findSimilar(Song seed, List<Song> allSongs, int topN) {

        PriorityQueue<double[]> topSongs = new PriorityQueue<>(
                Comparator.comparingDouble(a -> a[0])
        );

        for (int i = 0; i < allSongs.size(); i++) {
            Song candidate = allSongs.get(i);

            if (candidate.getTrackId().equals(seed.getTrackId())) continue;

            // Compute audio feature similarity
            double audioScore = cosineSimilarity(
                    seed.toFeatureVector(),
                    candidate.toFeatureVector()
            );

            // Compute genre similarity
            double genreScore = genreSimilarity(seed, candidate);

            // Blend into one final score
            double finalScore = (AUDIO_WEIGHT * audioScore)
                    + (GENRE_WEIGHT * genreScore);

            topSongs.offer(new double[]{finalScore, i});

            if (topSongs.size() > topN) topSongs.poll();
        }

        List<Song> results = new ArrayList<>();
        for (double[] entry : topSongs) {
            results.add(allSongs.get((int) entry[1]));
        }
        Collections.reverse(results);
        return results;
    }
    /**
     * Returns a genre similarity score between two songs.
     * 1.0 = exact same genre
     * 0.5 = genres share a root word (e.g. "indie-pop" and "indie")
     * 0.0 = completely different genres
     */
    public static double genreSimilarity(Song a, Song b) {
        String genreA = a.getGenre().toLowerCase().trim();
        String genreB = b.getGenre().toLowerCase().trim();

        // Exact match
        if (genreA.equals(genreB)) return 1.0;

        // Partial match — one genre contains the other (e.g. "pop" in "indie-pop")
        if (genreA.contains(genreB) || genreB.contains(genreA)) return 0.5;

        // No match
        return 0.0;
    }
    /**
     * Cosine similarity between two feature vectors.
     * Returns 0.0 (totally different) to 1.0 (identical)
     */
    public static double cosineSimilarity(double[] a, double[] b) {
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot   += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) return 0;
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}