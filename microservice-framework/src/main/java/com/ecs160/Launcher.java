package com.ecs160;

import java.util.List;

public class Launcher {

    private final MicroserviceRegistry registry = new MicroserviceRegistry();
    private SimpleHttpServer server; 

    //register multiple microservices
    public void registerAll(List<Object> services) {
        for (Object s : services) registry.register(s);
    }

    //launch the server at the given port
    public void launch(int port) {
        RequestDispatcher dispatcher = new RequestDispatcher(registry);
        this.server = new SimpleHttpServer(port, dispatcher); 
        server.start();
    }
    
    //stop the server for testing purpose
    public void stopServer() {
        if (this.server != null) {
            this.server.stop();
        }
    }
}