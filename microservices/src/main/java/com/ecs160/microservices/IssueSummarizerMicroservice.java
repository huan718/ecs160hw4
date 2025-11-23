package com.ecs160.microservices;

// import the Microservice annotation we created
import com.ecs160.annotations.Microservice;
import com.ecs160.clients.*;
import com.ecs160.annotations.Endpoint;

@Microservice
public class IssueSummarizerMicroservice {
    private final AIClient client; 

    public IssueSummarizerMicroservice(AIClient client) {
        this.client = client;
    }

    @Endpoint(url = "/summarize_issue")
    public String summarizeIssue(String issueJson) {            
        try {
            String prompt = "Summarize this GitHub issue and output the JSON in issue format:\n" + issueJson;
            String ollamaResponse = client.ask(prompt);
            
            if (ollamaResponse == null) {
                return "{\"error\":\"Ollama returned null\"}";
            }

            return ollamaResponse;

        } catch (Exception e) {
            return "{ \\\"error\\\": \\\"" + e.getMessage() + "\\\" }";
        }
    }
}

