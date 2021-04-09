package com.gene.workflow.custom.devops.java

import com.gene.workflow.interfaces.ArtifactoryUploadInterface

public class ArtifactoryUploaderGradle extends ArtifactoryUploader implements ArtifactoryUploadInterface {
    ArtifactoryUploaderGradle(Script scriptObj) {
        super(scriptObj)
    }
    @Override
    public void artifactoryUploadMainOperations() {
        logger.info("artifactory upload the artifact in gradle pipeline")
        scriptObj.sh "gradle artifactoryPublish -x test"
    }
}