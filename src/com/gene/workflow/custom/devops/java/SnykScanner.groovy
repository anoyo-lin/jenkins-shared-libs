package com.gene.workflow.custom.devops.java

import com.gene.workflow.interfaces.SnykScanInterface

public class SnykScanner extends com.gene.snyk.SnykRunner implements SnykScanInterface {
    protected snykResult

    SnykScanner(Script scriptObj) {
        super(scriptObj)
    }
    public void snykScanPreOperations() {
        scriptObj.stageStats["${scriptObj.env.STAGE_NAME}"] = 'failed'
        logger.info("empty snykScan pre Operations")
        scriptObj.sh " if [ -f gradlew ]; then chmod +x gradlew; fi"
        def snykId = paramsReader.readPipelineParams('snykTokenId') ? paramsReader.readPipelineParams('snykTokenId') : 'SNYK_HK_ORG'
        scriptObj.echo snykId
        scriptObj.withCredentials([scriptObj.string(credentialsId: snykId, variable: 'SNYK_TOKEN')]) {
            scriptObj.sh "snyk auth -d ${scriptObj.env.SNYK_TOKEN}"
        }
    }
    public void snykScanMainOperations() {
        snykResult = super.run()
    }
    public void snykScanPostOperations() {
        logger.info('empty snykScan post Opertions')
        scriptObj.snykResult = snykResult
        scriptObj.stageStats["${scriptObj.env.STAGE_NAME}"] = 'successful'
    }

}