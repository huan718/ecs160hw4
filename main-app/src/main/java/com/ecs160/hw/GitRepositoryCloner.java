package com.ecs160.hw;

import java.io.IOException;
import java.nio.file.*;

public class GitRepositoryCloner {

    /**
     * Ensures the repo is cloned under ./clones/<repoName>.
     * If it already exists, we just reuse it.
     */
    public Path ensureRepoCloned(String repoName, String url)
            throws IOException, InterruptedException {

        Path clonesDir = Paths.get("clones");
        if (!Files.exists(clonesDir)) {
            Files.createDirectories(clonesDir);
        }

        Path repoDir = clonesDir.resolve(repoName);

        // If .git exists, assume it's already cloned
        if (Files.exists(repoDir.resolve(".git"))) {
            System.out.println("[INFO] Using existing clone at " + repoDir);
            return repoDir;
        }

        if (Files.exists(repoDir)) {
            System.out.println("[INFO] Directory " + repoDir + " exists but no .git – reusing anyway.");
            return repoDir;
        }

        System.out.println("[INFO] Cloning repo from " + url + " into " + repoDir);

        ProcessBuilder pb = new ProcessBuilder(
                "git", "clone", "--depth", "1", url, repoDir.toString());
        pb.inheritIO();
        Process proc = pb.start();
        int exitCode = proc.waitFor();
        if (exitCode != 0) {
            System.err.println("[WARN] git clone exited with code " + exitCode);
        }

        return repoDir;
    }
}
