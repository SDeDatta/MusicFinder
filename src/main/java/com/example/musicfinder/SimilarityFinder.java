package com.example.musicfinder;

import java.util.*;

public class SimilarityFinder {

    /**
     * Finds the top N songs most similar to the seed song
     * using cosine similarity on normalized audio feature vectors
     */
    public static List<Song> findSimilar(Song seed, List<Song> allSongs, int topN) {

        // Priority queue sorted by score ascending — lowest score gets evicted
        // when queue exceeds topN, keeping only the best results
        PriorityQueue<double[]> topSongs = new PriorityQueue<>(
                Comparator.comparingDouble(a -> a[0])
        );

        for (int i = 0; i < allSongs.size(); i++) {
            Song candidate = allSongs.get(i);

            // Don't compare a song to itself
            if (candidate.getTrackId().equals(seed.getTrackId())) continue;

            double score = cosineSimilarity(
                    seed.toFeatureVector(),
                    candidate.toFeatureVector()
            );

            topSongs.offer(new double[]{score, i});

            // Evict lowest scorer when we exceed topN (based on what person asks for)
            if (topSongs.size() > topN) topSongs.poll();
        }

        // Collect results — note they come out lowest first so we reverse
        List<Song> results = new ArrayList<>();
        for (double[] entry : topSongs) {
            results.add(allSongs.get((int) entry[1]));
        }
        Collections.reverse(results);
        return results;
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