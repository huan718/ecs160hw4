package com.ecs160.hw;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class GitRepoClonerTest {

    public TemporaryFolder tempFolder = new TemporaryFolder();

    private void deleteDirectory(File file) {
        File[] contents = file.listFiles();
        if (contents != null) {
            for (File f : contents) {
                deleteDirectory(f);
            }
        }
        file.delete();
    }

    @Test
    public void Returns_Git_DIR_IF_EXIST_Test() throws IOException, InterruptedException {
        File clonesDir = new File("clones");
        File repoDir = new File(clonesDir, "test-repo");
        File gitDir = new File(repoDir, ".git");

        try {
            assertTrue(gitDir.mkdirs());

            GitRepositoryCloner cloner = new GitRepositoryCloner();
            Path result = cloner.ensureRepoCloned("test-repo", "http://fake.url");

            assertEquals(repoDir.toPath().toAbsolutePath(), result.toAbsolutePath());
            
        } finally {
            deleteDirectory(clonesDir);
        }
    }

    @Test
    public void Returns_DIR_if_GIT_Doesnt_exist_Test() throws IOException, InterruptedException {
        File clonesDir = new File("clones");
        File repoDir = new File(clonesDir, "test-repo-no-git");

        try {
            assertTrue(repoDir.mkdirs());

            GitRepositoryCloner cloner = new GitRepositoryCloner();
            Path result = cloner.ensureRepoCloned("test-repo-no-git", "http://fake.url");

            assertEquals(repoDir.toPath().toAbsolutePath(), result.toAbsolutePath());

        } finally {
            deleteDirectory(clonesDir);
        }
    }
}