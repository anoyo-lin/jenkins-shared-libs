package com.gene.workflow.custom.devops.java

import com.gene.workflow.interfaces.ArtifactoryDownloadInterface

public class ArtifactoryDownloadGradle extends ArtifactoryDownload implements ArtifactoryDownloadInterface {
    def ARTIFACTID
    def VERSION
    def GROUPID
    def ARTIFACT_PACKAGING
    def ARTIFACTORY_URL
    def ARTIFACTORY_URL_CONTEXT

    ArtifactoryDownloadGradle(Script scriptObj) {
        super(scriptObj)
    }
    @Override
    public void artifactoryDownloadMainOperations() {
        this.ARTIFACTID = scriptObj.sh(script: "cat gradle.prop|grep 'name'|awk '{print \$2}'", returnStdout: true)
        this.VERSION = scriptObj.sh(script: "cat gradle.prop|grep 'version'|awk '{print \$2}'", returnStdout: true)
        this.GROUPID = scriptObj.sh(script: "cat gradle.prop|grep 'group'|awk '{print \$2}'", returnStdout: true)
        this.ARTIFACT_PACKAGING = scriptObj.sh(script: "if [[ \$(cat gradle.prop|grep 'jar') != '' ]]; then echo 'jar'; fi", returnStdout: true)
        this.ARTIFACTORY_URL = scriptObj.sh(script: "cat gradle.prop|grep 'artifatoryContextUrl'|awk '{print \$2}'", returnStdout: true)
        this.ARTIFACTORY_URL_CONTEXT = null    
        if (this.VERSION.contains('-SNAPSHOT')) {
            ARTIFACTORY_URL_CONTEXT = 'libs-snapshot-local/'
        } else {
            ARTIFACTORY_URL_CONTEXT = 'libs-release-local/'
        }
        ARTIFACTORY_URL += ARTIFACTORY_URL_CONTEXT
        logger.info("the upload target URL is: ${ARTIFACTORY_URL}")
        if (framework == 'java' || framework == 'javaGradle' || framework == 'javaMaven' ) {
            def fileName = "${ARTIFACTID}-${VERSION}.${ARTIFACT_PACKAGING}"
            // def fileName = ArtifactoryUtil.donwloadArtifact(scriptObj)
            def fileName = ArtifactoryUtil.downloadArtifact(scriptObj, ARTIFACTORY_URL, GROUPID, ARTIFACTID, VERSION, ARTIFACT_PACKAGING, fileName)
            scriptObj.sh """if [[ ! -d target ]]; then mkdir target; fi
            cp ${fileName} target/
            """
        } else {
            throw new Exception("please check project based on Java?")
        }
    }
}