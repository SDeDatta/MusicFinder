package com.example.musicfinder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

public class SimilarityFinder {

    /**
     * Finds the top N songs most similar to the given seed song
     * using cosine similarity on audio feature vectors
     */
    public static List<Song> findSimilar(Song seed, List<Song> allSongs, int topN) {

        // A priority queue sorted by similarity score, lowest first
        // We keep only the top N results
        PriorityQueue<double[]> topSongs = new PriorityQueue<>(
                Comparator.comparingDouble(a -> a[0])
        );

        for (Song candidate : allSongs) {

            // Don't compare a song to itself
            if (candidate.getTrackId().equals(seed.getTrackId())) continue;

            // Compute how similar this candidate is to the seed
            double score = cosineSimilarity(seed.toFeatureVector(),
                    candidate.toFeatureVector());

            topSongs.offer(new double[]{score, allSongs.indexOf(candidate)});

            // Keep the queue trimmed to only the top N
            if (topSongs.size() > topN) topSongs.poll();
        }

        // Pull the results out in order
        List<Song> results = new ArrayList<>();
        for (double[] entry : topSongs) {
            results.add(allSongs.get((int) entry[1]));
        }
        return results;
    }

    /**
     * Computes cosine similarity between two feature vectors.
     * Returns a value between 0.0 (completely different) and 1.0 (identical)
     */
    private static double cosineSimilarity(double[] a, double[] b) {
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot   += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        // Guard against division by zero if a vector is all zeros
        if (normA == 0 || normB == 0) return 0;
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
