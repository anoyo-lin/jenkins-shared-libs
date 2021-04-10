package com.gene.workflow.custom.devops.java
import com.gene.workflow.interfaces.AksPushInterface
import com.gene.logger.*
import com.gene.parameters.*
import com.gene.git.GitUtil

class AksPusher implements AksPushInterface {
    protected Script scriptObj
    protected ParametersReader paramsReader
    protected Logger logger
    // artifact name & version
    private String lowerArtifactId
    private String lowerVersion
    // docker registry path
    private String DOCKER_REGISTRY_URL = 'artifactory.gene.com'
    private String DOCKER_REGISTRY_CONTEXT = "/docker"
    private String ACR_LOGIN_URL = DOCKER_REGISTRY_URL + DOCKER_REGISTRY_CONTEXT
    // remote repo path
    private String helmTemplateRepo = "ssh://git@git.gene.com:8080/helm/tempate.git"
    private List helmTemplateBranches = [[name: 'master']]
    // local helm path at workspace
    private String pipelinePropertiesFolder
    private String chartVersion = '1.0.0'
    private String k8sPath
    private String helmChartPath

    private String helmChartRepoPath
    private String helmNetworkChartPath
    private String helmChartTemplatePath
    private Boolean helmRollBack

    AksPusher(Script scriptObj) {
        this.scriptObj = scriptObj 
        this.logger = new Logger(scriptObj, Level.INFO)
        this.paramsReader = new ParametersReader(scriptObj)
        this.lowerArtifactId = scriptObj.env.ARTIFACTID.toLowerCase()
        this.lowerVersion = scriptObj.env.VERSION.toLowerCase()
    }
    // docker build push to ACR => snyk scan 
    public void aksPushPreOperations() {
        // az push
        logger.info("==================Auth with Azure================")
        def credentialsId = paramsReader.readPipelineParams('AZURE_SERVICE_PRINCIPAL_SECRET_ID') ? paramsReader.readPipelineParams('AZURE_SERVICE_PRINCIPAL_SECRET_ID') : 'AZURE_SERVICE_PRINCIPAL_SECRET'

        scriptObj.withCredentials([
            scriptObj.string(credentialsId: "${credentialsId}", variable: 'AZURE_SERVICE_PRINCIPAL_SECRET')
        ]) {
            scriptObj.sh "az login --service-principal -u ${paramsReader.readPipelineParams('AZURE_SERVICE_PRINCIPAL_ID')} -p ${scriptObj.env.AZURE_SERVICE_PRINCIPAL_SECRET} -t ${paramsReader.readPipelineParams('AZURE_TENANT_ID')}"
            // scriptObj.sh "az resource list --resource-group ${paramsReader.readPipelineParams('AZURE_RESOURCE_GROUP') --output table"
        }
        // init the variables
        !paramsReader.readPipelineParams('HELM_TEMPLATE_REPO') ?: (this.helmTemplateRepo = paramsReader.readPipelineParams('HELM_TEMPLATE_REPO'))
        !paramsReader.readPipelineParams('HELM_TEMPLATE_BRANCH') ?: (this.helmTemplateBranches = paramsReader.readPipelineParams('HELM_TEPLATE_BRANCH'))
        !paramsReader.readPipelineParams('pipelinePropertiesFolder') ?: (this.pipelinePropertiesFolder = paramsReader.readPipelineParams('pipelinePropertiesFolder'))
        !paramsReader.readPipelineParams('HELM_CHART_VERSION') ?: (this.pipelinePropertiesFolder = paramsReader.readPipelineParams('HELM_CHART_VERSION'))
        !paramsReader.readPipelineParams('K8S_PATH') ?: (this.pipelinePropertiesFolder = paramsReader.readPipelineParams('K8S_PATH'))
        !paramsReader.readPipelineParams('HELM_CHART_PATH') ?: (this.pipelinePropertiesFolder = paramsReader.readPipelineParams('HELM_CHART_PATH'))
        !paramsReader.readPipelineParams('HELM_ROLLBACK') ? (this.helmRollBack = true) : ( this.helmRollBack = paramsReader.readPipelineParams('HELM_ROLLBACK'))

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
        if(scriptObj.env.pipelineName == "ci") {
            // notary's .docker
            GitUtil.cloneRepository(
                scriptObj,
                "ssh://git.gene.com/devopsutils/notary-trust-store.git",
                scriptObj.scm.getUserRemoteConfigs()[0].getCredentialsId(),
                [[name:"master"]],
                ".docker"
            )
            // docker build
            String JAR_FILE = "${scriptObj.env.ARTIFACTID}-${scriptObj.env.VERSION}.jar"
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
            }

            // if notary update, commit it
            scriptObj.dir(".docker") {
                String divertCounter = scriptObj.sh(script: "git status -s | wc -l", returnStdout: true, label: "check key changes")
                def isChanged = divertCounter.toInteger()
                if ( isChanged > 0 ) {
                    logger.info("Commit DCT target key for new app ${scriptObj.env.ARTIFACTID}")
                    scriptObj.sh """
                    git config --global user.name gene && \
                    git config --global user.email gene@qq.com && \
                    git add --all && \
                    git commit --all -m "commit the new target key for App:[${scriptObj.env.ARTIFACTID}]" && \
                    git push origin  HEAD:master
                    """
                } else {
                    logger.info("no DCT key need to commit")
                }
            }
            // scriptObj.sh """
            // cd .docker
            // if [ \$(git status -s | wc -l) -gt 0 ]; then
                // echo "Commit DCT Target Key for new App ${ARTIFACTID}"
                // git config --global user.name gene
                // git config --global user.email gene@qq.com
                // git add --all
                // git commit --all -m 'commit the new target key for App" [${ARTIFACTID}]"
                // git push origin HEAD:master
            // else
                // echo "No DCT key need to commit" fi
            // """

            // snyk scan for Dockerfile
            logger.info('snyk scanning the dockerFile')
            try {
                def snykId = paramsReader.readPipelineParams('snykTokenId')?paramsReader.readPipelineParams('snykTokenId'):'SNYK_HK_ORG'
                scriptObj.withCredentials([scriptObj.string(credentialsId: snykId, variable: 'SNYK_TOKEN')]) {
                    scriptObj.sh "snyk auth -d ${scriptObj.env.SNYK_TOKEN}"
                }
                scriptObj.configuration['snykDockerUrl'] = "${ACR_LOGIN_URL}/${lowerArtifactId}:${lowerVersion}"
                scriptObj.configuration['snykFile'] = "Dockerfile"
                scriptObj.dir("${pipelinePropertiesFolder}/docker-build") {
                    def snykRunnerObj = new com.gene.snyk.SnykRunner(scriptObj)
                    snykRunnerObj.run()
                }
            } catch ( Exception e ) {
                logger.info("${e}\n===================Error in Snyk, will continue===================")
            }
            // bump up the versionNumber and upload helm package to artifactory
            logger.info("===================Helm Packaging and Upload to artifactory=================")
            scriptObj.withCredentials([scriptObj.usernamePassword(credentialsId: 'DOCKER_SECRET', usernameVariable: 'DOCKER_NAME', passwordVariable: 'DOCKER_PASSWORD')]) {
                scriptObj.sh "az aks get-credentials --resource-group ${paramsReader.readPipelineParams('AZURE_RESOURCE_GROUP')} --name ${paramsReader.readPipelineParams('AZURE_AKS_CLUSTER_NAME')} --admin --overwrite-existing"
                scriptObj.sh """
                helm repo add helm https://artifactory.gene.com/artifactory/helm --username ${scriptObj.env.DOCKER_NAME} --password ${scriptObj.env.DOCKER_PASSWORD}
                helm repo update
                """
                scriptObj.sh(script: "kubectl get svc/svc-${lowerArtifactId} -n ${paramsReader.readPipelineParams('K8S_TARGET_NAMESPACE')}", returnStdout: true)
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


    }
    // for serializable object, it couldn't use Jenkin CPS method to resume the previous state, thus we need to initial 
    // a standAlone process with @NonCPS annotation
    @NonCPS
    public String getRegionId() {
        try {
            def regionId = scriptObj.pipelineParams.AZURE_RESOURCE_GROUP.toLowerCase().trim()
            def matcher = ( regionId =~ /^([A-Za-z]+)-/ )
            return matcher[0][1].toString().trim()
        } catch (Exception err) {
            return null
        }
    }
    public void aksPushMainOperations() {
        // get values.yaml and collects git_commit. generate a list of running Instances ,do the helm upgrade
        // refresh the running Instances list, and helm upgrade affliated network components by new_list
        logger.info("==========Deploy to Aks============")
        def valuesFile, valueYamlSuffix
        if ( getRegionId() || getRegionId() != '' ) {
            valueYamlSuffix = getRegionId()
            valueFile = pipelinePropertiesFolder + "/" + k8sPath + "/values/values-${scriptObj.env.targetEnvironment}-${valueYamlSuffix}.yaml"
        } else {
            valueFile = pipelinePropertiesFolder + "/" + k8sPath + "/values/values-${scriptObj.env.targetEnvironment}.yaml"
        }
        def build_commit = ''

        if(scriptObj.env.pipelineName != 'ci') {
            scriptObj.withCredentials([scriptObj.usernamePassword(credentialsId: 'DOCKRE_SECRET', usernameVariable: 'DOCKER_NAME', passwordVariable: 'DOCKER_PASSWORD')]) {
                scriptObj.sh "az aks get-credentials --resource-group ${paramsReader.readPipelineParams('AZURE_RESOURCE_GROUP')} --name ${paramsReader.readPipelineParams('AZURE_AKS_CLUSTER_NAME')} --admin --overwrite-existing"
                scriptObj.sh """
                helm repo add helm https://artifactory.gene.com/artifactory/helm --username ${scriptObj.env.DOCKER_NAME} --password ${scriptObj.env.DOCKER_PASSWROD}
                helm repo update
                """
            }
            build_commit = scriptObj.sh(script: "unzip -p target/*.jar \$(unzip -l target/*.jar | grep git | awk '{print \$4}' | grep 'git.commit.id.abbrev' | awk -F= '{print \$2}'", returnStdout: true).trim()

        } else {
            build_commit = scriptObj.sh(script: "grep 'git.commit.id.abbrev' target/classes/git.properties | awk -F= '{print \$2}'", returnStdout: true).trim()
        }

        def dnsAppName = lowerArtifactId.replaceAll("\\.", "-") + "_" + lowerVersion.replaceAll('\\.', '-')
        scriptObj.sh """
        echo "runningInstances: " > runningInstances.yaml && \
        kubectl get pod -n ${paramsReader.readPipelineParams('K8S_TARGET_NAMESPACE')} --field-selector=status.phase=Running -l app.kubernetes.io/appName=${lowerArtifactId} -o json | jq '.items[].metadata.labels | ."app.kubernetes.io/instance"' -r \
        |while IFS= read -r line; do printf "  - \$line\\n"; done >> runningInstances.yaml
        """
        def paramsStr = "--atomic"
        if ( !this.helmRollBack) {
            paramsStr = "--wait --timeout 5m0s"
        }
        scriptObj.sh """helm upgrade -i ${paramsStr} --cleanup-on-fail ${lowerArtifactId}-${lowerVersion}-${chartVersion} helm/${lowerArtifactId}-${lowerVersion} \
        --version ${chartVersion} \
        --set image.repository=${ACR_LOGIN_URL} \
        --set appName=${lowerArtifactId} \
        --set appVersion=${lowerVersion} \
        --set gitCommit=${build_commit}
        --namespace ${paramsReader.readPipelineParams('K8S_TARGET_NAMESPACE')} \
        --values ${valuesFile}
        """
        scriptObj.sh "kubectl wait --for=condition=available --timeout=60s deployment -l app.kubernetes.io/appName=${lowerArtifactId},app.kubernetes.io/appVersion=${lowerVersion} -n ${paramsReader.readPipelineParams('K8S_TARGET_NAMESPACE')}"
        scriptObj.sh "kubectl get pods -n ${paramsReader.readPipelineParams('K8S_TARGET_NAMESPACE')}"
        def isExist = scriptObj.sh (script: "grep ${lowerArtifactId}-${lowerVersion}-${chartVersion} runningInstances.yaml", returnStdout: true)
        if ( isExist != 0 ) {
            scriptObj.sh """printf "  - ${lowerArtifactId}-${lowerVersion}-${chartVersion}\\n" >> runningInstances.yaml
            """
        }

        scriptObj.sh """helm upgrade -i ${paramsStr} --cleanup-on-fail ${lowerArtifactId}-network-${chartVersion} helm/${lowerArtifactId}-network \
        --version ${chartVersion}
        --set image.repository=${ACR_LOGIN_URL} \
        --set appName=${lowerArtifactId} \
        --set appVersion=${lowerVersion} \
        --set gitCommit=${build_commit} \
        --namespace ${paramsReader.readPipelineParams('K8S_TARGET_NAMESPACE')} \
        --values ${valueFile} \
        --values runningInstances.yaml
        """
        
    }
    public void aksPushPostOperations() {
        logger.info("empty aksPushPostOperations body, please inject your customs code here")
    }



}