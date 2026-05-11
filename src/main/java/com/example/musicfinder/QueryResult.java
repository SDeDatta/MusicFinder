package com.example.musicfinder;

import java.util.Map;

public class QueryResult {

    private WeightVector weights;
    private String seedSong;
    private String seedArtist;
    private boolean sameArtistOnly;
    private boolean differentLanguage;
    private String targetLanguage;
    private double minimumQuality;          // ← new
    private Map<String, Double> vibeTargets;
    private String seedLanguage; // ← new

    public QueryResult(WeightVector weights, String seedSong,
                       String seedArtist, boolean sameArtistOnly,
                       boolean differentLanguage, String targetLanguage,
                       double minimumQuality,
                       Map<String, Double> vibeTargets, String seedLanguage) {
        this.weights           = weights;
        this.seedSong          = seedSong;
        this.seedArtist        = seedArtist;
        this.sameArtistOnly    = sameArtistOnly;
        this.differentLanguage = differentLanguage;
        this.targetLanguage    = targetLanguage;
        this.minimumQuality    = minimumQuality;
        this.vibeTargets       = vibeTargets;
        this.seedLanguage = seedLanguage;
    }

    public WeightVector getWeights()              { return weights; }
    public String getSeedSong()                   { return seedSong; }
    public String getSeedArtist()                 { return seedArtist; }
    public boolean isSameArtistOnly()             { return sameArtistOnly; }
    public boolean isDifferentLanguage()          { return differentLanguage; }
    public String getTargetLanguage()             { return targetLanguage; }
    public double getMinimumQuality()             { return minimumQuality; }
    public Map<String, Double> getVibeTargets()   { return vibeTargets; }

    public String getSeedLanguage() {
        return seedLanguage;
    }

    @Override
    public String toString() {
        return "QueryResult{seedSong='" + seedSong + "', artist='"
                + seedArtist + "', minQuality=" + minimumQuality
                + ", vibeTargets=" + vibeTargets + ", " + weights + "}";
    }
}