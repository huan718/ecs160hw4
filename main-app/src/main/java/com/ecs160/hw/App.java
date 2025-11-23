package com.ecs160.hw;

import com.google.gson.*;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.file.*;
import java.util.List;
import java.util.Map;

/**
 * Part D driver:
 *  - Uses SelectedRepoConfigLoader to read selected_repo.dat
 *  - Uses RepoMetadataRepository/RedisLoader to get repo + issues from Redis
 *  - Uses GitRepositoryCloner to ensure a local clone
 *  - Uses MicroserviceClient to call Microservice A, B, C
 */
public class App {

    public static void main(String[] args) throws Exception {

        // 1. Read selected_repo.dat via dedicated loader
        SelectedRepoConfigLoader configLoader = new SelectedRepoConfigLoader();
        SelectedRepoConfig cfg;
        try {
            cfg = configLoader.load();
        } catch (IOException e) {
            System.err.println("[ERROR] Could not read selected_repo.dat – exiting Part D.");
            System.err.println("        " + e.getMessage());
            return;
        }

        String repoName = cfg.getRepoName();

        try (RedisLoader redis = new RedisLoader("localhost", 6379)) {

            // 2. Load repo metadata + issues from Redis
            Map<String, String> repoData = redis.getRepo(repoName);
            if (repoData.isEmpty()) {
                System.out.println("=== Repo: " + repoName + " ===");
                System.out.println("  (No data in Redis for this repo)");
                return;
            }

            System.out.println("=== Repo: " + repoName + " ===");
            System.out.println("  Url       : " + repoData.get("Url"));
            System.out.println("  CreatedAt : " + repoData.get("CreatedAt"));
            System.out.println("  Author    : " + repoData.get("AuthorName"));

            // 3. Clone repo (or reuse if already cloned)
            GitRepositoryCloner cloner = new GitRepositoryCloner();
            String repoUrl = repoData.get("Url");
            Path repoDir = cloner.ensureRepoCloned(repoName, repoUrl);

            // 4. Microservice HTTP client (A, B, C)
            MicroserviceClient micro = new MicroserviceClient("http://localhost:8080");

            // === IssueList1 via Microservice A (summarize_issue) ===
            List<String> issueKeys = redis.getIssueKeysForRepo(repoName);
            System.out.println("  Issues    : " + issueKeys.size());

            JsonArray issueList1 = new JsonArray();

            for (String issueKey : issueKeys) {
                String description = redis.getIssueDescription(issueKey);
                if (description == null || description.isBlank()) {
                    continue;
                }

                System.out.println("\n  --- Issue " + issueKey + " ---");
                System.out.println("  Description (truncated): ");
                System.out.println("  " + truncate(description, 200));

                try {
                    // for simplicity we use the key as the "title"
                    String llmResponse = micro.summarizeIssue(issueKey, description);
                    System.out.println("  LLM summary raw response:");
                    System.out.println(llmResponse);

                    // Try to parse as JSON and add to IssueList1 (skip error objects)
                    try {
                        JsonElement elem = JsonParser.parseString(llmResponse);
                        if (elem.isJsonObject()) {
                            JsonObject obj = elem.getAsJsonObject();
                            if (obj.has("error")) {
                                System.out.println("  [WARN] Microservice A error: "
                                        + obj.get("error").getAsString());
                            } else {
                                issueList1.add(obj);
                            }
                        }
                    } catch (JsonSyntaxException e) {
                        System.out.println("  [WARN] summarize_issue returned non-JSON, skipping for IssueList1.");
                    }
                } catch (SocketTimeoutException e) {
                    System.out.println("  LLM response: <TIMED OUT>");
                } catch (IOException e) {
                    System.out.println("  LLM response: <IO ERROR: " + e.getMessage() + ">");
                } catch (Exception e) {
                    System.out.println("  LLM response: <ERROR: " + e.getMessage() + ">");
                }
            }

            JsonArray issueList2 = new JsonArray();

            System.out.println("\n=== Analyzing C files with Microservice B ===");
            for (String relPath : cfg.getCFiles()) {
                Path cFilePath = repoDir.resolve(relPath);
                if (!Files.exists(cFilePath)) {
                    System.out.println("  [WARN] C file not found: " + cFilePath);
                    continue;
                }

                String cSource = Files.readString(cFilePath);
                System.out.println("  Analyzing file: " + relPath);

                try {
                    String bugResponse = micro.findBugs(cSource);
                    System.out.println("  Microservice B response:");
                    System.out.println(bugResponse);

                    try {
                        JsonElement elem = JsonParser.parseString(bugResponse);
                        if (elem.isJsonArray()) {
                            JsonArray arr = elem.getAsJsonArray();
                            for (JsonElement e : arr) {
                                if (e.isJsonObject()) {
                                    issueList2.add(e.getAsJsonObject());
                                }
                            }
                        } else if (elem.isJsonObject()) {
                            JsonObject obj = elem.getAsJsonObject();
                            if (obj.has("error")) {
                                System.out.println("  [WARN] Microservice B error: "
                                        + obj.get("error").getAsString());
                            } else {
                                issueList2.add(obj);
                            }
                        }
                    } catch (JsonSyntaxException e) {
                        System.out.println("  [WARN] find_bugs returned non-JSON, skipping for IssueList2.");
                    }

                } catch (SocketTimeoutException e) {
                    System.out.println("  [WARN] find_bugs TIMED OUT for file " + relPath);
                } catch (IOException e) {
                    System.out.println("  [WARN] IO ERROR calling find_bugs for file "
                            + relPath + ": " + e.getMessage());
                } catch (Exception e) {
                    System.out.println("  [WARN] ERROR calling find_bugs for file "
                            + relPath + ": " + e.getMessage());
                }
            }

            System.out.println("\n=== Microservice C: Common issues between IssueList1 and IssueList2 ===");

            try {
                String commonResponse = micro.checkEquivalence(issueList1, issueList2);
                System.out.println("  Microservice C response (common issues):");
                System.out.println(commonResponse);
            } catch (IOException e) {
                System.out.println("  [ERROR] Failed to call check_equivalence: " + e.getMessage());
            } catch (Exception e) {
                System.out.println("  [ERROR] Unexpected error calling check_equivalence: " + e.getMessage());
            }
        }
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return "";
        if (s.length() <= maxLen) return s;
        return s.substring(0, maxLen) + "...";
    }
}
