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
    //mock service
    @Microservice
    public static class TestHttpService {
        @Endpoint(url = "/hello")
        public String handleRequest(String input) {
            return "Hello, " + input;
        }
    }
    //server setup and shutdown
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
    public void shutDown() {
        server.stop();
    }

    @Test
    public void Correct_Response_Test() throws Exception {
        URL url = new URL("http://localhost:" + port + "/hello");
        HttpURLConnection temp = (HttpURLConnection) url.openConnection();

        temp.setRequestMethod("POST");
        temp.setDoOutput(true);

        try (OutputStream os = temp.getOutputStream()) {
            byte[] input = "World".getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        assertEquals(200, temp.getResponseCode());

        BufferedReader reader = new BufferedReader(new InputStreamReader(temp.getInputStream()));
        String response = reader.readLine().trim();
        reader.close();

        assertEquals("Hello, World", response);
    }

    @Test
    public void Unknown_URL_Test() throws Exception {
        URL url = new URL("http://localhost:" + port + "/missing");
        HttpURLConnection temp = (HttpURLConnection) url.openConnection();
        temp.setRequestMethod("GET");

        assertEquals(404, temp.getResponseCode());
    }
}