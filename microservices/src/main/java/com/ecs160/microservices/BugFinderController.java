package com.ecs160.microservices;

import com.ecs160.clients.AIClient;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

// Mark as Rest API controller
@RestController
public class BugFinderController {
    private final AIClient client;
    private final Gson gson = new Gson();

    public BugFinderController(AIClient client) {
        this.client = client;
    }

    // HTTP POST request
    @PostMapping("/find_bugs")
    public String findBugs(@RequestBody String cSource) {
        try {
            if (cSource == null || cSource.isBlank()) {
                return "{\"error\":\"Empty C source\"}";
            }

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

            if (raw == null || raw.isBlank()) {
                return "{\"error\":\"LLM returned null or blank\"}";
            }

            int start = raw.indexOf('[');
            int end = raw.lastIndexOf(']');

            if (start == -1 || end == -1 || start >= end) {
                return "{\"error\":\"No JSON found\"}";
            }

            String json = raw.substring(start, end + 1).trim();

            try {
                gson.fromJson(json, JsonArray.class);
            } catch (Exception e) {
                return "{\"error\":\"Invalid JSON returned\"}";
            }

            return json;
        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg == null || msg.isBlank()) msg = e.getClass().getSimpleName();
            msg = msg.replace("\"", "'");
            return "{\"error\":\"" + msg + "\"}";
        }
    }
}