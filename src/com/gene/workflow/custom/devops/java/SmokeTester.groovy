package com.gene.workflow.custom.devops.java

import com.gene.logger.*
import com.gene.workflow.interfaces.SmokeTestInterface
import com.gene.parameters.ParametersReader

class SmokeTester implements SmokeTestInterface {
    protected Script scriptObj
    protected Logger logger
    protected ParametersReader paramsReader
    SmokeTester(Script scriptObj) {
        this.scriptObj = scriptObj
        this.logger = new Logger(scriptObj, Level.INFO)
        this.paramsReader = new ParametersReader(scriptObj)
    }
    public void smokeTestPreOperations() {
        scriptObj.stageStats["${scriptObj.env.STAGE_NAME}"] = 'failed'
        logger.info("========== Run Smoke Test==========")
        scriptObj.sh "rm -rf smoke-test && git clone " +
        paramsReader.readPipelineParams('smokeTestRepo') +
        " smoke-test"
        scriptObj.sh "cd smoke-test && gti checkout " +
        paramsReader.readPipelineParams('smokeTestBranch')
    }
    public void smokeTestMainOperations() {
        scriptObj.sh "cd smoke-test && mvn --settings settings.xml -U clean test -Dreportium-job-name=${scriptObj.env.JOB_NAME} -Dreportium-job-number=${scriptObj.env.BUILD_NUMBER} " +
        paramsReader.readPipelineParams('smokeTestParameters')
    }
    public void smokeTestPostOperations() {
        def testResults = scriptObj.readJSON file: "smoke-test/test-results/meta-info.json"
        def report = scriptObj.readJSON file: "smoke-test/" + testResults.reports[0].dir + '/meta-info.json'

        logger.info("${testResults.reports[0]} - ${report}")

        if(report.status == 'fail') {
            logger.info('Smoke Test Failed')
        } else {
            scriptObj.stageStats["${scriptObj.env.STAGE_NAME}"] = 'successful'
        }
    }
}