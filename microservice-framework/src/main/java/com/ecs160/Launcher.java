package com.ecs160;

import java.util.List;

public class Launcher {

    private final MicroserviceRegistry registry = new MicroserviceRegistry();
    private SimpleHttpServer server; // Track server instance for control

    /**
     * Registers a list of microservice instances with the registry.
     * @param services A list of microservice objects annotated with @Microservice.
     */
    public void registerAll(List<Object> services) {
        for (Object s : services) registry.register(s);
    }

    /**
     * Initializes and starts the HTTP server.
     * @param port The port to run the server on.
     */
    public void launch(int port) {
        RequestDispatcher dispatcher = new RequestDispatcher(registry);
        // FIX: Use the non-blocking constructor: SimpleHttpServer(port, dispatcher)
        this.server = new SimpleHttpServer(port, dispatcher); 
        // FIX: Use the non-blocking start() method
        server.start();
    }
    
    /**
     * Helper method to stop the server gracefully, essential for testing.
     */
    public void stopServer() {
        if (this.server != null) {
            this.server.stop();
        }
    }
}