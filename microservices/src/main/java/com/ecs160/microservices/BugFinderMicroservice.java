package com.ecs160.microservices;

// import the Microservice annotation we created
import com.ecs160.annotations.Microservice;
import com.ecs160.annotations.Endpoint;
import com.ecs160.clients.*;

// uncomment the annotations when you have created them
@Microservice
public class BugFinderMicroservice {
    private final AIClient client; 

    public BugFinderMicroservice(AIClient client) {
        this.client = client;
    }

    @Endpoint(url = "/find_bugs")
    public String findBugs(String code) {
        try {
            String prompt = "Analyze this C code and extract potential bugs. " +
                        "Return ONLY a JSON array of issue strings.\n\n" + code;
            String ollamaResponse = client.ask(prompt);
            if (ollamaResponse == null || ollamaResponse.isBlank()) {
                return "{ \"error\": \"LLM returned null or blank response\" }";
            }   
            return ollamaResponse;

        } catch (Exception e) {
            return "{ \\\"error\\\": \\\"" + e.getMessage() + "\\\" }";
        }
        
    }   
}

