package com.ecs160;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

import com.ecs160.clients.OllamaClient;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class OllamaTest {

    @Test
    public void Ask_Successful_Test() throws IOException {
        String fakeJsonBody = "{\"response\": \"Hello World\", \"done\": true}";
        
        OkHttpClient mockHttpClient = new OkHttpClient() {
            @Override
            public Call newCall(Request request) {
                return new MockCall(request, fakeJsonBody);
            }
        };

        OllamaClient client = new OllamaClient("http://test-url", "test-model", mockHttpClient);

        String result = client.ask("Say hi");

        assertEquals("Hello World", result);
    }

    @Test(expected = IOException.class)
    public void Ask_Networkfailure_Test() throws IOException {
        OkHttpClient mockHttpClient = new OkHttpClient() {
            @Override
            public Call newCall(Request request) {
                return new MockCall(request, null) {
                    @Override
                    public Response execute() throws IOException {
                        throw new IOException("Network error");
                    }
                };
            }
        };

        OllamaClient client = new OllamaClient("http://test-url", "test-model", mockHttpClient);

        client.ask("Crash me");
    }

    //mock call
    private static class MockCall implements Call {
        private final Request request;
        private final String responseBody;

        public MockCall(Request request, String responseBody) {
            this.request = request;
            this.responseBody = responseBody;
        }

        @Override
        public Response execute() throws IOException {
            return new Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(ResponseBody.create(responseBody, MediaType.get("application/json")))
                    .build();
        }
        //unused methods
        @Override public Request request() { return request; }
        @Override public void enqueue(Callback r) { }
        @Override public void cancel() { }
        @Override public boolean isExecuted() { return true; }
        @Override public boolean isCanceled() { return false; }
        @Override public Call clone() { return this; }
        @Override public okio.Timeout timeout() { return okio.Timeout.NONE; }
    }
}