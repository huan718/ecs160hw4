package com.ecs160;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.junit.After;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;

import com.ecs160.annotations.Endpoint;
import com.ecs160.annotations.Microservice;

public class SimpleHttpServerTest {

    private SimpleHttpServer server;
    private MicroserviceRegistry registry;
    private RequestDispatcher dispatcher;

    private final int port = 8085; 

    @Microservice
    public static class TestHttpService {
        @Endpoint(url = "/hello")
        public String handleRequest(String input) {
            // "input" comes from the HTTP Request Body
            return "Hello, " + input;
        }
    }

    @Before
    public void setUp() throws Exception {
        registry = new MicroserviceRegistry();
        dispatcher = new RequestDispatcher(registry);
        registry.register(new TestHttpService());
        server = new SimpleHttpServer(port, dispatcher);
        server.start();
        Thread.sleep(100);
    }

    @After
    public void tearDown() {
        server.stop();
    }

    @Test
    public void testHttpRequestReturnsCorrectResponse() throws Exception {
        // 1. Use pure path (Avoids 404 if server doesn't strip query params)
        URL url = new URL("http://localhost:" + port + "/hello");

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        
        // 2. Use POST to send data in the Body (since Dispatcher reads Body)
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);

        // 3. Write "World" to the output stream
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = "World".getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int status = conn.getResponseCode();
        assertEquals(200, status);

        BufferedReader reader =
                new BufferedReader(new InputStreamReader(conn.getInputStream()));

        String response = reader.readLine().trim();
        reader.close();

        // 4. Expectation matches
        assertEquals("Hello, World", response);
    }

    @Test
    public void testUnknownUrlReturns404() throws Exception {
        URL url = new URL("http://localhost:" + port + "/missing");

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        int status = conn.getResponseCode();
        assertEquals(404, status);
    }
}