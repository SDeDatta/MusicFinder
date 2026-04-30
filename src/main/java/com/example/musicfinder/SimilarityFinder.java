package com.example.musicfinder;

import java.util.*;

public class SimilarityFinder {

    /**
     * Finds the top N songs most similar to the seed song
     * using cosine similarity on normalized audio feature vectors
     */
    // How much genre influences the final score vs audio features
// 0.7 audio + 0.3 genre = genre matters but audio features dominate
    private static final double AUDIO_WEIGHT = 0.5;
    private static final double GENRE_WEIGHT = 0.5;

    public static List<Song> findSimilar(Song seed, List<Song> allSongs, int topN) {
        // Organizes songs based on their similarity score (see line 40 with topSongs input)
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
    /**
     * Weighted cosine similarity — same math as regular cosine similarity
     * but each dimension is scaled by its corresponding weight before comparison.
     * Higher weight = that dimension pulls the score more strongly.
     */
    public static double weightedCosineSimilarity(double[] a, double[] b, double[] weights) {
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot   += weights[i] * a[i] * b[i];
            normA += weights[i] * a[i] * a[i];
            normB += weights[i] * b[i] * b[i];
        }
        if (normA == 0 || normB == 0) return 0;
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * Overloaded findSimilar that accepts a WeightVector.
     * Uses weighted cosine similarity and applies popularity bias
     * from the weight vector to the final score.
     */
    public static List<Song> findSimilar(Song seed, List<Song> allSongs,
                                         int topN, WeightVector weights) {
        PriorityQueue<double[]> topSongs = new PriorityQueue<>(
                Comparator.comparingDouble(a -> a[0])
        );

        double[] weightArray = weights.toArray();

        for (int i = 0; i < allSongs.size(); i++) {
            Song candidate = allSongs.get(i);
            if (candidate.getTrackId().equals(seed.getTrackId())) continue;

            // Weighted audio similarity
            double audioScore = weightedCosineSimilarity(
                    seed.toFeatureVector(),
                    candidate.toFeatureVector(),
                    weightArray
            );

            // Genre similarity
            double genreScore = genreSimilarity(seed, candidate);

            // Popularity adjustment — normalize popularity to 0-1 scale
            // then apply the bias from the query
            double popularityScore = 0;
            if (weights.popularityBias != 0) {
                double normalizedPop = candidate.getPopularity() / 100.0;
                // positive bias rewards popular songs, negative bias rewards obscure ones
                popularityScore = (weights.popularityBias > 0)
                        ? normalizedPop * (weights.popularityBias / 100.0)
                        : (1.0 - normalizedPop) * (-weights.popularityBias / 100.0);
            }

            // Final blended score
            double finalScore = (AUDIO_WEIGHT * audioScore)
                    + (GENRE_WEIGHT * genreScore)
                    + popularityScore;

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
}