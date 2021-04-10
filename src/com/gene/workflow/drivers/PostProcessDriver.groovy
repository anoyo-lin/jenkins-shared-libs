package com.gene.workflow.drivers

public class PostProcessDriver extends BaseDriver {
    PostProcessDriver(Script scriptObj) {
        super(scriptObj, 'PostProcessor')
    }
    PostProcessDriver(Script scriptObj, String className) {
        super(scriptObj, className)
    }
    PostProcessDriver(Script scriptObj, String classname, String framework) {
        super(scriptObj, classname, framework)
    }
    @Override
    public void main() {
        def postProcessorObj = super.classReflection()
        postProcessorObj.postProcessPreOperations()
        postProcessorObj.postProcessMainOperations()
        postProcessorObj.postProcessPostOperations()
    }
    
}