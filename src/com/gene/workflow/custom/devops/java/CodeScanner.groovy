package com.gene.workflow.custom.devops.java
import com.gene.workflow.interfaces.CodeScanInterface
import com.gene.logger.*

class CodeSCanner implements CodeScanInterface {
    protected Script scriptObj
    protected Logger logger
    CodeScanInterface(Script scriptObj) {
        this.scriptObj = scriptObj
        this.logger = new Logger(scriptObj, Level.INFO)

    }
    public void codeScanPreOperations() {
        scriptObj.stageStats["${scriptObj.env.STAGE_NAME}"] = 'failed'
        logger.info("===========SonarQube Scan==============")
        // init at the pipelineParamsReader stage
        // scriptObj.sonarQubeResult = new SonarQuebeResult()
        // scriptObj.sonarQubeResult.message = " project status UNKNOWN. sonarQuebe wasn't called."
        scriptObj.sh "mkdir -p target-sonar"
    }
    public void codeScanMainOperations() {
        scriptObj.withSonarScanEnv("New Sonar") {
            scriptObj.sh """#!/bin/bash -e
            |set -x
            |test -d target-sonar/classes && java -jar /tech/jacoco/jacococli.jar report jacoco-server.exec --classfiles target-sonar/classes --xml jacoco-report.xml
            |JAVA_HOME='/usr/lib/jvm/java-11' mvn --settings settings.xml -Dsonar.java.binaries=target-sonar -Dsonar.coverage.jacoco.xmlReportPaths=jacoco-report.xml sonar:sonar
            |""".stripMargin()
        }
    }
    public void codeScanPostOperations() {
        def qualityGate = scriptObj.waitForQualityGate()
        logger.info("Sonar Gate: ${qualityGate.status}")
        if (qualityGate.status == "OK") {
            scriptObj.sonarQubeResult.codeQualityGatePassed = true
            scriptObj.sonarQubeResult.message = "Project PASSED Code Quality Gate!"

        } else {
            scriptObj.sonarQubeResult.codeQualityGatePassed = false
            scriptObj.sonarQubeResult.message = "Project FAILED Code Quality Gate!"
        }
        scriptObj.stageStats["${scriptObj.env.STAGE_NAME}"] = 'successful'
    }
}