package com.gene.provisioning


import com.gene.provisioning.Utils
import com.gene.logger.logger
import com.gene.logger.Level

/*
* it will extract the information from manifest.yml and fullfill the request object
*/
class BuildManifest implements Serializable {
    private Script scriptObj
    private def manifestYaml
    public Request requestObj

    BuildManifest(Script scriptObj) {
        this.scriptObj = scriptObj
        this.logger = new Logger(scriptObj, Level.INFO)
        this.requestObj = new Request()
    }
    private void readManifestFile() {
        def yaml = scriptObj.readFile file: "./${scriptObj.pipelineParams.manifestFileName}"
        this.manifestYaml = scriptObj.readYaml text: "${yaml}"
    }

    private String getStack() {
        // manifestYaml.application.stack = [ stack: name1, stack: name2 ]
        def stack = ''
        if (manifestYaml.applications.stack[0]) {
            for (def stack : manifestYaml.applications.stack) {
                stack += stack.toString().replaceAll('\\[|\\]', '').replaceAll('stack: ', '')
            } 
        } else {
            logger.info("No Stack Found")
            throw new Exception("No Stack Found")
        }
        this.requestObj.stack = stack
        return stack
    }
    // -- enviromentVariables '{"key": "value", "key1": "value1" }'
    private String getEnvironmentVariables() {
        def envVariables = ''
        if (manifestYaml.applications.env[0]) {
            // --environmentVariables '{"key1": "value01", "key2": "value02", "key3": "value03", "key4": "value04"}'
            for (def env : manifestYaml.applications.env[0]) {
                logger.info("${env}")
                envVariables += "\"${env.key}\" : \"${env.value}\", "
            }
            envVariables = envVariables.substring(0, envVariables.length() - 2)
        } else {
            scriptObj.logger.info("No environment variables found")
        }
        this.requestObj.env = envVariables
        return envVariables
    }
    private String getRoutes() {
        def routes = ''
        if (manifestYaml.applications.routes[0]) {
            for ( def route: manifestYaml.applications.routes) {
                routes += route.toString().replaceAll('\\[|\\]', '').replaceAll('route:', "")
            }
        } else {
            logger.info("[ERROR]: No routes were Found in the manifest file, a route is mandatory if you are deploying to PCF")
        }
        return routes
    }
    private String genRoutes(String suffix) {
        def routes = ''
        if (manifestYaml.applications.routes[0]) {
            for (def route : manifestYaml.applications.routes) {
                // logger.info("${route}")
                def prefix = route.toString().replaceAll("\\[|\\]", "").replaceAll("route: ", "").split("\\.")[0]
                routes += route.toString().replaceAll("\\[|\\]", "").replaceAll("route: ", "").replaceAll("${prefix}", "${prefix}-${suffix}")
            }
        } else {
            logger.info("No Routes Found")
            throw new Exception("No Routes Found")
        }
        if (suffix == "new") {
            this.requestObj.newRoutes = routes
        } else if (suffix == 'old'){
            this.requestObj.oldRoutes = routes
        } else if (routes != '') {
            this.requestObj.routes = routes
        }
        return routes
    }
    private String getServices() {
        def services = ""
        if (manifestYaml.applications.services[0]) {
            for (def service : manifestYaml.applications.services) {
                services += service.toString().replaceAll('\\[|\\]', '').replaceAll('service:', '')
            }
        } else {
                scriptObj.logger.info('ERROR: No services were found in the manifest file, a service is optional if you are deploying to PCF')
        }
        return services
    }
    private String getSourcePath() {
        def sourcePath = ''
        if (manifestYaml.applications.sourcePath) {
            sourcePath = manifestYaml.applications.sourcePath.toString().replaceAll("\\[|\\]", "").replaceAll("sourcePath :", "")
        } else {
            logger.info("No sourcePath found")
            throw new Exception("no sourcePath found, please define it in manifestFileName")
        }
        this.requestObj.sourcePath = sourcePath
        return sourcePath
    }
    private String getBuildpack() {
        buildpacks = ''
        if (manifestYaml.applications.buildpacks[0]) {
            for (def buildpack : manifestYaml.applications.buildpacks) {
                buildpacks += buildpack.toString().replaceAll("\\[|\\]", "").replaceAll("buildpack: ", "")
            }
        } else if (manifestYaml.applications.buildpack) {
            buildpacks += manifestYaml.applications.buildpack.toString().replaceAll("\\[|\\]", "").replaceAll("buildpack: ", "")

        } else {
            scriptObj.logger.info("No buildpack found")
        }
        this.requestObj.buildpacks = buildpacks
        return buildpacks
    }
    private String getAppName(String suffix = null) {
        def appName = ''
        if (manifestYaml.applications.appName && suffix != null) {
            appName = manifestYaml.applications.appName.toString().replaceAll("\\[|\\]", "").replaceAll("appName :", "") + "-" + "${suffix}"
        } else if (manifestYaml.applications.appName) {
            appName = manifestYaml.applications.appName.toString().replaceAll("\\[|\\]", "").replaceAll("appName :", "")
        } else {
            logger.info("No appName Found")
            throw new Exception("No appName Found")
        }
        if (suffix == 'new') {
            this.requestObj.appNewName = appName
        } else if (suffix == 'old') {
            this.requestObj.appOldName = appName
        } else if (appName != ''){
            this.requestObj.appName = appName
        }
        return appName
    }
    private String genNewRoutes() {
        def newRoutes = ''
        def newRouteSuffix = getEndpoint(scriptObj.pipelineParams.deployTargetFoundation, scriptObj.pipelineParams.deployTargetOrg)
        def newRoutePrefix = manifestYaml.applications.name[0].toString().trim() + '-new'
        newRoutes = "${newRoutePrefix}.${newRouteSuffix}"
        return newRoutes
        // def routes = ''
        // if (manifestYaml.application.routes[0]) {
            // for (def route: manifestYaml.applications.routes) {
                // routes += route.toString().replaceALl('\\[|\\]', '').replaceAll('route:', '')

            // }
        // } else {
            // throw new Exception('ERROR: No routes were Found in the manifest file, a route is mandatory if you are deploying to PCF')
        // }
        // return routes
    }
    private String getSourceFrameworkFromManifest() {
        def framework = ''
        try {
            if ( this.getBuildpack().contains('java')) {
                framework = 'java'
            } else if ( this.getBuildpack().contains('node')) {
                framework = 'node'
            }
        } catch ( Exception err ) {
            throw new Exception(err)
        }
        this.requestObj.framework = framework
        return framework
    }
    // static String getEndpoint(String foundation, String org) {
        // if (foundation == "CHINA") {
            // return "apps.china.pcf.com"
        // } else if (foundation == "USA") {
            // if (checkIfProd(foundation, org)) {
                // return "apps.usa.pcf.com"
            // } else {
                // return "apps.usa.preview.pcf.com"
            // }
        // } else {
            // throw new Exception('ERROR: can not get pcf domain according the infromation you provided')
        // }
    // } 
    // static boolean checkIfProd(String foundation, String org) {
        // def environment = ''
        // def patternList = org.split('-')
        // def lastPattern = patternList[ patternList.length - 1 ].toString().toUpperCase()
        // if ( foundation = 'USA' || foundation = 'SANDBOX') {
            // environment = 'SANDBOX'
        // } else if (lastPattern == 'EXT') {
           // environment = patternList[ patternList.length - 2 ].toString().toUpperCase() 
        // }
        // if (environment == 'PROD' || environment == 'UAT') {
            // return true
        // } else {
            // return false
        // }
    // }

}
