package com.ecs160;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import okhttp3.*;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Part D:
 *  - Read selected_repo.dat (repoId + list of .c files)
 *  - Load repo + issues from Redis (HW1 data)
 *  - Clone the repo using git
 *  - Microservice A on issues  -> IssueList1
 *  - Microservice B on C files -> IssueList2
 *  - Microservice C on both lists -> print common issues
 */
public class PartDRunner {

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)   // allow LLM to be slow
            .writeTimeout(10, TimeUnit.SECONDS)
            .build();

    private static final MediaType JSON
            = MediaType.get("application/json; charset=utf-8");

    private static final MediaType TEXT
            = MediaType.get("text/plain; charset=utf-8");

    private static final Path SELECTED_REPO_FILE = Paths.get("selected_repo.dat");
    private static final Path CLONE_ROOT = Paths.get("clones");

    private static class SelectedRepoConfig {
        final String repoName;      // e.g., "curl"
        final List<String> files;   // e.g., ["src/tool_operate.c", "lib/file.c", ...]

        SelectedRepoConfig(String repoName, List<String> files) {
            this.repoName = repoName;
            this.files = files;
        }
    }

    public static void main(String[] args) throws Exception {
        SelectedRepoConfig cfg = readSelectedRepoConfig();
        String repoName = cfg.repoName;
        List<String> filesToAnalyze = cfg.files;

        try (RedisLoader redis = new RedisLoader("localhost", 6379)) {
            // 1) Load repo hash from Redis
            Map<String, String> repoData = redis.getRepo(repoName);
            if (repoData.isEmpty()) {
                System.out.println("No data in Redis for repo: " + repoName);
                return;
            }

            System.out.println("=== Selected Repo: " + repoName + " ===");
            System.out.println("  Url       : " + repoData.get("Url"));
            System.out.println("  CreatedAt : " + repoData.get("CreatedAt"));
            System.out.println("  Author    : " + repoData.get("AuthorName"));

            String repoUrl = repoData.get("Url");

            // 2) Clone the repo (if not already cloned)
            Path cloneDir = CLONE_ROOT.resolve(repoName);
            cloneRepoIfNeeded(repoUrl, cloneDir);

            // 3) IssueList1: Microservice A on issues
            List<String> issueKeys = redis.getIssueKeysForRepo(repoName);
            System.out.println("  Issues in Redis: " + issueKeys.size());

            List<String> issueList1 = new ArrayList<>();

            for (String issueKey : issueKeys) {
                String description = redis.getIssueDescription(issueKey);
                if (description == null || description.isBlank()) {
                    continue;
                }

                System.out.println("\n  --- Issue " + issueKey + " ---");
                System.out.println("  Description (truncated): ");
                System.out.println("  " + truncate(description, 200));

                try {
                    String summaryJson = callSummarizeIssue("Issue from " + repoName, description);
                    System.out.println("  Microservice A response:");
                    System.out.println(summaryJson);
                    issueList1.add(summaryJson);
                } catch (SocketTimeoutException e) {
                    System.out.println("  Microservice A: <TIMED OUT>");
                } catch (IOException e) {
                    System.out.println("  Microservice A: <IO ERROR: " + e.getMessage() + ">");
                }
            }

            // 4) IssueList2: Microservice B on selected .c files
            List<String> issueList2 = new ArrayList<>();

            System.out.println("\n=== Analyzing C files with Microservice B ===");
            for (String relPath : filesToAnalyze) {
                Path filePath = cloneDir.resolve(relPath);
                if (!Files.exists(filePath)) {
                    System.out.println("  File not found (skipping): " + filePath);
                    continue;
                }

                System.out.println("  Analyzing file: " + filePath);

                String code = Files.readString(filePath, StandardCharsets.UTF_8);
                try {
                    String bugsJson = callFindBugs(code);
                    System.out.println("  Microservice B response:");
                    System.out.println(bugsJson);
                    issueList2.add(bugsJson);
                } catch (SocketTimeoutException e) {
                    System.out.println("  Microservice B: <TIMED OUT>");
                } catch (IOException e) {
                    System.out.println("  Microservice B: <IO ERROR: " + e.getMessage() + ">");
                }
            }

            // 5) Microservice C: send both lists and print common issues
            System.out.println("\n=== Microservice C: Common issues between IssueList1 and IssueList2 ===");
            try {
                String commonJson = callMatchIssues(issueList1, issueList2);
                System.out.println("  Microservice C response (common issues):");
                System.out.println(commonJson);

                // Optional: pretty print or parse the "common" array
                printCommonIssues(commonJson);
            } catch (SocketTimeoutException e) {
                System.out.println("  Microservice C: <TIMED OUT>");
            } catch (IOException e) {
                System.out.println("  Microservice C: <IO ERROR: " + e.getMessage() + ">");
            }
        }
    }

    // ========= Helpers ==========

    private static SelectedRepoConfig readSelectedRepoConfig() throws IOException {
        List<String> lines = Files.readAllLines(SELECTED_REPO_FILE, StandardCharsets.UTF_8);
        if (lines.isEmpty()) {
            throw new IllegalStateException("selected_repo.dat is empty");
        }
        String repoName = lines.get(0).trim();
        List<String> files = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (!line.isEmpty() && !line.startsWith("#")) {
                files.add(line);
            }
        }
        return new SelectedRepoConfig(repoName, files);
    }

    private static void cloneRepoIfNeeded(String repoUrl, Path cloneDir) throws IOException, InterruptedException {
        if (Files.exists(cloneDir)) {
            System.out.println("  Repo already cloned at: " + cloneDir);
            return;
        }

        Files.createDirectories(cloneDir.getParent());
        System.out.println("  Cloning repo from " + repoUrl + " into " + cloneDir + " ...");

        ProcessBuilder pb = new ProcessBuilder("git", "clone", "--depth", "1", repoUrl, cloneDir.toString());
        pb.inheritIO(); // show git output in console

        Process p = pb.start();
        int exitCode = p.waitFor();
        if (exitCode != 0) {
            throw new IOException("git clone failed with exit code " + exitCode);
        }
        System.out.println("  Clone completed.");
    }

    /**
     * Microservice A: summarize an issue.
     * POST /summarize_issue with JSON: {"title": "...", "body": "..."}
     */
    private static String callSummarizeIssue(String title, String body)
            throws IOException {

        // Limit body length to keep prompts manageable
        int maxLen = 1000;
        String trimmedBody = (body != null && body.length() > maxLen)
                ? body.substring(0, maxLen)
                : body;

        JsonObject json = new JsonObject();
        json.addProperty("title", title);
        json.addProperty("body", trimmedBody);

        Request request = new Request.Builder()
                .url("http://localhost:8080/summarize_issue")
                .post(RequestBody.create(json.toString(), JSON))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return "Error from Microservice A: HTTP " + response.code();
            }
            return response.body() != null ? response.body().string() : "";
        }
    }

    /**
     * Microservice B: given C source, return JSON describing bugs found.
     * Align this with your /find_bugs endpoint implementation.
     */
    private static String callFindBugs(String code) throws IOException {
        Request request = new Request.Builder()
                .url("http://localhost:8080/find_bugs")
                .post(RequestBody.create(code, TEXT))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return "Error from Microservice B: HTTP " + response.code();
            }
            return response.body() != null ? response.body().string() : "";
        }
    }

    /**
     * Microservice C: given IssueList1 + IssueList2, return common issues.
     * Expected payload (adjust to your actual Microservice C design):
     * {
     *   "issueList1": [...],
     *   "issueList2": [...]
     * }
     */
private static String callMatchIssues(List<String> issueList1, List<String> issueList2) throws IOException {
    JsonObject payload = new JsonObject();

    JsonArray arr1 = new JsonArray();
    for (String s : issueList1) {
        // store each as a JSON string value
        arr1.add(s);
    }

    JsonArray arr2 = new JsonArray();
    for (String s : issueList2) {
        arr2.add(s);
    }

    payload.add("issueList1", arr1);
    payload.add("issueList2", arr2);

    Request request = new Request.Builder()
            .url("http://localhost:8080/check_equivalence")  // <--- changed here
            .post(RequestBody.create(payload.toString(), JSON))
            .build();

    try (Response response = client.newCall(request).execute()) {
        if (!response.isSuccessful()) {
            return "Error from Microservice C: HTTP " + response.code();
        }
        return response.body() != null ? response.body().string() : "";
    }
}


    private static void printCommonIssues(String json) {
        try {
            JsonElement elem = com.google.gson.JsonParser.parseString(json);
            if (elem.isJsonObject()) {
                JsonObject obj = elem.getAsJsonObject();
                if (obj.has("commonIssues")) {
                    JsonArray arr = obj.getAsJsonArray("commonIssues");
                    System.out.println("  Common Issues (" + arr.size() + "):");
                    for (JsonElement e : arr) {
                        System.out.println("    - " + e.getAsString());
                    }
                    return;
                }
            }
            // Fallback: just print raw JSON
            System.out.println("  (Raw JSON) " + json);
        } catch (Exception e) {
            System.out.println("  Could not parse common issues JSON: " + e.getMessage());
            System.out.println("  Raw: " + json);
        }
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return "";
        if (s.length() <= maxLen) return s;
        return s.substring(0, maxLen) + "...";
    }
}
