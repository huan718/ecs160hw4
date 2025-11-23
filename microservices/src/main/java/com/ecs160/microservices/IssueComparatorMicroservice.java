package com.ecs160.microservices;

// 1. IMPORT OLLAMA CLIENT (Required because packages differ)
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


    @Endpoint(url = "/check_equivalence")
    public String checkEquivalence(String body) {
        try {
            JsonObject root = JsonParser.parseString(body).getAsJsonObject();

            JsonArray list1 = root.getAsJsonArray("issueList1");
            JsonArray list2 = root.getAsJsonArray("issueList2");

            Set<String> set1 = convertToSet(list1);
            Set<String> set2 = convertToSet(list2);

            set1.retainAll(set2); // intersection

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

    private Set<String> convertToSet(JsonArray arr) {
        Set<String> set = new HashSet<>();
        if (arr == null) return set;
        for (JsonElement el : arr) {
            set.add(el.toString());  // exact match
        }
        return set;
    }
}
