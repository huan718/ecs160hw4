package com.ecs160;

import static org.junit.Assert.assertNotNull;
import org.junit.Before;
import org.junit.Test;

import com.ecs160.annotations.Endpoint;
import com.ecs160.annotations.Microservice;

public class MicroserviceRegistryTest {

    private MicroserviceRegistry registry;

    @Before
    public void setUp() {
        registry = new MicroserviceRegistry();
    }

    // ========== Valid Service for Reference ==========
    @Microservice
    public static class ValidService {
        @Endpoint(url = "/ok")
        public String handleRequest(String input) { return "OK"; }
    }

    // ========== Missing @Microservice Annotation ==========
    public static class NoMicroserviceAnnotation {
        @Endpoint(url = "/bad")
        public String handleRequest(String input) { return "BAD"; }
    }

    // ========== Invalid Return Type ==========
    @Microservice
    public static class InvalidReturnTypeService {
        @Endpoint(url = "/wrongReturn")
        public int handleRequest(String input) { return 123; }
    }

    // ========== Invalid Parameter Count ==========
    @Microservice
    public static class InvalidParamCountService {
        @Endpoint(url = "/wrongParams")
        public String handleRequest(String input, String extra) { return "No"; }
    }

    // ========== Invalid Parameter Type ==========
    @Microservice
    public static class InvalidParamTypeService {
        @Endpoint(url = "/wrongType")
        public String handleRequest(int notString) { return "No"; }
    }


    // ===================================================================
    //                            TEST CASES
    // ===================================================================

    @Test(expected = RuntimeException.class)
    public void testMissingMicroserviceAnnotationThrows() {
        registry.register(new NoMicroserviceAnnotation());
    }

    @Test(expected = RuntimeException.class)
    public void testInvalidReturnTypeThrows() {
        registry.register(new InvalidReturnTypeService());
    }

    @Test(expected = RuntimeException.class)
    public void testInvalidParameterCountThrows() {
        registry.register(new InvalidParamCountService());
    }

    @Test(expected = RuntimeException.class)
    public void testInvalidParameterTypeThrows() {
        registry.register(new InvalidParamTypeService());
    }

    @Test
    public void testValidServiceRegistersNormally() {
        registry.register(new ValidService());
        assertNotNull(registry.get("/ok"));
    }
}
