package com.ecs160;

import java.util.List;

public class Launcher {

    private final MicroserviceRegistry registry = new MicroserviceRegistry();

    public void registerAll(List<Object> services) {
        for (Object s : services) registry.register(s);
    }

    public boolean launch(int port) {
        RequestDispatcher dispatcher = new RequestDispatcher(registry);
        SimpleHttpServer server = new SimpleHttpServer(dispatcher);
        return server.start(port);
    }
}