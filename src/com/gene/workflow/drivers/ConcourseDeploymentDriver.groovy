package com.gene.workflow.drivers

public class ConcourseDeploymentDriver extends BaseDriver {
    ConcourseDeploymentDriver(Script scriptObj) {
        super(scriptObj, 'ConcourseDeployer')
    }
    ConcourseDeploymentDriver(Script scriptObj, String className) {
        super(scriptObj, className)
    }
    ConcourseDeploymentDriver(Script scriptObj, String className, String framework){
        super(scriptObj, className, framework)
    }
    @Override
    public void main() {
        def concourseDeploymentDrvierObj = super.classReflection()
        concourseDeploymentDrvierObj.concourseDeployPreOperations()
        concourseDeploymentDrvierObj.concourseDeployMainOperations()
        concourseDeploymentDrvierObj.concourseDeployPostOperations()
    }
}