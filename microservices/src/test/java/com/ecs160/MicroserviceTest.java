// delete later

package com.ecs160.tests;

import okhttp3.*;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class MicroserviceTest {

    private static final OkHttpClient client = new OkHttpClient();
    private static final String BASE_URL = "http://localhost:8080";

    // Helper method to send GET request with JSON as query param
    private static String sendGetRequest(String endpoint, String jsonInput) {
        try {
            String encodedInput = URLEncoder.encode(jsonInput, StandardCharsets.UTF_8.toString());
            String url = BASE_URL + "/" + endpoint + "?input=" + encodedInput;

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    return response.body().string();
                } else {
                    return "Request failed with code: " + response.code();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void main(String[] args) {
        // Test Microservice A: Issue Summarizer
        String issueJson = "{\"title\":\"Login bug\",\"body\":\"Users cannot log in after update\"}";
        String summaryResponse = sendGetRequest("summarize_issue", issueJson);
        System.out.println("=== Issue Summarizer Response ===");
        System.out.println(summaryResponse);

        // Test Microservice B: Bug Finder
        String cFileJson = "{\"code\":\"int main() { int x; return x; }\"}";
        String bugFinderResponse = sendGetRequest("find_bugs", cFileJson);
        System.out.println("\n=== Bug Finder Response ===");
        System.out.println(bugFinderResponse);

        // Test Microservice C: Issue Summary Comparator
        String listJson = "{\"list1\":[{\"title\":\"Bug1\"}],\"list2\":[{\"title\":\"Bug1\"},{\"title\":\"Bug2\"}]}";
        String comparatorResponse = sendGetRequest("check_equivalence", listJson);
        System.out.println("\n=== Issue Comparator Response ===");
        System.out.println(comparatorResponse);
    }
}
