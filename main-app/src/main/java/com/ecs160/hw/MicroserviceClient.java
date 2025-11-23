package com.ecs160.hw;

import okhttp3.*;
import com.google.gson.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class MicroserviceClient {

    private final OkHttpClient client;
    private final String baseUrl;

    private static final MediaType JSON =
            MediaType.get("application/json; charset=utf-8");
    private static final MediaType PLAIN =
            MediaType.get("text/plain; charset=utf-8");

    public MicroserviceClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)   // allow LLM to be slow
                .writeTimeout(10, TimeUnit.SECONDS)
                .build();
    }

    public String summarizeIssue(String title, String body) throws IOException {
        int maxLen = 1000;
        String trimmedBody = (body != null && body.length() > maxLen)
                ? body.substring(0, maxLen)
                : body;

        JsonObject json = new JsonObject();
        json.addProperty("title", title);
        json.addProperty("body", trimmedBody);

        Request request = new Request.Builder()
                .url(baseUrl + "/summarize_issue")
                .post(RequestBody.create(json.toString(), JSON))
                .build();

        return execute(request);
    }

    /**
     * Microservice B: /find_bugs
     * We truncate large C files to avoid enormous prompts that cause timeouts.
     */
    public String findBugs(String cSource) throws IOException {
        int maxLen = 4000;  // keep prompt manageable
        String trimmed = (cSource != null && cSource.length() > maxLen)
                ? cSource.substring(0, maxLen)
                : cSource;

        Request request = new Request.Builder()
                .url(baseUrl + "/find_bugs")
                .post(RequestBody.create(trimmed, PLAIN))
                .build();

        return execute(request);
    }

    public String checkEquivalence(JsonArray issues1, JsonArray issues2) throws IOException {
        JsonObject payload = new JsonObject();
        payload.add("issues1", issues1);
        payload.add("issues2", issues2);

        Request request = new Request.Builder()
                .url(baseUrl + "/check_equivalence")
                .post(RequestBody.create(payload.toString(), JSON))
                .build();

        return execute(request);
    }

    private String execute(Request request) throws IOException {
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return "Error from microservice: HTTP " + response.code();
            }
            return response.body() != null ? response.body().string() : "";
        }
    }
}
