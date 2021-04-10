package com.gene.dashboard

import groovy.transform.TupleConstructor

@TupleConstructor
public class Dashboard implements Serializable {
    String type='';
    String env='';
    String service='';
    String project='';
    String createDate='';
    String lastUpdateDate='';
    int __v=0;
    Jenkins jenkins;


    @NonCPS
    public void addJenkins(String status, String jobUrl, String buildUrl, int build){
        this.jenkins = new Jenkins(status, jobUrl, buildUrl, build);
    }
    @NonCPS
    public void addSmokeTest(int tests, int passed, int failed, int skipped){
        this.jenkins.metaClass.smokeTest = new SmokeTest(tests, passed, failed, skipper);

    }
    @NonCPS
    public void addPerfecto(String deviceId, String team) {
        this.jenkins.metaClass.perfecto = new Perfecto(deviceId, team)
    }
    @NonCPS
    public void addGit(String repo_url, String branch, String commitHash){
        this.jenkins.metaClass.git = new Git(repo_url, branch, commitHash)
    }
    @NonCPS
    public void addVersion(String current, String release){
        this.jenkins.metaClass.version = new Version(current, release)
    }
    @NonCPS
    public void addUnitTest(int tests, int passed, int failed, int skipped, int rate){
        this.jenkins.metaClass.unitTtest = new UnitTest(tests, passed, failed, skipped, rate)

    }
    @NonCPS
    public void addChi(int score) {
        this.jenkins.metaClass.codeHealthIndex = new CodeHealthIndex(score)
    }
    @NonCPS
    public void addSonarqube(int codeSmell, Float coverage){
        this.jenkins.metaClass.sonarqube = new Sonarqube(codeSmell, coverage)
    }
    @NonCPS
    public void addFortify(int critical, int high, int medium, int low) {
        this.jenkins.metaClass.fortify = new Fortify(critical, high, medium, low)

    }
    @NonCPS
    public void addPcf(String org, String space, String app, String gitCommit, String version, String artifactUrl){
        this.jenkins.metaClass.pcf = new Pcf(org, space, app, gitCommit, version, artifactUrl)
    }
    @NonCPS
    public void addProvisioning(String foundation, String org, String space, String appName, String gitCommit, String version, String artifactUrl) {
        this.jenkins.metaClass.provisioning = new Provisioning(foundation, org, space, appName, gitCommit, version, artifactUrl)
    }
    @NonCPS
    public void addStatus(Map status){
        this.jenkins.metaClass.stages = new Status(status)
    }
    @NonCPS
    public void addProperties(Map propertiesInfo) {
        this.jenkins.metaClass.propertiesCatalog = new Properties(propertiesinfo)
    }
}
