import java.lang.reflect.Method;
import java.util.*;
import org.reflections.Reflections;

import com.ecs160.annotations.Microservice;
import com.ecs160.annotations.Endpoint;
import com.ecs160.EndpointDefinition;

public class MicroserviceRegistry {
    private final Map<String, EndpointDefinition> routes = new HashMap<>();

    public void scanAndRegister(String... packages) {
        if (packages == null || packages.length == 0) {
            throw new IllegalArgumentException("At least one package to scan must be provided.");
        }

        for (String pkg : packages) {
            Reflections reflections = new Reflections(pkg);
            Set<Class<?>> svcClasses = reflections.getTypesAnnotatedWith(Microservice.class);
            for (Class<?> clazz : svcClasses) {
                try {
                    Object instance = clazz.getDeclaredConstructor().newInstance();
                    for (Method m : clazz.getDeclaredMethods()) {
                        Endpoint ep = m.getAnnotation(Endpoint.class);
                        if (ep != null) {
                            validateMethodSignature(m, clazz);
                            String url = normalizeUrl(ep.url());
                            if (routes.containsKey(url)) {
                                throw new IllegalStateException("Duplicate endpoint url: " + url);
                            }
                            m.setAccessible(true);
                            routes.put(url, new EndpointDefinition(instance, m, url));
                        }
                    }
                } catch (ReflectiveOperationException ex) {
                    throw new RuntimeException("Failed to instantiate microservice: " + clazz.getName(), ex);
                }
            }
        }
    }

    private String normalizeUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("Endpoint url cannot be null/blank.");
        }
        if (!url.startsWith("/")) {
            return "/" + url;
        }
        return url;
    }

    private void validateMethodSignature(Method m, Class<?> clazz) {
        // must return String
        if (!String.class.equals(m.getReturnType())) {
            throw new IllegalArgumentException("Method " + m.getName() + " in " + clazz.getName() + " must return String.");
        }
        Class<?>[] params = m.getParameterTypes();
        if (params.length != 1 || !String.class.equals(params[0])) {
            throw new IllegalArgumentException("Method " + m.getName() + " in " + clazz.getName() + " must accept a single String parameter.");
        }
    }

    public Optional<EndpointDefinition> find(String path) {
        return Optional.ofNullable(routes.get(path));
    }

    public Set<String> getRegisteredUrls() {
        return Collections.unmodifiableSet(routes.keySet());
    }
}
