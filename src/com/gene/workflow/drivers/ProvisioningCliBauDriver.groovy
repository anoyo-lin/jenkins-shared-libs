package com.gene.workflow.devops.java

public class ProvisioningCliBauDriver extends BaseDriver {
    ProvisioningCliBauDriver(Script scriptObj) {
        super(scriptObj, 'ProvisioningCliBau')
    }
    ProvisioningCliBauDriver(Script scriptObj, String className) {
        super(scriptObj, className)
    }
    ProvisioningCliBauDriver(Script scriptObj, String className, String framework) {
        super(scriptObj, className, framework)
    }
    @Override
    public void main() {
        def provisioningCliBauObj = super.classReflection()
        provisioningCliBauObj.provisioningCliPreOperations()
        provisioningCliBauObj.provisioningCliPostOperations()
        provisioningCliBauObj.provisioningCliMainOperations()
        provisioningCliBauObj.pro
    }
}