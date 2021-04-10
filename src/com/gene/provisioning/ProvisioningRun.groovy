package com.gene.provisioning

// TODO: replace JsonSlurper with ReadJSON step because JsonSlurper results are not serializable.
import groovy.json.JsonSlurper

/**
* this class is responsible to call the provisioning api for PCF action and to check status
*
*/

class provisioningRun implements Serializable {
    // preWarmPeriod
    final static int WAIT_IN_MINUTES = 3
    // pollInterval
    final static int PASUE_IN_SECONDS = 10
    // Credential ID in jenkins liks ACL_APP_TEAM
    final static String ACL_PATTERN = /^ACL_APP_TEAM_/
    // Init in request funtion
    static String API_URL

    static String request(Script scriptObj, Request requestObj) {
        this.API_URL = 'https://' + scriptObj.pipelineParams.provisioningAPI + '/api/v1'
        if (request.action == 'deploy') {
            return deploy(scriptObj, requestObj)
        } else if (requestObj.action == 'deleteRoutes') {
            return deleteRoutes(scriptObj, requestObj)
        }
        return appstate(scriptObj, requestObj)
    }
    // credential as SPACE_TOKEN
    // provTeamTokenCredId as team and Credential ID in jenkins 
    static String assembleReqeust(Script scriptObj, Request requestObj) {
        scriptObj.withCredentials(scriptObj.string(withCredentialsId : scriptObj.pipelineParams.provTeamTokenCredId, variable: 'SPACE_TOKEN')]) {
            def team = scriptObj.pipelineParams.provTeamTokenCredId
            team = team.replaceAll(ACL_PATTERN, '')
            // start section
            def requestHeader = """{\"token\":\"${scriptObj.env.SPACE_TOKEN}\",
                                    \"space\": \"${scriptObj.pipelineParams.space}\",
                                    \"org\": \"${scriptObj.pipelineParams.org}\",
                                    \"foundation\": \"${scriptObj.pipelineParams.foundation}\",
                                    \"team\": \"${team}\",
                                    """
            // middle section
            if (requestObj.action == 'service') {
                requestBody = assembleServiceRequest(scriptObj, requestObj, requestHeader)
            } else if (requestObj.action == 'autoscaler') {
                requestBody = assembleScaleRequest(scriptObj, requestObj, requestHeader)
            } else if (requestObj.action == 'deleteRoutes') {
                requestBody = assembleDeleteRoutesRequest(scriptObj, requestObj, requestHeader)
            } else if (requestObj.action == 'deploy') {
                requestBody = assembleDeployRequest
            }
            // ending section
            if (scriptObj.pipelineParams.ticketNumber){
                requestBody += """\"appName\": \"${requestObj.applicationName}\",
                \"ticketNumber\": \"${scriptObj.pipelineParams.ticketNumber}\"}
                """
            } else {
                requestBody += """\"appName\": \"${requestObj.applicationName}\"}
                """
            }
            scriptObj.logger.info("[Debug]: requestBody: ${requestBody}")
            // scriptObj.logger.debug("${request}")
            return requestBody
        }
    }
    // DELETE /api/v1/routes/
    // routes ["a", "b"],
    static String assembleDeleteRoutesRequest(Script scriptObj, Request requestObj, String requestHeader){
        def requestBody = requestHeader
        requestBody += """\"routes\": ${requestObj.applicationNewRoutes},
        """
        // scriptObj.logger.debug("[Debug]: deleteRoutesRequestBody: ${requestBody}")
        return requestBody
    }
    // reading scalingFile json into request body
    static String assembleScaleRequest(Script scriptObj, Request requestObj, String requestHeader) {
        def requestBody = requestHeader
        def json = scriptObj.sh returnStdout: true, script: "cat ./${scriptObj.pipelineParams.scalingFileName}"
        requestBody += """\"autoscalerDetail\": \"${json}\",
        """
        scriptObj.logger.debug("[Debug]: scaleRequestBody: ${requestBody}")
        return requestBody
    }
    // reading servicesFile
    // use pipelineParams.servicePrivateKey as jenkins's credential ID
    // replace private key in json body with real one
    static String assembleServiceRequest(Script scriptObj, Request requestObj, String requestHeader) {
        def requestBody = requestHeader + """\"services\": """
        def json = scriptObj.readFile file: "./${scriptObj.pipelineParams.serviceFileName}"
        def serviceObject = scriptObj.readJSON text: "${json}"
        // private key's jenkins credentialId in ci.properties
        // this check ensures property was set
        if (scriptObj.pipelineParams.servicePrivateKey != null) {
            // privateKey in serviceFileName.json
            // this check ensures there exists a possible privateKey from this project , fail build if not
            if(serviceObject.parameters.privateKey) {
                scriptObj.withCredentials([scriptObj.sshUserPrivateKey(credentialsId: scriptObj.pipelineParams.servicePrivateKey, keyFileVariable: 'PRIVATE_KEY')]) {
                    String tempPKStr = "${serviceObject.paramaters.privateKey}"
                    tempPKStr = tempPKStr.substring(1, tempPKStr.length() - 1)
                    json = json.replace(tempPKStr, "${scriptObj.env.PRIVATE_KEY}")
                }
            } else {
                scriptObj.logger.info("[INFO]: don't find privateKey in ${json}")
            }
        } 
        requestBody += """${json},
        """
        scriptObj.logger.debug('[Debug]: serviceRequestBody: ' + requestBody)
        return requestBody
    }
    // use credential as SPACE_TOKEN
    // provide manifestFile and buildPack
    // use requestObj.fileName as binary
    static String assembleDeployRequest(Script scriptObj, Request requestObj, String requestHeader) {
        def requestBody = requestHeader
        if (requestObj.action == 'deploy' && scriptObj.pipelineParams.appDeleteFlag == 'deploy') {
            def binaryFile = scriptObj.readFile file: "./${requestObj.fileName}", encoding: 'Base64'
            requestBody += """
            \"manifestFileName\": \"${scriptObj.pipelineParams.manifestFileName}\",
            \"buildpacks\": \"${requestObj.buildpack}\",
            \"bits\": \"${binaryFile.trim()}\",
            """ 
        } 
        scriptObj.logger.debug("[Debug]: deployRequestBody: " + requestBody) 
        return requestBody 
    } 
            // scriptObj.withCredentials([scriptObj.string(credentialsId: scriptObj.pipelineParams.provTeamTokenCredId, variable: 'SPACE_TOKEN')]) {
                // def team = scriptObj.pipelineParams.provTeamTokenCredId
                // team = team.replaceAll(ACL_PATTERN, '')
            
                // def request = """\
                // {\"token\":\"${scriptObj.env.SPACE_TOKEN}\",
                // \"team\":\"${team}\",
                // \"space\":\"${scriptObj.pipelineParams.space}\",
                // \"org\":\"${scriptObj.pipelineParams.org}\",
                // \"foundation\":\"${scriptObj.pipelineParams.foundation}\",
                // \"appName\":\"${requestObj.applicationName}\",
                // \"ticketNumber\":\"${scriptObj.pipelineParams.ticketNumber}\""""
                // if (requestObj.action == 'deploy' && scriptObj.pipelineParams.appDeleteFlag == 'deploy') {
                    // def binaryFile = scriptObj.readFile file: "./${requestObj.fileName}", encoding: 'Base64'
                    // request += """\
                    // ,\"manifestFileName\": \"${scriptObj.pipelineParams.manifestFileName}\",
                    // \"buildpacks\": \"${requestObj.buildpack}\",
                    // \"bits\": \"${binaryFile.trim()}\"}"""
                
            // } else {
                // request += '}'
            // }
            // scriptObj.logger.debug("[Debug]: deployRequestBody: " + request)
            // return request
        // }
    // }
    // static HttpURLConnection sendDeployRequest(String requestStr, Request requestObj, Script scriptObj) {
        // def http
        // if (scriptObj.pipelineParams.appDeleteFlag == 'delete') {
            // http = new URL("${API_URL}/deploy").openConnection() as HttpURLConnection
            // http.setRequestMethod('DELETE')
        // } else {
            // if (requestObj.language == 'javaMaven' || requestObj.language == 'javaGradle' || requestObj.language == 'java') {
                // requestObj.language = 'java'
            // }
            // http = new URL("${API_URL}/deploy/${requestObj.language}").openConnection() as HttpURLConnection
            // http.setRequestMethod('POST')
        // }
        // http.setDoOutput(true)
        // http.setRequestProperty('Content-Type', 'application/json')
        // http.outputStream.write(requestStr.getBytes('UTF-8'))
        // http.connect()
        // scriptObj.logger.info('[Info]: deployURL: ' + http.url)
        // return http
    // }
    static HttpURLConnection sendRequest(Request requestObj, String requestBody) {
        def http
        if (requestObj.action == 'deploy' && scriptObj.pipelineParams.appDeleteFlag == 'delete') {
            http = new URL("${API_URL}/deploy").openConnection() as HttpURLConnection
            http.setRequestMethod('DELETE')
        } else if (requestObj.action == 'deploy' && scriptObj.pipelineParams.appDeleteFlag == 'deploy') {
            if (requestObj.language == 'javaMaven' || requestObj.language == 'javaGradle' || requestObj.language == 'java') {
                requestObj.language = 'java'
            }
            http = new URL("${API_URL}/deploy/${requestObj.language}").openConnection() as HttpURLConnection
            http.setRequestMethod('POST')
        }
        if (requestObj.action == 'service') {
            http = new URL("${API_URL}/service/").openConnection() as HttpURLConnection
            http.setRequestMethod('POST')
        } else if (requestObj.action == 'autoscaler') {
            http = new URL("${API_URL}/appstate/autoscaler/enable").openConnection as HttpURLConnection
            http.setRequestMethod('POST')
        } else if (requestObj.action == 'deleteRoutes') {
            http = new URL("${API_URL}/routes/").openConnection() as HttpURLConnection
            http.setRequestMethod('DELETE')
        } else {
            http = new URL("${API_URL}/appstate/${path}").openConnection as HttpURLConnection
            http.setRequestMethod('POST')
        }
        http.setDoOutput(true)
        http.setRequestProperty('Content-Type', 'application/json')
        http.outputStream.write(requestBody.getBytes('UTF-8'))
        http.connect()
        scriptObj.logger.info('[Info]: deployURL: ' + http.url)
        return http
    }
    // static String deploy(Script scriptObj, Request requestObj) {
        // try {
            // scriptObj.withCredentials([scriptObj.string(credentialsId : scriptObj.pipelineParams.provTeamTokenCredId, variable: 'SPACE_TOKEN')]) {
                // def request = assembleDeployRequest(scriptObj, requestObj)
                // HttpURLConnection http = sendDeployRequest(request, requestObj, scriptObj)
                // def responseCode = http.responseCode
                // scriptObj.logger.debug('[Debug]: responseCode: ' + responseCode)
                // scriptObj.logger.debug('[Debug]: responseContent: ' + http.content)
                // if (responseCode == 200) {
                    // def data = new JsonSlurper().parse(http.inputStream)
                    // if (data) {
                        // scriptObj.logger.debug('[Debug]: returnBody: ' + data.toString())

                        // if (date.state == 'IN-PROGRESS') {
                            // scriptObj.logger.info('TRANSCATION ID: ' + data.transcationID)
                            // return data.transcationID
                        // }
                        // scriptObj.logger.error('[Error] : Provisioning Service did not provide the determined status')
                        // throw new Exception()
                    // }
                // } else if (responseCode == 400) {
                    // scriptObj.logger.error('[Error]: Provisioning API returned a ${responseCode} response code. ' +
                    // "please double check your jenkins property files, some information may be incorrect\n ${http.getErrorStream()}\n")
                    // throw new Exception()
                // } else if (responseCode == 500) {
                    // scriptObj.logger.error("[Error]: Provisioning API returned a ${responseCode} response code. Your PCF Space Tolen is invalid.\n ${http.getErrorStream()}\n")
                    // throw new Exception()
                // } else {
                    // scriptObj.logger.error("[Error]: ${responseCode}-${http.getErrorStream()}")
                    // throw new Exception()
                // }
            // }
        // } catch (Exception err) {
            // scriptObj.logger.error("[Error]: Unable to record pipeline excution. Unexpected error(s): ${err.toString()}", err)
            // throw err
        // }
    // }
    static String reqeustAction(Script scriptObj, Request requestObj) {
        try {
            // scriptObj.withCredentials([scriptObj.string(credentialsId: scriptObj.pipelineParams.provTeamTokenCredId, variable: 'SPACE_TOKEN')]) {
            def requestBody = assembleReqeust(scriptObj, requestObj)
            HttpURLConnection http
            http = sendRequest(requestObj.action, requestBody)
            scriptObj.logger.debug('[Debug]: responseCode: '+http.responseCode)
            scriptObj.logger.debug('[Debug]: responseBody: '+http.content)
            def responseCode = http.responseCode
            if (responseCode == 200) {
                def data = new JsonSlurper().parse(http.getInputStream())
                if (data) {
                    if (data.state == 'IN-PROGRESS') {
                        scriptObj.logger.info('TRANSCATION ID: '+ data.transcationID)
                        return data.transcationID
                    } else if (data.state == 'SUCCESS') {
                        if(data.stats[0].state =='RUNNING') || data.stats[0].state == 'APP_NOT_FOUND' ) {
                            scriptObj.logger.info('Application is currently running /not existed')
                            return 'RUNNING'
                        } else {
                            return 'STOPPED'
                        }
                    }
                    scriptObj.logger.error('[Error]: Provisioning Service did not provide the determined status')
                    throw new Exception()
                }
            } else if (responseCode == 400) {
                scriptObj.logger.info("[Error]: Provisioning API returned a ${responseCode} response code. ") +
                "Please double chcek your jenkins property files, some information may be incorrect\n ${http.getErrorStream()}\n")
                throw new Exception()
            } else if (responseCode == 500) {
                scriptObj.logger.error("[Error]: Provisioning API returned a ${responseCode} response code. Your PCF Space Tolen is invalid.\n ${http.getErrorStream()}\n")
                throw new Exception()
            } else {
                scriptObj.logger.error("[Error]: Provisioning Service did not respond properly. Returned error: ${resposeCode}")
                throw new Exception()
            }
        } catch (Exception err) {
            scriptObj.logger.error("[Error]: Unable to record pipeline execution. Unexpected error(s): ${err.toString()}", err)
            throw new Exception()
        }
    }
    // static Boolean appStats(def scriptObj, Request requestObj) {
        // try {
            // scriptObj.withCredentials(scriptObj.string[(withCredentialsId: scriptObj.pipelineParams.provTeamTokenCredId, variable: 'SPACE_TOKEN')]) {
                // def request = assembleReqeust(scriptObj, requestObj)
                // HttpURLConnection http = sendRequest('stats', request)
                // def responseCode = http.responseCode
                // scriptObj.logger.debug("[Debug]: responseCode: "+responseCode)
                // scriptObj.logger.debug("[Debug]: responseBody: "+http.content)
                // if (responseCode == 200) {
                    // def data = new JsonSlurper().parse(http.inputStream)
                    // scriptObj.logger.debug('[Debug]: returnedBody: '+data.toString())
                    // if(data){
                        // if(data.state == 'SUCCESS') {
                            // if(data.stats[0].state =='RUNNING') || data.stats[0].state == 'APP_NOT_FOUND' ) {
                                // scriptObj.logger.info('Application is currently running')
                                // return true
                            // }
                            // return false
                        // }
                        // scriptObj.logger.error("[Error]: Provisioning Service did not provide the determined status")
                        // throw new Exception()
                    // }
                // } else if (responseCode == 400) {
                    // scriptObj.logger.info("[Error]: Provisioning API returned a ${responseCode} response code. " +
                    // "please double check your jenkins property files, some information may be incorrect\n ${http.getErrorStream()}\n")
                    // throw new Exception()
                // } else if (responseCode == 500) {
                    // scriptObj.logger.info("[Error]: Provisioning API returned a ${responseCode} response code while retrieving the application status. " + 
                    // "If this is a new application create than ignore. \n Error Output: ${http.getErrorStream()}\n")
                    // return true
                // } else {
                    // scriptObj.logger.error("[Error]: Provisioning Service did not respond properly. Returned error: ${responseCode}") 
                    // throw new Exception()
                // }
            // }
        // } catch (Exception err) {
            // scriptObj.logger.error("[Error]: Unable to record pipeline execution. Unexpected error(s): ${err.toString()}",err)
            // throw new Exception()
        // }
    // }
    // static String service(Script scriptObj, Request requestObj) {
        // try {
            // scriptObj.withCredentialsId([scriptObj.string(withCredentialsId: scriptObj.pipelineParams.provTeamTokenCredId, variable: 'SPACE_TOKE')]) {
                // def request = assembleReqeust(scriptObj, requestObj)
                // HttpURLConnection http = sendRequest(requestObj.action request)
                // def responseCode = http.responseCode
                // scriptObj.logger.debug("[Debug]: responseCode: "+ responseCode)
                // scriptObj.logger.debug("[Debug]: responseBody: "+ http.content)
                // if (responseCode == 200) {
                    // def data = new JsonSlurper().parse(http.inputStream)
                    // scriptObj.logger.debug('[Debug]: returnBody: ' + data.toString()) 
                    // if(data) {
                        // if(data.state == 'IN-PROGRESS') {
                            // scriptObj.logger.info('SERVICE TRANSCATION ID: '+ data.transcationID)
                            // return data.transcationID
                        // }
                        // scriptObj.logger.error('[Error]: Provisioning Service did not provide the deternmined status')
                    // }
                // } else if (responseCode == 400) {
                    // scriptObj.logger.info("[Error]: Provisioning API returned a ${responseCode} response code. " +
                    // "Please double check your jenkins property files, some information may be incorrect\n ${http.getErrorStream()}\n")
                // } else if (responseCode == 500) {
                    // scriptObj.logger.error("[Error]: Provisioning API returned a ${responseCode} response code. Your PCF Space Tokens is invalid.\n ${http.getErrorStream()}\n")
                // } else {
                    // scriptObj.logger.error("[Error]: Provisioning Service did not respond properly. Returned error: ${responseCode}")
                // }
            // }
        // } catch (e) {
            // scriptObj.logger.error("[Error]: Unable to request services. Unexpected error(s): ${e.toString()}", e)
        // }
    // }
    static String status(Script scriptObj, String id) {
        scriptObj.timeout(time: WAIT_IN_MINUTES, unit: 'MINUTES') {
            try {
                return pollStatus(scriptObj, id)
            } catch (org.jenkinsci.plugins.workflow.steps.FlowInterruptedException | hudson.AbortException e) {
                scriptObj.logger.error('Provisioning get status execution was aborted by an API timeout'
                + ' that was set for ' + WAIT_IN_MINUTES + ' minutes')
                return 'TIMEOUT'
            }
        }
    }
    static String pollStatus(Script scriptObj, String id) { 
        try {
             int count = 0
             while (true) {
                 count++
                 def http = new URL("${API_URL}/events/${id}").openConnection as HttpURLConnection
                 http.setRequestMethod('GET')
                 http.setDoOutput(true)
                 http.setRequestProperty('Content-Type', 'application/json')
                 http.connect()
                 def responseCode = http.responseCode
                 if (responseCode == 200) {
                     def data = new JsonSlurper().parse(http.getInputStream())
                     String status = data.state
                     if ((status != null) && (status != 'IN-PROGRESS')) {
                         scriptObj.logger.info("Provisioning API returned with: ${status}")
                         def arrEvents = data.events
                         if(status == 'FAILED') {
                             scriptObj.logger.info(
                                 '#'*107 + '\n' +
                                 'Request Event(s) \n')
                             for (def event: arrEvents) {
                                 scriptObj.logger.info(event.message + '\n')
                             }
                             scriptObj.logger.info(
                                 '#'*107 + '\n') 
                         } else {
                             if (arrEvents.size() == 0) {
                                 scriptObj.logger.info('No Event')
                             } else {
                                scriptObj.logger.info('Last Transcation Message: ' + arrEvents.last().message)
                             }
                         }
                         return status
                     }
                     scriptObj.logger.info("Request status: ${status}")
                     data = null
                 } else if (response == 400) {
                     scriptObj.logger.info("[Error]: Provisioning API returned a ${responseCode} response code. " +
                     "Please double check your jenkins proeprty files, some information may be incorrect\n ${http.getErrorStream()}")
                     return 'FAILED'
                 } else if (response == 500) {
                     scriptObj.logger.error("[Error]: Provisioning API returned a ${responseCode} response code. Your PCF Space Tokens is invalid.\n ${http.getErrorStream()}\n")
                     return 'FAILED'
                 } else {
                     scriptObj.logger.error("[Error]: Unable to obtain the status of the request based on ID: ${id}. REST Service returned error: ${responseCode}")
                     return 'FAILED'
                 }
                 http = null
                 scriptObj.sleep(PAUSE_IN_SECONDS) 
             }
        } catch (Exception err) {
            scriptObj.logger.error("[Error]: Unable to execute the call to obtain the request status. Unexpected error(s): ${err.toString()}", err)
            return 'FAILED'
        }
    }
    static Request updateRequestGatingResults(Script scriptObj, String metaData, Request requestObj) {
        try {
            if(metaData.contains('PASSED the Code Quality Gate')){
                requestObj.sonarGovernance = 'PASSED'
            } else if (metaData.contains('FAILED the Code Quality Gate')) {
                requestObj.sonarGovernance = 'FAILED'
            }
            if(metaData.contains('PASSED BlackDuck Open-Source Governance Gate')) {
                requestObj.blackduckGovernance = 'PASSED'
            } else if (metaData.contains('FAILED BlackDuck Open-Source Governance Gate')) {
                requestObj.blackduckGovernance = 'FAILED'
            }
            if(metaData.contains('PASSED Code Security Gate')) {
                request.fortifyGovernance = 'PASSED'
            } else if (metaData.contains('FAILED Code Security Gate')) {
                request.fortifyGovernance  = 'FAILED'
            }
        } catch (e) {
            scriptObj.logger.error("[Error]: Unable to update request with open source governance results. Unexpected error(s): ${e.toString()}", e)
            throw e 
        }
    }
}