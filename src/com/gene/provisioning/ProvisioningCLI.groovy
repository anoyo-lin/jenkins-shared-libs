package com.gene.provisioning

// import java.util.regex.Matcher
// import java.util.regex.Pattern
import com.gene.logger.Logger
import com.gene.logger.Level
import com.gene.parameters.ParametersReader

/**
* this class is responsible for setting up and calling the provisioning CLI for PCF actions
* At the moment this class only supports the blue/green deployment with smoke tests action
*/

class ProvisioningCLI implements Serializable {

    final static String TOOL_VERSION = 'latest'
    protected scriptObj
    protected provisionObj
    protected logger
    protected paramsReader

    ProvisioningCLI(Script scriptObj) {
        this.scriptObj = scriptObj
        this.provisionObj = new Provision()
        this.logger = new Logger(scriptObj, Level.INFO)
        this.paramsReader = new ParametersReader(scriptObj)
    }

    protected String getOperationSystem(Script scriptObj) {
        //Get current operation system
        logger.info("[INFO]: Node_name :" + scriptObj.env.NODE_NAME)
        def operationSystem
        if (scriptObj.isUnix()) {
            if (scriptObj.env.NODE_NAME.toLowerCase().contains('linux') || scriptObj.env.NODE_NAME.toLowerCase().contains('master')) {
                operationSystem = "linux"
            } else {
                operationSystem = "mac"
            }
        } else {
            operationSystem = "windows"
        }
        return operationSystem
    }
    protected void configureProvisioningCLI(Script scriptObj){
        scriptObj.withCredentials([scriptObj.string(credentialsId: scriptObj.pipelineParams.prvoTeamTokenCredId, variable: 'SPACE_TOKEN')]) {
            def operationSystem = getOperationSystem(scriptObj)

            scriptObj.sh "wget -c https://www.artifactory.com/artifactory/bin-local/provisioning-cli-v${TOOL_VERSION}.${operationSystem}.tar.bz2 -O - | tar -xvz"

            scriptObj.sh "chmod +x ./provisioning-cli"
            scriptObj.sh "./provisioning-cli config set-api ${scriptObj.pipelineParams.provisioningAPI}"
            scriptObj.sh "./provisioning-cli config set-token ${SPACE_TOKEN}"
            scriptObj.sh "./provisioning-cli config set-org ${scriptObj.pipelineParams.deployTargetOrg}"
            scriptObj.sh "./provisioning-cli config set-foundation ${scriptObj.pipelineParams.deployTargetFoundation}"
            scriptObj.sh "./provisioning-cli config set-space ${scriptObj.pipelineParams.deployTargetSpace}"

        }
    }
    protected void initInteractiveApproval() {
        def incidentNumber = scriptObj.env.INCIDENT_TICKET_NO
        def crNumber = scriptObj.env.CHANGE_TICKET_NO
        def assembleCommands = new assembleCommands(scriptObj, provisionObj)
        def snowChangeCommand = assembleCommands.getSnowChangeCommand()
        // logger.info("${incidentNumber}, ${crNumber}, ${Utils.checkIfProd(provisionObj.foundation, provisionObj.org)}, ${snowChangeCommand}")
        if ( Utils.checkIfProd(provisionObj.foundation, provisionObj.org) && ((incidentNumber == null || incidentNumber == '') && (crNumber == null || crNumber = ''))) {
            // initInteractiveApproval
            if (snowChangeCommand == null) {
                if (!scriptObj.fileExists("initedProvisioning")) {
                    def initCommand = "./provisioning-cli config initInteractiveApproval > initedProvisioning"
                    logger.info(runShell(initCommand))
                } else {
                    logger.info("already initiated interactive approval")
                }
            }
        }
    }
    protected String runShell(String requestBody) {
        if (!requestBody || requestBody == "null" || requestBody == null ) {
            return ""
        }
        logger.info("=========== runShell request: ${requestBody}")
        def responseCode = scriptObj.sh returnStatus: true, script: "set -o pipefail; ${requestBody} | tee tmp.txt"
        def responseBody = scriptObj.readFile(file: "tmp.txt")
        def responseBodyText = "${responseBody}".replaceAll("\\n", "")
        logger.info("============= runShell Special: requestBody.contains(\"CF-ServicePlanNotUpdateable\"): ${responseBodyText.contains("ServicePlanNotUpdateable")}")
        if (responseBodyText.contains("CF-ServicePlanNotUpdateable") || responseCode == 0) {
            // -- ignore error
            logger.info("============= it will ignore CF-ServicePlanNotUpdateable error ===========")
            return "${responseBody}"
        } else {
            throw new Exception("${responseBody}")
        }
    }
    protected void provisioningCLIPreOperations() {
        logger.info('empty provisioningCLIPreOperations for custom Logic')
    }
    protected void provisioningCLIPostOperations() {
        logger.info('empty provisioningCLIPostOperations for custom Logic')
    }
    protected void pushApp() {
        // BuildManifest buildManifest = new BuildManifest(scriptObj)
        // buildManifest.readManifestFile()
        // def appName = buildManifest.getAppName()
        // def route = buildManifest.getRoutes()
        def assembleCommands = new AssembleCommands(scriptObj, provisionObj)

        try {
            // pushCommand(scriptObj, requestObj, appName, routes)
            runShell(assembleCommands.pushCommand())
        } catch (Exception err) {
            // scriptObj.echo "pushApp throw an exception ${err}"
            if ( scriptObj.pipelineParams.smokeTestFileName && scriptObj.fileExists("${scriptObj.pipelineParams.smokeTestFileName}")) {
                logger.info("pushApp failed with ${error}, the pipeline will execute housekeeping Operations, please wait it ")

                // def rollBackCommand = "./provisioning-cli app delete --appName ${provisionObj.appNewName}"
                // def response - runShell("${rollBackCommand}")
                // logger.info("${response}")

                // delete new routes named "${app-Name}-new"
                // def newRoutes = buildManifest.genNewRoutes()
                // requestObj.action = 'deleteRoutes'
                // requestObj.applicationName = "${appName}-new"
                // requestObj.applicationNewRoutes = "[\"${newRoutes}\"]"
                // requestObj.id = ProvisioningRun.request(scriptObj, requestObj)
                // def statusStr = ProvisioningRun.status(scriptObj, requestObj.id)
                // if (statusStr == 'FAILED' || statusStr == "TIMEOUT") {
                    // throw new Exception("${statusStr}")
                // }
                scriptObj.sh "if [ -f provisioningAPI_smokeTestOutput.tmp ]; then cat provisioningAPI_smokeTestOutput.tmp; fi "
                scriptObj.sh "if [ -f curl_debugging.tmp ]; then cat curl_debugging.tmp; fi"
                throw new Exception("housekeeping finished after encountered failure.")
            } else {
                throw error
            }
        }
    }
    protected String getAppStatus(String appName) {
        def statsCommand = "./provisioning-cli app stats --appName \"${appName}\""
        def responseBody = scriptObj.sh(returnStatus: true,
        script: statsCommand).trim()
        def returnJson = scriptObj.readJSON text: responseBody.toString().replaceAll("\\[|\\]", "")
        return returnJson.state
    }


    static void blueGreenDeployment(Script scriptObj, Request requestObj) {
        // BuildManifest buildManifest = new BuildManifest(scriptObj)
        // buildManifest.readManifestFile()
        // def appName = buildManifest.getAppName()
        // def appNewName = buildManifest.genAppNewName()
        // def appOldName = buildManifest.genAppOldName()
        // def routes = buildManifest.getRoutes()
        // def newRoutes = buildManifest.genNewRoutes()
        def assembleCommands = new AssembleCommands(scriptObj, provisionObj)

        def deleteOldApp = false
        // push new app & new route
        logger.info(assembleCommands.pushCommand(provisionObj.appNewName, provisionObj.newRoutes))
        long timeoutMs = 5*60*1000
        long timeoutExpiredMs = System.currentTimeMillis() + timeoutMs

        while (getAppStatus(provisionObj.appNewName) != 'RUNNING') { 
            if (timeoutExpiredMs - System.currentTimeMillis() <= 0) {
                logger.info("timeout exceeds ${timeoutMs} milliseconds")
                break
            } else {
                long interval = 10*1000
                logger.info("sleep ${interval} milliseconds for apps warming up")
                sleep(10*1000)
            }
        }
        for (def route : provisionObj.newRoutes.split(",")) {
            logger.info(runShell(scriptObj, "if [[ \$(curl -s -o /dev/null -L -w '%{http_code}' ${route} != '200' ]]; then exit -1; fi"))
        }
        // map new app ----> route
        logger.info(assembleCommands.mapCommand(provisionObj.appNewName, provisionObj.routes))
        // unmap new app ---X--> new route
        logger.info(assembleCommands.unmapCommand(provisionObj.appNewName, provisionObj.newRoutes))

        def willHaveOldApp = false
        if (getAppStatus(provisionObj.appName) != 'APP_NOT_FOUND') {
            // unmap original ---X---> route
            logger.info(assembleCommands.unmapCommand(provisionObj.appName, provisionObj.routes))
            if (getAppStatus(provisionObj.appOldName) != 'APP_NOT_FOUND') {
                logger.info(assembleCommands.deleteCommand(provisionObj.appOldName))
            }
            logger.info(assembleCommands.renameCommand(provisionObj.appName, provisionObj.appOldName))
            willHaveOldApp = true
        }
        // if have old -> keep or delete
        logger.info(assembleCommands.renameCommand(provisionObj.appNewName, provisionObj.appName))
        if (willHaveOldApp) {
            if (deleteOldApp) {
                logger.info(assembleCommands.deleteCommand(provisionObj.appOldName))
            } else {
                logger.info(assembleCommands.stopCommand(provisonObj.appOldName))
            }
        }
    }

    protected void enableAutoScale(Script scriptObj, Request requestObj){
        if (scriptObj.pipelineParams.autoScaleYaml &&
         scriptObj.fileExists("${scriptObj.pipelineParams.autoScaleYaml}")
         ){
            // def autoScaleCommand = ''
            // BuildManifest buildManifest = new BuildManifest(scriptObj)
            // buildManifest.readManifestFile()
            // buildManifest.readAutoScaleYaml()
            // def appName = buildManifest.getAppName()
            // if (buildManifest.checkAutoScaleYaml()){
                // autoScaleCommand = """./provisioning-cli app autosacle enable \
                // --appName ${appName} \
                // --params ${scriptObj.pipelineParams.autoScaleYaml}"""
                // def result = runShell(scriptObj, autoScaleCommand)
                // scriptObj.logger.info("${result}")
            // } else {
                // throw new Exception("autoScaleYaml content is invalid")
            // }
            def assembleCommands = new AssembleCommands(scriptObj, provisionObj)
            runShell(assembleCommands.getAutoScaleCommand())

        } else {
            logger.info("no autoScale.yml fileExists or Properties defined in ci.properties")
        }
    }

    protected void createServices() {
        if (
            scriptObj.pipelineParams.serviceJSON &&
            scriptObj.fileExists("${scriptObj.pipelineParams.serviceJSON}")
        ) {
            // BuildManifest buildManifest = new BuildManifest(scriptObj)
            // buildManifest.readServiceYaml()
            // String[] commandList = buildManifest.getServiceCommandList()
            // String[] commandListJSON = buildManifest.getServiceCommandListFromJSON()
            // scriptObj.echo "${commandListJSON}"
            def assembleCommands = new AssembleCommands(scriptObj, provisionObj)
            String[] commandListJSON = assembleCommands.getServiceJsonCommands()
            if (commandListJSON.size() > 0 ) {
                for ( def command : commandListJSON) {
                    // def response = runShell(scriptObj,  "${command}")
                    // logger.info("${response}")
                    if (command|| command!= "") {
                        runShell("${command}")
                    }
                }
            } else {
                logger.info("empty or invalid serviceJson content")
            }
        } else if (
            scriptObj.pipelineParams.serviceYaml &&
            scriptObj.fileExists("${scriptObj.pipelineParams.serviceYaml}")
        ) {
            def assembleCommands = new AssembleCommands(scriptObj, provisionObj)
            String[] commandListYaml = assembleCommands.getServiceYamlCommands()
            if (commandListYaml.size() > 0 ) {
                for (def command : commandListYaml) {
                    runShell("${command}")
                }
            } else {
                logger.info("empty or invalid serviceYaml content")
            }
        } else {
            logger.info("cannot find the serviceYaml/serviceJson path in scriptObj.pipelineParams")
        }
    }
    protected void restartAppOnPcf() {
        def assembleCommands = new AssembleCommands(scriptObj, provisionObj)
        def restartCommand = assembleCommands.getRestartCommand()
        logger.info(runShell(restartCommand))
    }
    protected void restartAppOnPcf(String appName) {
        def assembleCommands = new AssembleCommands(scriptObj, provisionObj)
        def restartCommand = assembleCommands..getRestartCommand(appName)
        logger.info(runShell(restartCommand)) 
    }
    protected void rollBackAppOnPcf() {
        def assembleCommands = new AssembleCommands(scriptObj, provisionObj)
        def rollBackCommand = assembleCommands.getRollBackCommand()
        logger.info(runShell(rollBackCommand))
    }
    protected void upsertAppOnPcf() {
        def assembleCommands = new AssembleCommands(scriptObj, provisionObj)
        def upsertCommand = assembleCommands.getProxyUpsertCommand()
        logger.info(runShell(upsertCommand))
    }
    protected void startAppOnPcf() {
        def assembleCommands = new AssembleCommands(scriptObj, provisionObj)
        def startCommand = assembleCommands.getStartCommand()
        logger.info(runShell(startCommand))
    }
    protected void stopAppOnPcf() {
        def assembleCommands = new AssembleCommands(scriptObj, provisionObj)
        def stopCommand = assembleCommands.getStopCommand()
        logger.info(runShell(stopCommand))
    }
    protected void deleteAppOnPcf() {
        def assembleCommands = new AssembleCommands(scriptObj, provisonObj)
        def deleteCommand = assembleCommands.getDeleteCommand()
        logger.info(runShell(deleteCommand))
    }
    protected void deleteServiceOnPcf(String serviceName) {
        def assembleCommands = new AssembleCommands(scriptObj, provisonObj)
        def deleteServiceCommand = assembleCommands.getServiceDeleteCommand(serviceName)
        logger.info(runShell(deleteServiceCommand))
    }
    protected void otherBauOnPcf() {
        logger.info("================== Provisioning Other Bau Task =================")
        def fileExists = scriptObj.fileExists scriptObj.env.mangeScriptFile
        // -- TODO add opperation permisison control
        if (fileExists) {
            scriptObj.sh "git update-index --chmod +x ./${scriptObj.env.manageScriptFile}"
            scriptObj.sh "chmod +x ./${scriptObj.env.manageScriptFile}"
            scriptObj.sh "./${scriptObj.env.mangeScriptFile}"
        }
        logger.info("================== Provisioning Other Bau Task ==================")

    }
    protected void pushAppOnPcf() {
        // def assembleCommands = new AssembleCommands(scriptObj, provisionObj)
        if (
            scriptObj.pipelineParams.manifestFileName &&
            scriptObj.pipelineParams.smokeTestFileName &&
            scriptObj.pipelineParams.snowChangeYaml &&
            Utils.checkIfProd(provisionObj.foundation, provisionObj.org)
        ) {
            logger.info("create SNOW ticket automatically and blueGreen pushing to Production")
            // def snowChangeCommand = assembleCommands.getSnowChangeCommand()
            // logger.info("${snowChangeCommand}")
            // scriptObj.sh "${snowChangeCommand}"
            pushApp()
            // scriptObj.sh "./provisioning-cli change close"
        } else if (
            scriptObj.pipelineParams.manifestFileName &&
            scriptObj.pipelineParams.smokeTestFileName &&
            Utils.checkIfProd(provisionObj.foundation, provisionObj.org)
        ) {
            logger.info("blue Green deployment with smokeTestScript.sh on Prod")
            // scriptObj.sh "./provisioning-cli config initInteractiveApproval"
            pushApp()
            // scriptObj.sh "./provisioning-cli change close"
        } else if (
            scriptObj.pipelineParams.manifestFileName &&
            Utils.checkIfProd(provisionObj.foundation, provisionObj.org)
        ) {
            logger.info("direct deployment on PROD")
            // scriptObj.sh "./provisioning-cli config initInteractiveApproval"
            pushApp()
            // scriptObj.sh "./provisioning-cli change close"
        } else if (
            scriptObj.pipelineParams.manifestFileName &&
            scriptObj.pipelineParams.smokeTestFileName &&
            !Utils.checkIfProd(provisionObj.foundation, provisionObj.org)
        ) {
            logger.info("blue Green Deployment with smokeTestScript.sh on OTHER")
            pushApp()
        } else if (
            scriptObj.pipelineParams.manifestFileName &&
            !Utils.checkIfProd(provisionObj.foundation, provisionObj.org)
        ) {
            logger.info("direct push app without smoke test on non-Production environment")
            pushApp()
        } else {
            // logger.info("mandatory option in ci.properties [manifestFileName, smokeTestFileName, snowChangeYaml(optional for create ticket from CLI) ] in PROD, [ manifestFileName ] in OTHER environment ")
            throw new Exception("mandatory option in ci.properties [manifestFileName, smokeTestFileName, snowChangeYaml(optional for create ticket from CLI) ] in PROD, [ manifestFileName ] in OTHER environment ")

        }
    }
    static void packageApplication(Script scriptObj, Request requestObj) {
        if (scriptObj.pipelineParams.smokeTestFileName) {
            scriptObj.sh "chmod +x ${scriptObj.pipelineParams.smokeTestFileName}"
            logger.info("Found smoke test script, gave executing permission to script")
        }
        if (provisionObj.framework == 'node') {
            def packageName = scriptObj.sh(returnStdout: true, script: '''node -p "require('./package.json').name"''').trim()
            def packageVersion = scriptObj.sh(returnStdout: true, script: '''node -p "require('./package.json).version"''').trim()
            def artifactName = packageName + '-' + packageVersion + '.tar.gz'

            if(scriptObj.env.targetEnvironment == 'dev') {
                artifactName = packageName + '-' + packageVersion + '-SNAPSHOT.tar.gz'
            }
            scriptObj.sh """
            rm -rf package.zip
            rm -rf temp
            mkdir temp
            cp -ar dist/* temp/
            """

            scriptObj.sh "cat ${scriptObj.pipelineParams.manifestFileName}"
            if (scriptObj.fileExists('temp/.scannerwork')) {
                scriptObj.sh 'rm -r temp/.scannerwork'
            }
            scriptObj.sh "cp --parents ${scriptObj.pipelineParams.manifestFileName} temp"
            scriptObj.sh "cp -R .npmrc temp"
            scriptObj.sh "ls -la temp/"
        } else if (requestObj.language == 'html') {
            def packageName = scriptObj.sh(returnStdout: true, script: '''node -p "require('./package.json').name"''').trim()
            scriptObj.sh "tar -zxf ${packageName}.tgz"
            scriptObj.sh "rm ${packageName}.tgz"
            scriptObj.sh "cat package/${scriptObj.pipelineParams.manifestFileName}"
            if (scriptObj.fileExists('package/.scannerwork')) { 
                scriptObj.sh 'rm -r package/.scannerwork'
            }
            scriptObj.sh 'ls -la package/'
        } else if (requestObj.language == 'javaMaven' || requestObj.language == 'javaGradle' || requestObj.language == 'java') {
            def mavenArtifactId = scriptObj.readMavenPom().getArtifactId()
            def mavenVersion = scriptObj.readNavenPom().getVersion()
            scriptObj.sh "rm -fr package.zip"
            scriptObj.sh "rm -fr temp"
            scriptObj.sh "mkdir temp"

            scriptObj.sh "cat ${scriptObj.pipelineParams.manifestFileName}"
            // scriptObj.sh "cp --parents ${scriptObj.pipelineParams.manifestFileName}"

            scriptObj.sh "cp -R target/${mavenArtifactId}-${mavenVersion}.jar temp || true"
            scriptObj.sh "cp -R target/*.jar temp || true"
            // scriptObj.sh "cp -R ${scriptObj.pipelineParams.manifestFileName} package"

            scriptObj.sh 'ls -la temp/'

            // scriptObj.sh "rm -fr ${mavenArtifactId}.zip"
            // scriptObj.sh "zip -r ${mavenArtifactId}.zip package >/dev/null"
            // requestObj.fileName = "${mavenArtifactId}.zip"

        } else if (requestObj.language = "dotnetcore") {
            scriptObj.sh "cat ${scriptObj.pipelineParams.manifestFileName}"
            scriptObj.sh "unzip ${scriptObj.pulishName}.nupkg -d package"
            scriptObj.sh "cp -R ${scriptObj.pipelineParams.manifestFileName} package"
            scriptObj.sh "cp -R package/publish/* package"
            if (scriptObj.fileExists('package/.scannerwork')) {
                scriptObj.sh 'rm -r package/.scannerwork'
            }
            scriptObj.sh 'ls -al package/'
        }

    }
}