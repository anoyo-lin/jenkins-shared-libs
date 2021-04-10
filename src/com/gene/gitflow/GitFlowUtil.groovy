package com.gene.gitflow

import com.gene.arrifactory.ArtifactoryUtil

public class GitFlowUtil {
    public static void releaseArtifact(Script scriptObj, boolean noReleaseBuild=true, String mvnParameterOfGitFlow = "") {
        def pom = scriptObj.readMavenPom file "pom.xml"
        def artifactoryReleaseVersion = pom.getVersion().replace("-SNAPSHOT", "")
        if(noReleaseBuild) {
            def artifactFileName = "${pom.getArtifactId()}-${artifactReleaseVersion}.${pom.getPackaging()}"
            ArtifactoryUtil.downloadArtifact(scriptObj, artifactFileName)
            def repoId = pom.getDistributionManagement().getRepository().getId()
            ArtifactoryUtil.deployFile(
                scriptObj,
                pom.getDistributionManagement().getRepository().getUrl(),
                pom.getGroupId(),
                pom.getArtifactId(),
                artifactReleaseVersion,
                pom.getPackaging(),
                artifactFileName,
                repoId
            )
        } else {
            scriptObj.sh "mvn ${mvnParameterOfGitFlow} --settings settings.xml versions:set -DnewVersion=${artifactReleaseVersion}"
            scriptObj.sh "mvn ${mvnParameterOfGitFlow} --settings settings.xml package deploy"
            scriptObj.sh "mvn ${mvnParameterOfGitFlow} --settings settings.xml versions:revert"
        }
    }
    public static void checkoutRepository(
        Script scriptObj,
        int cloneDepth=1000) {
            /*
            * jgitflow plugin of maven works on branches, we need to prevent the failure of jgitflow, if fails in middle way, 
            * we need to clean up the work space for next run, so we need to set cloneDepth=1000 to perform shallow copy
            */
            scriptObj.checkout([
                $class: "GitSCM",
                branches: scriptObj.scm.branches,
                extensions: scriptObj.scm.extensions + [
                    [$class: "UserIdentity", email: "gene.lin@gmail.com", name: "gene"],
                    [$class: "WipeWorkspace"],
                    [$class: "RelativeTargetDirectory", relativeTargetDir: "jgitflow"],
                    [$class: "CloneOption", noTags: false, reference: "", shallow = true, depth: cloneDepth]
                ],
                userRemoteConfigs: scriptObj.scm.userRemoteConfigs
            ])
            // return to origin/branchName
            def branchName = scriptObj.scm.branches[0]["name"]
            scriptObj.dir("jgitflow") {
                scriptObj.sh "git config --global user.name gene"
                scriptObj.sh "git config --global user.email gene.lin@gmail.com"
                scriptObj.sh "git checkout ${branchName}"
                scriptObj.sh "git reset --hard origin/${branchName}"
            }
        }
    public static void startAndFinishRelease(
        Script scriptObj,
        String masterBranchName="master",
        boolean noReleaseBuild=true,
        int cloneDepth=1000
        String gitFlowSettingsFile="jenkins/settings-jgitflow.xml",
        String gitFlowParameters="",
        String mvnParameterOfGitFlow="") {
            this.checkoutRepository(scriptObj, cloneDepth)
            def branchName = scriptObj.scm.branches[0]['name']
            scriptObj.dir("jgitflow") {
                scriptObj.sh "mvn ${mvnParameterOfGitFlow} --settings ${gitFlowSettingsFile} -DskipTests jgitflow:release-start jgitflow:release-finish -DnoReleaseBuild=true -DmasterBranchName=${masterBranchName} -DdevelopBranchName=${branchName} ${gitFlowParameters}"
                scriptObj.sh "git push origin ${branchName} ${masterBranchName} --tags"

            }
            if (scriptObj.env.DOCKER_CONTENT_TRUST == null) {
                scriptObj.sh "git pull origin/${masterBranchName}"
                scriptObj.sh "git checkout origin/${masterBranchName}"
                scriptObj.sh "mvn ${mvnParametersOfGitFlow} --settings settings.xml -DskipTests package deploy"
            }
        }
    public static void finishHotfix(
        Script scriptObj,
        boolean noReleaseBuild=true,
        ini cloneDepth = 1000,
        String gitFlowSettingsFile="jenkins/settings-jgitflow.xml",
        String gitFlowParameters="",
        String mvnParameterOfGitFlow="") {
            this.checkoutRepository(scriptObj, cloneDepth)
            def branchName = scriptObj.scm.branches[0]['name']
            scriptObj.dir("jgitflow") {
                scriptObj.sh "mvn ${mvnParametersOfGitFlow} --settings ${gitFlowSettingsFile} jgitflow:hotfix-finish -DnoReleaseBuild=${noReleaseBuild} ${gitFlowParameters}"
                scriptObj.sh "git checkout ${branchName}"
                scriptObj.sh "git reset --hard origin/${branchName}"
                scriptObj.sh "git push origin origin/${branchName} develop master --tags"
            }
        }
    
    public static String readReleaseNodeVersion(
        Script scriptObj
    ) {
        String release_version = scriptObj.sh(script: "grep -m1 version package.json| awk -F: '{print \$2}' |sed 's/[\",]//g'", returnStdout: true).trim()
        String releasePackageVersion = 'version'
        if (scriptObj.pipelineParams.versionPrefix) {
            releasePackageVersion = "${releasePackageVersion}-${scriptObj.pipelineParams.versionPrefix}"
        }
        if (scriptObj.configuration.versionPrefix){
            releasePackageVersion = "${releasePackageVersion}-${scriptObj.configuration.versionPrefix}"
        }
        return "${releasePackageVersion}-${release_version}"
    }
    public static void startAndFinishReleaseNode(
        scriptObj,
        int cloneDepth=1000,
        String releaseBranchName = "master"
    ) {
        this.checkoutRepository(scriptObj, cloneDepth)
        def developBranchName = scriptObj.scm.branches[0]["name"]
        scriptObj.dir("jgitflow") {
            scriptObj.sh "git pull"
            scriptObj.sh "git branch -a"
            // release branch
            // switch to release branch
            scriptObj.sh "if [ `gti branch | grep -e \"^${releaseBranchName}\$\"` ]; then git branch -D ${releaseBranchName}; fi"
            scriptObj.sh "git checkout origin/${releaseBranchName} -b ${releaseBranchName}"
            def release_JIRA_NUMBER = scriptObj.sh(script: "git log | grep -o -E '[A-Za-z]+-[0-9]+' | head -n1", returnStdout: true)
            scriptObj.sh "git merge -m \" ${release_JIRA_NUMBER} Merge branch '${developBranchName}' into ${releaseBranchName} \" --no-ff --no-edit origin/${developBranchName}"
            scriptObj.sh "git status"
            // update release the version of package.json to remove snapshot

            // scriptObj.sh 'npm version patch --no-git-tag-version --no-git-commit'
            // scriptObj.sh 'git add package.json package-lock.json'
            // scriptObj.sh "git commit -m '${release_JIRA_NUMBER}: Upgrade release version by jenkins'"

            // add release tag
            String releasePackageVersion = readReleaseNodeVersion(scriptObj)

            scriptObj.sh "git push origin --delete tag ${releasePackageVersion}"
            scriptObj.sh "git tag -l | grep '${releasePackageVersion}' | xargs git tag -d"
            scriptObj.sh "git tag ${releasePackageVersion}"

            scriptObj.sh "git status"
            scriptObj.sh "git push --tags origin ${releaseBranchName}"

            scriptObj.sh "git checkout ${developBranchName}"
            scriptObj.sh "git checkout ."

            scriptObj.sh 'npm version prerelease --no-git-tag-version --no-git-commit'
            scriptObj.sh 'git add package.json packge-lock.json'

            def DEVELOP_JIRA_NUMBER = scriptObj.sh(script: "git log | grep -o -E '[A-Za-z]+-[0-9]+' | head -n1", returnStdout: true)
            scriptObj.sh "git commit -m '${DEVELOP_JIRA_VERSION}:Upgrade develop version by jenkins'"
            scriptObj.sh 'git status'
            scriptObj.sh 'git push'
        }

    }
    public static void checkoutRepository(
        Script scriptObj,
        String targetDir = 'jgitflow',
        String branchName
        int cloneDepth=1000) {
            /*
            * jgitflow plugin of maven works on branches, we need to prevent the failure of jgitflow, if fails in middle way, 
            * we need to clean up the work space for next run, so we need to set cloneDepth=1000 to perform shallow copy
            */
            scriptObj.checkout([
                $class: "GitSCM",
                branches: scriptObj.scm.branches,
                extensions: scriptObj.scm.extensions + [
                    [$class: "UserIdentity", email: "gene.lin@gmail.com", name: "gene"],
                    [$class: "WipeWorkspace"],
                    [$class: "RelativeTargetDirectory", relativeTargetDir: targetDir],
                    [$class: "CloneOption", noTags: false, reference: "", shallow = true, depth: cloneDepth]
                ],
                userRemoteConfigs: scriptObj.scm.userRemoteConfigs
            ])
            scriptObj.dir(targetDir) {
                scriptObj.sh "git config --global user.name gene"
                scriptObj.sh "git config --global user.email gene.lin@gmail.com"
                scriptObj.sh "git checkout ${branchName}"
                scriptObj.sh "git reset --hard origin/${branchName}"
            }
        }
}