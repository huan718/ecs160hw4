package com.ecs160.microservices;

// import the Microservice annotation we created
import com.ecs160.annotations.Microservice;
import com.ecs160.annotations.Endpoint;
package com.ecs160.ollama;

// uncomment the above imports when the annotations are created

@Microservice
public class IssueComparatorMicroservice {
    @Endpoint(url = "/check_equivalence")
    public String checkEquivalence(String issueJSonArray) {
        try {
            String prompt = "Given these two lists of Issues in JSON format, create a JSON array of Issues that appeared in both:\n" + issueJSonArray;
            String ollamaResponse = OllamaClient.askDeepCoder(prompt);

            return ollamaResponse;

        } catch (Exception e) {
            return "{ \\\"error\\\": \\\"" + e.getMessage() + "\\\" }";
        }
    }
}

