package com.gene.workflow.drivers

public class FortifyScanDriver extends BaseDriver {
    FortifyScanDriver(Script scriptObj) {
        super(scriptObj, 'FortifyScanner')
    }
    FortifyScanDriver(Script scriptObj, String className) {
        super(scriptObj, className)
    }
    FortifyScanDriver(Script scriptObj, String className, String framework){
        super(scriptObj, className, framework)
    }
    @Override
    public void main() {
        def fortifyScanDriverObj = super.classReflection()
        fortifyScanDriverObj.fortifyScanPreOperations()
        fortifyScanDriverObj.fortifyScanMainOperations()
        fortifyScanDriverObj.fortifyScanPostOperations()
    }
}