package com.gene.provisioning

class Utils implements Serializable {
    static String getEndpoint(String foundation, String org) {
        if (foundation == 'EAST_ASIA') {
            return "apps.eas.pcf.gene.com"
        } else if (foundation == 'SOUTH_EAST_ASIA') {
            if (checkIfProd(foundation, org)) {
                return "apps.sea.pcf.gene.com"
            } else {
                return "apps.sea.preview.pcf.gene.com"
            }
        } else if (foundation == 'CANADA') {
            if (checkIfProd(foundation, org)) {
                return "apps.cac.pcf.gene.com"
            } else {
                return "apps.cac.preview.pcf.gene.com"
            }
        } else if (foundation == 'CAE') {
            return "apps.cae.pcf.gene.com"
        } else if (foundation == "USE" || foundation == "SANDBOX") {
            return "apps.use.sandbox.pcf.gene.com"
        } else {
            throw new Exception("ERROR: cannot get pcf endpoint according the information you provided")

        }
        
    }
    static boolean checkIfProd(String foundation, String org) {
        def environment = ''
        environment = this.getPcfEnv(foundation, org)
        return environment == 'PROD'
    }

    static String getPcfEnv(String foundation, String org){
        def environment = ''
        def patternList = org.split('-')
        def lastPattern = patternList[patternList.length - 1].toString().toUpperCase()
        if (foundation == 'USE' || foundation == 'SANDBOX') {
            environment = 'SANDBOX'
        } else if (lastPattern == 'EXT') {
            environment = patternList[patternList.length - 2].toString().toUpperCase()
        } else {
            environment = lastPattern
        }
        return environment
    }

    static String getSourceFramework(Script scriptObj) {
        def paramsReader = new com.gene.parameters.ParametersReader(scriptObj)
        def framework = ''
        if(paramsReader.readPipelineParams('sourceLanguage')) {
            framework = paramsReader.readPipelineParams('souceLanguage').toLowerCase()
        }
        if ((framework == null || framework == '') && paramsReader.readPipelineParams('sourceFramework')) {
            framework = paramsReader.readPipelineParams('sourceFramework')
        }
        if (framework == '' || framework == null) {
            try {
                def buildManifest = new com.gene.provisioning.BuildManifest(scriptObj)
                buildManifest.readManifestFile()
                framework = buildManifest.getSourceFrameworkFromManifest()
            } catch (Exception err) {
                scriptObj.echo "Exceptional Message: ${err}"
                if (scriptObj.env.framework && scriptObj.env.framework != '') {
                    framework = scriptObj.env.framework
                } else {
                    scriptObj.echo "couldn't judge the sourceFramework you defined for CI&CD pipeline"
                    return null 
                }
            }
        }
        framework = framework.contains("java") ? "java" : framework
        return framework
    }
}