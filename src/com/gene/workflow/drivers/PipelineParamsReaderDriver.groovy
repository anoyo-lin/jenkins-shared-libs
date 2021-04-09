package com.gene.workflow.drivers

public class PipelineParamsReaderDriver extends BaseDriver {
    PipelineParamsReaderDriver(Script scriptObj) {
        super(scriptObj, 'PipelineParamsReader')
    }
    PipelineParamsReaderDriver(Script scriptObj, String className) {
        super(scriptObj, className)
    }
    PipelineParamsReaderDriver(Script scriptObj, String className, String framework) {
        super(scriptObj, className, framework)
    }
    @Override
    public void main() {
        def pipelineParamsReaderObj = super.classReflection()
        pipelineParamsReaderObj.pipelineParamsReadPreOperations()
        pipelineParamsReaderObj.pipelineParamsReadMainOperations()
        pipelineParamsReaderObj.pipelineParamsReadPostOperations()
        
    }
}