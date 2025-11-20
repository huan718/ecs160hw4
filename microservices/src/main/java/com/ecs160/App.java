package com.ecs160;

import com.ecs160.MicroserviceRegistry;
import com.ecs160.Launcher;

import com.ecs160.microservices.*;

import java.util.List;

public class App 
{
    public static void main( String[] args )
    {
        //System.out.println( "Hello World!" );
        MicroserviceRegistry registry = new MicroserviceRegistry();
        Launcher launcher = new Launcher();

        launcher.registerAll(List.of(
            new IssueSummarizerMicroservice(),
            new BugFinderMicroservice(),
            new IssueComparatorMicroservice()
        ));

        launcher.launch(8080);         

        System.out.println("Server running at http://localhost:8080");
    }
}
