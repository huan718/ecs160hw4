package com.ecs160.microservices;

// import the Microservice annotation we created
import com.ecs160.annotations.Microservice;
import com.ecs160.annotations.Endpoint;
import com.ecs160.clients.*;
import com.google.gson.Gson;

@Microservice
public class IssueSummarizerMicroservice {
    private final AIClient client; 

    public IssueSummarizerMicroservice(AIClient client) {
        this.client = client;
    }

    @Endpoint(url = "/summarize_issue")
    public String summarizeIssue(String issueJson) {            
        try {
            String prompt = 
                "Given the Github issues summarize it and respond ONLY with a valid JSON object using keys {\"title\", \"body\"}. " +
                "DO NOT include explanations, thoughts, or preamble and ONLY output pure JSON:\n" + issueJson;  

            // Null and edge case handling
            String raw = client.ask(prompt);

            if (raw == null || raw.isBlank())
                return "{\"error\":\"LLM returned null\"}";

            // Extract first JSON object
            int start = raw.indexOf("{");
            int end = raw.lastIndexOf("}");

            if (start == -1 || end == -1)
                return "{\"error\":\"No JSON found\"}";

            String json = raw.substring(start, end + 1);

            // Validate JSON
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

