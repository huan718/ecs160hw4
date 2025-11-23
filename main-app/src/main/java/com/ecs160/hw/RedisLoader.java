package com.ecs160.hw;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import redis.clients.jedis.Jedis;

public class RedisLoader implements AutoCloseable {
    private final Jedis jedis;

    public RedisLoader(String host, int port) {
        this.jedis = new Jedis(host, port);
    }

    /**
     * Protected no-arg constructor for testing subclasses.
     * Prevents real Jedis connection initialization.
     */
    protected RedisLoader() {
        this.jedis = null;
    }

    /**
     * Protected method acting as a "Seam" for testing.
     * Tests can override this to return mock data without Mockito.
     */
    protected Map<String, String> fetchMapFromRedis(String key) {
        return jedis.hgetAll(key);
    }

    public Map<String, String> getRepo(String repoName) {
        String key = "repo-" + repoName;
        // MUST call the protected method, not jedis directly
        return fetchMapFromRedis(key);
    }

    public List<String> getIssueKeysForRepo(String repoName) {
        Map<String, String> repoData = getRepo(repoName);
        if (repoData == null || repoData.isEmpty()) return List.of();

        String issuesField = repoData.get("Issues");
        if (issuesField == null || issuesField.isBlank()) return List.of();

        return Arrays.stream(issuesField.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    public Map<String, String> getIssue(String issueKey) {
        return fetchMapFromRedis(issueKey);
    }

    public String getIssueDescription(String issueKey) {
        Map<String, String> issueData = getIssue(issueKey);
        // Handle null if the mock returns null
        if (issueData == null) return ""; 
        return issueData.getOrDefault("Description", "");
    }

    @Override
    public void close() {
        if (jedis != null) {
            jedis.close();
        }
    }
}