package com.ecs160;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

import com.ecs160.annotations.Endpoint;
import com.ecs160.annotations.Microservice;

public class LauncherTest {

    private Launcher launcher;
    private final int port = 8090;  // test port
    private final String BASE_URL = "http://localhost:" + port;

    // --- Mock Services ---

    @Microservice
    public static class HelloService {
        @Endpoint(url = "/hello")
        public String handleRequest(String input) {
            return "Hello " + input;
        }
    }

    @Microservice
    public static class EchoService {
        @Endpoint(url = "/echo")
        public String handleRequest(String input) {
            return "Echo: " + input;
        }
    }

    // --- Reflection Helper ---
    
    private MicroserviceRegistry getRegistry(Launcher launcher) {
        try {
            Field registryField = Launcher.class.getDeclaredField("registry");
            registryField.setAccessible(true);
            return (MicroserviceRegistry) registryField.get(launcher);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to access MicroserviceRegistry field via reflection.", e);
        }
    }

    // --- Setup and Teardown ---

    @Before
    public void setUp() {
        launcher = new Launcher();
    }

    @After
    public void tearDown() {
        launcher.stopServer();
    }

    // -------------------------------------------------------------------
    // Test 1: registerAll should store all endpoints in registry
    // -------------------------------------------------------------------
    @Test
    public void testRegisterAllRegistersAllMicroservices() {
        HelloService h = new HelloService();
        EchoService e = new EchoService();

        launcher.registerAll(Arrays.asList(h, e));

        MicroserviceRegistry registry = getRegistry(launcher);

        assertNotNull("The /hello endpoint should be registered.", registry.get("/hello"));
        assertNotNull("The /echo endpoint should be registered.", registry.get("/echo"));
    }

    // -------------------------------------------------------------------
    // Test 2: launch() starts the HTTP server and responds to requests
    // -------------------------------------------------------------------
    @Test
    public void testLauncherStartsServerAndResponds() throws Exception {
        // Register services
        launcher.registerAll(Arrays.asList(new HelloService(), new EchoService()));

        // Launch server
        launcher.launch(port);

        // Allow server time to bind
        Thread.sleep(150);

        // 1. Use clean URL (no query params)
        URL url = new URL(BASE_URL + "/hello");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        
        // 2. Use POST to send body data
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);

        // 3. Write "World" to the request body
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = "World".getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        assertEquals(200, conn.getResponseCode());

        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line = br.readLine().trim();
        br.close();

        // Service returns "Hello " + input -> "Hello World"
        assertEquals("The /hello endpoint should return the correct response.", "Hello World", line);
    }

    // -------------------------------------------------------------------
    // Test 3: Server should stop cleanly
    // -------------------------------------------------------------------
    @Test
    public void testStopServerStopsHttpServer() throws Exception {
        launcher.registerAll(Arrays.asList(new HelloService()));
        launcher.launch(port);
        Thread.sleep(100);

        launcher.stopServer();
        Thread.sleep(100);

        boolean connectionFailed = false;

        try {
            URL url = new URL(BASE_URL + "/hello");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            // Try to connect (should fail)
            conn.connect(); 
            conn.getResponseCode(); 
        } catch (Exception ex) {
            connectionFailed = true;
        }

        assertTrue("Server should stop and refuse new connections", connectionFailed);
    }
}