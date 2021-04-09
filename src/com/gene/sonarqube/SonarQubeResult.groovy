package com.gene.sonarqube
import com.gene.parameters.PipelineParams
// CAN NOT IMPORT THE CLASS HERE, FOR GROOVY INHERITANCE ITI IS STRANGE IMPORT WILL DESTROY THE BEHAVIOR INHERITANCE OF JAVA

class SonarQubeResult implements Serializable {
    String message
    boolean codeQualityGatePassed
    SonarQubeResult() {}
    // SonarQubeResult(Script scriptObj) {
        // this.message = "Project status UNKNOWN. sonarQube wasn't called."
        // if(scriptObj.paramsDriverObj.pipelineParamsObj.readPipelineParms('skipCodeQualityScan')) {
            // this.codeQualityGatePassed = true
            // this.message = "Project SKIPPED Code Quality Gate!"
        // }
    // }
}