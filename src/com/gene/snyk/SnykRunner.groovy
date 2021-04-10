package com.gene.snyk

import com.gene.logger.*
import com.gene.parameters.ParametersReader

class SnykRunner implements Serialiable {
    protected Script scriptObj
    protected Logger logger
    protected ParametersReader paramsReader 

    SnykRunner(Script scriptObj){
        this.scriptObj = scriptObj
        this.logger = new Logger(scriptObj, Level.INFO)
        this.paramsReader = new ParametersReader(scriptObj)
    }

    protected String assembleCommand() {
        def assembleCommand = 'snyk test'
        if (paramsReader.readPipelineParams('snykDetectionDepth')) {
            assembleCommand += " --detection-depth=\"${paramsReader.readPipelineParams('snykDetectionDepth')}\""
        }
        if (paramsReader.readPipelineParams('snykExcludeDir')) {
            assembleCommand += " --exclude=\"${paramsReader.readPipelineParams('snykExcludeDir')}\""
        }
        if (paramsReader.readPipelineParams('snykOrg')) {
            assembleCommand += " --org=\"${paramsReader.readPipelineParams('snykOrg')}\""
        }
        if (paramsReader.readPipelineParams('snykFile')) {
            assembleCommand += " --file=\"${paramsReader.readPipelineParams('snykFile')}\""
        }
        if (paramsReader.readPipelineParams('snykDockerUrl')) {
            assembleCommand += " --docker \"${paramsReader.readPipelineParams('snykDockerUrl')}\""
        }
        if (paramsReader.readPipelineParams('snykSeverityThreshold')) {
            assembleCommand += " --severity-threshold=\"${paramsReader.readPipelineParams('snykSeverityThresold')}\""
        }
        // logger.info(assembleCommand)
        return assembleCommand
    }
    protected SnykResult run() {
        def snykCommand = this.assembleCommand()
        def snykResult = new  SnykResult()
        snykResult.message = ''
        // test
        logger.info(snykCommand)
        try {
            def testResponse
            if (scriptObj.fileExists('mvnw')) {
                testResponse = scriptObj.sh (
                    returnStdout: true,
                    script: "rm -rf mvn*"
                )
                logger.info(testResponse)
            }
            testResponse = scriptObj.sh (
                returnStdout: true,
                script: snykCommand
            )
            logger.info(testResponse)
            snykResult.message = "Snyk CLI PASSED the snyk test.\n"
            snykResult.governanceGatePassed = true
        } catch (Exception error) {
            snykResult.message = "Snyk CLI FAILED the snyk test, error stack: ${error}.\n"
            snykResult.governanceGatePassed = false
        }
        // monitor & upload to snyk.io
        if ( snykCommand.contains('test')) {
            snykCommand = snykCommand.replace('test', 'monitor')
            logger.info(snykCommand)
            try {
                def monitorResponse = scriptObj.sh (
                    returnStdout: true,
                    script: snykCommand
                )
                logger.info(monitorResponse)
                def jsonBody = scriptObj.readJSON monitorResponse.toString()
                snykResult.message += "please check snyk result ${jsonBody.uri}.\n"
            } catch (Exception error) {
                // throw new Exception(error)
                snykResult.message += "Snyk CLI FAILED to upload the snyk test to snyk.io. error stack: ${error}.\n"
            }
        }
       return snykResult
    }
}