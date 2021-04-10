package com.gene.fortify
// import com.gene.parameters.PipelineParams
// CAN NOT IMPORT THE CLASS HERE, FOR GROOVY INHERITENCE IT IS STRAGE IMPORT WILL 
// DESTROY THE BEHAVIOR INHERITENCE OF JAV


class FortifyResult implements Serializable {
    String message
    boolean codeSecurityGatePassed
    FortifyResult(){}
    // FortifyResult(Script scriptObj) {
        // this.message = "Project Status UNKNOWN. Fortify Scan wasn't called"
        // if ( scriptObj.paramsDriverObj.PipelineParamsObj.readPipelineParams('skipFortifyScan')){
            // this.codeSecurityGatePassed = true
            // this.message = "Project SKIPPED Security Quality Gate!"
        // }
    // }
}
    