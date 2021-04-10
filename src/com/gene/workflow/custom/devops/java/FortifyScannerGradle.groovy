package com.gene.workflow.custom.devops.java
import com.gene.workflow.interfaces.FortifyScanInterface

public class FortifyScannerGradle extends FortifyScanner implements FortifyScanInterface {
    def ARTIFACT_PACKAGING
    def ARTIFACTID
    def GROUPID
    def VERSION
    FortifyScannerGradle(Script scriptObj){
        super(scriptObj)
    }
    @Override
    public void fortifyScanPreOperations() {
        scriptObj.stageStats["${scriptObj.env.STAGE_NAME}"] = 'failed'
        /* TODO why the constructor cann't execute the cps groovy function such as scriptObj.readMavenPom(),
        * please refer to the cps mismatch http://www.jenkines.io/doc/book/pipeline/cps-method-mismatches/
        */
        this.ARTIFACTID = scriptObj.sh(script: "cat gradle.prop|grep 'name'|awk '{print \$2}'", returnStdout: true).trim()
        this.VERSION = scriptObj.sh(script: "cat gradle.prop|grep 'version'|awk '{print \$2}'", returnStdout: true).trim()
        this.GROUPID = scriptObj.sh(script: "cat gradle.prop|grep 'group'|awk '{print \$2}'", returnStdout: true).trim()
        this.ARTIFACT_PACKAGING = scriptObj.sh(script: "if [[ \$(cat gradle.prop|grep 'jar') != '' ]]; then echo 'jar'; fi", returnStdout: true)

        scriptObj.fortifyClean buildID: "${ARTIFACTID}",
        logFile: "${ARTIFACTID}-clean.log"

        scriptObj.fortifyTranslate buildID: "${ARTIFACTID}",
        logFIle: "${ARTIFACTID}-translate.log",
        verbose: true,
        projectScanType: scriptObj.fortifyGradle(gradleOptions: "-x test", gradleTask: "build"),
        excludeList: "${paramsReader.readPipelineParams('fortifyScanIgnoreList')}"
    }
}