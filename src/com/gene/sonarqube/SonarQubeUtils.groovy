package com.gene.sonarqube

class SonarQubeUtils {
    public static boolean shouldPerformFullSonarQubeScanning(String branchName) {
        // on a regular push we don't scan a project with SonarQube
        // we scan the feature branch on merge requests only.

        // TODO: when we look into the sonarQube new branching mechanism, we should look into ephemeral dashboards for feature branches
        return !branchName || !branchName.matches('(feature|fix)/.*')
    }
}