package com.ecs160;

import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assume.assumeNotNull;
import org.junit.BeforeClass;
import org.junit.Test;

public class EndpointTest {
    // mocked data
    static class MockObject {
        public String getData(int value) {
            return "Data " + value;
        }
    }

    private static final Object MOCK_INSTANCE = new MockObject();
    private static Method MOCK_METHOD;
    private static final String MOCK_URL = "/api/v1/data";

    @BeforeClass
    public static void setup() throws NoSuchMethodException {
        // FIXED 1: Method name must match MockObject ("getData", not "data")
        MOCK_METHOD = MockObject.class.getMethod("getData", int.class);
        assumeNotNull(MOCK_METHOD); 
    }

    // --- Test Cases ---

    @Test
    public void testConstructorAndGetters_validInput() {
        // FIXED 2: Changed 'Endpointdef' to 'Endpoint'
        Endpointdef endpoint = new Endpointdef(MOCK_INSTANCE, MOCK_METHOD, MOCK_URL);

        assertNotNull(endpoint);
        assertSame(MOCK_INSTANCE, endpoint.getInstance());
        assertSame(MOCK_METHOD, endpoint.getMethod());
        assertEquals(MOCK_URL, endpoint.getUrl());
    }

    // null tests
    @Test(expected = NullPointerException.class)
    public void nullinstance_test() {
        new Endpointdef(null, MOCK_METHOD, MOCK_URL);
    }

    @Test(expected = NullPointerException.class)
    public void nullmethod_test() {
        new Endpointdef(MOCK_INSTANCE, null, MOCK_URL);
    }

    @Test(expected = NullPointerException.class)
    public void nullurl_test() {
        new Endpointdef(MOCK_INSTANCE, MOCK_METHOD, null);
    }
}