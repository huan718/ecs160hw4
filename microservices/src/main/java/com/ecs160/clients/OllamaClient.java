package com.ecs160.clients;

import okhttp3.*;
import com.google.gson.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.concurrent.TimeUnit;   

// Ollama implementation of AIClient
public class OllamaClient implements AIClient {    
   private final String url;
   private final String model;
   private final OkHttpClient client;
   private final Gson gson;

   private static final MediaType JSON_MEDIA_TYPE = 
         MediaType.get("application/json; charset=utf-8");

   // Default constructor 
   public OllamaClient(String url, String model) {
      this.url = url;
      this.model = model;

      // Timeout contraints
      this.client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)    // how long to wait to connect
            .readTimeout(120, TimeUnit.SECONDS)      // how long to wait for the model to stream tokens
            .writeTimeout(10, TimeUnit.SECONDS)
            .build();

      this.gson = new Gson();
   }

   // Constructor with custom client 
   public OllamaClient(String url, String model, OkHttpClient client) {
      this.url = url;
      this.model = model;
      this.client = client;
      this.gson = new Gson();
   }

   // Main method to build JSON request and convert to HTTP
   @Override
   public String ask(String prompt) throws IOException {
      JsonObject payload = buildPayload(prompt);
      Request request = createRequest(payload);

      try (Response response = client.newCall(request).execute()) {
         return parseStreamResponse(response);
      }
   }

   // Helper function to build a prompt and send request to Ollama server via HTTP
   private JsonObject buildPayload(String prompt) {
      JsonObject payload = new JsonObject();
      payload.addProperty("model", this.model);
      payload.addProperty("prompt", prompt);

      return payload;
   }
   
   // Helper method to create JSON post request
   private Request createRequest(JsonObject payload){
      RequestBody body = RequestBody.create(
            payload.toString(),                  
            JSON_MEDIA_TYPE
      );

      return new Request.Builder()
            .url(this.url)
            .post(body)
            .build();
   }

   // Parses HTTP response from Ollama server
   private String parseStreamResponse(Response response) throws IOException {
      // Error code checking such as 400
      if (!response.isSuccessful()) {
         String errBody = response.body() != null ? response.body().string() : "";
         throw new IOException("Unexpected code " + response.code() + " body=" + errBody);
      }

      // Reads response line by line 
      BufferedReader reader = new BufferedReader(
            new InputStreamReader(response.body().byteStream())
      );
      String line;
      // Store full resposne as a string
      StringBuilder fullResponse = new StringBuilder();

      // Parses and cleans each streamed line
      while ((line = reader.readLine()) != null) {
         // Trim white space and empty lines
         line = line.trim();
         if (line.isEmpty()) continue;

         JsonObject obj = gson.fromJson(line, JsonObject.class);

         // Append to response if response key is present
         if (obj.has("response")) {
            fullResponse.append(obj.get("response").getAsString());
         }

         // Stops reading once done done is set to true
         if (obj.has("done") && obj.get("done").getAsBoolean()) {
            break;
         }
      }

      return fullResponse.toString();
   }
}
