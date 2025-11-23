package com.ecs160.hw;

import com.google.gson.JsonArray;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import static org.junit.Assert.*;

public class MicroserviceClientTest {

    private HttpServer server;
    private MicroserviceClient client;
    private int port;

    @Before
    public void setUp() throws IOException {
        // Setup a simple HTTP server on a random port to act as the backend
        server = HttpServer.create(new InetSocketAddress(0), 0);
        port = server.getAddress().getPort();
        server.setExecutor(null); // creates a default executor
        server.start();

        client = new MicroserviceClient("http://localhost:" + port);
    }

    @After
    public void shutDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    public void SUMMARIZE_ISSUES_TEST() throws IOException {
        // Define server behavior for this endpoint
        server.createContext("/summarize_issue", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                String response = "{ \"summary\": \"This is a summary\" }";
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            }
        });

        String result = client.summarizeIssue("Bug Title", "Bug Body");
        assertTrue(result.contains("This is a summary"));
    }

    @Test
    public void FIND_BUGS_TEST() throws IOException {
        server.createContext("/find_bugs", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                String response = "[ { \"bug\": \"Buffer Overflow\" } ]";
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            }
        });

        String cCode = "int main() { return 0; }";
        String result = client.findBugs(cCode);
        assertTrue(result.contains("Buffer Overflow"));
    }

    @Test
    public void CHECK_EQUIVALENCE_TEST() throws IOException {
        server.createContext("/check_equivalence", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                String response = "{ \"equivalent\": true }";
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            }
        });

        String result = client.checkEquivalence(new JsonArray(), new JsonArray());
        assertTrue(result.contains("true"));
    }
    
    @Test
    public void HTTP_ERROR_HANDLING_TEST() throws IOException {
        server.createContext("/find_bugs", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                exchange.sendResponseHeaders(500, -1); // Internal Server Error
            }
        });

        String result = client.findBugs("code");
        assertTrue(result.startsWith("Error from microservice: HTTP 500"));
    }
}