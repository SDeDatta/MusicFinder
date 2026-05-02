package com.example.musicfinder;

public class QueryResult {

    private WeightVector weights;
    private String seedSong;
    private String seedArtist;
    private boolean sameArtistOnly;    // ← new
    private boolean differentLanguage; // ← new

    public QueryResult(WeightVector weights, String seedSong,
                       String seedArtist, boolean sameArtistOnly,
                       boolean differentLanguage) {
        this.weights          = weights;
        this.seedSong         = seedSong;
        this.seedArtist       = seedArtist;
        this.sameArtistOnly   = sameArtistOnly;
        this.differentLanguage = differentLanguage;
    }

    public WeightVector getWeights()        { return weights; }
    public String getSeedSong()             { return seedSong; }
    public String getSeedArtist()           { return seedArtist; }
    public boolean isSameArtistOnly()       { return sameArtistOnly; }
    public boolean isDifferentLanguage()    { return differentLanguage; }

    @Override
    public String toString() {
        return "QueryResult{seedSong='" + seedSong + "', seedArtist='"
                + seedArtist + "', sameArtistOnly=" + sameArtistOnly
                + ", differentLanguage=" + differentLanguage
                + ", " + weights + "}";
    }
}