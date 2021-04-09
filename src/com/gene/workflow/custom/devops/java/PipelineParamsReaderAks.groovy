package com.gene.workflow.custom.devops.java

import com.gene.workflow.interfaces.PipelineParamsReadInterface
import com.gene.parameters.ParametersReaderAks

class PipelineParamsReaderAks extends PipelineParamsReader implements PipelineParamsReadInterface {
    private paramsReaderAksObj
    PipelineParamsReaderAks(Script scriptObj) {
        super(scriptObj)
    }
    @Override
    public void pipelineParamsReadMainOperations() {
        this.paramsReaderAksObj = new ParametersReaderAks(scriptObj)
        scriptObj.pipelineParams = paramsReaderAksObj.assembleParams()
    }
}