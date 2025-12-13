package com.ecs160.microservices;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.ecs160.clients.AIClient;
import com.google.gson.Gson;

// Mark as Rest API controller
@RestController
public class IssueSummarizerMicroservice {
    private final AIClient client; 

    public IssueSummarizerMicroservice(AIClient client) {
        this.client = client;
    }

    // Mapping HTTP POST request
    @PostMapping("/summarize_issue")
    public String summarizeIssue(@RequestBody String issueJson) {            
        try {
            String prompt = 
                "Given the Github issues summarize it and respond ONLY with a valid JSON object using keys {\"title\", \"body\"}. " +
                "DO NOT include explanations, thoughts, or preamble and ONLY output pure JSON:\n" + issueJson;  

            String raw = client.ask(prompt);

            if (raw == null || raw.isBlank())
                return "{\"error\":\"LLM returned null\"}";

            int start = raw.indexOf("{");
            int end = raw.lastIndexOf("}");

            if (start == -1 || end == -1)
                return "{\"error\":\"No JSON found\"}";

            String json = raw.substring(start, end + 1);

            try {
                new Gson().fromJson(json, Object.class);
            } catch (Exception e) {
                return "{\"error\":\"Invalid JSON returned\"}";
            }

            return json;

        } catch (Exception e) {
            return "{\"error\":\"" + e.getMessage() + "\"}";
        }
    }
}