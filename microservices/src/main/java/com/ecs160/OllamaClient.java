package com.ecs160.microservices;

import okhttp3.*;
import com.google.gson.*;

// delete later
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

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
      } catch (IOException e) {
         e.printStackTrace();
      }

      return null;
   }
}