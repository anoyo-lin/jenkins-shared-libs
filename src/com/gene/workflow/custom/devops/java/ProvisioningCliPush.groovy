package com.gene.workflow.custom.devops.java

import com.gene.provisioning.*
import com.gene.workflow.interfaces.ProvisioningCliInterface

public class ProvisioningCliPush extends ProvisioningCli implements ProvisioningCliInterface {
    public ProvisioningCliPush(Script scriptObj) {
        super(scriptObj)
    }
    public void provisionObjInit() {
        provisionObj.framework = Utils.getSourceFramework(scriptObj)
        if (scriptObj.fileExists(paramsReader.readPipelineParams('manifestFileName'))) {
            // provisionObj init
            def buildManifest = new BuildManifest(scriptObj)
            buildManifest.readManifestFile()
            provisionObj.stack = buildManifest.getStack()
            provisionObj.environmentVariables = buildManifest.getEnvironmentVariables()
            if ( buildManifest.getRoutes() ! = '' ) {
                provisionObj.routes = buildManifest.getRoutes()
                provisionObj.newRoutes = buildManifest.genNewroutes()
            }
            provisionObj.services = buildManifest.getServices()
            provisionObj.sourcePath = buildManifest.getSourcePath()
            provisionObj.buildPack = buildManifest.getBuildPack()
            provisionObj.appName = buildManifest.getAppName()
            provisionObj.appNewName = buildManifest.genAppNewName()
            provisionObj.appOldName = buildManifest.genAppOldName()
            provisionObj.deleteOldApp = paramsReader.readPipelineParams('deleteOldApp')
            provisionObj.createDeploymentMarker = paramsReader.readPipelineParams('createDeploymentMarker')
            provisionObj.healthCheckType = paramsReader.readPipelineParams('healthCheckType')
            provisionObj.healthCheckHttpEndpoint = paramsReader.readPipelineParams('healthCheckHtppEndpoint')
            provisionObj.healthCheckTimeout = paramsReader.readPipelineParams('healthCheckTimeout')
            provisonObj.noStart = paramsReader.readPipelineParams('noStart')
            provisonObj.manifestFileName = paramsReader.readPipelineParams('manifestFileName')
            // packaging the binaries
            if (scriptObj.params.PROVISIONING_BAU_CONTROL == null || scriptObj.env.PROVISIONING_BAU_CONTROL == null) {
                def mavenFiles = scriptObj.findFiles(glob: 'target/*.jar').length + 
                scriptObj.findFiles(glob: 'target/*.war').length
                def mavenExists = mavenFiles > 0
                def gradleFiles = scriptObj.findFiles(glob: 'build/libs/*.jar').length +
                scriptObj.findFiles(glob: 'build/libs/*.war').length
                def gradleExists = gradleFiles > 0
                if ( !gradleExists && !mavenExists) {
                    if ( scriptObj.findFiles(glob: 'pom.xml').length > 0  ) {
                        if (provisionObj.framework == 'java' ) {
                            scriptObj.sh "mvn --settings settings.xml -U -DskipTests clean package"
                        }
                    } else if ( scriptObj.findFiles(glob: 'build.gradle').length > 0) {
                        if (provisionObj.framework == 'java' ) {
                            scriptObj.sh "gradle assemble -x test"
                        }
                    } else {
                        throw new Exception('no artifact or build configuration files both in gradle & maven')
                    }
                }
                super.packageApplication()
            }

        }
        provisionObj.api = scriptObj.pipelineParams.provisioningAPI
        provisionObj.org = scriptObj.pipelineParams.deployTargetOrg
        provisionObj.space = scriptObj.pipelineParams.deployTargetSpace
        provisionObj.foundation = scriptObj.pipelineParams.deployTargetFoundation
        provisionObj.teamEmail = scriptObj.pipelineParams.teamEmail
        logger.info(provisionObj.toString())
        scriptObj.provisionObj = provisionObj
    }
    @Override
    public void PreOperations() {
        scriptObj.stageStats["${scriptObj.env.STAGE_NAME}"] = 'failed'
        provisionObjInit()
        // provisioningCLI init
        def operatingSystem = super.getOperatingSystem()

    }
}