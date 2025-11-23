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
    private final int port = 8090;
    private final String BASE_URL = "http://localhost:" + port;

    //mock services
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

    private MicroserviceRegistry getRegistry(Launcher launcher) {
        try {
            Field registryField = Launcher.class.getDeclaredField("registry");
            registryField.setAccessible(true);
            return (MicroserviceRegistry) registryField.get(launcher);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to access MicroserviceRegistry field via reflection.", e);
        }
    }
    //setup and shutdown
    @Before
    public void setUp() {
        launcher = new Launcher();
    }

    @After
    public void shutDown() {
        launcher.stopServer();
    }

    @Test
    public void Register_Microservice_Test() {
        HelloService h = new HelloService();
        EchoService e = new EchoService();

        launcher.registerAll(Arrays.asList(h, e));
        MicroserviceRegistry registry = getRegistry(launcher);

        assertNotNull(registry.get("/hello"));
        assertNotNull(registry.get("/echo"));
    }

    @Test
    public void Server_Launch_And_Response_Test() throws Exception {
        launcher.registerAll(Arrays.asList(new HelloService(), new EchoService()));
        launcher.launch(port);
        Thread.sleep(100);

        URL url = new URL(BASE_URL + "/hello");
        HttpURLConnection temp = (HttpURLConnection) url.openConnection();
        
        temp.setRequestMethod("POST");
        temp.setDoOutput(true);

        try (OutputStream os = temp.getOutputStream()) {
            byte[] input = "World".getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        assertEquals(200, temp.getResponseCode());

        BufferedReader br = new BufferedReader(new InputStreamReader(temp.getInputStream()));
        String line = br.readLine().trim();
        br.close();

        assertEquals("Hello World", line);
    }

    @Test
    public void Server_Stop_Test() throws Exception {
        launcher.registerAll(Arrays.asList(new HelloService()));
        launcher.launch(port);
        Thread.sleep(100);

        launcher.stopServer();
        Thread.sleep(100);

        boolean connectionFailed = false;
        try {
            URL url = new URL(BASE_URL + "/hello");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.connect();
            conn.getResponseCode();
        } catch (Exception ex) {
            connectionFailed = true;
        }

        assertTrue(connectionFailed);
    }
}