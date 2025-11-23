package com.ecs160.hw;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class SelectedRepoConfigLoader {

    public SelectedRepoConfig load() throws IOException {
        List<Path> candidates = getCandidatePaths();

        Path found = null;
        for (Path p : candidates) {
            if (Files.exists(p)) {
                found = p;
                break;
            }
        }

        if (found == null) {
            throw new IOException("selected_repo.dat not found in expected locations.");
        }

        List<String> lines = Files.readAllLines(found);
        List<String> cleaned = new ArrayList<>();
        for (String line : lines) {
            String t = line.trim();
            if (!t.isEmpty() && !t.startsWith("#")) {
                cleaned.add(t);
            }
        }

        if (cleaned.isEmpty()) {
            throw new IOException("selected_repo.dat is empty.");
        }

        String repoName = cleaned.get(0);
        List<String> cFiles = cleaned.subList(1, cleaned.size());

        System.out.println("[INFO] Loaded selected_repo.dat from " + found
                + ": repo=" + repoName + ", files=" + cFiles);
        return new SelectedRepoConfig(repoName, new ArrayList<>(cFiles));
    }

    /**
     * Returns the list of paths to check for the config file.
     * Protected so tests can override it.
     */
    protected List<Path> getCandidatePaths() {
        return List.of(
                Paths.get("selected_repo.dat"),
                Paths.get("main-app/src/main/java/com/ecs160/hw/resources/selected_repo.dat"),
                Paths.get("src/main/java/com/ecs160/hw/resources/selected_repo.dat")
        );
    }
}