import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

public class Launcher {

    private final MicroserviceRegistry registry;

    public MicroserviceLauncher(MicroserviceRegistry registry) {
        this.registry = registry;
    }

    public boolean launch(int port, String... packagesToScan) {
        try {
            registry.scanAndRegister(packagesToScan);

            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

            // create contexts for all registered URLs
            for (String url : registry.getRegisteredUrls()) {
                server.createContext(url, new DispatchHandler(registry));
            }

            server.setExecutor(Executors.newCachedThreadPool());
            server.start();

            System.out.println("Microservice server started on port " + port + ". Registered endpoints: " + registry.getRegisteredUrls());

            // block indefinitely until interrupted
            try {
                Thread.currentThread().join();
                // unreachable normally
                return true;
            } catch (InterruptedException e) {
                // shutdown requested
                server.stop(0);
                System.out.println("Microservice server stopped.");
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static class DispatchHandler implements HttpHandler {
        private final MicroserviceRegistry registry;

        DispatchHandler(MicroserviceRegistry registry) {
            this.registry = registry;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            try {
                EndpointDefinition def = registry.find(path)
                        .orElseThrow(() -> new IllegalStateException("No endpoint mapped for path: " + path));

                String requestBody = readAll(exchange.getRequestBody());
                String result = invoke(def, requestBody);

                byte[] bytes = result.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } catch (Throwable t) {
                t.printStackTrace();
                String err = "Error handling request: " + t.getMessage();
                byte[] bytes = err.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
                exchange.sendResponseHeaders(500, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } finally {
                exchange.close();
            }
        }

        private String readAll(InputStream is) throws IOException {
            if (is == null) return "";
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                if (sb.length() > 0) sb.setLength(sb.length() - 1); // drop trailing newline
                return sb.toString();
            }
        }

        private String invoke(EndpointDefinition def, String input) {
            try {
                Object r = def.getMethod().invoke(def.getInstance(), input);
                return r == null ? "" : r.toString();
            } catch (IllegalAccessException | InvocationTargetException ex) {
                throw new RuntimeException("Failed to invoke endpoint method", ex);
            }
        }
    }
}