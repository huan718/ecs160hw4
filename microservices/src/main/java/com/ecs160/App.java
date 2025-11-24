package com.ecs160;

import com.ecs160.microservices.*;
import com.ecs160.clients.*;

import java.util.List;

public class App 
{
    public static void main( String[] args )
    {
        MicroserviceRegistry registry = new MicroserviceRegistry();
        Launcher launcher = new Launcher();

        // Set up AI client with appropriate model and url 
        String ollamaUrl = "http://localhost:11434/api/generate";
        String model = "deepcoder:1.5b";
        AIClient aiClient = new OllamaClient(ollamaUrl, model);

        // Register all microservices with launcher
        launcher.registerAll(List.of(
            new IssueSummarizerMicroservice(aiClient),
            new BugFinderMicroservice(aiClient),
            new IssueComparatorMicroservice(aiClient)
        ));

        // Start port and print confirmation message
        launcher.launch(8080);         

        System.out.println("Server running at http://localhost:8080");
    }
}
