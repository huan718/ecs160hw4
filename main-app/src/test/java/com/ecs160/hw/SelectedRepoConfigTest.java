package com.ecs160.hw;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class SelectedRepoConfigTest {

    @Test
    public void Retain_Config_Data_Test() {
        String repoName = "test-user/test-repo";
        List<String> files = Arrays.asList("file1.c", "src/file2.c");

        SelectedRepoConfig config = new SelectedRepoConfig(repoName, files);

        assertEquals(repoName, config.getRepoName());
        assertEquals(files, config.getCFiles());
        assertEquals(2, config.getCFiles().size());
    }
}