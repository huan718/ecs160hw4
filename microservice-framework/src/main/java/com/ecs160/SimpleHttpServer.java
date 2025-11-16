package com.ecs160;

import java.io.OutputStream;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpServer;

public class SimpleHttpServer {

   private final RequestDispatcher dispatcher;

   public SimpleHttpServer(RequestDispatcher dispatcher) {
      this.dispatcher = dispatcher;
   }

   public boolean start(int port) {
      try {
         HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

         server.createContext("/", exchange -> {
               String path = exchange.getRequestURI().getPath();
               String body = new String(exchange.getRequestBody().readAllBytes());
               String response = dispatcher.dispatch(path, body);

               exchange.sendResponseHeaders(200, response.getBytes().length);
               try (OutputStream os = exchange.getResponseBody()) {
                  os.write(response.getBytes());
               }
         });

         server.start();
         System.out.println("Server running on port " + port);

         // Keeps server alive
         while (true) Thread.sleep(1000);
      } catch (Exception e) {
         e.printStackTrace();
         return false;
      }
   }
}