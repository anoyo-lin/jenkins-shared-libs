package com.gene.workflow.custom.devops.java

import com.gene.parameters.ParametersReader
import com.gene.workflow.interfaces.PipelineParamsReadInterface
import com.gene.fortify.FortifyResult
import com.gene.sonarqube.SonarQubeResult
import com.gene.snyk.SnykResult

class PipelineParamsReader extends ParametersReader implements PipelineParamsReadInterface {
    protected String currentEnv
    protected String pipelineName = ''
    PipelineParamsReader(Script scriptObj) {
        super(scriptObj)
        this.pipelineName = scriptObj.pipelineName
        scriptObj.stageStats = new HashMap<>()
    }
    public String getTargetEnv() {
        def targetEnv = scriptObj.JOB_URL.tokenize('/')[6]
        if (targetEnv.contains("dr")) {
            targetEnv = "${scriptObj.JOB_URL.tokenize('/')[8]}-dr"
        }
        scriptObj.env.targetEnvironment = targetEnv
        this.currentEnv = targetEnv
        return targetEnv
    }
    /**
    * user input parameter
    */
    public void waitUserInput() {
        def targetEnv = getTargetEnv()
        def inParamsters = []

        scriptObj.echo "======= begin waiting user input request ============="
        def validateParameters = []

        // -- append branch name or tag version choice
        boolean chooseBranchTag = "deployment,operations".contains(this.pipelineName)
        def oneKey = ''
        if(chooseBranchTag) {
            scriptObj.sh "git tag --list v* --sort=-version:refname > git-branch-tags.txt"
            scriptObj.sh "git branch -r --list origin/release* --sort=-version:refname | \
            sed \"s#  origin\\/##g\" >> git-branch-tags.txt"
            scriptObj.sh "git branch -r --list origin/master* --sort=-version:refname | \
            sed \"s#  origin\\/##g\" >> git-branch-tags.txt"
            def git_branch_tags = scriptObj.readFile "git-branch-tags.txt"
            def vp = [
                key: "branchName",
                value: "BRANCH_NAME"
            ]
            validateParameters.add(vp)
            inParamsters.add(scriptObj.choice(
                choices: git_branch_tags,
                description: "Select the git tag or branch for deployment.",
                name: "${vp.key}"
            ))
            oneKey = "${vp.value}"
        }

        boolean appendTicketNumberFlag = "${targetEnv}".contains("prod")

        // --proxyUpsert
        def BAU_TASKS = scriptObj.env.PROVISIONING_BAU_CONTROL
        if (BAU_TASKS == "proxyUpsert") {
            def promoteTargetEnv = "test"
            if ("${currentEnv}".contains('uat')) {
                promoteTargetEnv = "preprod-int,preprod-ext"
            } else if ("${currentEnv}".contains("prod")) {
                promoteTargetEnv = "prod-int,prod-int"
            }
            scriptObj.env.UPSERT_PROXY_TARGET_ENV = promoteTargetEnv
            if ( promoteTargetEnv && "${promoteTargetEnv}".contains("prod")) {
                appendTicketNumberFlag = true
                def vp = [
                    key: "ApigeeProxyEnvironment",
                    value: "UPSERT_PROXY_TARGET_ENV"
                ]
                validateParameters.add(vp)

                def target_env_list = []
                for (string e in "${promoteTargetEnv}".split(",")) {
                    target_env_list.add(e)

                }
                inParamsters.add(scriptObj.choice(
                    choices: target_env_list,
                    name: "${vp.key}",
                    description: "select the apigee proxy environment for deployment."
                ))
                oneKey = "${vp.value}"
            }
        } else if ( "${BAU_TASKS}".contains("App") && appendTicketNumberFlag ) {
            appendTicketNumberFlag = false
            def vp = [
                key: "IncidentNumber",
                value: "INCIDENT_TICKET_NO"
            ]
            validateParameters.add(vp)
            inParamsters.add(scriptObj.string(
                description: """
                | please enter the incident ticket number (only valid for app start/stop/restart).
                """.stripMargin(),
                name: "${vp.key}"
            ))
            oneKey = "${vp.value}"
        }
        // -- apend ticket number
        if (appendTicketNumberFlag) {
            def vp = [
                key: "changeTicketNumber",
                value: "CHANGE_TICKET_NO"
            ]
            validateParameters.add(vp)
            inParamsters.add(scriptObj.string(
                description: """
                | please enter the SNOW change ticket number (required for PROD deployments).
                """.stripMargin(),
                name: "${vp.key}"
            ))
            oneKey = "${vp.value}"
        }
        if (inParamsters && inParamsters.size() > 0) {
            def chosenRef
            scriptObj.timeout(time: 5, unit: 'MINUTES') {
                chosenRef = scriptObj.input(
                    message: "Enter Content",
                    ok: "continue",
                    parameters: inParamsters
                )
                scriptObj.echo "================ chosenRef: ${chosenRef}"
            }
            if (!chosenRef) {
                throw new Exception("inValidate input")
            }
            if (inParamsters.size() == 1) {
                scriptObj.env."${oneKey}" = "${chosenRef}"
            } else {
                for (def validateParameter: validateParameters) {
                    def validateParameterValue = chosenRef["${validateParameter.key}"]
                    if (!validateParameterValue || validateParameterValue == "") {
                        throw new Exception("inValidate ${validateParameter.key}")
                    }
                    scriptObj.env."${validateParameter.value}" = validateParameterValue
                }
            }
            if (chooseBranchTag) {
                def branchTagName = scriptObj.env.BRANCH_NAME
                scriptObj.sh """#!/bin/bash -e
                | set -x
                | git branch
                | git checkout master
                | git pull origin ${branchTagName}
                | git checkout ${branchTagName}
                | git branch
                | git status
                """.stripMargin()
            }
        }
        scriptObj.echo "===== end waiting user input request ===="
    }
    @Override
    public void pipelineparamsReadPreOperations() {
        scriptObj.stageStats["${scriptObj.env.STAGE_NAME}"] = 'failed'
        scriptObj.fortifyScanResult = "Unknown"
        // toggle for testing the code snippet
        // configuration.skipUnitTest = true
        // configuration.skipCodeQualityScan = true
        // configuration.skipFortifyScan = true
        // configuration.skipSnykScan = true
        // configuration.skipUploadArtifactory = true
        // configuration.skipUpdateBranches = false
        // configuration.skipDeploy = true
        // configuration.isLibrary = false
        // configuration.useProvisioningCli = false
        // binaries checking
        scriptObj.echo "Running on Pipeline : ${scriptObj.class.name}"
        scriptObj.echo "===========Checking Tools============="
        scriptObj.sh "env"
        scriptObj.sh "id"
        scriptObj.sh "date"
        scriptObj.sh "git --version"
        scriptObj.sh "java --version"
        scriptObj.sh "mvn -v"
        scriptObj.sh "sonar-scanner -v"
        scriptObj.sh "sourceanalyzer -v"
        scriptObj.sh "node --version"
        scriptObj.sh "npm --version"
        scriptObj.sh "fly --version"
        scriptObj.sh "python -v"
        scriptObj.sh "gcc --version"
        scriptObj.sh "kubectl config view"
        scriptObj.sh "docker -v"
        scriptObj.sh "docker run --rm artifactory.gene.com/docker/hello-world"

        this.currentEnv = this.getTargetEnv()
        scriptObj.echo "current env : ${this.currentEnv}"
        scriptObj.echo "current pipeline name : ${this.pipelineName}"
        // -- cr
        // this.waitUserInput()
    }
    @Override
    public void pipelineParamsReadPostOperations() {
        scriptObj.sh "rm -fr target || true"
        // copy the settings.xml to WORKSPACE
        if(super.readPipelineParams('settingsFilePath')) {
            def checkSettingsExist = scriptObj.fileExists super.readPipelineParams('settingsFilePath')
            if (checkSettingsExist) {
                scriptObj.sh "cp -f ${super.readPipelineParams('settingsFilePath')} ."
            }
        }
        // startup the jacoco server
        logger.info("wiping out the java compiling path ./target")
        scriptObj.sh "rm -rf ./target"

        def sonarQubeResult = new SonarQubeResult()
        def fortifyResult = new FortifyResult()
        def snykResult = new SnykResult()
        sonarQubeResult.message = "Project status UNKNOWN. SonarQube wasn't called."
        if (super.readPipelineParams('skipCodeQualityScan')) {
            sonarQubeResult.skipCodeQualityGatePassed = true
            sonarQubeResult.message = "Project SKIPPED Code Quality Gate!"
        }
        fortifyResult.message = "Project status UNKNOWN. Fortify Scan wasn't called."
        if (super.readPipelineParams('skipFortifyScan')) {
            fortifyResult.codeSecurityGatePassed = true
            fortifyResult.message = "Project SKIPPED Security Quality Gate!"
        }
        snykResult.message = "Project status UNKNOWN. Snyk Scan wan't called"
        if (super.readPipelineParams("skipSnykScan")) {
            snykResult.governanceGatePassed = true
            snykResult.message = "Project SKIPPED Snyk Governance Gate"
        }
        scriptObj.fortifyResult = fortifyResult
        scriptObj.sonarQubeResult = sonarQubeResult
        scriptObj.snykResult = snykResult
        scriptObj.stageStats["${scriptObj.env.STAGE_NAME}"] = 'successful'
    }
    @Override
    public void pipelineParamsReadMainOperations() {
        scriptObj.pipelineParams = super.assembleParams()
    }
}