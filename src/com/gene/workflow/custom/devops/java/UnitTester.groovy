package com.gene.workflow.custom.devops.java

import com.gene.workflow.interfaces.UnitTestInterface
import com.gene.logger.*
import com.gene.provisioning.Utils

public class UnitTester implements UnitTestInterface {
    protected Script scriptObj
    protected Logger logger
    protected String language
    protected Properties pipelineParams

    UnitTester(Script scriptObj) {
        this.scriptObj = scriptObj
        this.logger = new Logger(scriptObj, Level.INFO)
    }
    public void unitTestPreOperations() {
        this.language = Utils.getSourceFramework(scriptObj)
        this.pipelineParams = scriptObj.pipelineParams
        scriptObj.stageStats["${scriptObj.env.STAGE_NAME}"] = 'failed'
        if ( language == 'java') {
            logger.info('Running Jacoco TCP Server')
            scriptObj.sh "rm -f jacoco-server.exec && java -jar /tech/jacoco/jacoco-tcpserver.jar &"

        } else {
            logger.info("empty unitTest preOperations")
            logger.info("==========Unit Test===========")
        }
    }
    public void unitTestMainOperations() {
        if ( this.language == 'java') {
            scriptObj.sh "mvn --settings settings.xml clean test package -Djacoco.output=tcpclient -Djacoco.address=localhost -Djacoco.port=6300 -Djacoco.reset=true -Djacoco.append=true"

        } else if ( this.language == 'python') {
            scriptObj.sh "sudo pip3 install --index-url ${scriptObj.env.PYTHON+PACKAGES_URL} --trusted-host ${scriptObj.env.PYTHON_TRUSTED_HOST} -r requirements.txt"
            scriptObj.sh "coverage run ${scriptObj.pipelineParams.coverageParams}"
            scriptObj.sh "coverage report"
            scriptObj.sh "coverage xml -i"
        }
    }
    public void unitTestPostOperations() {
        if ( this.language == 'java' ) {
            scriptObj.sh "rm -rf target-sonar && cp -R target target-sonar"
        } else if (this.language == 'python') {
            scriptObj.sh "rm -rf .scannerwork"
            scriptObj.sh "rm -rf coverage_html"
        }
        scriptObj.stageStats["${scriptObj.env.STAGE_NAME}"] = 'successful'
    }
}