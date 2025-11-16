package com.ecs160.microservices;

// import the Microservice annotation we created
import com.ecs160.annotations.Microservice;
import com.ecs160.annotations.Endpoint;
package com.ecs160.ollama;

@Microservice
public class IssueSummarizerMicroservice {
    
    @Endpoint(url = "/summarize_issue")
    public String summarizeIssue(String issueJson) {            
        try {
            String prompt = "Summarize this GitHub issue and output the JSON in issue format:\n" + issueJson;
            String ollamaResponse = OllamaClient.askDeepCoder(prompt);

            return ollamaResponse;

        } catch (Exception e) {
            return "{ \\\"error\\\": \\\"" + e.getMessage() + "\\\" }";
        }
    }
}

