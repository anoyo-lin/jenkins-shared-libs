package com.gene.sonarqube

class EnvironmentVariablesInitializer {
    public static getSonarQubeRTokenName(String environment ) {
        if("Production" == environment ) {
            return 'SonarQubeToken'
        }
        return 'SonarQubeToken_Test'
    }
    public static getSonarQubeServerName(String environment) {
        if ("Production" == environment ) {
            return "Sonar Server (main)"
        }
        return 'Sonar (UAT server)'
    }
    public static getSonarQubeServerURL(String environment) {
        if ("Production" == environment ) {
            return 'https://sonar.gene.com'
        }
        return 'https://sonar-test.gene.com'
    }
}