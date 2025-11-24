package com.ecs160.microservices;

import com.ecs160.clients.*;
import com.ecs160.annotations.Microservice;
import com.ecs160.annotations.Endpoint;
import com.google.gson.*;

import java.util.HashSet;
import java.util.Set;

@Microservice
public class IssueComparatorMicroservice {
    private final AIClient client; 

    public IssueComparatorMicroservice(AIClient client) {
        this.client = client;
    }

    // Simpler and faster implementation with java code over calling Ollama
    @Endpoint(url = "/check_equivalence")
    public String checkEquivalence(String body) {
        try {
            // Convert string to JSON object and seperate lists
            JsonObject root = JsonParser.parseString(body).getAsJsonObject();

            JsonArray list1 = root.getAsJsonArray("issueList1");
            JsonArray list2 = root.getAsJsonArray("issueList2");

            Set<String> set1 = convertToSet(list1);
            Set<String> set2 = convertToSet(list2);

            set1.retainAll(set2); 

            // Build JSON response
            JsonArray common = new JsonArray();
            for (String s : set1) {
                common.add(JsonParser.parseString(s));
            }

            JsonObject resp = new JsonObject();
            resp.add("commonIssues", common);

            return resp.toString();

        } catch (Exception e) {
            return "{\"error\":\"" + e.getMessage() + "\"}";
        }
    }

    // Helper function to convert JSON array to set of strings
    private Set<String> convertToSet(JsonArray arr) {
        Set<String> set = new HashSet<>();
        if (arr == null) return set;
        
        for (JsonElement el : arr) {
            set.add(el.toString());  // exact match
        }
        return set;
    }
}
