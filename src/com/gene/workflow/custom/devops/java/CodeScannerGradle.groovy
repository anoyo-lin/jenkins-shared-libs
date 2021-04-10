package com.gene.workflow.custom.devops.java
import com.gene.workflow.interfaces.CodeScanInterface

public class CodeScannerGradle extends CodeScanner implements CodeScanInterface {
    CodeScannerGradle(Script scriptObj) {
        super(scriptObj)
    }
    @Override
    public void codeScanPreOperations() {
        scriptObj.stageStats["${scriptObj.env.STAGE_NAME}"] = 'failed'
        logger.info("==========SonarQube Scan=========")
        // init at the pipelineParamsReader stage
        // scriptObj.sonarQubeResult = new SonarQubeResult()
        // scriptObj.sonarQubeResult.message = "Project status UNKNOWN. SonarQube wasn't called."

    }
    @Override
    public void codeScanMainOperations() {
        scriptObj.withSonarQubeEnv("New Sonar") {
            scriptObj.sh """#!/bin/bash -e
            |set -x
            |test -d build/classes && java -jar /tech/jacoco/jacococli.jar report jacoco-server.exec --classfiles build/classes --xml jacoco-report.xml
            |gradle sonarqube
            |""".stripMargin()
        }
    }
}