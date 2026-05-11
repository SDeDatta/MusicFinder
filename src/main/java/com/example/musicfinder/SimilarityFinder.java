package com.example.musicfinder;

import java.util.*;

public class SimilarityFinder {

    /**
     * Finds the top N songs most similar to the seed song
     * using cosine similarity on normalized audio feature vectors
     */
    // How much genre influences the final score vs audio features
// 0.7 audio + 0.3 genre = genre matters but audio features dominate
    private static final double AUDIO_WEIGHT = 0.75;
    private static final double GENRE_WEIGHT = 0.25;

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

        List<Song> deduped = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();
        for (Song s : results) {
            if (seenIds.add(s.getTrackId())) {
                deduped.add(s);
            }
        }
        return deduped;
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
        if (genreA.contains(genreB) || genreB.contains(genreA)) return 0.75;

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

// Deduplicate before returning
        List<Song> deduped = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();

        for (Song s : results) {
            if (seenIds.add(s.getTrackId())) {
                deduped.add(s);
            }
        }

        return deduped;
    }
    /**
     * Scores how well a song matches the requested vibe targets.
     * vibeTargets maps feature names to ideal values (0.0-1.0).
     * Returns a score between 0.0 and 1.0.
     * A score of 1.0 means the song perfectly matches every target.
     *
     * This is different from cosine similarity — it measures absolute
     * feature distance from targets rather than directional alignment
     * with the seed song. It answers "how groovy is this song"
     * rather than "how similar is this to the seed."
     */
    public static double vibeScore(Song song, Map<String, Double> vibeTargets) {
        if (vibeTargets == null || vibeTargets.isEmpty()) return 1.0;

        double totalScore = 0;
        int count = 0;

        for (Map.Entry<String, Double> entry : vibeTargets.entrySet()) {
            double target = entry.getValue();
            double actual = getFeatureValue(song, entry.getKey());
            // Score = 1 - absolute distance from target
            // A song hitting exactly the target gets 1.0
            // A song 0.5 away gets 0.5
            double featureScore = 1.0 - Math.abs(actual - target);
            totalScore += featureScore;
            count++;
        }

        return count > 0 ? totalScore / count : 1.0;
    }

    /**
     * Returns the value of a named audio feature from a song.
     * Used by vibeScore to look up features by name.
     */
    private static double getFeatureValue(Song song, String featureName) {
        switch (featureName.toLowerCase()) {
            case "energy":           return song.getEnergy();
            case "valence":          return song.getValence();
            case "danceability":     return song.getDanceability();
            case "acousticness":     return song.getAcousticness();
            case "instrumentalness": return song.getInstrumentalness();
            case "liveness":         return song.getLiveness();
            case "speechiness":      return song.getSpeechiness();
            case "loudness":         return (song.getLoudness() + 60) / 60.0;
            case "tempo":            return song.getTempo() / 250.0;
            default:                 return 0.5;
        }
    }
    /**
     * Quality-gated recommendation finder.
     * Instead of always returning exactly topN results, returns all
     * candidates that score above minimumQuality.
     * Combines three signals:
     *   1. Weighted cosine similarity to seed song (how similar is it?)
     *   2. Vibe score against absolute targets (does it have the right vibe?)
     *   3. Popularity bias from query
     *
     * The final score blends all three. Only songs above minimumQuality
     * are returned, ensuring every result is a genuine quality match.
     * maxResults caps the output in case threshold is too permissive.
     */
    public static List<Song> findSimilarQualityGated(
            Song seed,
            List<Song> candidates,
            WeightVector weights,
            Map<String, Double> vibeTargets,
            double minimumQuality,
            int maxResults) {

        double[] weightArray = weights.toArray();

        // Score every candidate
        List<double[]> scored = new ArrayList<>(); // [score, index]

        for (int i = 0; i < candidates.size(); i++) {
            Song candidate = candidates.get(i);
            if (candidate.getTrackId().equals(seed.getTrackId())) continue;

            // Signal 1 — weighted cosine similarity to seed
            double similarityScore = weightedCosineSimilarity(
                    seed.toFeatureVector(),
                    candidate.toFeatureVector(),
                    weightArray
            );

            // Signal 2 — absolute vibe match
            double vibe = vibeScore(candidate, vibeTargets);

            // Signal 3 — genre similarity
            double genreScore = genreSimilarity(seed, candidate);

            // Signal 4 — popularity bias
            double popularityScore = 0;
            if (weights.popularityBias != 0) {
                double normalizedPop = candidate.getPopularity() / 100.0;
                popularityScore = (weights.popularityBias > 0)
                        ? normalizedPop * (weights.popularityBias / 100.0)
                        : (1.0 - normalizedPop) * (-weights.popularityBias / 100.0);
            }

            // Blend signals
            // If vibeTargets exist, vibe score matters a lot
            // If no vibe targets, similarity to seed dominates
            double vibeWeight = vibeTargets.isEmpty() ? 0.0 : 0.35;
            double simWeight  = vibeTargets.isEmpty() ? 0.65 : 0.40;
            double genreWeight = 0.15;
            double popWeight   = 0.10;

            // Normalize weights to sum to 1.0
            double totalWeight = simWeight + vibeWeight + genreWeight + popWeight;

            double finalScore = ((simWeight  * similarityScore)
                    +  (vibeWeight * vibe)
                    +  (genreWeight * genreScore)
                    +  (popWeight  * popularityScore))
                    / totalWeight;

            scored.add(new double[]{finalScore, i});
        }

        // Sort by score descending
        scored.sort((a, b) -> Double.compare(b[0], a[0]));

        // Apply quality gate — only keep songs above minimumQuality
        List<Song> results = new ArrayList<>();
        Set<String> seenNormalized = new HashSet<>();

        for (double[] entry : scored) {
            double score = entry[0];

            // Stop if below quality threshold
            if (score < minimumQuality) break;

            Song s = candidates.get((int) entry[1]);

            // Deduplicate by normalized name+artist
            String key = s.getTrackName().toLowerCase()
                    .replaceAll("[^a-z0-9]", "")
                    + "|"
                    + s.getArtists().toLowerCase()
                    .replaceAll("[^a-z0-9]", "");

            if (seenNormalized.add(key)) {
                results.add(s);
                System.out.println("  ✓ [" + String.format("%.3f", score) + "] "
                        + s.getTrackName() + " by " + s.getArtists());
            }

            if (results.size() >= maxResults) break;
        }

        System.out.println("Quality gate (" + minimumQuality + "): "
                + results.size() + " results passed from "
                + candidates.size() + " candidates");

        return results;
    }
}