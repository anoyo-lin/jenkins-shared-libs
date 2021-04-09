package com.gene.workflow.drivers

public class ArtifactoryUploadDriver extends BaseDriver {
    ArtifactoryUploadDriver(Script scriptObj) {
        super(scriptObj, 'ArtifactoryUploader')
    }
    ArtifactoryUploadDriver(Script scriptObj, String className) {
        super(scriptObj, className)
    }
    ArtifactoryUploadDriver(Script scriptObj, String className, String framework) {
        super(scriptObj, className, framework)
    }
    @Override
    public void main() {
        def artifactoryUploadObj = super.classReflection()
        artifactoryUploadObj.artifactoryUploadPreOperations()
        artifactoryUploadObj.artifactoryUploadMainOperations()
        artifactoryUploadObj.artifactoryUploadPostOperations()
    }
  
}