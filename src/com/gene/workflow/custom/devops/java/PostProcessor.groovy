package com.gene.workflow.custom.devops.java

import com.gene.concourse.ConcourseUtil
import com.gene.logger.*
import com.gene.parameters.ParametersReader
import com.gene.workflow.interfaces.PostProcessInterface

import com.gene.util.QualityGateCheck
import com.gene.dashboard.DashboardUtil
import com.gene.jenkins.JenkinsUtil

public class PostProcessor implements PostProcessInterface {
    protected Script scriptObj
    protected Logger logger
    protected ParametersReader ParametersReader
    PostProcessor(Script scriptObj) {
        this.scriptObj = scriptObj
        this.logger = new Logger(scriptObj, Level.INFO)
        this.paramsReader = new ParametersReader(scriptObj)
    }
    public void postProcessPreOperations() {
        if ( scriptObj.pipelineName = "ci" ) {
            logger.info("##########################################################\n" +
            "Code Quality Gate: ${scriptObj.sonarQubeResult.message}\n" +
            "Code Security Gate: ${scriptObj.fortifyResult.message}\n" +
            "Code Governance Gate: ${scriptObj.snykReuslt.message}\n" +
            "##########################################################\n")
            // Junit test results
            def testReportMask = "target/surefire-reports/*.xml"
            scriptObj.junit allowEmptyResults: true, testResults: testReportMask
            scriptObj.jacoco()
            // configuration.codeSecurityScanSuccessOnGatingFailure
            // configuration.codeQualityScanSuccessOnGatingFailure
            if (!QualityGateCheck.isCodeScanPassed(scriptObj)) {
                scriptObj.currentBuild.result = "UNSTABLE"
            }
            scriptObj.codeScanResult = QualityGateCheck.isCodeScanPassed(scriptObj)
            
        }
        if (scriptObj.currentBuild.result == "ABORTED" ) {
            String currentConcourseJobName = paramsReader.readPipelineParams('current_concourse_job_name')
            if (currentConcourseJobName) {
                String concourseTarget = paramsReader.readPipelineParams('concourseTarget')
                String concoursePipelineName = paramsReader.readPipelineParams('concoursePipelineName')
                ConcourseUtil.abortBuild(scriptObj, concourseTarget, concoursePipelineName, currentConcourseJobName)
            }
        }
    }
    public void postProcessMainOperations(){
        try {
            def dashboard = DashboardUtil.addDashboardInfo(scriptObj)
            DashboardUtil.addJenkinsInfo(scriptObj, dashboard)
            DashboardUtil.addGitInfo(scriptObj, dashboard)
            DashboardUtil.addStatusInfo(scriptObj, dashboard, scriptObj.stageStats)

            scriptObj.configuration['propertiesValid'] = scriptObj.propertiesValid
            DashboardUtil.addPropertiesInfo(scriptObj, dashboard, scriptObj.configuration)

            if (scriptObj.pipelineName == "ci" ) {
                if (!paramsReader.readPipelineParams('skipUnitTest')) {
                    DashboardUtil.addUnitTestInfo(scriptObj, dashboard)
                }
                def ARTIFACTID = scriptObj.ARTIFACTID
                def VERSION = scriptObj.VERSION
                def GROUPID = scriptObj.GROUPID
                DashboardUtil.addVersionInfo(scriptObj, dashboard, VERSION)
                DashboardUtil.addSonarqubeInfo(scriptObj, dashboard, GROUPID, ARTIFACTID)
                // provisioningAPI
                if(paramsReader.readPipelineParams('useProvisioningCli') && ! paramsReader.readPipelineParams('skipDeploy') && !paramsReader.readPipelineParams('isLibrary')) {
                    DashboardUtil.addProvisioningInfo(scriptObj, dashboard)

                } else if (paramsReader.readPipelineParams('skipDeployToAks') && !paramsReader.readPipelineParams('skipDeploy') && !paramsReader.readPipelineParams('isLibrary')) {
                    logger.info('comming soon !')

                } else {
                    DashboardUtil.addPcfInfo(scriptObj, dashboard, GROUPID, ARTIFACTID, VERSION)
                }
                scriptObj.withCredentials([scriptObj.usernamePassword(credentialsId: 'fortify_ssc', usernameVariable: 'USERNAME', passwordVariable: "PASSWORD")]) {
                    DashboardUtil.addFortifyInfo(scriptObj, dashboard, ARTIFACTID, VERSION, scriptObj.env.USERNAME, scriptObj.env.PASSWORD, scriptObj.fortifyScanResult)
                }
                DashboardUtil.addCodeScanResultInfo(scriptObj, dashboard)
            } else {
                scriptObj.env.codeScanResultQuery = scriptObj.env.GIT_COMMIT
                def codeScanPassed = DashboardUtil.getCodeScanResultInfo(scriptObj)
                if ("${codeScanPassed}" == "true") {
                    scriptObj.codeScanResult = true 
                    DashboardUtil.addCodeScanResultInfo(scriptObj, dashboard)
                }
            }
            logger.info("[ Save all info ]")
            DashboardUtil.setDashboard(scriptObj, dashboard) 
        } catch ( Exception err ) {
            logger.info("Exception in update Dashboard ${err}")
        }
    }
    public void postProcessPostOperations() {
        /* remove the condition as this will override the status of build defined
        above if both fortify and sonarqube fails.
        if (!sonarQubeResult.codeQualityGatePassed) {
            currentBuild.result = "FAILURE"
        } */
        def body = "", subject = ""
        def buildResult = scriptObj.currentBuild.result
        def emailRecipientList = paramsReader.readPipelineParams('emailRecipientList')
        scriptObj.echo "======== emailRecipientList: ${emailRecipientList}"

        if (scriptObj.pipelineName == "ci") {
            if(scriptObj.fileExists('target/dependacy-check-report.html')) {
                scriptObj.publishHTML([allowMissing: true, alwaysLinkToLastBuild: false, keepAll: false, reportDir: "target", reportFiles: "dependacy-check-report.html", reportName: "OWASP", reportTitles: "OWASP"])
            }
            if (scriptObj.fileExists('smoke-test/dashboard.htm')) {
                scriptObj.publishHTML([allowMissing: true, alwaysLinkToLastBuild: false, keepAll: false, reportDir: "smoke-test", reportFiles: "dashboard.htm", reportName: "QMETRY", reportTitles: "QMETRY"])

            }
            subject = "${JenkinsUtil.getMultibranchJobRealName(scriptObj, scriptObj.env.JOB_NAME}" + " ${scriptObj.env.JOB_BASE_NAME} - ${scriptObj.env.BUILD_NUMBER} is ${buildResult}"
            body = "Code Quality Gate: ${scriptObj.sonarQubeResult.message}\n" +
            "Code Security Gate: ${scriptObj.fortifyResult.message}\n" +
            "please check ${scriptObj.env.BUILD_URL} for the build details; and ${scriptObj.env.JOB_URL}/QMETRY/ for Qmetry Report\n"
            
        } else {

            subject = "${JenkinsUtil.getMultibranchJobRealName(scriptObj, scriptObj.env.JOB_NAME}" + " ${scriptObj.env.JOB_BASE_NAME} - ${scriptObj.env.BUILD_NUMBER} is ${buildResult}"
            body = "Please check ${scriptObj.env.BUILD_URL} for the build details"
        }

        scriptObj.emailext (
            recipientProviders: [
                [$class: "DevelopersRecipientProvider"],
                [$class: "RequesterRecipientProvider"]
            ],
            to: emailRecipientList,
            subject: subject,
            body: body,
            attachLog: true
        )
    }
}