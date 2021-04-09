package com.gene.util
import com.gene.sonarqube.SonarQubeResult
import com.gene.fortify.FortifyResult
import com.gene.parameters.ParametersReader

class QualityGateCheck {
    public static isCodePassed(Script scriptObj) {
        SonarQubeResult sqr = scriptObj.sonarQubeResult
        FortifyResult fr = scriptObj.fortifyResult
        def paramsReader = new ParametersReader(scriptObj)
        if (
            (!sqr.codeQualityGatePassed && !paramsReader.readPipelingParams('codeSecurityScanSuccessOnGatingFailure')) ||
            (!fr.codeSecurityGatePassed && !paramsReader.readPipelingParams('codeQualityScanSuccessOnGatingFailure'))

        ) {
            return false
        } else {
            return true
        }
    }
}