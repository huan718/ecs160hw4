package com.ecs160;

public class RequestDispatcher {

    private final MicroserviceRegistry registry;

    //constructor
    public RequestDispatcher(MicroserviceRegistry registry) {
        this.registry = registry;
    }

    //dispatch request to appropriate microservice endpoint
    public String dispatch(String path, String body) {
        Endpointdef endpoint = registry.get(path);
        if (endpoint == null) return "404 Not Found: " + path;

        try {
            Object result = endpoint.getMethod().invoke(endpoint.getInstance(), body);
            return result.toString();
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            return "500 Internal Server Error: " + cause.getMessage();
        }
    }
}