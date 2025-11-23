package com.ecs160;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

import com.ecs160.annotations.Endpoint;
import com.ecs160.annotations.Microservice;

public class RequestDispatcherTest {

    private MicroserviceRegistry registry;
    private RequestDispatcher dispatcher;

    //mock service
    @Microservice
    public static class TestService {
        private static final String SUCCESS_URL = "/yes";
        private static final String ERROR_URL = "/no";

        @Endpoint(url = SUCCESS_URL)
        public String handleBody(String body) {
            return "Processed: " + body.toUpperCase();
        }

        @Endpoint(url = ERROR_URL)
        public String throwInternalError(String body) {
            throw new RuntimeException("Simulated service error");
        }
    }

    @Before
    public void setUp() {
        registry = new MicroserviceRegistry();
        dispatcher = new RequestDispatcher(registry);
        registry.register(new TestService());
    }

    @Test
    public void Dispatch_Success_Returns_Correct_Test() {
        String path = TestService.SUCCESS_URL;
        String body = "test data";
        
        String response = dispatcher.dispatch(path, body);

        assertEquals("Processed: TEST DATA", response);
    }

    @Test
    public void Dispatch_Failed_Return_404_Test() {
        String path = "/missing";
        String body = "any";
        //mock expected 404 response
        String response = dispatcher.dispatch(path, body);
        String expectedMessage = "404 Not Found: " + path;
        
        assertEquals(expectedMessage, response);
    }
    
    @Test
    public void Dispatch_Endpoint_Returns_500_Test() {
        String path = TestService.ERROR_URL;
        String body = "data";

        String response = dispatcher.dispatch(path, body);

        assertTrue(response.startsWith("500 Internal Server Error:"));
        // FIX: Removed the word "internal" to match the actual exception message
        assertTrue(response.contains("Simulated service error")); 
    }
}