package com.ecs160;
import com.ecs160.microservices.BugFinderMicroservice;

import com.ecs160.clients.AIClient;
import static org.junit.Assert.*;
import org.junit.Test;

import java.io.IOException;

public class BugServiceTest {

    @Test
    public void Returns_Correct_Test() {
        String expectedResponse = "[\"Buffer Overflow\", \"Null Pointer\"]";
        
        AIClient mockClient = new AIClient() {
            @Override
            public String ask(String prompt) throws IOException {
                if (prompt.contains("Analyze this C code")) {
                    return expectedResponse;
                }
                return null;
            }
        };

        BugFinderMicroservice service = new BugFinderMicroservice(mockClient);

        String cCode = "int main() { return 0; }";
        String result = service.findBugs(cCode);

        assertEquals(expectedResponse, result);
    }

    @Test
    public void Null_Reponse_Test() {
        AIClient nullClient = new AIClient() {
            @Override
            public String ask(String prompt) {
                return null;
            }
        };

        BugFinderMicroservice service = new BugFinderMicroservice(nullClient);
        String result = service.findBugs("some code");

        assertTrue(result.contains("\"error\""));
    }
}