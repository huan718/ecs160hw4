package com.ecs160.hw;

import java.util.List;

public class SelectedRepoConfig {
    private final String repoName;
    private final List<String> cFiles;

    public SelectedRepoConfig(String repoName, List<String> cFiles) {
        this.repoName = repoName;
        this.cFiles = cFiles;
    }

    public String getRepoName() {
        return repoName;
    }

    public List<String> getCFiles() {
        return cFiles;
    }
}
