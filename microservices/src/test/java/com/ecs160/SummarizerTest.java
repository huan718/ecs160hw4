package com.ecs160;

import com.ecs160.clients.AIClient;
import com.ecs160.microservices.IssueSummarizerMicroservice;
import org.junit.Test;
import static org.junit.Assert.*;

import java.io.IOException;

public class SummarizerTest {

    @Test
    public void SummarizeIssue_Success_Test() {
        String mockSummary = "{ \"summary\": \"This is a short summary\" }";

        //mock client
        AIClient mockClient = new AIClient() {
            @Override
            public String ask(String prompt) throws IOException {
                if (prompt.contains("Given the Github issues summarize it")) {
                    return mockSummary;
                }
                throw new IOException("Unexpected prompt: " + prompt);
            }
        };

        IssueSummarizerMicroservice controller = new IssueSummarizerMicroservice(mockClient);

        String inputJson = "{ \"title\": \"Bug in UI\", \"body\": \"Buttons are broken\" }";
        String result = controller.summarizeIssue(inputJson);

        //success case
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

        IssueSummarizerMicroservice controller = new IssueSummarizerMicroservice(errorClient);
        
        String result = controller.summarizeIssue("{}");

        //fail case
        assertTrue(result.contains("Network failure") || result.contains("error"));
    }
}