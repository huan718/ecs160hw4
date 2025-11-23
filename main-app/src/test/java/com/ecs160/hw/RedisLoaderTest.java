package com.ecs160.hw;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

public class RedisLoaderTest {

    private RedisLoader fakeLoader;
    // Simple in-memory storage to act as our "Redis"
    private Map<String, Map<String, String>> mockRedisData;

    @Before
    public void setUp() {
        mockRedisData = new HashMap<>();

        // Initialize the loader as an anonymous subclass overriding the database call
        fakeLoader = new RedisLoader() {
            @Override
            protected Map<String, String> fetchMapFromRedis(String key) {
                // Return data from our local HashMap instead of Jedis
                return mockRedisData.getOrDefault(key, new HashMap<>());
            }
        };
    }

    @Test
    public void testGetRepoReturnsData() {
        // Setup Mock Data
        Map<String, String> repoData = new HashMap<>();
        repoData.put("Url", "http://github.com/test");
        repoData.put("Issues", "1, 2, 3");
        mockRedisData.put("repo-testRepo", repoData);

        // Execute
        Map<String, String> result = fakeLoader.getRepo("testRepo");

        // Verify
        assertNotNull(result);
        assertEquals("http://github.com/test", result.get("Url"));
        assertEquals("1, 2, 3", result.get("Issues"));
    }

    @Test
    public void testGetIssueKeysParsesString() {
        // Setup Mock Data
        Map<String, String> repoData = new HashMap<>();
        repoData.put("Issues", "KEY-1, KEY-2,  KEY-3 "); // includes spaces to test trimming
        mockRedisData.put("repo-myRepo", repoData);

        // Execute
        List<String> keys = fakeLoader.getIssueKeysForRepo("myRepo");

        // Verify
        assertEquals(3, keys.size());
        assertEquals("KEY-1", keys.get(0));
        assertEquals("KEY-2", keys.get(1));
        assertEquals("KEY-3", keys.get(2));
    }

    @Test
    public void testGetIssueKeysReturnsEmptyListWhenNoData() {
        // No data in mockRedisData for "unknownRepo"
        List<String> keys = fakeLoader.getIssueKeysForRepo("unknownRepo");
        assertTrue(keys.isEmpty());
    }

    @Test
    public void testGetIssueKeysReturnsEmptyListWhenFieldMissing() {
        Map<String, String> repoData = new HashMap<>();
        repoData.put("Url", "http://something");
        // "Issues" field is missing
        mockRedisData.put("repo-emptyRepo", repoData);

        List<String> keys = fakeLoader.getIssueKeysForRepo("emptyRepo");
        assertTrue(keys.isEmpty());
    }

    @Test
    public void testGetIssueDescription() {
        Map<String, String> issueData = new HashMap<>();
        issueData.put("Description", "This is a bug");
        mockRedisData.put("ISSUE-101", issueData);

        String desc = fakeLoader.getIssueDescription("ISSUE-101");
        assertEquals("This is a bug", desc);
    }

    @Test
    public void testGetIssueDescriptionReturnsEmptyIfMissing() {
        Map<String, String> issueData = new HashMap<>();
        issueData.put("Status", "Open");
        mockRedisData.put("ISSUE-102", issueData);

        String desc = fakeLoader.getIssueDescription("ISSUE-102");
        assertEquals("", desc);
    }
}