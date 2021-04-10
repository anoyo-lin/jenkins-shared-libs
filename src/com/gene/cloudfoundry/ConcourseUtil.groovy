package com.gene.cloudfoundry

import com.gene.artifactory.ArtifactoryUtil

public class ConcourserUtil {
    public static void authenticateWithUAA(
        Script scriptObj,
        String url,
        String teamUrl,
        String target,
        String pcfUrl,
        String pcfCredentialUser,
        String pcfCredentialPassword,
        String pcfSpace
    ) {
        def CONCOURSE_TOKEN = scriptObj.sh (
            script: "cd /tech/UAA-Tool && node index.js ${pcfUrl} ${teamUrl}${pcfSpace} '${pcfCredentialUser}' '${pcfCredentialPassword}'",
            returnStdout: true
        ).trim()
        scriptObj.echo "CONCOURSE_TOKEN: ${CONCOURSE_TOKEN}"
        if (CONCOURSE_TOKEN.trim()) {
            scriptObj.echo "Starting UAA Login ..."
            scriptObj.sh """fly --target ${target} login --team-name ${pcfSpace} --concourse-url ${url} --insecure << EOF
            ${CONCOURSE_TOKEN}
            EOF"""
        } else {
            scriptObj.currentBuild.result = "FAILED"
            scriptObj.error("Fail to obtain Concourse token.")
        }
    }
    public static void deployPipeline(
        Script scriptObj,
        String target,
        String pipelineName,
        String pipelineConfiguration,
        String pipelineCredentials,
        String pipelineVariables,
        String concourseJobName,
        String targetEnvironment) {
            def pom = scriptObj.readMavenPom file: "pom.xml"

            def groupId = pom.getGroupId()
            def artifactId = pom.getArtifactId()
            def artifactPackaging = pom.getPackaging()

            /*
            this two variable will now be assigned dynamically
            depending on the target environment. If artifact will be deployed to higher environment (sit, uat, preprod, prod)
            automatically it will look for the release repository in the artifactory. version was also been manipulated to remove 
            the -SNAPSHOT.
            */

            def repoUrl
            def artifactVersion

            if( targetEnvironment == null || targetEnvironment.contains('dev') ) {
                artifactVersion = pom.getVersion()
                repoUrl = pom.getVersion().endsWith("-SNAPSHOT") ? pom.getDistributionManagement().getSnapshotRepository().getUrl() : pom.getDistributionManagement().getUrl()
            } else {
                artifactVersion = pom.getVersion().replace("-SNAPSHOT", "")
                repoUrl = pom.getDistributionManagement().getRepository().getUrl()
            }

        def fly_vars = "--var group_id=${groupId} --var artifact_id=${artifactId} --var artifact_version=${artifactVersion} --var artifact_packaging=${artifactPackaging} --var repo_url=${repoUrl}"
        scriptOj.sh "fly --target ${target} set-pipeline --non-interactive --pipeline ${pipelineName} --config ${pipelineConfiguration} --load-vars-from ${pipelineVariables} --load-vars-from ${pipelineCredentials} ${fly_vars}"
    }

    public static void deployPipelineByTag(
        Script scriptObj,
        String target,
        String pipelineName,
        String pipelineConfiguration,
        String pipelineCredentials,
        String pipelineVariables,
        String gitBranchTag) {
            def fly_vars = "--var git-branch-tag=${gitBranchTag}"
            scriptObj.sh "fly --target ${target} set-pipeline --non-interactive --pipeline ${pipelineName} --config ${pipelineConfiguration} --load-vars-from ${pipelineVariables} --load-vars-from ${pipelineCredentials} ${fly_vars}"
    }
    public static void deployPipelineByBranchOrTag(
        Script scriptObj,
        String target,
        String pipelineName,
        String pipelineConfiguration,
        String pipelineCredentials,
        String pipelineVariables,
        String fly_vars) {
            scriptObj.sh "fly --target ${target} set-pipeline --non-interactive --pipeline ${pipelineName} --config ${pipelineConfiguration} --load-vars-from ${pipelineVariables} --load-vars-from ${pipelineCredentials} ${fly_vars}"
    }

    public static void unpausePipeline(
        Script scriptObj,
        String target,
        String pipelineName,
        String jobName) {
            scriptObj.sh "fly --target ${target} unpause-pipeline --pipeline ${pipelineName}"
        }
    public static void runPipeline(
        Script scriptObj,
        String target,
        String pipelineName,
        String jobName
    ) {
        scriptObj.sh "fly --target ${target} trigger-job --job ${pipelineName}/${jobName} --watch"
    }
    public static void destroyPipeline(
        Script scriptObj,
        String target,
        String pipelineName
    ) {
        scriptObj.sh "fly --target ${target} dp -p ${pipelineName} -f"
    }
    public static void abortBuild(
        Script scriptObj,
        String target,
        String pipelineName,
        String jobName 
    ) {
        def buildNumber = scriptObj.sh(
            script: "fly --target ${target} builds --job ${pipelinaName}/${jobName} | head -1 | awk -F' ' '{print \$3}'",
            returnStdout: true
        )
        scriptObj.sh "fly --target ${target} ab --job ${pipelineName}/${jobName} -b ${buildNumber}"
    }
    public static void pollJobStatus(
        Script scriptObj,
        String target,
        String pipelineName,
        String jobName
    ) {
        /*
         * All cases other than 'succeeded' are considered fail.
         */
            scriptObj.sh """#!/bin/bash
            #watch status of job
            fly --target ${target} watch --job ${pipelineName}/${jobName}
            result=`fly --target ${target} builds --job ${pipelineName}/${jobName} | head -1`
            build_status=`echo \"\${result}\" | awk -F' ' '{print \$4}'`
            while [ \"\$build_status\" == 'pending' -o \"\$build_status\" == 'started' ]; do
                sleep 30
                result=`fly --target ${target} builds --job ${pipelineName}/${jobName} | head -1`
                build_status=`echo \"\${result}\" | awk -F' ' '{print \$4}'`
                echo \"build_status: \${build_status}\"
            done
            [ \"\${build_status}\" == 'succeeded' ] && exit 0
            exit 1
            """.stripMargin()
        
    }
    public static void deployWebStaticPipeline(
        Script scriptObj,
        String target,
        String pipelineName,
        String pipelineConfiguration,
        String pipelineCredentials,
        String pipelineVariables,
        String groupId,
        String artifactId,
        String artifactVersion,
        String artifactPackage,
        String repoUrl
    ) {
        def fly_vars = "--var group_id=${groupId} --var artifact_id=${artifactId} --var artifact_version=${artifactVersion} --var artifact_package=${artifactPackage} --var repo_url=${repoUrl}"
        scriptObj.sh "fly --target ${target} set-pipeline --non-interactive --pipeline ${pipelineName} --config ${pipelineConfiguration} --load-vars-from ${pipelineVariables} --load-vars-from ${pipelineCredentials} ${fly_vars}"
    }


}