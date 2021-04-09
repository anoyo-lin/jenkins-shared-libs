package com.gene.sonarqube

public class SonarQubeUtil {
    public static void scanCliWithNotBranchSupport(
        Script scriptObj,
        String jobName,
        String branchName ) {
        /*
         * This is to support projects that use free Community Version of SonarQube.
         * Run Sonar scan using CLI to a SonarQube Server instance with no branch support.
         * There will be one SonarQube project per scanning result of each branch.
         * Branch support comes with Developer Edition of SonarQube server Which requires license.
         */
         def sonarScannerHome = scriptObj.tool "Sonar"
         def sonarProperties = scriptObj.readFile "sonar-project.properties"
         scriptObj.echo "Run Sonar Scanner with properties"
         scriptObj.echo "-------------------------"
         scriptObj.echo "${sonarProperties}"
         scriptObj.echo "-------------------------"
         def sonarProjectNamePrefix = jobName.split("/")[-2]
         scriptObj.echo "sonarProjectNamePrefix=${sonarProjectNamePrefix}"
         def sonarProjectNameSuffix = branchName.replaceAll("/", "_")
         scriptObj.echo "sonarProjectNameSuffix=${sonarProjectNameSuffix}"
         def sonarProjectName = "${sonarProjectNamePrefix}-${sonarProjectNameSuffix}"

         def pom = scriptObj.readMavenPom file: "pom.xml"
         def sonarProjectVersion = pom.getVersion()
         scriptObj.echo "sonarProjectVersion=${sonarProjectVersion}"
         def sonarExtraArgs = " -Dsonar.projectVersion=${sonarProjectVersion}"

         scriptObj.sh "sonar-scanner -Dsonar.projectKey=${sonarProjectName} -Dsonar.projectName=${sonarProjectName} ${sonarExtraArgs}"
    }
}