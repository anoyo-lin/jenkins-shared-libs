package com.gene.workflow.custom.devops.java

import com.gene.workflow.interfaces.UnitTestInterface

public class UnitTesterGradle extends UnitTester implements UnitTestInterface {
    UnitTesterGradle(Script scriptObj) {
        super(scriptObj)

    }
    @Override 
    public void unitTestMainOperations() {
        scriptObj.sh "gradle test"
        if ( scriptObj.env.pipelineName == "ci" ) {
            scriptObj.sh "gradle assemble -x test"
        }
    }
    @Override
    public void unitTestPostOperations() {
        scriptObj.stageStats["${scriptObj.env.STAGE_NAME}"] = 'successful'
    }
}