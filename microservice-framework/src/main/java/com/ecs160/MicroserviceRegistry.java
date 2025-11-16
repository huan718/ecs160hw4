package com.ecs160;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import com.ecs160.annotations.Endpoint;
import com.ecs160.annotations.Microservice;

public class MicroserviceRegistry {

   private final Map<String, Endpointdef> endpoints = new HashMap<>();

   public void register(Object service) {
      Class<?> clazz = service.getClass();

      // Only register if annotated with @Microservice
      if (!clazz.isAnnotationPresent(Microservice.class)) {
         throw new IllegalArgumentException(clazz.getName() + " is not annotated with @Microservice");
      }

      // Look for methods annotated with @Endpoint
      for (Method method : clazz.getDeclaredMethods()) {
         if (method.isAnnotationPresent(Endpoint.class)) {
               String url = method.getAnnotation(Endpoint.class).url();
               validateEndpointMethod(method);
               endpoints.put(url, new Endpointdef(service, method, url));
         }
      }
   }

   public Endpointdef get(String url) {
      return endpoints.get(url);
   }

   private void validateEndpointMethod(Method method) {
      // Method must be: String handleRequest(String input)
      if (!method.getReturnType().equals(String.class)
               || method.getParameterCount() != 1
               || !method.getParameterTypes()[0].equals(String.class)) {
         throw new IllegalArgumentException("Invalid endpoint signature: must be String handleRequest(String input)");
      }
   }
}