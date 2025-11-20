package com.ecs160.microservices;

import okhttp3.*;
import com.google.gson.*;

import java.io.IOException;

public class OllamaClient {   
   private static final String OLLAMA_URL = "http://localhost:11434/api/generate";
   private static final OkHttpClient client = new OkHttpClient();
   private static final Gson gson = new Gson();  

   public static String askDeepCoder(String prompt) throws IOException {
      JsonObject payload = new JsonObject();
      payload.addProperty("model", "deepcoder:1.5b");
      payload.addProperty("prompt", prompt);

      RequestBody body = RequestBody.create(
            payload.toString(),                  
            MediaType.get("application/json; charset=utf-8")
            );

      Request request = new Request.Builder()
            .url(OLLAMA_URL)
            .post(body)
            .build();
      
      try (Response response = client.newCall(request).execute()) {
         if (response.isSuccessful() && response.body() != null) {
            return response.body().string(); 
         } else {
            System.err.println("Request failed: " + response.code());
         }
      } catch (IOException e) {
         e.printStackTrace();
      }

      return null;
   }
}