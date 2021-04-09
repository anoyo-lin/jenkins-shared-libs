package com.gene.workflow.drivers

public class ArtifactoryDownloadDriver extends BaseDriver {
    ArtifactoryDownloadDriver(Script scriptObj) {
        super(scriptObj, 'ArtifactoryDownload')
    }
    ArtifactoryDownloadDriver(Script scriptObj, String className) {
        super(scriptObj, className)
    }
    ArtifactoryDownloadDriver(Script scriptObj, String className, String framework) {
        super(scriptObj, className, framework)
    }
    @Override
    public void main(){
        def artifactoryDownloadObj = super.classReflection()
        artifactoryDownloadObj.artifactoryDownloadPreOperations()
        artifactoryDownloadObj.artifactoryDownloadMainOperations()
        artifactoryDownloadObj.artifactoryDownloadPostOperations()
    }
}