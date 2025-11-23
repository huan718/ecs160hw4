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

    // --- Mock Service Definitions ---

    @Microservice
    public static class TestService {
        private static final String SUCCESS_URL = "/greet";
        private static final String ERROR_URL = "/fail";

        @Endpoint(url = SUCCESS_URL)
        public String handleBody(String body) {
            // The dispatcher passes the entire HTTP body as the input string
            return "Processed: " + body.toUpperCase();
        }

        @Endpoint(url = ERROR_URL)
        public String throwInternalError(String body) {
            // Simulate an unhandled exception inside the microservice method
            throw new RuntimeException("Simulated internal service error");
        }
    }

    // --- Setup ---

    @Before
    public void setUp() {
        // 1. Initialize Registry and Dispatcher
        registry = new MicroserviceRegistry();
        dispatcher = new RequestDispatcher(registry);

        // 2. Register the test service
        registry.register(new TestService());
    }

    // --- Test Cases ---

    @Test
    public void testDispatch_success_returnsCorrectResult() {
        // Arrange
        String path = TestService.SUCCESS_URL;
        String body = "test data";
        
        // Act
        String response = dispatcher.dispatch(path, body);

        // Assert
        assertEquals("The dispatcher should successfully invoke the method and return the result.", 
                     "Processed: TEST DATA", response);
    }

    @Test
    public void testDispatch_unregisteredUrl_returns404() {
        // Arrange
        String path = "/missing";
        String body = "any";

        // Act
        String response = dispatcher.dispatch(path, body);

        // Assert
        String expectedMessage = "404 Not Found: " + path;
        assertEquals("Dispatching an unknown URL should return the expected 404 message.", 
                     expectedMessage, response);
    }

    @Test
    public void testDispatch_endpointThrowsException_returns500() {
        // Arrange
        String path = TestService.ERROR_URL;
        String body = "data";

        // Act
        String response = dispatcher.dispatch(path, body);

        // Assert
        // Check that the response starts with the 500 prefix and includes the error message.
        assertTrue("Dispatching an endpoint that throws an exception should return a 500 error message.", 
                   response.startsWith("500 Internal Server Error:"));
        assertTrue("The 500 error message should contain the exception message.", 
                   response.contains("Simulated internal service error"));
    }
}