package com.example.musicfinder;

/**
 * Holds the importance weight for each audio feature dimension.
 * Weights default to 1.0 (equal importance) and get adjusted
 * by QueryUnderstanding based on the user's query keywords.
 * Higher weight = this feature matters more for this query.
 */
public class WeightVector {

    // One weight per feature in toFeatureVector()
    // Order must match exactly: energy, valence, danceability,
    // acousticness, instrumentalness, liveness, speechiness, loudness, tempo
    public double energy;
    public double valence;
    public double danceability;
    public double acousticness;
    public double instrumentalness;
    public double liveness;
    public double speechiness;
    public double loudness;
    public double tempo;

    // Popularity is separate from audio features — it's a ranking modifier
    // positive = prefer more popular songs
    // negative = prefer less popular (less mainstream) songs
    // zero = popularity doesn't matter for this query
    public double popularityBias;

    /**
     * Default constructor — all weights equal at 1.0, no popularity bias.
     * This represents a neutral query with no modifiers.
     */
    public WeightVector() {
        energy           = 1.2;
        valence          = 1.1;
        danceability     = 1.0;
        acousticness     = 1.2;
        instrumentalness = 0.5;
        liveness         = 0.3;
        speechiness      = 0.4;
        loudness         = 1.3;
        tempo            = 0.8;
        popularityBias   = 0.0;
    }

    /**
     * Returns weights as a double array in the same order
     * as Song.toFeatureVector() so they can be applied directly
     * in weighted cosine similarity
     */
    public double[] toArray() {
        return new double[]{
                energy, valence, danceability, acousticness,
                instrumentalness, liveness, speechiness, loudness, tempo
        };
    }

    @Override
    public String toString() {
        return String.format(
                "Weights [energy=%.1f, valence=%.1f, dance=%.1f, acoustic=%.1f, " +
                        "instrumental=%.1f, live=%.1f, speech=%.1f, loud=%.1f, tempo=%.1f, " +
                        "popularityBias=%.1f]",
                energy, valence, danceability, acousticness,
                instrumentalness, liveness, speechiness, loudness, tempo, popularityBias
        );
    }
}
