package com.ecs160.microservices;

import com.ecs160.annotations.Microservice;
import com.ecs160.annotations.Endpoint;
import com.ecs160.clients.AIClient;
import com.google.gson.Gson;
import com.google.gson.JsonArray;

@Microservice
public class BugFinderMicroservice {

    private final AIClient client;
    private final Gson gson = new Gson();

    public BugFinderMicroservice(AIClient client) {
        this.client = client;
    }

    @Endpoint(url = "/find_bugs")
    public String findBugs(String cSource) {
        try {
            if (cSource == null || cSource.isBlank()) {
                return "{\"error\":\"Empty C source\"}";
            }

            // Prompt the model to return ONLY JSON
            String prompt =
                    "You are a static analysis assistant for C code.\n" +
                    "You will be given the full C source file.\n\n" +
                    "Your job is to identify potential bugs or issues in the code.\n\n" +
                    "Respond ONLY with a JSON array. Each element must be an object with keys " +
                    "{\"title\", \"body\"}:\n" +
                    "  - \"title\": one short phrase naming the bug (e.g., \"Null pointer dereference\").\n" +
                    "  - \"body\": 1–3 sentences explaining why this is a bug, referencing specific functions/lines.\n\n" +
                    "If you do not see any bugs, respond with an empty JSON array: []\n\n" +
                    "CRITICAL RULES:\n" +
                    "  - DO NOT include any explanation, commentary, or markdown outside the JSON.\n" +
                    "  - DO NOT use code fences.\n" +
                    "  - DO NOT include a top-level object; the top-level must be a JSON array.\n\n" +
                    "Here is the C source code:\n\n" +
                    cSource;

            String raw = client.ask(prompt);

            if (raw == null || raw.isBlank()) {
                return "{\"error\":\"LLM returned null or blank\"}";
            }

            // Extract the FIRST JSON array from the response
            int start = raw.indexOf('[');
            int end = raw.lastIndexOf(']');

            if (start == -1 || end == -1 || start >= end) {
                // The model didn't give us any [ ... ], so we can't parse JSON
                return "{\"error\":\"No JSON found\"}";
            }

            String json = raw.substring(start, end + 1).trim();

            // Validate that it's a JSON array
            try {
                gson.fromJson(json, JsonArray.class);
            } catch (Exception e) {
                return "{\"error\":\"Invalid JSON returned\"}";
            }

            return json;
        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg == null || msg.isBlank()) {
                msg = e.getClass().getSimpleName();
            }
            // Avoid breaking JSON if message has quotes
            msg = msg.replace("\"", "'");
            return "{\"error\":\"" + msg + "\"}";
        }
    }
}
