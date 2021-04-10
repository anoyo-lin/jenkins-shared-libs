package com.gene.workflow.drivers

public class GitflowUpdateDriver extends BaseDriver {
    GitflowUpdateDriver(Script scriptObj) {
        super(scriptObj, 'GitflowUpdater')
    }
    GitflowUpdateDriver(Script scriptObj, String className) {
        super(scriptObj, className)
    }
    GitflowUpdateDriver(Script scriptObj, String className, String framework) {
        super(scriptObj, className, framework)
    }
    @Override
    public void main() {
        def gitflowUpdateObj = super.classReflection()
        gitflowUpdateObj.gitflowUpdatePreOperations()
        gitflowUpdateObj.gitflowUpdateMainOperations()
        gitflowUpdateObj.gitflowUpdatePostOperations()
    }
}