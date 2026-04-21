package com.example.musicfinder;

/**
 * Holds the full output of QueryUnderstanding.interpretQuery().
 * Bundles the weight vector and seed song info together so
 * AppLoader only needs to make one call to get everything.
 */
public class QueryResult {

    private WeightVector weights;
    private String seedSong;
    private String seedArtist;

    public QueryResult(WeightVector weights, String seedSong, String seedArtist) {
        this.weights    = weights;
        this.seedSong   = seedSong;
        this.seedArtist = seedArtist;
    }

    public WeightVector getWeights()   { return weights; }
    public String getSeedSong()        { return seedSong; }
    public String getSeedArtist()      { return seedArtist; }

    @Override
    public String toString() {
        return "QueryResult{seedSong='" + seedSong + "', seedArtist='" +
                seedArtist + "', " + weights + "}";
    }
}