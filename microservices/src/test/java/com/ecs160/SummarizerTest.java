package com.ecs160;

import com.ecs160.clients.AIClient;
import com.ecs160.microservices.IssueSummarizerController;
import org.junit.Test;
import static org.junit.Assert.*;

import java.io.IOException;

public class SummarizerTest {

    @Test
    public void SummarizeIssue_Success_Test() {
        String mockSummary = "{ \"summary\": \"This is a short summary\" }";

        // Mock client 
        AIClient mockClient = new AIClient() {
            @Override
            public String ask(String prompt) throws IOException {
                if (prompt.contains("Given the Github issues summarize it")) {
                    return mockSummary;
                }
                throw new IOException("Unexpected prompt: " + prompt);
            }
        };

        IssueSummarizerController controller = new IssueSummarizerController(mockClient);

        String inputJson = "{ \"title\": \"Bug in UI\", \"body\": \"Buttons are broken\" }";
        String result = controller.summarizeIssue(inputJson);

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

        IssueSummarizerController controller = new IssueSummarizerController(errorClient);
        
        String result = controller.summarizeIssue("{}");

        // Fail case
        assertTrue(result.contains("Network failure") || result.contains("error"));
    }
}