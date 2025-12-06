package com.ecs160;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.ecs160.clients.AIClient;
import com.ecs160.clients.OllamaClient;

@SpringBootApplication
public class MicroserviceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MicroserviceApplication.class, args);
    }

    @Bean
    public AIClient aiClient() {
        // Configure your Ollama settings here
        String ollamaUrl = "http://localhost:11434/api/generate";
        String model = "deepcoder:1.5b";
        return new OllamaClient(ollamaUrl, model);
    }
}