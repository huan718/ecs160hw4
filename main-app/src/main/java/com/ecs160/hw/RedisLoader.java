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

    public Map<String, String> getRepo(String repoName) {
        String key = "repo-" + repoName;
        return jedis.hgetAll(key);
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
        return jedis.hgetAll(issueKey);
    }

    public String getIssueDescription(String issueKey) {
        Map<String, String> issueData = getIssue(issueKey);
        return issueData.getOrDefault("Description", "");
    }

    @Override
    public void close() {
        jedis.close();
    }
}
