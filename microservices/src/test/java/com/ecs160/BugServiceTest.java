package com.ecs160;

import com.ecs160.clients.AIClient;
import com.ecs160.microservices.BugFinderController;
import org.junit.Test;
import static org.junit.Assert.*;

import java.io.IOException;

public class BugServiceTest {

    @Test
    public void Returns_Correct_Test() {
        String expectedResponse = "[\"Buffer Overflow\", \"Null Pointer\"]";
        
        String cCode = "int main() { return 0; }";

        // Mock client
        AIClient mockClient = new AIClient() {
            @Override
            public String ask(String prompt) throws IOException {
                if (prompt.contains(cCode) || prompt.contains("identify potential bugs")) {
                    return expectedResponse;
                }
                return null;
            }
        };

        BugFinderController controller = new BugFinderController(mockClient);

        String result = controller.findBugs(cCode);
        // Mocks accurate response
        assertEquals(expectedResponse, result);
    }

    @Test
    public void Null_Response_Test() {
        // Mock null client
        AIClient nullClient = new AIClient() {
            @Override
            public String ask(String prompt) {
                return null;
            }
        };

        BugFinderController controller = new BugFinderController(nullClient);
        String result = controller.findBugs("some code");

        assertTrue(result.contains("\"error\""));
    }
}