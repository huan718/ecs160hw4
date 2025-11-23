package com.ecs160;

import java.io.OutputStream;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpServer;

/**
 * HTTP server that accepts requests and delegates processing to the RequestDispatcher.
 * Updated to be non-blocking and store the HttpServer instance for graceful shutdown.
 */
public class SimpleHttpServer {

    private final RequestDispatcher dispatcher;
    private final int port;
    private HttpServer server;

    // CONSTRUCTOR CHANGE: Takes port in the constructor, matching the test.
    public SimpleHttpServer(int port, RequestDispatcher dispatcher) {
        this.port = port;
        this.dispatcher = dispatcher;
    }

    // START METHOD CHANGE: Takes no arguments and returns void.
    // It starts the server and returns control immediately (non-blocking).
    public void start() {
        try {
            this.server = HttpServer.create(new InetSocketAddress(this.port), 0);

            server.createContext("/", exchange -> {
                String path = exchange.getRequestURI().toString();
                
                // Read the request body (will be empty for GET)
                String body = new String(exchange.getRequestBody().readAllBytes());
                
                String response = dispatcher.dispatch(path, body);

                // Determine status code based on dispatcher response
                int statusCode = 200;
                if (response.startsWith("404")) {
                    statusCode = 404;
                } else if (response.startsWith("500")) {
                    statusCode = 500;
                }

                // Send headers and response body
                exchange.sendResponseHeaders(statusCode, response.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            });

            // Use a default executor (recommended for simple servers)
            server.setExecutor(null); 
            server.start();
            System.out.println("Server running on port " + this.port);
            
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to start HTTP server", e);
        }
    }

    /**
     * New method to stop the server gracefully, required by the test's @After method.
     */
    public void stop() {
        if (server != null) {
            // Stops the server immediately (0 delay)
            server.stop(0); 
            System.out.println("Server stopped.");
        }
    }
}