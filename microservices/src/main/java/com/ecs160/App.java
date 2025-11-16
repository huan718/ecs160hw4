package com.ecs160;

import com.ecs160.microservice.MicroserviceRegistry;
import com.ecs160.microservice.MicroserviceLauncher;

public class App 
{
    public static void main( String[] args )
    {
        //System.out.println( "Hello World!" );
        MicroserviceRegistry registry = new MicroserviceRegistry();
        MicroserviceLauncher launcher = new Launcher();

        launcher.launch(8080, "com.ecs160.microservices");         

        System.out.println("Server running at http://localhost:8080");
    }
}
