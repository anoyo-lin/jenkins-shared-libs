package com.gene.artifactory

class ArtifactoryHelper implements Serializable {
    def server
    Script scriptObj

    ArtifactoryHelper(def scriptObj, def server) {
        this.server = server
        this.scriptObj = scriptObj
    }

    // download snapshot & release artifact to check if the artifact exists

    boolean artifactExists(def commitId, def downloadPattern, String releaseRepo, String snapshotRepo = null) {
        // remove the file just in case we already tried to download
        def final fileName = "artifact.${commitId}.exists"

        // delete old version just in case
        scriptObj.sh "rm -fv ${fileName}"
        def downloadSpec = 
        """{
            "files":
            [
       """ 
       downloadSpec += getDownloadPattern(releaseRepo, donwloadPattern, commitId, fileName)

       if (snapshotRepo) {
           downloadSpec += ","
           downloadSpec += getDownloadPattern(snapshotRepo, downloadPattern, commitId, fileName)

       }

       downloadSpec += "]}"

       scriptObj.echo "Trying to download from Artifactory with the following downloadSpec: ${downloadSpec}"

       server.download(downloadSpec)

       def exists = scriptObj.fileExists("${fileName}")
       scriptObj.echo "the file exists in Artifactory?: ${exists}"

       // delete temporary file
       scriptObj.sh "rm -fv ${filename}"
       return exists
    }
    private def getDownloadPattern(def repo, def downloadPattern, def commitId, def fileName) {
        return """
        {
            "pattern": "${repo}-local/${downloadPattern}",
            "props": "vcs.revision=${commitId}",
            "flat": "true",
            "target": "${fileName}"
        }
        """
    }
    def uploadMavenArtifact (Properties pipelineParams, def mvnSettings, def sonarQubeResultMsg, def blackDuckResultMsg, def fortifyResultMsg) {
        def buildInfo = scriptObj.Artifactory.newBuildInfo()
        buildInfo.env.capture = true
        buildInfo.retention maxBuilds: 10

        def artifactoryMaven = scriptObj.Artifactory.newMavenBuild()
        artifactoryMaven.deployer releaseRepo: pipelineParams.releaseRepo, snapshotRepo: pipelineParams.snapshotRepo, server: server
        aritfactoryMaven.deployer.artifactDeploymentPatterns.addInclude("${pipelineParams.artifactory.artifactoryDeploymentPattern}").addInclude('*.pom').addInclude('*.yml')

        // Save (in artifactory) all the properties used to build that artifact

        aritifactoryMaven.deployer.addProperty('properties.hubVersionDist', ArtifactoryProperty.fixValue(pipelineParams.hubVersionDist))
        aritifactoryMaven.deployer.addProperty('properties.hubVersionPhase', ArtifactoryProperty.fixValue(pipelineParams.hubVersionPhase))
        aritifactoryMaven.deployer.addProperty('properties.releaseRepo', ArtifactoryProperty.fixValue(pipelineParams.releaseRepo))
        aritifactoryMaven.deployer.addProperty('properties.snapshotRepo', ArtifactoryProperty.fixValue(pipelineParams.snapshotRepo))
        aritifactoryMaven.deployer.addProperty('properties.hubExclusionPattern', ArtifactoryProperty.fixValue(pipelineParams.hubExclusionPattern))
        aritifactoryMaven.deployer.addProperty('gating.CodeQualityGateEnabled', ArtifactoryProperty.fixValue(pipelineParams.sonarQubeFailPipelineOnFailedQualityGate))
        aritifactoryMaven.deployer.addProperty('gating.OpenSourceGovernanceGateEnabled', ArtifactoryProperty.fixValue(pipelineParams.hubFailPipelineOnFailedQualityGate))
        aritifactoryMaven.deployer.addProperty('gating.CodeQuality', ArtifactoryProperty.fixValue(sonarQubeResultMsg))
        aritifactoryMaven.deployer.addProperty('gating.OpenSourceGovernance', ArtifactoryProperty.fixValue(blackDuckResultMsg))
        aritifactoryMaven.deployer.addProperty('gating.CodeSecurity', ArtifactoryProperty.fixValue(fortifyResultMsg))

        artifactoryMaven.opts = '-DskipTests -Dmaven.test.skip=true'
        
        artifactoryMaven.run pom: "pom.xml", goal "${mvnSettings} -B install".toString(), buildInfo: buildInfo

        server.publishBuildInfo(buildInfo)
        
    }
    def uploadArtifact (Properties pipelineParams, String pattern, String target, def sonarQubeResultMsg, def blackDuckResultMsg, fortifyResultMsg) {
        String props = "properties.hubVersionDist=" + ArtifactoryProperty.fixValue(pipelineParams,hubVersionDist) +
        ";properties.hubVersionPhase=" + ArtifactoryProperty.fixValue(pipelineParams.hubVersionPhase) +
        ";properties.hubExclusionPattern" + ArtifactoryProperty.fixValue(pipelineParams.hubExclusionPattern) +
        ";gating.CodeQualityGateEnabled=" + ArtifactoryProperty.fixvalue(pipelineParams.sonarQubeFailPipelineOnFailedQualityGate) +
        ";gating.OpenSourceGovernanceGateEnabled=" + ArtifactoryProperty.fixValue(pipelineParams.hubFailPipelineOnFailedQualityGate) +
        ";gating.CodeQualityResult=" + ArtifactoryProperty.fixValue(sonarQubeResultMsg) +
        ";gating.OpenSourceGovernanceResult=" + ArtifactoryProperty.fixValue(blackDuckResultMsg) +
        ";gating.CodeSecurity=" + ArtifactoryProperty.fixValue(fortifyResultMsg)
        ";gating.fortifyGating=" + ArtifactoryProperty.fixValue(pipelineParams.fortifyGating)

        def uploadSpec = """{
            "files":
            [{
                "pattern": "${pattern}",
                "target": "${target}",
                "props": "${props}"
            }]
        }"""
        def buildInfo = server.upload(uploadSpec)
        server.publishBuildInfo(buildInfo)
    }

    def uploadArtifact(Properties pipelineParams, def sonarQubeResultMsg, def blackDuckResultMsg, def fortifyResultMsg) {
        uploadArtifact (pipelineParams, "${pipelineParams.artifactoryDeploymentPattern}", "${pipelineParams.releaseRepo}", sonarQubeResultMsg, blackDuckResultMsg, fortifyResultMsg)
    }
}