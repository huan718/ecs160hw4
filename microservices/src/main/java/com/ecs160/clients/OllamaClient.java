package com.ecs160.clients;

import okhttp3.*;
import com.google.gson.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

public class OllamaClient implements AIClient {    
   private final String url;
   private final String model;
   private final OkHttpClient client;
   private final Gson gson;

   private static final MediaType JSON_MEDIA_TYPE = 
         MediaType.get("application/json; charset=utf-8");

   public OllamaClient(String url, String model) {
      this.url = url;
      this.model = model;
      this.client = new OkHttpClient();
      this.gson = new Gson();
   }
   // ADD THIS CONSTRUCTOR FOR TESTING
   public OllamaClient(String url, String model, OkHttpClient client) {
      this.url = url;
      this.model = model;
      this.client = client;
      this.gson = new Gson();
   }

   @Override
   public String ask(String prompt) throws IOException {
      JsonObject payload = buildPayload(prompt);
      Request request = createRequest(payload);

      try (Response response = client.newCall(request).execute()) {
         return parseStreamResponse(response);
      }
   }

   private JsonObject buildPayload(String prompt) {
      JsonObject payload = new JsonObject();
      payload.addProperty("model", this.model);
      payload.addProperty("prompt", prompt);

      return payload;
   }

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

   private String parseStreamResponse(Response response) throws IOException {
      if (!response.isSuccessful()) {
            throw new IOException("Unexpected code " + response);
         }

         BufferedReader reader = new BufferedReader(new InputStreamReader(response.body().byteStream()));
         String line;
         StringBuilder fullResponse = new StringBuilder();

         while ((line = reader.readLine()) != null) {
            line = line.trim();
               if (line.isEmpty()) continue;

               JsonObject obj = gson.fromJson(line, JsonObject.class);

               if (obj.has("response")) {
                  fullResponse.append(obj.get("response").getAsString());
               }

               if (obj.has("done") && obj.get("done").getAsBoolean()) {
                  break;
               }
            }

      return fullResponse.toString();
   }
}
   