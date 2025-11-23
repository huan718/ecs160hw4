package com.ecs160.hw;

import okhttp3.*;
import com.google.gson.*;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Part D driver:
 *  - Reads selected_repo.dat (repo id + list of C files)
 *  - Loads repo + issues from Redis (HW1 data)
 *  - Clones the repo locally (if needed)
 *  - Uses Microservice A to summarize GitHub issues  -> IssueList1
 *  - Uses Microservice B to find bugs in C files     -> IssueList2
 *  - Sends both lists to Microservice C and prints common issues
 *
 * IMPORTANT: No GitHub API calls here, only Redis + local microservices.
 */
public class App {

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)   // allow LLM to be slow
            .writeTimeout(10, TimeUnit.SECONDS)
            .build();

    private static final MediaType JSON =
            MediaType.get("application/json; charset=utf-8");
    private static final MediaType PLAIN =
            MediaType.get("text/plain; charset=utf-8");

    // Simple holder for data from selected_repo.dat
    private static class SelectedRepoConfig {
        final String repoName;
        final List<String> cFiles;

        SelectedRepoConfig(String repoName, List<String> cFiles) {
            this.repoName = repoName;
            this.cFiles = cFiles;
        }
    }

    public static void main(String[] args) throws Exception {

        // 1. Read selected_repo.dat
        SelectedRepoConfig cfg = loadSelectedRepoConfig();
        if (cfg == null) {
            System.err.println("[ERROR] Could not read selected_repo.dat – exiting Part D.");
            return;
        }

        String repoName = cfg.repoName;

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
            String repoUrl = repoData.get("Url");
            Path repoDir = ensureRepoCloned(repoName, repoUrl);

            // 4. Build IssueList1 via Microservice A (summarize_issue)
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
                    String llmResponse = callSummarizeIssue(
                            "Issue from " + repoName,
                            description
                    );

                    System.out.println("  LLM response:");
                    System.out.println(llmResponse);

                    // Try to treat Microservice A response as a JSON "Issue"
                    try {
                        JsonElement elem = JsonParser.parseString(llmResponse);
                        if (elem.isJsonObject()) {
                            issueList1.add(elem.getAsJsonObject());
                        }
                    } catch (JsonSyntaxException e) {
                        System.out.println("  [WARN] summarize_issue returned non-JSON, skipping for IssueList1.");
                    }
                } catch (SocketTimeoutException e) {
                    System.out.println("  LLM response: <TIMED OUT>");
                } catch (IOException e) {
                    System.out.println("  LLM response: <IO ERROR: " + e.getMessage() + ">");
                }
            }

            // 5. Build IssueList2 via Microservice B (find_bugs) on the selected .c files
            JsonArray issueList2 = new JsonArray();

            System.out.println("\n=== Analyzing C files with Microservice B ===");
            for (String relPath : cfg.cFiles) {
                Path cFilePath = repoDir.resolve(relPath);
                if (!Files.exists(cFilePath)) {
                    System.out.println("  [WARN] C file not found: " + cFilePath);
                    continue;
                }

                String cSource = Files.readString(cFilePath);
                System.out.println("  Analyzing file: " + relPath);

                try {
                    String bugResponse = callFindBugs(cSource);
                    System.out.println("  Microservice B response:");
                    System.out.println(bugResponse);

                    // Microservice B is supposed to return a list of JSON Issues.
                    // Try to parse and add them into IssueList2.
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
                            issueList2.add(elem.getAsJsonObject());
                        }
                    } catch (JsonSyntaxException e) {
                        System.out.println("  [WARN] find_bugs returned non-JSON, skipping for IssueList2.");
                    }

                } catch (SocketTimeoutException e) {
                    System.out.println("  [WARN] find_bugs TIMED OUT for file " + relPath);
                } catch (IOException e) {
                    System.out.println("  [WARN] IO ERROR calling find_bugs for file " + relPath + ": " + e.getMessage());
                }
            }

            // 6. Call Microservice C (check_equivalence) with IssueList1 + IssueList2
            System.out.println("\n=== Microservice C: Common issues between IssueList1 and IssueList2 ===");

            JsonObject payload = new JsonObject();
            payload.add("issues1", issueList1);
            payload.add("issues2", issueList2);

            try {
                String commonResponse = callCheckEquivalence(payload.toString());
                System.out.println("  Microservice C response (common issues):");
                System.out.println(commonResponse);
            } catch (IOException e) {
                System.out.println("  [ERROR] Failed to call check_equivalence: " + e.getMessage());
            }
        }
        client.connectionPool().evictAll();
         client.dispatcher().executorService().shutdown();

        if (client.cache() != null) {
            client.cache().close();
}

    }

    /**
     * Loads repo id + C file list from selected_repo.dat.
     * Tries a few likely locations:
     *   ./selected_repo.dat
     *   ./main-app/src/main/java/com/ecs160/hw/selected_repo.dat
     *   ./main-app/src/main/java/com/ecs160/hw/resources/selected_repo.dat
     */
    private static SelectedRepoConfig loadSelectedRepoConfig() {
    List<Path> candidates = List.of(
            // if working dir is repo root:
            Paths.get("selected_repo.dat"),
            Paths.get("main-app/src/main/java/com/ecs160/hw/resources/selected_repo.dat"),
            // if working dir is main-app:
            Paths.get("src/main/java/com/ecs160/hw/resources/selected_repo.dat")
    );

    Path found = null;
    for (Path p : candidates) {
        if (Files.exists(p)) {
            found = p;
            break;
        }
    }

    if (found == null) {
        System.err.println("[ERROR] selected_repo.dat not found at:");
        for (Path p : candidates) {
            System.err.println("  " + p.toString());
        }
        System.err.println("[ERROR] Could not read selected_repo.dat – exiting Part D.");
        return null;
    }

    try {
        List<String> lines = Files.readAllLines(found);
        List<String> cleaned = new ArrayList<>();
        for (String line : lines) {
            String t = line.trim();
            if (!t.isEmpty() && !t.startsWith("#")) {
                cleaned.add(t);
            }
        }

        if (cleaned.isEmpty()) {
            System.err.println("[ERROR] selected_repo.dat is empty.");
            return null;
        }

        String repoName = cleaned.get(0);
        List<String> cFiles = cleaned.subList(1, cleaned.size());

        System.out.println("[INFO] Loaded selected_repo.dat from " + found
                + ": repo=" + repoName + ", files=" + cFiles);
        return new SelectedRepoConfig(repoName, new ArrayList<>(cFiles));
    } catch (IOException e) {
        System.err.println("[ERROR] Failed to read selected_repo.dat: " + e.getMessage());
        return null;
    }
}


    /**
     * Ensures the repo is cloned under ./clones/<repoName>.
     * If it already exists, we just reuse it.
     */
    private static Path ensureRepoCloned(String repoName, String url)
            throws IOException, InterruptedException {

        Path clonesDir = Paths.get("clones");
        if (!Files.exists(clonesDir)) {
            Files.createDirectories(clonesDir);
        }

        Path repoDir = clonesDir.resolve(repoName);

        // If .git exists, assume it's already cloned
        if (Files.exists(repoDir.resolve(".git"))) {
            System.out.println("[INFO] Using existing clone at " + repoDir);
            return repoDir;
        }

        if (Files.exists(repoDir)) {
            System.out.println("[INFO] Directory " + repoDir + " exists but no .git – reusing anyway.");
            return repoDir;
        }

        System.out.println("[INFO] Cloning repo from " + url + " into " + repoDir);

        ProcessBuilder pb = new ProcessBuilder(
                "git", "clone", "--depth", "1", url, repoDir.toString());
        pb.inheritIO();
        Process proc = pb.start();
        int exitCode = proc.waitFor();
        if (exitCode != 0) {
            System.err.println("[WARN] git clone exited with code " + exitCode);
        }

        return repoDir;
    }

    /**
     * Calls your HW2 microservice /summarize_issue endpoint (Microservice A).
     */
    private static String callSummarizeIssue(String title, String body)
            throws IOException {

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
                return "Error from LLM service: HTTP " + response.code();
            }
            return response.body() != null ? response.body().string() : "";
        }
    }

    /**
     * Calls Microservice B /find_bugs with C source code as plain text.
     */
    private static String callFindBugs(String cSource) throws IOException {
        Request request = new Request.Builder()
                .url("http://localhost:8080/find_bugs")
                .post(RequestBody.create(cSource, PLAIN))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return "Error from Bug Finder service: HTTP " + response.code();
            }
            return response.body() != null ? response.body().string() : "";
        }
    }

    /**
     * Calls Microservice C /check_equivalence with IssueList1 + IssueList2.
     */
    private static String callCheckEquivalence(String payloadJson) throws IOException {
        Request request = new Request.Builder()
                .url("http://localhost:8080/check_equivalence")
                .post(RequestBody.create(payloadJson, JSON))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return "Error from Issue Comparator service: HTTP " + response.code();
            }
            return response.body() != null ? response.body().string() : "";
        }
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return "";
        if (s.length() <= maxLen) return s;
        return s.substring(0, maxLen) + "...";
    }
}
