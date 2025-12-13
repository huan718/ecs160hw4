package com.ecs160.microservices;

import com.ecs160.clients.AIClient;
import com.google.gson.*;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashSet;
import java.util.Set;

// Mark as Rest API controller
@RestController
public class IssueComparatorController {
    private final AIClient client; 

    public IssueComparatorController(AIClient client) {
        this.client = client;
    }

    // Mapping HTTP POST request
    @PostMapping("/check_equivalence")
    public String checkEquivalence(@RequestBody String body) {
        try {
            JsonObject root = JsonParser.parseString(body).getAsJsonObject();

            JsonArray list1 = root.getAsJsonArray("issueList1");
            JsonArray list2 = root.getAsJsonArray("issueList2");

            Set<String> set1 = convertToSet(list1);
            Set<String> set2 = convertToSet(list2);

            set1.retainAll(set2); 

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
            set.add(el.toString());
        }
        return set;
    }
}