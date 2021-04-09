package com.gene.workflow.custom.devops.java

import com.gene.workflow.interfaces.ArtifactoryUploadInterface
import com.gene.logger.*

class ArtifactoryUploader implements ArtifactoryUploadInterface {
    protected Script scriptObj
    protected Logger logger
    ArtifactoryUploader(Script scriptObj) {
        this.scriptObj = scriptObj
        this.logger = new Logger(scriptObj, Level.INFO)
    }
    public void artifactoryUploadPreOperations() {
        scriptObj.stageStats["${scriptObj.env.STAGE_NAME}"] = 'failed'
        logger.info("==========Upload to Artifactory=========")
    }
    public void artifactoryUploadMainOperations() {
        scriptObj.sh "mvn --settings settings.xml -U -DskipTests deploy"

    }
    public void artifactoryUploadPostOperations() {
        logger.info("empty artifactoryUpload Post Operations()")
        scriptObj.stageStats["${scriptObj.env.STAGE_NAME}"] = 'successful'
    }
}