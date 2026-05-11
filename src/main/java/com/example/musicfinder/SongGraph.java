package com.example.musicfinder;

import java.util.*;

public class SongGraph {

    // Adjacency list — maps each song's trackId to a list of similar song trackIds
    // This is the core graph structure
    private Map<String, List<String>> adjacencyList;

    // Reference to the song map for quick Song object lookup by trackId
    private Map<String, Song> songMap;
    private Set<String> builtGenres = new HashSet<>();

    public SongGraph(Map<String, Song> songMap) {
        this.songMap = songMap;
        this.adjacencyList = new HashMap<>();
    }

    /**
     * Builds edges between songs whose combined similarity score
     * exceeds the given threshold. Run once at startup.
     * Only compares songs within the same genre to keep it efficient.
     */
    public void buildGraph(List<Song> songs, double similarityThreshold) {

        // Group songs by genre first — this avoids comparing every song
        // to every other song (which would be 89,741 x 89,741 comparisons)
        // Instead we only compare songs within the same genre
        Map<String, List<Song>> songsByGenre = new HashMap<>();
        for (Song song : songs) {
            String genre = song.getGenre().toLowerCase().trim();
            // computeIfAbsent creates a new list for this genre if one doesn't exist yet
            songsByGenre.computeIfAbsent(genre, k -> new ArrayList<>()).add(song);
        }

        //System.out.println("Building graph across " + songsByGenre.size() + " genres...");

        int totalEdges = 0;
        int genresProcessed = 0;

        // Process each genre group separately
        for (Map.Entry<String, List<Song>> entry : songsByGenre.entrySet()) {
            String genre = entry.getKey();
            List<Song> genreSongs = entry.getValue();

            // Compare every song in this genre against every other song in the same genre
            // i+1 avoids comparing a song to itself and avoids duplicate comparisons
            for (int i = 0; i < genreSongs.size(); i++) {
                Song songA = genreSongs.get(i);

                for (int j = i + 1; j < genreSongs.size(); j++) {
                    Song songB = genreSongs.get(j);

                    // Compute audio feature similarity
                    double audioScore = SimilarityFinder.cosineSimilarity(
                            songA.toFeatureVector(),
                            songB.toFeatureVector()
                    );

                    // Compute genre similarity (will always be 1.0 here since
                    // we're within the same genre, but kept for consistency)
                    double genreScore = SimilarityFinder.genreSimilarity(songA, songB);

                    // Blend into final score using same weights as SimilarityFinder
                    double finalScore = (0.9 * audioScore) + (0.1 * genreScore);

                    // Only create an edge if similarity exceeds threshold
                    if (finalScore >= similarityThreshold) {
                        addEdge(songA.getTrackId(), songB.getTrackId());
                        addEdge(songB.getTrackId(), songA.getTrackId());
                        totalEdges++;
                    }
                }
            }

            genresProcessed++;
            // Print progress every 10 genres so you know it's still running
            /*if (genresProcessed % 10 == 0) {
                System.out.println("Processed " + genresProcessed +
                        " / " + songsByGenre.size() + " genres | " +
                        "Edges so far: " + totalEdges);
            }*/
        }

        /*System.out.println("Graph complete. Total edges: " + totalEdges);
        System.out.println("Total nodes with connections: " + adjacencyList.size());*/
    }
    public void buildForGenre(String genre, List<Song> allSongs, double similarityThreshold) {
        String genreKey = genre.toLowerCase().trim();

        // If this genre has already been built, do nothing
        if (builtGenres.contains(genreKey)) {
            return;
        }

        System.out.println("Building graph for genre: " + genreKey);

        List<Song> genreSongs = new ArrayList<>();

        for (Song s : allSongs) {
            if (s.getGenre().toLowerCase().trim().equals(genreKey)) {
                genreSongs.add(s);
            }
        }

        for (int i = 0; i < genreSongs.size(); i++) {
            Song songA = genreSongs.get(i);

            for (int j = i + 1; j < genreSongs.size(); j++) {
                Song songB = genreSongs.get(j);

                double audioScore = SimilarityFinder.cosineSimilarity(
                        songA.toFeatureVector(),
                        songB.toFeatureVector()
                );

                double genreScore = SimilarityFinder.genreSimilarity(songA, songB);

                double finalScore = (0.9 * audioScore) + (0.1 * genreScore);

                if (finalScore >= similarityThreshold) {
                    addEdge(songA.getTrackId(), songB.getTrackId());
                    addEdge(songB.getTrackId(), songA.getTrackId());
                }
            }
        }

        builtGenres.add(genreKey);

        System.out.println("Finished building genre: " + genreKey
                + " (" + genreSongs.size() + " songs)");
    }

    /**
     * Adds a directed edge from one song to another in the adjacency list
     */
    private void addEdge(String fromId, String toId) {
        adjacencyList.computeIfAbsent(fromId, k -> new ArrayList<>()).add(toId);
    }

    /**
     * BFS traversal from a seed song outward through the graph.
     * Returns a pool of candidate songs within maxDepth hops of the seed.
     * This candidate pool then gets ranked by SimilarityFinder.
     */
    public List<Song> bfsTraversal(String seedId, int maxDepth, int maxCandidates) {

        List<Song> candidates = new ArrayList<>();

        // Track visited song IDs to avoid revisiting nodes
        Set<String> visited = new HashSet<>();
        Set<String> seenCandidateIds = new HashSet<>();

        // Queue holds pairs of [trackId, currentDepth]
        // Each entry is a song ID and how many hops away from seed it is
        Queue<String[]> queue = new LinkedList<>();

        // Start BFS from the seed song
        queue.offer(new String[]{seedId, "0"});
        visited.add(seedId);

        while (!queue.isEmpty() && candidates.size() < maxCandidates) {

            String[] current = queue.poll();
            String currentId = current[0];
            int currentDepth = Integer.parseInt(current[1]);

            // Don't add the seed song itself to candidates
            if (!currentId.equals(seedId)) {
                Song currentSong = songMap.get(currentId);
                if (currentSong != null && !seenCandidateIds.contains(currentSong.getTrackId())) {
                    seenCandidateIds.add(currentSong.getTrackId());
                    candidates.add(currentSong);
                }
            }

            // Stop going deeper if we've hit the max depth
            if (currentDepth >= maxDepth) continue;

            // Get all neighbors of the current song
            List<String> neighbors = adjacencyList.getOrDefault(currentId, new ArrayList<>());

            for (String neighborId : neighbors) {
                // Only visit each song once
                if (!visited.contains(neighborId)) {
                    visited.add(neighborId);
                    queue.offer(new String[]{neighborId, String.valueOf(currentDepth + 1)});
                }
            }
        }

        return candidates;
    }

    /**
     * Returns the direct neighbors of a song (depth 1 only)
     * Useful for quick lookups without full BFS
     */
    public List<Song> getNeighbors(String trackId) {
        List<String> neighborIds = adjacencyList.getOrDefault(trackId, new ArrayList<>());
        List<Song> neighbors = new ArrayList<>();
        for (String id : neighborIds) {
            if (songMap.containsKey(id)) {
                neighbors.add(songMap.get(id));
            }
        }
        return neighbors;
    }

    /**
     * Returns how many edges a song has — useful for debugging
     * A song with 0 neighbors wasn't connected to anything above the threshold
     */
    public int getDegree(String trackId) {
        return adjacencyList.getOrDefault(trackId, new ArrayList<>()).size();
    }

    /**
     * Returns total number of songs that have at least one edge
     */
    public int getNodeCount() {
        return adjacencyList.size();
    }

    /**
     * Returns total number of directed edges in the graph
     */
    public int getEdgeCount() {
        return adjacencyList.values().stream().mapToInt(List::size).sum();
    }
}
