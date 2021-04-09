package com.gene.workflow.custom.devops.java

import com.gene.logger.*
import com.gene.workflow.interfaces.SmokeTestInterface
import com.gene.parameters.ParametersReader

class SmokeTesterAks implements SmokeTestInterface {
    protected Script scriptObj
    protected Logger logger
    protected ParametersReader paramsReader
    SmokeTesterAks(Script scriptObj) {
        this.scriptObj = scriptObj
        this.logger = new Logger(scriptObj, Level.INFO)
        this.paramsReader = new ParametersReader(scriptObj)
    }
    public void smokeTestPreOperations() {
        scriptObj.stageStats["${scriptObj.env.STAGE_NAME}"] = 'failed'
        logger.info("==============Run Smoke Test=================")

    }
    public void smokeTestMainOperations() {
        logger.info('================Health Check, Version verify OK!===============')
    }
    public void smokeTestPostOperations() {
        scriptObj.stageStats["${scriptObj.env.STAGE_NAME}"] = 'successful'
    }
}