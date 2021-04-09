package com.gene.workflow.custom.devops.java

import com.gene.logger.*
import com.gene.parameters.ParametersReader
import com.gene.workflow.interfaces.GitFlowUpdateInterface
import com.gene.gitflow.GitFlowUtil
import com.gene.gitflow.GitUtil

class GitFlowUpdater implements GitFlowUpdateInterface {
    protected Script scriptObj
    protected Logger logger
    protected ParametersReader paramsReader
    protected String gitFlowParameters
    GitFlowUpdater(Script scriptObj){
        this.scriptObj = scriptObj
        this.logger = new Logger(scriptObj, Level.INFO)
        this.paramsReader = new ParametersReader(scriptObj)
        this.gitFlowParameters = ''
    }
    public void gitFlowUpdatePreOperations() {
        scriptObj.stageStats["${scriptObj.env.STAGE_NAME}"] = 'failed'
        // read the version from pom.xml
        def pom = scriptObj.readMavenPom file: "pom.xml"
        def artifactReleaseVersion = pom.getVersion().replace("-SNAPSHOT", "")
        scriptObj.sh "mvn --settings settings.xml version:set -DnewVersion=${artifactReleaseVersion}"
        // upload the artifact w/o -SNAPSHOT suffix in name to artifactory
        scriptObj.sh "mvn --settings settings.xml -U -DskipTests package deploy"
        if (scriptObj.env.DOCKER_CONTENT_TRUST) {
            def helmTemplateRepo, helmTemplateBranches
            def pipelinePropertiesFolder, chartVersion, k8sPath, helmChartPath
            // init the variables
            !paramsReader.readPipelineParams('HELM_TEMPLATE_REPO') ?: (this.helmTemplateRepo = paramsReader.readPipelineParams('HELM_TEMPLATE_REPO'))
            !paramsReader.readPipelineParams('HELM_TEMPLATE_BRANCH') ?: (this.helmTemplateBranches = paramsReader.readPipelineParams('HELM_TEPLATE_BRANCH'))

            !paramsReader.readPipelineParams('pipelinePropertiesDir') ?: (this.pipelinePropertiesFolder = paramsReader.readPipelineParams('pipelinePropertiesFolder'))
            !paramsReader.readPipelineParams('pipelinePropertiesFolder') ?: (this.pipelinePropertiesFolder = paramsReader.readPipelineParams('pipelinePropertiesFolder'))
            !paramsReader.readPipelineParams('HELM_CHART_VERSION') ?: (this.pipelinePropertiesFolder = paramsReader.readPipelineParams('HELM_CHART_VERSION'))
            !paramsReader.readPipelineParams('HELM_CHART_PATH') ?: (this.pipelinePropertiesFolder = paramsReader.readPipelineParams('HELM_CHART_PATH'))
            !paramsReader.readPipelineParams('K8S_PATH') ?: (this.pipelinePropertiesFolder = paramsReader.readPipelineParams('K8S_PATH'))

            this.helmChartRepoPath = "${pipelinePropertiesFolder}/${chartVersion}/${k8sPath}/${helmChartPath}"
            this.helmNetworkChartRepoPath = "${pipelinePropertiesFolder}/${chartVersion}/${k8sPath}/${helmChartPath}/network"
            this.helmChartTemplatePath = "${pipelinePropertiesFolder}/${chartVersion}/${k8sPath}/${helmChartPath}/release"
            /* CLone helm template from GIT */
            GitUtil.cloneRepository(
                scriptObj,
                helmTemplateRepo,
                scriptObj.scm.getUserRemoteConfigs()[0].getCredentialsId(),
                helmTemplateBranches,
                helmChartRepoPath
            )
            // if run ci, it will do the preOperations. otherwise skip it
            // notary's .docker
            GitUtil.cloneRepository(
                scriptObj,
                "ssh://git.gene.com/devopsutils/notary-trust-store.git",
                scriptObj.scm.getUserRemoteConfigs()[0].getCredentialsId(),
                [[name:"master"]],
                ".docker"
            )
            // docker build
            pom = scriptObj.readMavenPom file: "pom.xml"
            def ARTIFACTID = pom.getArtifactId()
            def VERSION =pom.getVersion()
            def lowerArtifactId = ARTIFACTID.toLowerCase()
            def lowerVersion = VERSION.toLowerCase()

            def DOCKER_REGISTRY_URL = "artifactory.gene.com"
            def DOCKER_REGISTRY_CONTEXT = "/docker"
            def ACR_LOGIN_URL = DOCKER_REGISTRY_URL + DOCKER_REGISTRY_CONTEXT

            String JAR_FILE = "${ARTIFACTID}-${VERSION}.jar"
            scriptObj.sh """
            mkdir -p ${pipelinePropertiesFolder}/docker-build/target && \
            cp target/${JAR_FILE} ${pipelinePropertiesFolder}/docker-build/target/ && \
            cp ${helmChartRepoPath}/docker/Dockerfile ${pipelinePropertiesFolder}/docker-build/ && \
            cd ${pipelinePropertiesFolder}/docker-build && \
            docker build --build-arg JAR_FILE=${JAR_FILE} . -t ${ACR_LOGIN_URL}"/"${lowerArtifactId}:${lowerVersion}
            """

            logger.info("==================Push the build image to artifactory================")
            // docker push to ACR
            // docker_secret is the artifactory credential
            scriptObj.withCredentials([
                scriptObj.string(credentialsId: 'DCT_ROOT_SECRET', variable: 'DOCKER_CONTENT_TRUST_ROOT_PASSPHRASE'),
                scriptObj.string(credentialsId: 'DCT_REPO_SECRET', variable: 'DOCKER_CONTENT_TRUST_REPOSITORY_PASSPHRASE'),
                scriptObj.usernamePassword(credentialsId: 'DOCKER_SECRET', usernameVariable: 'DOCKER_NAME', passwordVariable: 'DOCKER_PASSWORD')
            ]) {
                scriptObj.sh "docker login ${DOCKER_REGISTRY_URL} -u ${scriptObj.env.DOCKER_NAME} -p ${scriptObj.env.DOCKER_PASSWORD}"
                scriptObj.sh "docker push ${ACR_LOGIN_URL}/${lowerArtifactId}:${lowerVersion}"
                // bump up the versionNumber and upload helm package to artifactory
                logger.info("===================Helm Packaging and Upload to artifactory=================")
                scriptObj.sh """
                helm repo add helm https://artifactory.gene.com/artifactory/helm --username ${scriptObj.env.DOCKER_NAME} --password ${scriptObj.env.DOCKER_PASSWORD}
                helm repo update
                """
                scriptObj.sh """
                sed -i 's/appVersion: .*/appVersion: network/g' ${helmNetworkChartPath}/Chart.yaml && \
                sed -i 's/name: .*/name: ${lowerArtifactId}-network/g' ${helmNetworkChartPath}/Chart.yaml && \
                helm package ${helmNetworkChartPath}
                """
                scriptObj.sh "curl -u ${scriptObj.env.DOCKER_NAME}:${scriptObj.env.DOCKER_PASSWORD} -T ${lowerArtifactId}-networ-${chartVersion}.tgz 'https://artifactory.gene.com/artifactory/helm'"
                scriptObj.sh """
                sed -i 's/appVersion: .*/appVersion: ${lowerVersion}/g' ${helmChartTemplatePath}/Chart.yaml && \
                sed -i 's/name: .*/name: ${lowerArtifactId}-${lowerVersion}/g' ${helmChartTemplatePath}/Chart.yaml && \
                helm package ${helmChartTemplatePath}
                """
                scriptObj.sh "curl -u ${scriptObj.env.DOCKER_NAME}:${scriptObj.evn.DOCKER_PASSWORD} -T ${lowerArtifactId}-${lowerVersion}-${chartVersion}.tgz 'https://artifactory.gene.com.artifactory.helm'"
                scriptObj.sh "helm repo update"
            }
        }

        scriptObj.sh "mvn --settings settings.xml versions:revert"

        logger.info("============= Update and Release Branch By Gitflow =============")
        if ( !paramsReader.readPipelineParams('skipInputSCMCommentPrefix') &&
        paramsReader.readPipelineParams('scmCommentPrefixParams')) {
            def scmCommentPrefix = ''
            scriptObj.timeout(time: paramsReader.readPipelineParams('inputSCMCommentPrefixTimeoutTime'), unit: paramsReader.readPipelineParams('inputSCMCommentPrefixTimeoutUnit')) {
                scmCommentPrefix = scriptObj.input message: "Input Release Comment Prefix", ok: "Release", parameters: [
                    scriptObj.string(
                        description: "Input Release Comment Prefix",
                        name: "Comment Prefix"
                    )
                ]
            }
            if (scmCommentPrefix) {
                this.gitFlowParameters = paramsReader.readPipelineParams('scmCommentPrefixParams') + scmCommentPrefix.repalceAll(" ", "") + " "
            }
            // gitFlowParameters += '-DallowSnapshots=true '
        }
    }
    public void gitFlowUpdateMainOperations() {
        String masterBranchName = "master"
        if (paramsReader.readPipelineParams('masterBranchName')) {
            masterBranchName = paramsReader.readPipelineParams('masterBranchName')

        }
        String gitFlowSettingsFile = paramsReader.readPipelineParams('gitFlowSettingsFile')
        if (scriptObj.env.BRANCH_NAME.startWith("hotfix")) {
            GitFlowUtil.finishHotfix(scriptObj, masterBranchName, true, 1000, gitFlowSettingsFile, this.gitFlowParameters)
        } else {
            GitFlowUtil.startAndFinishRelease(scriptObj, masterBranchName, true, 100, gitFlowSettingsFile, this.gitFlowParameters)
        }
    }
    public void gitFlowUpdatePostOperations() {
        logger.info("empty gitFlowUpdatePostOperations body")
        scriptObj.stageStats["${scriptObj.env.STAGE_NAME}"] = 'successful'
    }
}