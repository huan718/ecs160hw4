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

    //mock services
    @Microservice
    public static class ValidService {
        @Endpoint(url = "/ok")
        public String handleRequest(String input) { return "OK"; }
    }

    public static class NoMicroserviceAnnotation {
        @Endpoint(url = "/bad")
        public String handleRequest(String input) { return "BAD"; }
    }

    @Microservice
    public static class InvalidReturnTypeService {
        @Endpoint(url = "/wrongReturn")
        public int handleRequest(String input) { return 123; }
    }

    @Microservice
    public static class InvalidParamCountService {
        @Endpoint(url = "/wrongParams")
        public String handleRequest(String input, String extra) { return "No"; }
    }

    @Microservice
    public static class InvalidParamTypeService {
        @Endpoint(url = "/wrongType")
        public String handleRequest(int notString) { return "No"; }
    }

    //test cases
    @Test(expected = RuntimeException.class)
    public void No_Annotation_Test() {
        registry.register(new NoMicroserviceAnnotation());
    }

    @Test(expected = RuntimeException.class)
    public void Invalid_Return_Type_Test() {
        registry.register(new InvalidReturnTypeService());
    }

    @Test(expected = RuntimeException.class)
    public void Invalid_Param_Count_Test() {
        registry.register(new InvalidParamCountService());
    }

    @Test(expected = RuntimeException.class)
    public void Invalid_Param_Type_Test() {
        registry.register(new InvalidParamTypeService());
    }

    @Test
    public void Valid_Serivce_Test() {
        registry.register(new ValidService());
        assertNotNull(registry.get("/ok"));
    }
}