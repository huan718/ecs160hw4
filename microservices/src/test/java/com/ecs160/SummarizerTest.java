package com.ecs160;
import com.ecs160.microservices.IssueSummarizerMicroservice;

import com.ecs160.clients.AIClient;
import static org.junit.Assert.*;
import org.junit.Test;

import java.io.IOException;

public class SummarizerTest {

    @Test
    public void SummarizeIssue_Success_Test() {
        String mockSummary = "{ \"summary\": \"This is a short summary\" }";

        // Mock client 
        AIClient mockClient = new AIClient() {
            @Override
            public String ask(String prompt) throws IOException {
                // FIX: Check for the actual string used in the Microservice
                // Using .contains() is safer than .startsWith()
                if (prompt.contains("Given the Github issues summarize it")) {
                    return mockSummary;
                }
                // This is what was causing the test to fail before
                throw new IOException("Unexpected prompt: " + prompt);
            }
        };

        IssueSummarizerMicroservice service = new IssueSummarizerMicroservice(mockClient);

        String inputJson = "{ \"title\": \"Bug in UI\", \"body\": \"Buttons are broken\" }";
        String result = service.summarizeIssue(inputJson);

        // Success case
        assertEquals(mockSummary, result);
    }

    @Test
    public void SummarizeIssue_Fail_Test() {
        AIClient errorClient = new AIClient() {
            @Override
            public String ask(String prompt) throws IOException {
                throw new IOException("Network failure");
            }
        };

        IssueSummarizerMicroservice service = new IssueSummarizerMicroservice(errorClient);
        
        String result = service.summarizeIssue("{}");

        // Fail case
        assertTrue(result.contains("Network failure"));
        assertTrue(result.contains("error"));
    }
}