package com.gene.provisioning

import com.gene.logger.*
import com.gene.parameters.ParametersReader

class AssembleCommands implements Serializable {
    private serviceYaml
    private serviceJson
    private snowChangeYaml
    private autoScaleYaml
    private scriptObj
    private provisionObj
    private proxyUpsertYaml
    private logger
    private paramsReader 
    AssembleCommands(Script scriptObj, Provision provisionObj){
        this.scriptObj = scriptObj
        this.provisionObj = provisionObj
        this.logger = new Logger(scriptObj, Level.INFO)
        this.paramsReader = new ParametersReader(scriptObj)
        
    }
    private void readSnowChangeYaml() {
        def yaml = scriptObj.readFile file: "./${scriptObj.pipelineParams.snowChangeYaml}"
        this.snowChangeYaml = scriptObj.readYaml text: "${yaml}"

    }
    private void readServiceYaml() {
        def yaml = scriptObj.readFile file: "./${scriptObj.pipelineParams.serviceYaml}"
        this.serviceYaml = scriptObj.readYaml text: "${yaml}"
    }
    private void readServiceJson() {
        def json = scriptObj.readFile file: "./${scriptObj.pipelineParams.serviceJson}"
        this.serviceJson = scriptObj.readJSON text: "${json}"
    }
    private void readProxyUpsertYaml() {
        def yaml = scriptObj.readFile file: "./${scriptObj.pipelineParams.proxyUpsertYaml}"
        this.proxyUpsertYaml = scriptObj.readYaml text: "${yaml}"
    }
    private void readAutoScaleYaml() {
        def yaml = scriptObj.readFile file: "./${scriptObj.pipelineParams.autoScaleYaml}"
        this.autoScaleYaml = scriptObj.readYaml text: "${yaml}"
    }
    private String getAbosoluteParameterFilePath(String serviceJsonPath, String parameterFileRelativePath) {
        def basePath = scriptObj.sh(script: "dirname ${serviceJsonPath}",
        returnStdout: true
        ).trim()
        return scriptObj.sh(script: "cd ${basePath}; readlink -f ${parameterFileRelativePath}",
        returnStdout: true
        ).trim().replace("${scriptObj.env.WORKSPACE}/", "")
    }
    private String appendChangeTicket(){
        def changeTicket = ""
        def incidentTicket = ""
        def result = ""
        // boolean isProd = Utils.checkIfProd(provisionObj.foundation, provisionObj.org)
        changeTicket = scriptObj.env.CHANGE_TICKET_NO
        incidentTicket = scriptObj.env.INCIDENT_TICKET_NO
        scriptObj.echo "------<debug> changeTicket:${changeTicket}, incidentTicket:${incidentTicket}"
        if (changeTicket != "" && incidentTicket != null) {
            result = " --changeTicket '${changeTicket}'"
        } else if (incidentTicket != "" && incidentTicket != null) {
            result = " --incidentTicket '${incidentTicket}'"
        }
        return result
    }
    private String[] getServiceJsonCommands() {
        this.readServiceJson()
        String[] result = new String[serviceJson.size()]
        int index = 0
        for (def service in serviceJson) {
            String serviceCommand = ""
            String serviceParameters = ""
            def ignore_create = service.ignore_create
            if (ignore_create == null || Boolean.valueOf("${ignore_create}")) {
                logger.info("ignore to create or update service: ${service.service_name}")
                continue
            }
            if (service.service_config) {
                String serviceJsonPath = scriptObj.pipelineParams.serviceJson
                String parameterFilePath = this.getAbosoluteParameterFilePath(serviceJsonPath, service.service_config.replaceAll("\n", ""))
                serviceCommand = """./provisioning-cli service create \\
                --name \"${service.service_name}\" \\
                --serviceName \"${service.service_type}\" \\
                --planName \"${service.service_plan}\" \\
                --parametersFile \"${parameterFilePath}\""""
            } else {
                serviceCommand = """./provisioning-cli service create \\
                --name \"${service.service_name}\" \\
                --serviceName \"${service.service_name}\" \\
                --planName \"${service.service_plan}\""""
            }
            if (this.appendChangeTicket()) {
                serviceCommand += this.appendChangeTicket()
            }
            logger.info("${serviceCommand}")
            result[index] = serviceCommand
            index++
        }
        // logger.info(result.toString())
        return result
    }
    // obsolete
    
    private  String[] getServiceYamlCommands() {
        this.readServiceYaml
        //logger.info("${serviceYaml.services.size()}")
        //logger.info("${serviceYaml.services}")

        String[] result = new String[serviceYaml.services.size()]
        if (serviceYaml.services) {
            int index = 0
            for (def service in serviceYaml.services) {
                String serviceCommand = ""
                String serviceParameters = ""
                String serviceCreationArgument = ""
                //add a certain logic to fetch if a service is for service key
                if (service.serviceKey == true){
                    serviceCreationArgument = "create-service-key"
                } else {
                    serviceCreationArgument = "create"
                }
                // for parameters
                if (service.parameters) {
                    for (def parameter: service.parameters) {
                        // logger.info("${parameter}")
                        serviceParameters += "\"${parameter.key}\": \"${parameter.value}\", "
                    }
                    serviceParameters = serviceParameters.substring(0, serviceParameters.length() - 2)
                    serviceCommand ="""./provisioning-cli service ${serviceCreationArgument} \\
                    --name \"${service.name}\" \\
                    --serviceName \"${service.serviceName}\" \\
                    --planName \"${service.planName}\" \\
                    --parameters \'{${serviceParameters}}\'"""
                    // for parametersFile
                } else if (service.parametersFile) {
                    serviceCommand = """./provisioning-cli service ${serviceCreationArgument} \\
                    --name \"${service.name}\" \\
                    --serviceName \"${service.serviceName}\" \\
                    --planName \"${service.planName}\""""
                }
                // result = ArrayUtils.add(result, serviceCommand)
                if (this.appendChangeTicket()) {
                    serviceCommand += this.appendChangeTicket()
                }
                // add a certain logic here to print service key
                if (service.serviceKey == true) {
                    serviceCommand += " && ./provisioning-cli service get-service-key --name ${service.serviceName} --serviceKeyName ${service.name}"
                    // if (this.appendChangeTicket()) {
                        // serviceCommand += this.appendChangeTicket()
                    //}
                }
                logger.info("${serviceCommand}")
                result[index] = serviceCommand
                index++
            }
        } else {
            logger.info("can not parser the service.yml for your application")
            return result
        }
        // logger.info(result.toString())
        return result
    }
    private String getSnowChangeCommand(){
        try {
            this.appendChangeTicket()
        } catch (Eception err) {
            def snowCommand = null
            return snowCommand
        }
        def snowCommand = null
        if (snowChangeYaml.changeTicket) {
            snowCommand = "./provisioning-cli change create "
            for (def key in snowChangeYaml.changeTicket.keySet()) {
                if (snowChangeYaml.changeTicket."${key}" != null || snowChangeYaml.changeTicket."${key}" != '') {
                    def value = snowChangeYaml.changeTicket."${key}"
                    snowCommand += "--${key} \"${value}\" "
                }
            }
            // --dal \"${snowChangeYaml.changeTicket.dal}\" \\
            // --assignmentGroup \"${snowChangeYaml.changeTicket.assignmentGroup}\" \\
            // --ci \"${snowChangeYaml.changeTicket.ci}\" \\
            // --impact \"${snowChangeYaml.changeTicket.impact}\" \\
            // --assignTo \"${snowChangeYaml.changeTicket.assignTo}\" \\
            // --requestedBy \"${snowChangeYaml.changeTicket.requestedBy}\" \\
            // --shortDescription \"${snowChangeYaml.changeTicket.shortDescription}\" \\
            // --description \"${snowChangeYaml.changeTicket.description}\" \\
            // --implementationPlan \"${snowChangeYaml.changeTicket.implementationPlan}\" \\
            // --testPlan \"${snowChangeYaml.changeTicket.testPlan}\" \\
            // --backoutPlan \"${snowChangeYaml.changTicket.backoutPlan}\" \\
            // --outageDescription \"${snowChangeYaml.changeTicket.outageDescription}\" \\
            // --justification \"${snowChangeYaml.changeTicket.justification}\" \\
            // --startOffsetMinutes ${snowChangeYaml.changeTicket.startOffsetMinutes} \\
            // --endOffsetMinutes ${snowchangeYaml.changeTicket.endOffsetMinutes}"""

        } else {
            logger.info("No Snow Change Ticket description")
        }
        return snowCommand
    }
    private String getProxyUpsertCommand() {
        this.readProxyUpsertYaml()
        def proxyCommand = ""
        if (proxyUpsertYaml.proxy) {
            proxyCommand = "./provisioning-cli proxy upsert "
            for (def key in proxyUpsertYaml.proxy.keySet()) {
                if (proxyUpsertYaml.proxy."${key}" != null || proxyUpsertYaml.proxy."${key}" != '') {
                    def value = proxyUpsertYaml.proxy."${key}"
                    logger.info("{\"${key}\": \"${value}\"}")
                    proxyCommand += "--${key} \"${value}\" "
                } 
            }
            for (def key in proxyUpsertYaml.settings.keySet()) {
                if (proxyUpsertYaml.settings."${key}" != null || proxyUpsertYaml.settings."${key}" != '') {
                    def value = proxyUpsertYaml.settings."${key}"
                    if (value.getClass().toString().split()[1] == 'java.lang.Boolean') {
                        logger.info("${key}"="${value}")
                        proxyCommand += "--${key}=${value} "
                    } else {
                        logger.info("wrong option value of boolean one, ${value.getClass()}, please correct your proxyUpsert.yml")
                    }
                }
            }
            // --proxyTeamName \"${proxyUpsertYaml.proxy.proxyTeamName}\" \\
            // --backendName \"${proxyUpsertYaml.proxy.backendName}\" \\
            // --businessEntity \"${proxyUpsertYaml.proxy.businessEntity}\" \\
            // --subBusinessEntity \"${proxyUpsertYaml.proxy.subBusinessEntity}\" \\
            // --countryCode \"${proxyUpsertYaml.proxy.countryCode}\" \\
            // --env ${proxyUpsertYaml.proxy.env} \\
            // --version \"${proxyUpsertYaml.proxy.version}\" \\
            // --microgateway=${proxyUpsertYaml.proxy.microgateway} \\
            // --proxyExt=${proxyUpsertYaml.proxy.proxyExt} \\
            // --basePath \'${proxyUpsertYaml.proxy.basePath}\' \\
            // --target \'${proxyUpsertYaml.proxy.target}\' \\
            // --moveProductsApps=${proxyUpsertYaml.proxy.moveProductsApps} \\
            // --ignoreProdNamingConvention=${proxyUpsertYaml.proxy.ignoreProdNamingConvention} \\
            // --ignoreAppNamingConvention=${proxyUpsertYaml.proxy.ignpreAppNamingConvention}"""
        } else {
            logger.info('No Proxy Upsert Description')
        }
        if (this.appendChangeTicket()) {
            proxyCommand += this.appendChangeTicket()
        }
        return proxyCommand
    }
    private String getAutoScaleCommand(String appName = provisionObj.appName) {
        this.readAutoScaleYaml()
        def autoScaleCommand = ''
        if (autoScaleYaml.intance_limits) {
            autoScaleCommand = """./provisionging-cli app autoscale enable \\
            --appName ${appName} \\
            --param ${paramsReader.readPipelineParms('autoScaleYaml')}"""
        } else {
            throw new Exception('invalid autoScaleYaml')
        }
        if (this.appendChangeTicket()) {
            autoScaleCommand += this.appendChangeTicket()
        }
        return autoScaleCommand
    }
    private String getRollbackCommand(String appName = provisionObj.appName) {
        def rollbackCommand = "./provisioning-cli app rollback --appName \"${appName}\""
        if (provisionObj.routes) {
            rollbackCommand += " --mainfestFile \"${provisionObj.manigestFileName}\""
        }
        return this.appendChangeTicket()?rollbackCommand+=this.appendChangeTicket():rollbackCommand
    }
    private String getRestartCommand(String appName = provisionObj.appName) {
        def restartCommand = "./provisioning-cli app restart --appName \"${appName}\""
        return  this.appendChangeTicket()?re(startCommand+=this.appendChangeTicket():restartCommand
    }
    private String getStartCommand(String appName = provisionObj.appName) {
        def startCommand = "./provisioning-cli app start --appName \"${appName}\""
        return this.appendChangeTicket()?restartCommand+=this.appendChangeTicket():startCommand
    }
    private String getStopCommand(String appName = provisionObj.appName) {
        def stopCommand = "./provisioning-cli app stop --appName \"${appName}\""
        return this.appendChangeTicket()?stopCommand+=this.appendChangeTicket():stopCommand
    }
    private String getDeleteCommand(String appName = provisionObj.appName) {
        def deleteCommand = "./provisioning-cli app delete --appName \"${appName}\""
        return this.appendChangeTicket()?deleteCommand+=this.appendChangeTicket():deleteCommand
    }
    private String getServiceDeleteCommand(String serviceName){
        def deleteCommand = "./provisioning-cli service delete --name \"${serviceName}\""
        return this.appendChangeTicket()?deleteCommand+=this.appendChangeTicket():deleteCommand
    }
    private String mapCommand(String appName, Sting routes) {
        def mapCommand = "./provisioning-cli app map-route --appName \"${appName}\" --routes \"${routes}\""
        return this.appendChangeTicket()?mapCommand+=this.appendChangeTicket():mapCommand
    }
    private String unmapCommand(String appName, String routes) {
        def unmapCommand = "./provisioning-cli unmap-route --appName \"${appName}\" --routes \"${routes}\""
        return this.appendChangeTicket()?unmapCommand+=this.appendChangeTicket():unmapCommand
    }
    private String deleteCommand(String appName) {
        def deleteCommand = "./provisioning-cli app delete --appName \"${appName}\""
        return this.appendChangeTicket()?deleteCommand+=this.appendChangeTicket():deleteCommand
    }
    private String renameCommand(String appName, String appNewName){
        def renameCommand = "./provisioning-cli app rename --appName \"${appName}\" --newAppName \"${appNewName}\""
        return this.appendChangeTicket()?renameCommand+=this.appendChangeTicket():renameCommand
    }
    private String stopCommand(String appName) {
        def stopCommand = "./provisioning-cli app stop --appName \"${appName}\""
        return this.appendChangeTicket()?stopCommand+=this.appendChangeTicket():stopCommand
    }
    private String pushOptionPattern(String parameterName, String pushCommand) {
        if(provisionObj."${parameterName}" && provisionObj."${parameterName}" != '') {
            def temp = provisionObj."${parameterName}"
            pushCommand += " --${parameterName} \'${temp}\'"
        }
        return pushCommand
    }
    private String pushCommand(String appName = provisionObj.appName, String routes = provisionObj.routes) {
        // Extract information from manifest file 
        // BuildManifest buildManifest = new BuildManifest(scriptObj)
        // buildManifest.readManifestFile()

        // appName?:appName = buildManifest.getAppName()
        // routes?:routes - buildManifest.getRoutes()

        // def buildpack = buildManifest.getBuildPack()
        // def environmentVariables = buildManifest.getEnvironmentVariables()
        // def sourcePath = buildeManifest.getSourcePath()
        // def stack = buildManifest.getStack()
        // def services = buildManifest.getServices()
        // def framework = 'java'

        def pushCommand = ''
        if ( scriptObj.pipelineParams.smokeTestFileName && scriptObj.fileExists("${scriptObj.pipelineParams.unitTestFileName}")) {
            pushCommand = """
            ./provisioning-cli app push-with-smoke-test \\
            --smokeTestScript \"${scriptObj.pipelineParams.smokeTestFileName}\" \\
            """
        } else {
            pushCommand = """
            ./provisioning-cli app push \\
            """
        }
        pushCommand += """--appName \"${appName}\" \\
        --manifestFile \"${provisionObj.manifestFileName}\" \\
        --stack \"${provisionObj.stack}\" \\
        --buildPacks \"${provisionObj.buildpack}\" \\
        --framework \"${provisionObj.framework}\" \\
        --sourcePath \"${provisionObj.sourcePath}\""""
        pushCommand = pushOptionPattern('routes', pushCommand)
        pushCommand = pushOptionPattern('environmentVariables', pushCommand)
        pushCommand = pushOptionPattern('services', pushCommand)
        pushCommand = pushOptionPattern('deleteOldApp', pushCommand)
        pushCommand = pushOptionPattern('createDeploymentMarker', pushCommand)
        pushCommand = pushOptionPattern('healthCheckType', pushCommand)
        pushCommand = pushOptionPattern('healthCheckHttpEndpoint', pushCommand)
        pushCommand = pushOptionPattern('healthCheckTimeout', pushCommand)
        pushCommand = pushOptionPattern('noStart', pushCommand)
        if (this.appendChangeTicket()){
            pushCommand += this.appendChangeTicket()
        }
        if ( paramsReader.readPipelineParms('provisioningCliDebugging') == true) {
            pushCommand = "DEBUG-true " + pushCommand
        }
        logger.info("${pushCommand}")
        return pushCommand
    }
    
}
