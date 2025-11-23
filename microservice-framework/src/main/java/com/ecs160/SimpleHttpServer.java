package com.ecs160;

import java.io.OutputStream;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpServer;
public class SimpleHttpServer {

   private final RequestDispatcher dispatcher;
   private final int port;
   private HttpServer server;

   //constructor
   public SimpleHttpServer(int port, RequestDispatcher dispatcher) {
      this.port = port;
      this.dispatcher = dispatcher;
   }

   //start the server
   public void start() {
      try {
         this.server = HttpServer.create(new InetSocketAddress(this.port), 0);

         server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().toString();
         
            //read the request body
            String body = new String(exchange.getRequestBody().readAllBytes());
            
            String response = dispatcher.dispatch(path, body);

            //determine status code based on dispatcher response
            int statusCode = 200;
            if (response.startsWith("404")) {
               statusCode = 404;
            } else if (response.startsWith("500")) {
               statusCode = 500;
            }

            exchange.sendResponseHeaders(statusCode, response.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
               os.write(response.getBytes());
            }
         });

         server.setExecutor(null); 
         server.start();
         System.out.println("Server running on port " + this.port);
         
      } catch (Exception e) {
         e.printStackTrace();
         throw new RuntimeException("Failed to start HTTP server", e);
      }
   }

   //stop the server
   public void stop() {
      if (server != null) {
         server.stop(0); 
         System.out.println("Server stopped.");
      }
   }
}