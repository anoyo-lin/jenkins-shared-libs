package com.gene.workflow.custom.devops.java

import com.gene.workflow.interfaces.GitFlowUpdateInterface
import com.gene.logger.*
import com.gene.parameters.*

class ArtifactReleaser implements GitFlowUpdateInterface {
    private Logger logger
    private Script scriptObj
    private ParametersReader paramsReader
    public ArtifactReleaser(Script scriptObj) {
        this.scriptObj = scriptObj
        this.logger = new Logger(scriptObj, Level.INFO)
        this.paramsReader = new ParametersReader(scriptObj)
    }
    public void gitFlowUpdatePreOperations() {
        scriptObj.stageStats["${scriptObj.env.STAGE_NAME}"] = 'failed'
        logger.info("=================== Release Artifact Only=============")
        def pom = scriptObj.readMavenPom file 'pom.xml'
        pom.version = pom.version.replace('--SNAPSHOT', '')
        scriptObj.writeMavenPom model: pom
    }
    public void gitFlowUpdateMainOperations() {
        // temporarily since rsf-parent do append the artifactory configuration for publishing and artifact
        def ARTIFACTID = scriptObj.readMavenPom().getArtifactId()
        def VERSION = scriptObj.readMavenPom().getVersion()
        def ARTIFACT_PACKAGING = scriptObj.readMavenPom().getPackaging()
        def commandStr = scriptObj.env.customMavenProfile ? 
        "mvn --settings settings.xml -U -DskipTests clean package ${scriptObj.env.customMavenProfile}" :
        "mvn --settings settings.xml -U -DskipTests clean package"
        scriptObj.sh commandStr
        def file = 'target/' + ARTIFACTID + '-' + VERSION + '.' + ARTIFACT_PACKAGING
        ArtifactoryUtil.uploadArtifact(file, 'lib-release')
    }
    public void gitFlowUpdatePostOperations() {
        logger.info("empty gitFlowUpdatePostOperations body, please inject your custom logic here.")
    }
}