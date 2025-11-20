// debug test code edit/delete later

package com.ecs160.tests;

import okhttp3.*;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class TestClient {

    private static final OkHttpClient client = new OkHttpClient();
    private static final String BASE_URL = "http://localhost:8080";

    private static String sendGet(String endpoint, String jsonInput) throws IOException {
        String encoded = URLEncoder.encode(jsonInput, StandardCharsets.UTF_8);
        String url = BASE_URL + endpoint + "?input=" + encoded;
        Request request = new Request.Builder().url(url).get().build();
        try (Response response = client.newCall(request).execute()) {
            return response.isSuccessful() && response.body() != null
                ? response.body().string()
                : "Request failed: " + response.code();
        }
    }

    public static void main(String[] args) throws IOException {
        System.out.println("=== summarize_issue ===");
        System.out.println(sendGet("/summarize_issue", "{\"title\":\"Login bug\",\"body\":\"Cannot login\"}"));

        System.out.println("=== find_bugs ===");
        System.out.println(sendGet("/find_bugs", "{\"code\":\"int main() { int x; return x; }\"}"));

        System.out.println("=== check_equivalence ===");
        System.out.println(sendGet("/check_equivalence",
            "{\"list1\":[{\"title\":\"Bug1\"}],\"list2\":[{\"title\":\"Bug1\"},{\"title\":\"Bug2\"}]}"));
    }
}
