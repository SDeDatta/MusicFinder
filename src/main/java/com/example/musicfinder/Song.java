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
        /**
         * Makes a best-guess at the song's language based on its genre tag.
         * This is imperfect but practical given the dataset doesn't have
         * an explicit language column.
         */
        public String inferredLanguage() {
                String g = genre.toLowerCase();

                // East Asian
                if (g.contains("cantopop") || g.contains("mandopop")
                        || g.contains("chinese") || g.contains("c-pop"))
                        return "zh";
                if (g.contains("j-pop") || g.contains("j-rock")
                        || g.contains("anime") || g.contains("japanese"))
                        return "ja";
                if (g.contains("k-pop") || g.contains("korean")
                        || g.contains("k-rap") || g.contains("k-indie"))
                        return "ko";

                // Southeast Asian — ADD THESE
                if (g.contains("opm") || g.contains("filipino")
                        || g.contains("pilipino") || g.contains("tagalog")
                        || g.contains("p-pop") || g.contains("pinoy"))
                        return "tl"; // Tagalog/Filipino
                if (g.contains("thai") || g.contains("t-pop"))
                        return "th";
                if (g.contains("vietnamese") || g.contains("v-pop"))
                        return "vi";
                if (g.contains("indonesian") || g.contains("dangdut"))
                        return "id";
                if (g.contains("malay") || g.contains("malaysian"))
                        return "ms";

                // South Asian
                if (g.contains("bollywood") || g.contains("filmi")
                        || g.contains("hindi") || g.contains("punjabi")
                        || g.contains("indian") || g.contains("desi"))
                        return "hi";
                if (g.contains("tamil") || g.contains("telugu")
                        || g.contains("kollywood") || g.contains("tollywood"))
                        return "ta";

                // Romance languages
                if (g.contains("latin") || g.contains("reggaeton")
                        || g.contains("salsa") || g.contains("bachata")
                        || g.contains("flamenco") || g.contains("spanish")
                        || g.contains("cumbia") || g.contains("tango")
                        || g.contains("mariachi") || g.contains("norteno")
                        || g.contains("grupero") || g.contains("banda"))
                        return "es";
                if (g.contains("mpb") || g.contains("sertanejo")
                        || g.contains("pagode") || g.contains("forro")
                        || g.contains("bossa") || g.contains("axe")
                        || g.contains("baile funk") || g.contains("portuguese"))
                        return "pt";
                if (g.contains("french") || g.contains("chanson")
                        || g.contains("variete"))
                        return "fr";

                // Germanic
                if (g.contains("german") || g.contains("schlager")
                        || g.contains("volksmusik"))
                        return "de";

                // Middle Eastern / Turkish
                if (g.contains("turkish") || g.contains("arabesque")
                        || g.contains("turk"))
                        return "tr";
                if (g.contains("arabic") || g.contains("khaleeji")
                        || g.contains("shaabi"))
                        return "ar";

                if (trackName != null) {
                        if (trackName.matches(".*[\\u3000-\\u9FFF\\uF900-\\uFAFF].*"))
                                return "ja"; // Japanese/Chinese characters
                        if (trackName.matches(".*[\\uAC00-\\uD7FF].*"))
                                return "ko"; // Korean hangul
                        if (trackName.matches(".*[\\u0400-\\u04FF].*"))
                                return "ru"; // Cyrillic
                        if (trackName.matches(".*[\\u0600-\\u06FF].*"))
                                return "ar"; // Arabic
                        if (trackName.matches(".*[\\u0900-\\u097F].*"))
                                return "hi"; // Devanagari (Hindi)
                        if (trackName.matches(".*[\\u0E00-\\u0E7F].*"))
                                return "th"; // Thai
                }

                // Default English
                return "en";
        }
        public String toString() {
                return String.format("\"%s\" by %s | Genre: %s | Popularity: %d | Energy: %.2f | Valence: %.2f",
                        trackName, artists, genre, popularity, energy, valence);
        }

        public void setGenre(String genre) {
                this.genre = genre;
        }

        public void setPopularity(int popularity) {
                this.popularity = popularity;
        }
}
