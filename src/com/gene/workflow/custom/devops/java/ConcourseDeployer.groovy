package com.gene.workflow.custom.devops.java

import com.gene.cloudfoundry.ConcourseUtil
import com.gene.workflow.interfaces.ConcourseDeploymentInterface
import com.gene.logger.*

class ConcourseDeployer implements ConcourseDeploymentInterface {
    protected Script scriptObj
    protected Logger logger
    protected String branchTagName

    protected Properties pipelineParams
    protected String deployStatus = ''
    protected String targetEnv

    protected String concourseTarget
    protected String concoursePipelineName

    ConcourseDeployer(Script scriptObj) {
        this.scriptObj = scriptObj
        this.logger = new Logger(scriptObj, Level.INFO)
        this.pipelineParams = scirptObj.pipelineParams
        this.targetEnv = scriptObj.env.targetEnvironment

        this.concourseTarget = "${pipelineParams.concourseTarget}"
        this.concoursePipelineName = "${pipelineParams.concoursePipelineName}"
    }
    @Override
    void concourseDeploymentPreOperations(){
        scriptObj.stageStats["${scriptObj.env.STAGE_NAME}"] = 'failed'
        logger.info("============== Begin Release With Concourse Pipeline ================")

        logger.info("============== Read Deployment Parameters================")
        scriptObj.sh "git status"
        this.branchTagName = scriptObj.env.BRANCH_NAME
        scriptObj.sh "git pull origin ${this.branchTagName} --allow-unrelated-histories && git fetch --all --tags --prune"
        this.authConcourse()
        this.deployConcoursePipeline()
        this.unPausePipeline()
        logger.info("=============== End Release with concourse Pipeline ===================")

    }
    @Override
    void concourseDeploymentMainOperations() {
        logger.info("======================== Beging Run Concourse Pipeline =================")
        def pipelineName = scriptObj.pipelineName
        if (pipelineName = "operations") {
            String bau_task_name = "${scriptObj.env.PROVISIONING_BAU_CONTROL}"
            logger.info("========= Begin Run Bau Task ===> ${bau_task_name}")
            this.runBAUTask(bau_task_name)
            logger.info("========= End Run Bau Task ===> ${bau_task_name}")
        } else {
            def createCNRSService = pipelineParams.get("SkipCNRSService")
            if (createCNRSService && createCNRSService == "false") {
                this.setUCNRSService()
            }
            def skipDeploy = pipelineParams.get("skipDeploy")
            if (skipDeploy && skipDeploy == "false") {
                this.deployApp()
            }
            def skipAutoScale = pipelineParams.get("skipAutoScale")
            if (skipAutoScale && skipAutoScale == "false") {
                this.enableAutoScale()
            }
        }
        logger.info("==================== End Run Concourse Pipeline ================")

    }

    void runBAUTask(String bau_task_name) {
        if (bau_task_name) {
            if ("${bau_task_name}" == 'restartApp') {
                this.runPipeline("restart-${targetEnv}")
            } else if ("${bau_task_name}" == 'rollbackApp') {
                logger.info("not yet support")
            } else if ("${bau_task_name}" == 'proxyUpsert') {
                this.runPipeline("upsert-proxy")
            } else if ("${bau_task_name}" == 'enableAutoScale') {
                this.runPipeline("autoscale-${targetEnv}")
            } else if ("${bau_task_name}" == 'createService') {
                this.runPipeline("setup-${targetEnv}")
            } else if ("${bau_task_name}" == 'startApp') {
                this.runPipeline("start-${targetEnv}")
            } else if ("${bau_task_name}" == 'stopApp') {
                this.runPipeline("stop-${targetEnv}")
            } else if ("${bau_task_name}" == 'deleteApp') {
                this.runPipeline("delete-${targetEnv}")
            } else if ("${bau_task_name}" == 'others') {
                this.runPipeline("manage-${targetEnv}")
            } else if ("${bau_task_name}" == 'flyway') {
                this.runPipeline("flyway-${scriptObj.env.FLYWAY_TASK}-${targetEnv}")
            }
        } else {
            throw new Exception("invalid parameters for provisioning Bau tasks")
        }
    }

    void authConcourse() {
        scriptObj.echo "============== Authenticate Concourse Credential With UAA ============"
        deployStatus="LoginConcourse"
        scriptObj.withCredentials([
            [
                $class: "UsernamePassowordMultiBinding",
                credetnialsId: pipelineParams.pcfCredential,
                usernameVariable: "PCF_CREDENTIAL_USR",
                passwordVariable: "PCF_CREDENTIAL_PSW"
            ]
        ]) {
            ConcourseUtil.authenticateWithUAA(
                scriptObj,
                pipelineParams.concourseUrl,
                pipelineParams.concourseTeamUrl,
                concourseTarget,
                pipelineParams.pcfUrl,
                scriptObj.PCF_CREDENTIAL_USR,
                scriptObj.PCF_CREDENTIAL_PSW,
                pipelineParams.pcfSpace

            )
        }
    }

    void deployConcoursePipeline() {
        scriptObj.echo "========= Deploy Concourse Pipeline ============"
        deployStatus = "DeployPipeline"
        def fly_vars = ""
        if (scriptObj.env.PROVISIONING_BAU_CONTROL == 'proxyUpsert') {
            def configYmlPath = pipelineParams['proxyUpsertYaml']
            def proxy_org = pipelineParams.get('deployTargetOrg')
            def porxy_foundation = pipelineParams.get('deployTargetFoundation')
            fly_vars = "--load-vars-from ${configYmlPath} --var proxy_org=${proxy_org} --var proxy_division=${proxy_foundation}"
        } else {
            def groupId = scriptObj.env.GROUPID 
            def ARTIFACTID = "${scriptObj.evn.ARTIFACTID}".replace("-parent", "")
            def artifactVersion = scriptObj.env.artifactVersion
            def appName = pipelineParams["cf-app-name-${targetEnv}"]
            def repoUrl = pipelineParams["artifactory_repo_url"]
            def artifactPackaging = scriptObj.env.artifactPackaging

            fly_vars = "--var group_id=${groupId} --var artifact_id=${ARTIFACTID} --var artifact_version=${artifactVersion} --var artifact_packaging=${artifactPackaging} --var repo_url=${repoUrl} --var cf-app-name-${targetEnv}=${appName}"
        }
        ConcourseUtil.deployPipelineByBranchOrTag(
            scriptObj,
            concourseTarget,
            concoursePipelineName,
            pipelineParams.concoursePipelineConfiguration,
            pipelineParams.concoursePipelineCredentials,
            pipelineParams.concoursePipelineVariables,
            fly_vars)
    }
    void unPausePipeline() {
        logger.info("============== UnPause Concourse Pipeline ===========")
        deployStatus="UnpausePipeline"
        ConcourseUtil.unPausePipeline(scriptObj, concourseTarget, concoursePipelineName, "")
    }
    void runPipeline(String ops) {
        logger.info("============== Start to run ${ops} pipeline ===========")
        String concourseJobName = "${ops}"
        this.pipelineParams.put("current_concourse_job_name", concourseJobName)
        ConcourseUtil.runPipeline(scriptObj, concourseTarget, concoursePipelineName, concourseJboName)
        this.pollConcourseJobStatus(concourseJboName)
        logger.info("============== End to create PCF service list ===========")
    }
    void pollConcourseJobStatus(
        String concourseJobName
    ) {
        deployStatus = 'Failed'
        logger.info("============== Get Concourse Job: ${concourseJobName} Status ===========")
        ConcourseUtil.pollJobStatus(scriptObj, concoursetarget, concoursePipelineName, concourseJobName)
        deployStatus = 'Success'
        
    }
    public void concourseDeploymentPostOperations() {
        if(pipelineParams.destroyPipeline && Boolean.valueOf("${pipelineParams.destroyPipeline}")) {
            ConcourseUtil.destroyPipeline(scriptObj, concourseTarget, concoursePipelineName)
            
        }
        if (pipelineParams.triggerNextPipeline && Boolean.valueOf("${pipelineParms.triggerNextPipeline}")) {
            def downStreamBranch = pipelineParams.downStreamBranch ? pipelineParams.downStreamBranch:scriptObj.env.BRANCH_NAME
            def urlEncodedBranch

            if (downStreamBranch.contains("/")) {
                urlEncodedBranch = downStreamBranch.replace("/", "%2F")
            } else {
                urlEncodedBranch = downStreamBranch
            }

            if (scriptObj.JOB_URL.contains("Microservice")) {
                try {
                    scriptObj.build job: "../../../${pipelineParams.downStreamEnv}/Mircoservice/${pipelineParams.downStreamJob}/${urlEncodedBranch}", wait: false, parameters: [scriptObj.string(name: 'GIT_BRANCH_TAG', value: "${downStreamBranch}")]
                } catch (Exception e) {
                    
                    scriptObj.build job: "../../../${pipelineParams.downStreamEnv}/${pipelineParams.downStreamJob}/${urlEncodedBranch}", wait: false, parameters: [scriptObj.string(name: 'GIT_BRANCH_TAG', value: "${downStreamBranch}")]
                }
            } else {
                scriptObj.build job: "../../${pipelineParams.downStreamEnv}/${pipelineParams.downStreamJob}/${urlEncodedBranch}", wait: false, parameters: [scriptObj.string(name: "GIT_BRANCH_TAG", value: "${downStreamBranch}")]
            }
        }
        scriptObj.stageStats["${scriptObj.env.STAGE_NAME}"] = 'successful'
    }
}
