package com.example.musicfinder;

/**
 * Lightweight container for enrichment data from the metadata dataset.
 * Holds only the fields we want to enrich existing songs with.
 */
public class SongMetadata {

    public final String genre;      // artist genre from Spotify
    public final int popularity;    // track popularity (-1 if unknown)

    public SongMetadata(String genre, int popularity) {
        this.genre      = genre;
        this.popularity = popularity;
    }
}