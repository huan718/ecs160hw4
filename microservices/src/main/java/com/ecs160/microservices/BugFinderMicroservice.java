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
                    "Given C source code file, identify potential bugs or issues in the code." +
                    " Give the response only in a JSON array where each element is an object with keys " +
                    "{\"title\", \"body\"}:\n" +
                    "  - \"title\": one short phrase naming the bug (e.g., \"Null pointer dereference\").\n" +
                    "  - \"body\": 1–3 sentences the bug with specific functions/lines.\n\n" + 
                    "Return an empty JSON array:[] if there are no bugs." +
                    "Additionally, do not include explanation, commentary, or markdown outside the JSON, code fences, or a top-level object that isn't a JSON array." +
                    "Here is the C source code:\n\n" +
                    cSource;

            String raw = client.ask(prompt);

            // Error handling with null source code/responses
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
