package com.ecs160.microservices;

import okhttp3.*;
import com.google.gson.*;

import java.io.IOException;

public class OllamaClient {   
   private static final String OLLAMA_URL = "http://localhost:11434/api/generate";
   private static final OkHttpClient client = new OkHttpClient();
   private static final Gson gson = new Gson();  

   public static String askDeepCoder(String prompt) throws IOException {
      // Finish implementation later
      return "";
   }
}