package com.ecs160.microservices;

// 1. IMPORT OLLAMA CLIENT (Required because packages differ)
import com.ecs160.clients.*;

// Import your framework annotations
import com.ecs160.annotations.Microservice;
import com.ecs160.annotations.Endpoint;

@Microservice
public class IssueComparatorMicroservice {
    private final AIClient client; 

    public IssueComparatorMicroservice(AIClient client) {
        this.client = client;
    }

    @Endpoint(url = "/check_equivalence")
    public String checkEquivalence(String issueJSonArray) {
        try {
            // Ideally, verify that 'issueJSonArray' actually contains the two lists 
            // you are asking the LLM to compare.
            String prompt = "Given these two lists of Issues in JSON format, create a JSON array of Issues that appeared in both:\n" + issueJSonArray;
            
            String ollamaResponse = client.ask(prompt);

            return ollamaResponse;

        } catch (Exception e) {
            // 2. FIXED JSON ESCAPING
            // Standard JSON: {"error": "message"}
            // In Java: "{ \"error\": \"" + msg + "\" }"
            return "{ \"error\": \"" + e.getMessage() + "\" }";
        }
    }
}