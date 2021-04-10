package com.gene.dashboard

import groovy.json.JsonOutput
import com.gene.dashboard.Dashboard
import hudson.tasks.test.AbstractTestResultAction
import com.gene.util.Strings


public class DashboardUtil {
    final static String DASHBOARD_ENDPOINT = 'gene.dashboard.com'

    public static void setDashBoard(Script scriptObj, Dashboard dashboard) {
        String jsonResult = JsonOutput.toJson(dashboard)
        scriptObj.echo "${jsonResult}"
        def postResponse = scriptObj.sh("curl --header 'Content-Type: application/json' --request POST -d '$jsonResult' http://${DASHBOARD_ENDPOINT}/api/builds")
        scriptObj.echo "postResponse '${postResponse}'"
    }
    public static String getCodeScanResultInfo(Script scriptObj) {
        def postResponse = scriptObj.sh(script: "curl --header 'Content-Type: application/json' --request GET 'http://${DASHBOARD_ENDPOINT}/api/codescan_checking/${scriptObj.env.codeScanResultQuery}'", returnStdout: true).toString().trim()
        scriptObj.echo "passing the code Scanning: ${postResponse}"
        return postResponse
    }
    public static Dashboard addDashboardInfo(Script scriptObj) {
        scriptObj.echo "[ Add Dashboard Infd ]"
        def type = "Jenkins"
        def job_name_tokenized = scriptObj.env.JOB_NAME.tokenize('/')

        def build_env = job_name_tokenized[1]
        def service = job_name_tokenized[3]
        def project = job_name_tokenized[2]

        def createDate = new Date().format("dd-MMMM-yyyy HH:mm", TimeZone.getTimeZone('UTC')) as String
        def lastUpdateDate = new Date().format("dd-MMMM-yyyy HH:mm", TimeZone.getTimeZone('UTC')) as String
        int __v = 0
        boolean codeScanPassed = false
        def dashboard = new Dashboard(type, build_env, service, project, createDate, lastUpdateDate, __v, codeScanPassed)
        return dashboard

    }
    public static void addJenkinsInfo(Script scriptObj, Dashboard dashboard) {
        scriptObj.echo "[ Add Jenkins Info ]"
        // Jenkins(String status, String jobUrl, String buildUrl, int build)
        int jenkins_build = scriptObj.env.BUILD_NUMBER as int 
        def jenkins_status = scriptObj.env.currentBuild.result 
        def jenkins_jobUrl = scriptObj.env.JOB_URL
        def jenkins_buildUrl = jenkins_jobUrl + jenkins_build + "/"
        dashboard.addJenkins(jenkins_status, jenkins_jobUrl, jenkins_buildUrl, jenkins_build)
    }
    public static void addGitInfo(Script scriptObj, Dashboard dashboard) {
        scriptObj.echo "[ Add Git Info ]"
        // Git(String repoUrl, String branch, String gitCommit)
        def git_repo_url = scriptObj.env.GIT_URL 
        def git_branch = scriptObj.env.GIT_BRANCH 
        def git_commit_hash = scriptObj.env.GIT_COMMIT 

        /*
        git_commit_hash = scriptObj.sh(script: """
        #!/bin/bash
        git rev-parse origin/${git_branch}
        """, returnStdout: true).trim()
        */
        dashboard.addGit(git_repo_url, git_branch, git_commit_hash)

        
    }
    public static void addUnitTestInfo(Script scriptObj, Dashboard dashboard) {
        scriptObj.echo "[ Add UnitTest Info ]"
        // UnitTest(int test, int passed, int failed, int skipped, int rate)
        def testStatus = []
        AbstractTestResultAction testResultAction = scriptObj.currentBuild.rawBuild.getAction(AbstractTestResultAction.class)
        if(testResultAction != null) {
            int unitTest_tests = testResultAction.totalCount
            int unitTest_failed = testResultAction.failCount
            int unitTest_skipped = testResultAction.skipCount
            int unitTest_passed = unitTest_tests - unitTest_failed - unitTest_skipped
            int unitTest_rate = (100 / unitTest_tests) * unitTest_passed
            dashboard.addUnitTest(unitTest_tests, unitTest_passed, unitTest_failed, unitTest_skipped, unitTest_rate)
        } else {
            scriptObj.echo "No UnitTest info added ! [ testResultAction = null ]"
        }
    }

    public static void addVersionInfo(Script scriptObj, Dashboard dashboard, String version_current) {
        scriptObj.echo "[ Add Version Info ]"
        // Version(String current, String release)
        // def version_current = "${VERSION}"
        def version_release = version_current.replace("-SNAPSHOT", "")
        dashboard.addVersion(version_current, version_release)
    }

    public static void addSonarqubeInfo(Script scriptObj, Dashboard dashboard, String GROUPID, String ARTIFACTID) {
        scriptObj.echo "[ Add Sonarqube Info ]"
        // Sonarqube(int codeSmell, Float coverage)
        def sonarqube_coverage_str = scriptObj.sh(returnStdout: true, script: Strings.trimAndShift("""
        #!/bin/bash
        curl -s -X GET 'https://sonar.gene.com/api/measures/search?projectKeys=$GROUPID:$ARTIFACTID&metricKeys=coverage' | jq .measures[0].value | sed 's/"//g'
        """)).trim()
        Float sonarqube_coverage = 0
        if (sonarqube_coverage_str && sonarqube_coverage_str != "null") {
            sonarqube_coverage = "${sonarqube_coverage_str}" as Float
        }
        def sonarqube_codesmell_str = scriptObj.sh(returnStdout: true, script: Strings.trimAndShift("""
        #!/bin/bash
        curl -s -X GET 'https://sonar.gene.com/api/issues/search?componentKeys=$GROUPID:$ARTIFACTID&facetMode=effor&facets=types&types=CODE_SMELL | jq '.facets[].values[] | select(.val=="CODE_SMELL") | .count'
        """)).trim()
        dashboard.addSonarqube(sonarqube_codeSmell, sonarqube_coverage, sonarqube_status_str)
        scriptObj.echo "[ End Sonarqube Info ]"
    }
    public static void addPcfInfo(Script scriptObj, Dashboard dashboard, String GROUPID, String ARTIFACTID, String VERSION) {
        scriptObj.echo "[ Add Pcf Info ]"
        def build_env = scriptObj.env.targetEnvironment

        def pcfCredentialsFile = scriptObj.pipelineParams.concoursePipelineCredentials
        def pcfConfigFile = scriptObj.pipelineParams.concoursePipelineVariables
        def pcfSpace = scriptObj.pipelineParams.pcfSpace

        def pcfDiv = scriptObj.sh(returnStdout: true, script: Strings.trimAndShift("""
        #!/bin/bash
        cat ${pcfCredentialsFile} | grep "cf-division-${build_env}" | awk '{print \$2}'
        """)).trim()

        def pcfOrg = scriptObj.sh(returnStdout: true, script: Strings.trimAndShift("""
        #!/bin/bash
        cat ${pcfCredentialsFile} | grep "cf-org-${build_env}" | awk '{print \$2}'
        """)).trim()

        def pcfAppName = scriptObj.sh(returnStdout: true, script: Strings.trimAndShift("""
        #!/bin/bash
        cat ${pcfConfigFile} | grep "cf-app-name-${build_env}" | awk '{print \$2}'
        """)).trim()

        scriptObj.echo ( "{ pcfInfo: {Div: \"${pcfDiv}\", Org: \"${pcfOrg}\", Apps: \"${pcfAppName}\"}")

        def pcfGitCommit = scriptObj.env.GIT_COMMIT
        def pcfVersion = scriptObj.env.VERSION

        def pcfArtifactUrl = ''
        def groupid_script = Strings.trimAndShift("""
        #!/bin/bash
        echo ${GROUPID} | tr '.' '/'
        """)

        def groupid_str = scriptObj.sh(returnStdout: true, script: groupid_script).trim()

        if(pcfVersion.contains("SNAPSHOT")) {
            pcfArtifactUrl = "https://artifactory.gene.com/artifactory/libs-snapshot-local/${groupid_str}/${ARTIFACTID}/${VERSION}/${ARTIFACTID}-${VERSION}.jar"

        } else {
            pcfArtifactUrl = "https://artifactory.gene.com/artifactory/libs-release-local/${groupid_str}/${ARTIFACTID}/${VERSION}/${ARTIFACTID}-${VERSION}.jar"

        }
        dashboard.addPcf(pcfOrg, pcfSpace, pcfAppName, pcfGitCommit, pcfVersion, pcfArtifactUrl)
    }
    public static void addProvisioningInfo(Script scriptObj, Dashboard dashboard) {
        if (!scriptObj.provisionObj.space) {
            return
        }
        scriptObj.echo "[ Add Provisioning Info ]"
        def foundation = scriptObj.provisionObj.foundation
        def org = scriptObj.provisionObj.org
        def space = scriptObj.provisionObj.space
        def appName = scriptObj.provisionObj.appName

        scriptObj.echo ("{ \"provisionInfo\": {\"appName\": \"${appName}\", \"foundation\": \"${foundation}\", \"org\": \"${org}\", \"space\": \"${space}\"} }")

        def gitCommit = scriptObj.env.GIT_COMMIT
        def version = scriptObj.VERSION
        def artifactId = scriptObj.ARTIFACTID
        def groupId = scriptObj.GROUPID

        groupId = scriptObj.sh(returnStdout: true, script: "echo ${groupId}| tr '.' '/'").trim()

        def artifactUrl = ''

        if(version.contains("SNAPSHOT")) {
            pcfArtifactUrl = "https://artifactory.gene.com/artifactory/libs-snapshot-local/${groupid_str}/${ARTIFACTID}/${VERSION}/${ARTIFACTID}-${VERSION}.jar"

        } else {
            pcfArtifactUrl = "https://artifactory.gene.com/artifactory/libs-release-local/${groupid_str}/${ARTIFACTID}/${VERSION}/${ARTIFACTID}-${VERSION}.jar"

        }
        dashboard.addProvisioning(foundation, org, space, appName, gitCommit, version, artifactUrl)
        scriptObj.echo "[ End Add Provisioning Info ]"

    }

    public static void addFortifyInfo(Script scriptObj, Dashboard dashboard, String ARTIFACTID, String VERSION, String USERNAME, Stirng PASSWORD, String status = 'defaultUnknown') {
        scriptObj.echo "[ Add Fortify Info  ]"
        // Fortify(int critical, int high, int medium, int low)
        int fortify_critical = 0
        int fortify_high = 0
        int fortify_medium = 0
        int fortify_low = 0

        def resp = scriptObj.sh(returnStdout: true, script: Strings.trimAndShift("""
        #!/bin/bash
        set -x
        if [[ \$(curl -s -w '%{http_code}' -o /dev/null -u '${USERNAME}:${PASSWORD}' -H 'Content-Type: application/json' -H 'Accept: application/json' -d "{ \\\"type\\\": \\\"UnifiedLoginToken\\\" }" https://fortify.gene.com/ssc/api/v1/tokens) != '201' ]]; then exit; fi
        TOKEN=\$(curl -s -u '${USERNAME}:${PASSWORD}' -H 'Content-Type: application/json' -H 'Accept: application/json' -d "{ \\\"type\\\": \\\"UnifiedLoginToken\\\" }" https://fortify.gene.com/ssc/api/v1/tokens | jq -r '.data.token')
        if [[ \$(curl -s -w '%{http_code}' -o /dev/null -H "Authorization: FortifyToken \${TOKEN}" -X GET 'https://fortify.gene.com/ssc/api/v1/projects?start=0&limit=200&q=name:%22${ARTIFACTID}522&fullextsearch-true') != '200' ]]; then exit; fi
        project_id=\$(curl  -s -H "Authorization: FortifyToken \${TOKEN}" -X GET 'https://fortify.gene.com/ssc/api/v1/projects?start=0&limit=200&q=name:%22${ARTIFACTID}522&fullextsearch-true' | jq '.data[] | select(.name=="${ARTIFACTID}") | .id' )
        if [[ \$project_id == '' ]]; then exit; fi
        if [[ \$(curl -s -w '%{http_code}' -o /dev/null -H "Authorization: FortifyToken \${TOKEN}" -X GET "https://fortify.gene.com/ssc/api/v1/projects/\${project_id}/versions") != '200' ]]; then exit; fi
        project_version_id=\$(curl -s -H "Authorization: FortifyToken \${TOKEN}" -X GET "https://fortify.gene.com/ssc/api/v1/projects/\${project_id}/versions" | jq '.data[] | select(.name == "${VERSION}") | .id' )
        if [[ \$project_verison_id == '' ]]; then exit; fi
        curl -s -H "Authorization: FortifyToken \${TOKEN}" -X GET "https://fortify.gene.com/ssc/api/v1/projectVersions/\${project_version_id}/issues"
        """)).trim()

        if ( resp != "" ) {
            try {
                scriptObj.readJSON text: resp 
                if ( scriptObj.env.DEBUT != null)
                    scriptObj.echo ( "DEBUG ==> ${resp}")
                
                def fortify_str = "", numOfIssue = ""
                def severity = [ "Critical", "High", "Medium", "Low"]
                severity.each {
                    fortify_str = Strings.trimAndShift("""
                    #!/bin/bash
                    #set -x
                    echo '${resp}' | jq '.data[] | select(.friority=="${it}") | . friority' | wc -l 
                    """)

                    numOfIssue = scriptObj.sh(returnStdout: true, script: fortify_str).trim()
                    switch (it) {
                        case "Critical":
                            fortify_critical = numOfIssue as int 
                            break
                        case "High":
                            fortify_high = numOfIssue as int 
                            break
                        case "Medium":
                            fortify_medium = numOfIssue as int 
                            break
                        case "Low":
                            fortify_low = numOfIssue as int 
                            break
                        default:
                            scriptObj.echo("Switch Fortify Severity Error: ${it}")
                            break
                        
                    }
                }
            } catch (Exception err) {
                scriptObj.echo "response Body is not JSON file, ${err}"
            }
        }
        dashboard.addFortify(fortify_critical, fortify_high, fortify_medium, fortify_low, status)
    }
    public static void addFortifyInfo(Script scriptObj, Dashboard dashboard, String ARTIFACTID, String VERSION, String TOKEN,  String status = 'defaultUnknown') {
        scriptObj.echo "[ Add Fortify Info  ]"
        // Fortify(int critical, int high, int medium, int low)
        int fortify_critical = 0
        int fortify_high = 0
        int fortify_medium = 0
        int fortify_low = 0

        def resp = scriptObj.sh(returnStdout: true, script: Strings.trimAndShift("""
        #!/bin/bash
        set -x
        if [[ \$(curl -s -w '%{http_code}' -o /dev/null -H "Authorization: FortifyToken ${TOKEN}" -X GET 'https://fortify.gene.com/ssc/api/v1/projects?start=0&limit=200&q=name:%22${ARTIFACTID}522&fullextsearch-true') != '200' ]]; then exit; fi
        project_id=\$(curl  -s -H "Authorization: FortifyToken ${TOKEN}" -X GET 'https://fortify.gene.com/ssc/api/v1/projects?start=0&limit=200&q=name:%22${ARTIFACTID}522&fullextsearch-true' | jq '.data[] | select(.name=="${ARTIFACTID}") | .id' )
        if [[ \$project_id == '' ]]; then exit; fi
        if [[ \$(curl -s -w '%{http_code}' -o /dev/null -H "Authorization: FortifyToken ${TOKEN}" -X GET "https://fortify.gene.com/ssc/api/v1/projects/\${project_id}/versions") != '200' ]]; then exit; fi
        project_version_id=\$(curl -s -H "Authorization: FortifyToken ${TOKEN}" -X GET "https://fortify.gene.com/ssc/api/v1/projects/\${project_id}/versions" | jq '.data[] | select(.name == "${VERSION}") | .id' )
        if [[ \$project_verison_id == '' ]]; then exit; fi
        curl -s -H "Authorization: FortifyToken ${TOKEN}" -X GET "https://fortify.gene.com/ssc/api/v1/projectVersions/\${project_version_id}/issues"
        """)).trim()

        if ( resp != "" ) {
            try {
                scriptObj.readJSON text: resp 
                if ( scriptObj.env.DEBUT != null)
                    scriptObj.echo ( "DEBUG ==> ${resp}")
                
                def fortify_str = "", numOfIssue = ""
                def severity = [ "Critical", "High", "Medium", "Low"]
                severity.each {
                    fortify_str = Strings.trimAndShift("""
                    #!/bin/bash
                    #set -x
                    echo '${resp}' | jq '.data[] | select(.friority=="${it}") | . friority' | wc -l 
                    """)

                    numOfIssue = scriptObj.sh(returnStdout: true, script: fortify_str).trim()
                    switch (it) {
                        case "Critical":
                            fortify_critical = numOfIssue as int 
                            break
                        case "High":
                            fortify_high = numOfIssue as int 
                            break
                        case "Medium":
                            fortify_medium = numOfIssue as int 
                            break
                        case "Low":
                            fortify_low = numOfIssue as int 
                            break
                        default:
                            scriptObj.echo("Switch Fortify Severity Error: ${it}")
                            break
                        
                    }
                }
            } catch (Exception err) {
                scriptObj.echo "response Body is not JSON file, ${err}"
            }
        }
        dashboard.addFortify(fortify_critical, fortify_high, fortify_medium, fortify_low, status)
    }
    public static void addNewRelicInfo(Script scriptObj, Dashboard dashboard, def fileName, String folderName = ".") {

        scriptObj.echo "[ Add NewRelic Info ]"
        def fileExists = scriptObj.fileExists "${folderName}/${fileName}"
        if (fileExists) {
            scriptObj.echo "## ${folderName}/${fileName} file found: " + fileExists
            def new_relic_str = scriptObj.sh(returnStdout: true, script: """
            #!/bin/bash
            cat ${folderName}/${fileName} | grep NEW_RELIC_APP_NAME
            """).trim()

            if (! new_relic_str.startsWith("#")) {
                def new_relic_app_name = scriptObj.sh(returnStdout: true, script: Strings.trimAndShift("""
                #!/bin/bash
                echo ${new_relic_str} | awk -F: '{print \$2}'
                """)).trim()

                dashboard.addNewRelic(new_relic_app_name)
            } else {
                scriptObj.echo "## NEW_RELIC_APP_NAME has been comment out !!"
            }
        } else {
            scriptObj.echo "## ${folderName}/${fileName} file not found: " + fileExists
        }
    }
    public static void addStatusInfo(Script scriptObj, Dashboard dashboard, Map status) {
        scriptObj.echo "[ DevOps Dashboard: Add StageStats Info ]"
        def tmp = [:]
        status.each{ k, v -> checkValue(tmp, k, v) }
        dashboard.addStatus(tmp)
    }
    public static void addPropertiesInfo(Script scriptObj, Dashboard dashboard, Map propertiesInfo) {
        scriptObj.echo "[ Devops Dashboard: Add Properties Info ]"
        dashboard.addProperties(propertiesInfo)
    }
    private static void checkValue(Map myMap, String key, String value) {
        if (value == "FAILED" || value == "ERROR" || value == "UNSTABLE" || value == "failed") {
            myMap[key] = false
        } else if (value == "Success" || value == "OK" || value == "SUCCESS" || value == "successful") {
            myMap[key] = true
        }
    }


}
