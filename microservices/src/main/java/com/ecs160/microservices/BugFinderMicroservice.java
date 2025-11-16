package com.ecs160.microservices;

// import the Microservice annotation we created
import com.ecs160.annotations.Microservice;
import com.ecs160.annotations.Endpoint;

// uncomment the annotations when you have created them
@Microservice
public class BugFinderMicroservice {
    
    @Endpoint(url = "/find_bugs")
    public String findBugs(String code) {
        try {
            String prompt = "Analyze this C code and return a JSON array of Issues:\n" + code;
            String ollamaResponse = OllamaClient.askDeepCoder(prompt);

            return ollamaResponse;

        } catch (Exception e) {
            return "{ \\\"error\\\": \\\"" + e.getMessage() + "\\\" }";
        }
        
    }
}

