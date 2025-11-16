package com.ecs160;

public class RequestDispatcher {

    private final MicroserviceRegistry registry;

    public RequestDispatcher(MicroserviceRegistry registry) {
        this.registry = registry;
    }

    public String dispatch(String path, String body) {
        Endpointdef endpoint = registry.get(path);
        if (endpoint == null) return "404 Not Found: " + path;

        try {
            Object result = endpoint.getMethod().invoke(endpoint.getInstance(), body);
            return result.toString();
        } catch (Exception e) {
            return "500 Internal Server Error: " + e.getMessage();
        }
    }
}