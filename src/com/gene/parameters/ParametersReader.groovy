package com.gene.parameters

import com.gene.ap.git.GitUtil
import com.gene.logger.*
import com.gene.util.notifications.NotificationsPropertiesCatalogBuilder
import com.gene.util.propertyFile.PropertiesCatalog
import com.gene.util.propertyFile.PropertiesFilesReader


class ParametersReader implements Serializable {
    protected Script scriptObj
    protected Map configuration
    protected Properties pipelineParams
    protected Logger logger

    ParametersReader(Script scriptObj){
        this.scriptObj = scriptObj
        this.configuration = scriptObj.configuration
        this.pipelineParams = scriptObj.pipelineParams
        this.logger = new Logger(scriptObj, Level.INFO)

    }

    protected Properties assembleParams() {
        // clone the properties' rep to ./golden_pipeline
        String pipelinePropertiesFolder
        List pipelineRepoBranches

        scriptObj.echo "configuration.skipDeploy is ${configuration.get('skipDeploy')}"

        if (configuration.pipelineRepository) {
            if (configuration.pipelinePropertiesFolder != null) {
                pipelinePropertiesFolder = configuration.pipelinePropertiesFolder
            } else {
                pipelinePropertiesFolder = 'golden_pipeline'
            }
            if (configuration.pipelineRepoBranches != null ) {
                pipelineRepoBranches = configuration.pipelineRepoBranches
            } else {
                pipelineRepoBranches = [[name:"master"]]
            }
            GitUtil.cloneRepository(
                scriptObj,
                configuration.pipelineRepository,
                scriptObj.scm.getUserRemoteConfigs()[0].getCredentialId(),
                pipelineRepoBranches,
                pipelinePropertiesFolder
            )
        }
        // init the default properties if not set
        if ( configuration.commonPropertiesFileName == null ){
            boolean fileExists = scriptObj.fileExists "cicd/jenkins/ci/common-ci.properties"
            if (fileExists) {
                configuration.commonPropertiesFileName = "cicd/jenkins/ci/common-ci.properties"

            } else {
                configuration.commonPropertiesFileName = "ci/common-ci.properties"
            }
        }
        if ( configuration.propertiesFolderName == null )
        configuration.propertiesFolderName = "golden_pipeline/jenkins"
        if ( configuration.propertiesFileName == null )
        configuration.propertiesFileName = "ci/ci.properties"
        PropertiesCatalog propertiesCatalog = new PropertiesCatalog()
        def properties = scriptObj.readProperties file: "${configuration.propertiesFolderName}/${configuration.propertiesFileName}"
        if ( !configuration.isLibrary || !Boolean.parseBoolean(properties['isLibrary']))
        propertiesCatalog = buildPropertiesCatalog(propertiesCatalog)

        boolean propertiesFileContentValid = PropertyFilesReader.read(
            scriptObj,
            configuration.propertiesFileName,
            propertiesCatalog,
            configuration.commonPropertiesFileName,
            pipelineParams,
            configuration.propertiesFolderName
        )
        if (!propertiesFileContentValid) {
            scriptObj.currentBuild.result = "FAILED"
            scriptObj.error("There are issues when reading files: you configured 'commonPropertyFileName' or 'propertiesFileName'. ")
        }
        scriptObj.propertiesValid = propertiesFileContentValid

        if (readPipelineParams('DEBUG') != null) {
            scriptObj.env.DEBUG = readPipelineParams('DEBUG')
        }
        if (!readPipelineParams('targetEnvironment')){
            scriptObj.env.targetEnvironment = scriptObj.JOB_URL.tokenize('/')[6]
        } else {
            scriptObj.env.targetEnvironment = readPipelineParams('targetEnvironment')
        }
        // set the default manifestYaml & serviceJson for provisioningCLI
        if (!readPipelineParams('userProvisioningCli')) {
            pipelineParams.setProperty('useProvisioningCli', 'false')
        }
        if (!readPipelineParams('manifestFileName') && scriptObj.env.targetEnvironment) {
            pipelineParams.manifestFileName = 'golden_pipeline/provisioningCLI/apps/manifest-' + scriptObj.env.targetEnvironment + '.yml'
            
        } else if ( readPipelineParams('manifestFileName')) {
            logger.info("you defined manifest file path: " + readPipelineParams('manifestFileName'))
        }
        if (!readPipelineParams('serviceJson') && scriptObj.env.targetEnvironment) {
            pipelineParams.serviceJson = 'golden_pipeline/provisioningCLI/services/' + scriptObj.env.targetEnvironment + '/assemble-servies.json'
        } else if ( readPipelineParams('serviceJson')) {
            logger.info("you defined services file path: " + readPipelineParams('serviceJson'))
        }
        if (!readPipelineParams('autoScaleYaml') && scriptObj.env.targetEnvironment) {
            pipelineParams.autoScaleYaml = 'golden_pipeline/provisioningCLI/autoscales/autoScale-' + scriptObj.env.targetEnvironment + '.yml'
        } else if ( readPipelineParams('autoScaleYaml')) {
            logger.info("you defined autoScale file path: " + readPipelineParams('autoScaleYaml'))
        }
        if (!readPipelineParams('proxyUpsertYaml') && scriptObj.env.targetEnvironment) {
            pipelineParams.proxyUpsertYaml = 'golden_pipeline/provisioningCLI/upserts/proxyUpsert-' + scriptObj.env.targetEnvironment + '.yml'
        } else if ( readPipelineParams('proxyUpsertYaml')) {
            logger.info("you defined proxyUpsert file path: " + readPipelineParams('proxyUpsertYaml'))
        }
        if (!readPipelineParams('smokeTestFileName') && scriptObj.env.targetEnvironment) {
            pipelineParams.smokeTestFileName = 'golden_pipeline/provisioningCLI/apps/smokeTestScript-' + scriptObj.env.targetEnvironment + '.yml'

        } else if ( readPipelineParams('smokeTestFileName')) {
            logger.info("you defined smokeTestScript file path: " + readPipelineParams('smokeTestFileName'))
        }
        scriptObj.echo "Configuration from ${scriptObj.class.name}:"
        scriptObj.echo configuration.toString()
        scriptObj.echo "Properties from file:"
        scriptObj.echo pipelineParams.toString()
        return pipelineParams
    }
    public readPipelineParams(String name) {
        if (scriptObj.configuration.get(name) != null) {
            logger.info("please move the properties ${name} : ${scriptObj.configuration.get(name)} to ci.properties.")
            return scriptObj.configuration.get(name)
        } else {
            return getBooleanFromPipelineParams(scriptObj.pipeline)
        }
    }
    protected def getBooleanFromPipelineParams(Properties pipelineParams, String name) {
        if (pipelineParams.getProperty(name) == null){
            return null
        } else if (pipelineParams.getProperty(name).toLowerCase().trim() == 'true') {
            return true
        } else if (pipelineParams.getProperty(name).toLowerCase().trim() == 'false') {
            return false
        } else {
            return pipelineParams.getProperty(name)
        }
    }
    public PropertiesCatalog buildPropertiesCatalog(PropertiesCatalog propertiesCatalog) {
        propertiesCatalog.addOptionalProperty("pcfCredential", "Missing the name of a PCF Credential entry defining the credentials that should be used to connect to Concourse via UAA", null)
        propertiesCatalog.addOptionalProperty("pcfUrl", "Missing PCF URL", null)
        propertiesCatalog.addOptionalProperty("pcfSpace", "Missing PCF space", null)
        propertiesCatalog.addOptionalProperty("concourseUrl", "Missing concourse URL", null)
        propertiesCatalog.addOptionalProperty("concourseTeamUrl", "Missing consourse Team URL", null)
        propertiesCatalog.addOptionalProperty("concoursePipelineConfiguration", "Missing concourse Pipeline configuration", null)
        propertiesCatalog.addOptionalProperty("concoursePipelineName", "Missing concourse Pipeline name", null)
        propertiesCatalog.addOptionalProperty("concourseJobName", "Missing concourse Job Name", null)
        propertiesCatalog.addOptionalProperty("concoursePipelineVariables", "Missing concourse Pipeline Variables", null)
        NotificationsPropertiesCatalogBuilder.build(propertiesCatalog)
        return propertiesCatalog
        
    }
}