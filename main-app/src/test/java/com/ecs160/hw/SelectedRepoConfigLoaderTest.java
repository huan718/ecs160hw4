package com.ecs160.hw;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Before;
import org.junit.Test;

public class SelectedRepoConfigLoaderTest {

    private final String TEST_FILENAME = "test_selected_repo.dat";
    private File tempFile;
    private SelectedRepoConfigLoader testLoader;

    @Before
    public void setUp() {
        tempFile = new File(TEST_FILENAME);
        if (tempFile.exists()) {
            tempFile.delete();
        }

        testLoader = new SelectedRepoConfigLoader() {
            @Override
            protected List<Path> getCandidatePaths() {
                return List.of(tempFile.toPath());
            }
        };
    }

    @After
    public void shutDown() {
        if (tempFile != null && tempFile.exists()) {
            tempFile.delete();
        }
    }

    @Test
    public void Load_Valid_Config_Test() throws IOException {
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("# This is a comment\n");
            writer.write("author/repo-name\n");
            writer.write("src/main.c\n");
            writer.write("src/utils.c\n");
        }

        SelectedRepoConfig config = testLoader.load();

        assertNotNull(config);
        assertEquals("author/repo-name", config.getRepoName());
        assertEquals(2, config.getCFiles().size());
        assertEquals("src/main.c", config.getCFiles().get(0));
        assertEquals("src/utils.c", config.getCFiles().get(1));
    }

    @Test(expected = IOException.class)
    public void Load_Empty_File_Test() throws IOException {
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("");
        }
        testLoader.load();
    }

    @Test(expected = IOException.class)
    public void Load_Missing_File_Test() throws IOException {
        testLoader.load();
    }
}