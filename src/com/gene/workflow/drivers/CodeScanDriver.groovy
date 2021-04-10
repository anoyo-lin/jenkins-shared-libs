package com.gene.workflow.drivers

public class CodeScanDriver extends BaseDriver {
    CodeScanDriver(Script scriptObj) {
        super(scriptObj, 'CodeScanner')
    }
    CodeSCanDriver(Script scriptObj, String className) {
        super(scriptObj, className)
    }
    CodeScanDriver(Script scriptObj, String className, String framework) {
        super(scriptObj, className, framework)
    }
    @Override
    public void main() {
        def codeScanDriverObj =  super.classReflection()
        codeScanDriverObj.codeScanPreOperations()
        codeScanDriverObj.codeScanMainOperations()
        codeScanDriverObj.codeScanPostOperations()
    }
}