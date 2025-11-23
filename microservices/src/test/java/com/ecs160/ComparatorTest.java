package com.ecs160;
import com.ecs160.microservices.IssueComparatorMicroservice;

import com.ecs160.clients.AIClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

public class ComparatorTest {

    private IssueComparatorMicroservice service;

    // Mock service setup
    @Before
    public void setUp() {
        AIClient dummyClient = new AIClient() {
            @Override
            public String ask(String prompt) {
                return "";
            }
        };
        service = new IssueComparatorMicroservice(dummyClient);
    }

    @Test
    public void check_equivalence_with_commons_Test() {
        JsonObject input = new JsonObject();
        
        JsonArray list1 = new JsonArray();
        list1.add("issue_A");
        list1.add("issue_B");
        
        JsonArray list2 = new JsonArray();
        list2.add("issue_B");
        list2.add("issue_C");
        
        input.add("issueList1", list1);
        input.add("issueList2", list2);

        String resultJson = service.checkEquivalence(input.toString());

        JsonObject resultObj = JsonParser.parseString(resultJson).getAsJsonObject();
        JsonArray common = resultObj.getAsJsonArray("commonIssues");
        
        // Only finds B as common
        assertEquals(1, common.size());
        assertEquals("issue_B", common.get(0).getAsString());
    }

    @Test
    public void check_equivalence_No_common_test() {
        JsonObject input = new JsonObject();
        
        JsonArray list1 = new JsonArray();
        list1.add("issue_X");
        
        JsonArray list2 = new JsonArray();
        list2.add("issue_Y");
        
        input.add("issueList1", list1);
        input.add("issueList2", list2);

        String resultJson = service.checkEquivalence(input.toString());

        JsonObject resultObj = JsonParser.parseString(resultJson).getAsJsonObject();
        JsonArray common = resultObj.getAsJsonArray("commonIssues");
        
        // Mocks no shared issues
        assertEquals(0, common.size());
    }
}