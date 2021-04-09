package com.gene.workflow.custom.devops.java

import com.gene.workflow.interfaces.FortifyScanInterface
import com.gene.logger.*
import com.gene.parameters.ParametersReader

public class FortifyScanner implements FortifyScanInterface {
    protected Script scriptObj
    protected Logger logger
    protected ParametersReader paramsReader

    protected ARTIFACTID
    protected VERSION
    protected GROUPID
    FortifyScanner(Scipt scriptObj) {
        this.scriptObj = scriptObj
        this.logger = new Logger(scriptObj, Level.INFO)
        this.paramsReader = new ParametersReader(scriptObj)
    }
    public void fortifyScanPreOperations() {
        scriptObj.stageStats["${scriptObj.env.STAGE_NAME}"] = 'failed'
        /* TODO why the constructor cann't execute the cps groovy function such as scriptObj.readMavenPom(),
        * please refer to the cps mismatch https://www.jenkins.io/doc/book/pipeline/cps-method-mismatch/
        */
        this.ARTIFACTID = scriptObj.readMavenPom().getArtifactId()
        this.VERSION = scriptObj.readMavenPom().getVersion()
        this.GROUPID = scriptObj.readMavenPom().getGroupId()

        scriptObj.fortifyClean buildID: "${ARTIFACTID}",
        logFile: "${ARTIFACTID}-clean.log"

        scriptObj.fortifyTranslate buildID: "${ARTIFACTID}",
        logFile: "${ARTIFACTID}-translate.log",
        verbose: true,
        projectScanType: scriptObj.fortifyMaven3(mavenOptions: '-DskipTests'),
        excludeList: "${paramsReader.readPipelineParams('fortifyScanIgnoreList')}"
    }
    public void fortifyScanMainOperations() {
        scriptObj.fortifyScan buildID: "${ARTIFACTID}",
        resultsFile: "${ARTIFACTID}.fpr",
        verbose: true,
        logFile: "${ARTIFACTID}-scan.log",
        maxHeap: "2500"
    }
    public void fortifyScanPostOperations() {
        scriptObj.withCredentials([scriptObj.usernamePassword(credentialsId: 'fortify_ssc', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {

            logger.info("cd /tech/fortify-tool && node index.js ${ARTIFACTID} ${VERSION} NOTOKEN \
            ${paramsReader.readPipelineParams('emailRecipientList').replace(" ", "")} \
            ${scriptObj.env.USERNAME} ${scriptObj.env.PASSWORD}")
            scriptObj.sh "cd /tech/fortify-tool && node index.js ${ARTIFACTID} ${VERSION} NOTOKEN \
            ${paramsReader.readPipelineParams('emailRecipientList').replace(" ","")} \
            ${scriptObj.env.USERNAME} ${scriptObj.env.PASSWORD}"
        }
        def failureCriteria = "[fortify priority order]:critical [fortify priority order]:high \
        [fortify priority order]:medium"
        if (paramsReader.readPipelineParams('codeSecurityScanSuccessOnGatingFailure')) {
            failureCriteria = ''
        }
        scriptObj.fortifyUpload appname: "${ARTIFACTID}",
        appVersion: "${VERSION}",
        resultsFile: "${ARTIFACTID}.fpr",
        failureCriteria: failureCriteria
        
        logger.info("Fortify Upload Status -> ${scriptObj.currentBuild.currentResult}")

        if (scriptObj.currentBuild,currentResult == 'SUCCESS') {
            scriptObj.fortifyResult.codeSecurityGatePassed = true
            scriptObj.fortifyScanResult = "Success"
            scriptObj.fortifyResult.message = "Project PASSED Security Quality Gate!"
        } else {
            scriptObj.fortifyResult.codeSecurityGatePassed = false
            scriptObj.fortifyScanResult = "Error"
            scriptObj.fortifyResult.message = "Project Failed Security Quality Gate!"

        }
        scriptObj.stageStats["${scriptObj.env.STAGE_NAME}"] = 'successful'
    }

}