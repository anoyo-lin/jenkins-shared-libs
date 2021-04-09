package com.gene.workflow.drivers

public class NewRelicDriver extends BaseDriver {
    NewRelicDriver(Script scriptObj) {
        super(scriptObj, 'NewRelic')
    }
    NewRelicDriver(Script scriptObj, String className) {
        super(scriptObj, className)
    }
    NewRelicDriver(Script scriptObj, String className, String framework) {
        super(scriptObj, className,, framework)
    }
    @Override
    public void main() {
        def newRelicObj = super.classReflection()
        newRelicObj.newRelicPreOperations()
        newRelicObj.newRelicMainOperations()
        newRelicObj.newRelicPostOperations()
    }
}
