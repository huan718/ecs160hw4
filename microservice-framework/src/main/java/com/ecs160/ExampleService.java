package com.ecs160;

import com.ecs160.annotations.Microservice;
import com.ecs160.annotations.Endpoint;

@Microservice
public class ExampleService {

    @Endpoint(url = "/hello")
    public String handleRequest(String input) {
        if (input == null || input.isBlank()) {
            return "Hello! No input provided.";
        }
        return "Hello! You said: " + input;
    }

    @Endpoint(url = "/echo")
    public String echo(String input) {
        return input == null ? "" : input;
    }
}