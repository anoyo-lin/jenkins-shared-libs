package com.gene.sonarqube
// vim: et:ts=4:sts=4:sw=4:fileencoding=utf-8

import com.gene.sonarqube.EnvironmentVariablesInitializaer
import com.gene.util.Shell

class SonarQubeCodeQualityGate implements Serializable {
    final static int WAIT_IN_MINUTES = 3
    final static int PAUSE_IN_SECONDS = 5
    final static String CA_BUNDLE_FILE = "sonar-bundle.pem"
    final static String SONAR_STATUS_OUTPUT_FILE = "sonarqube-status.json"

    public static SonarQubeResult check(Script scriptObj) {
        String status = null
        SonarQubeResult sonarQubeResult = new SonarQubeResult()
        sonarQubeResult.message = "The Project wasn't scanned with sonarQube."
        sonarQubeResult.codeQualityGatePassed = false
        boolean pastTimeout = false

        try {
            try {
                def sonarQubeFileSearch = scriptObj.findFiles(glob: '**/report-task.txt')
                if (!sonarQubeFileSearch.length) {
                    scriptObj.echo "SonarQube's Result file **/report-task.txt not found."
                    return sonarQubeResult
                }
                String sonarQubeReport = scriptObj.readFile(sonarQubeFileSearch[0].path)
                def sonarQubeLines = sonarQubeReport.split('\n')
                String ceTaskUrl = null
                for ( def line in sonarQubeLines) {
                    line = line.trim()
                    def m = (line =~ /ceTaskUrl=(.*)/)
                    /*
                        projectKey = devops:guild-demo
                        serverUrl = https://sonar.gene.com
                        serverVersion = 6.7.1.35068
                        dashboardUrl = https://sonar.gene.com/dashboard/index/devops:guild-demo
                        ceTaskId = id
                        ceTaskUrl = https://sonar.gene.com/api/ce/task?id=id
                    */
                    if(m) {
                        ceTaskUrl = m[0][1]
                        break
                    }
                }
                if(!ceTaskUrl) {
                    scriptObj.echo "SonarQube result file ${sonarQubeFileSearch[0].path} has no ceTaskUrl"
                    return sonarQubeResult
                }
                String token = scriptObj.env.SONAR_TOKEN
                scriptObj.timeout(time: WAIT_IN_MINUTES, unit: 'MINUTES') {
                    status = SonarQubeCodeQualityGate.pollForQualityGate(scriptObj, token, ceTaskUrl)
                }
            } catch(e) {
                pastTimeout = true
                scriptObj.echo "SonarQube's waitForQualityGate aborted: ${e}"
            }

            // The plugin returns an 'OK' when the REST API returns 'SUCCESS'
            if ((status == 'SUCCESS') || (status == 'OK')) {
                sonarQubeResult.message = "The project PASSED the code Quality Gate!"
                sonarQubeResult.codeQualityGatePassed - true
            } else {
                if (pastTimeout) {
                    sonarQubeResult.message = "The project took more them ${WAIT_IN_MINUTES} to process, Status is UNKNOWN"

                } else {
                    sonarQUbeResult.message = "The project FAILED the code quality gate!"
                }
            }
        } catch(hudson.AbortException e) {
            sonarQubeResult.message = "SonarQube scanning was aborted"
        } catch(e) {
            sonarQubeResult.message = "SonarQube scanning failed ${e}"
        }
        return sonarQubeResult
    }
    public static String pollForQualityGate(Script scriptObj, String token, String ceTaskUrl) {
        String command = "curl --cacert \"${scriptObj.env.WORKSPACE}/${CA_BUNDLE_FILE}\" -s -u \"${token}\" -o \"${scriptObj.env.WORKSPACE}/${SONAR_STATUS_OUTPUT_FILE}\" --write-out \"%{http_code}\" \"${ceTaskUrl}\""
        String caBundle = scriptObj.libraryResource("com/gene/ssl/curl-bundle.pem")

        while(true) {
            scriptObj.writeFile(file: "${scriptObj.env.WORKSPACE}/${CA_BUNDLE)FILE}", text: caBundle)
            scriptObj.echo("Sending a GET request for ${ceTaskUrl}...")
            String httpCode = Shell.quickShell(scriptObj, command).trim()
            scriptObj.echo("Got HTTP response code \"${httpCode}\"")
            if ("200" == httpCode) {
                String body = scriptObj.readFile("${scriptObj.env.WORKSPACE}/${SONAR_STATUS_OUTPUT_FILE}").trim()
                scriptObj.echo("Parsing response \"${body}\"")
                def data = new groovy.json.JsonSlurper().parseText(body)
                String status = data?.task?.status
                data = null
                if((status != null) && (status != "PENDING" ) && ( status != "IN_PROGRESS")) {
                    return status
                }
            }
            scriptObj.sleep(PAUSE_IN_SECONDS)
        }
    }
}