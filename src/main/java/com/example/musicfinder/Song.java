package com.example.musicfinder;

public class Song
{
        // Identity
        private String trackId;
        private String trackName;
        private String artists;
        private String albumName;
        private String genre;

        // Popularity
        private int popularity;

        // Audio features (all 0.0 – 1.0 scale unless noted)
        private double energy;
        private double valence;       // musical positivity
        private double danceability;
        private double acousticness;
        private double instrumentalness;
        private double liveness;
        private double speechiness;
        private double loudness;      // in decibels, typically -60 to 0
        private double tempo;         // in BPM

        // Misc
        private int durationMs;
        private boolean explicit;
        private int key;
        private int timeSignature;
        private int mode;

        public Song(String trackId, String trackName, String artists, String albumName,
                    String genre, int popularity, double energy, double valence,
                    double danceability, double acousticness, double instrumentalness,
                    double liveness, double speechiness, double loudness, double tempo,
                    int durationMs, boolean explicit, int key, int timeSignature, int mode) {
                this.trackId = trackId;
                this.trackName = trackName;
                this.artists = artists;
                this.albumName = albumName;
                this.genre = genre;
                this.popularity = popularity;
                this.energy = energy;
                this.valence = valence;
                this.danceability = danceability;
                this.acousticness = acousticness;
                this.instrumentalness = instrumentalness;
                this.liveness = liveness;
                this.speechiness = speechiness;
                this.loudness = loudness;
                this.tempo = tempo;
                this.durationMs = durationMs;
                this.explicit = explicit;
                this.key = key;
                this.timeSignature = timeSignature;
                this.mode = mode;
                // ... assign the rest
        }

        public double getAcousticness() {
                return acousticness;
        }

        public double getDanceability() {
                return danceability;
        }

        public double getEnergy() {
                return energy;
        }

        public double getInstrumentalness() {
                return instrumentalness;
        }

        public double getLiveness() {
                return liveness;
        }

        public double getLoudness() {
                return loudness;
        }

        public double getSpeechiness() {
                return speechiness;
        }

        public double getTempo() {
                return tempo;
        }

        public double getValence() {
                return valence;
        }

        public int getDurationMs() {
                return durationMs;
        }

        public int getKey() {
                return key;
        }

        public int getPopularity() {
                return popularity;
        }

        public int getTimeSignature() {
                return timeSignature;
        }

        public String getAlbumName() {
                return albumName;
        }

        public String getArtists() {
                return artists;
        }

        public String getGenre() {
                return genre;
        }

        public String getTrackId() {
                return trackId;
        }

        public String getTrackName() {
                return trackName;
        }

        public double[] toFeatureVector() {
                return new double[]{
                        energy         * 1.5,  // boosted — strong mood indicator
                        valence        * 1.5,  // boosted — strong mood indicator
                        danceability   * 1.0,  // normal weight
                        acousticness   * 1.3,  // slightly boosted — important for texture
                        instrumentalness * 0.8, // slightly reduced
                        liveness       * 0.5,  // reduced — rarely query-relevant
                        speechiness    * 0.5,  // reduced — rarely query-relevant
                        (loudness + 60) / 60.0 * 1.0,
                        tempo / 250.0  * 1.0
                };
        }
        public String toString() {
                return String.format("\"%s\" by %s | Genre: %s | Popularity: %d | Energy: %.2f | Valence: %.2f",
                        trackName, artists, genre, popularity, energy, valence);
        }
}
