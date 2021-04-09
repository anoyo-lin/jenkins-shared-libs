package com.gene.fortify
// vim: et:ts=4:sts=4:sw=4:fileencoding=utf-8

import com.gene.util.Conditions
import com.gene.util.Shell
import com.gene.util.Strings

public class FortifyRunner implements Serializable {
    Script scriptObj
    String localBranchName
    Properties pipelineParams
    String buildId
    String opts

    def fortifyApp
    def fortifyVer
    def fortifyAppDescr
    def fortifySSC
    def jobBaseName
    def scriptWeb

    protected String obtainFortifyRoot(Script scriptObj, boolean unix) {
        // this.env holds the environment of the host.
        // only this.sh runs within the container
        // the docker plugin does not start a Jenkins agent inside the container.
        // https://github.com/jenkinsci/docker-workflow-plugin/blob/master/src/main/resources/org/jenkinsci/plugins/docker/workflow/Docker.groovy
        // https://github.com/jenkinsci/docker-workflow-plugin/blob/master/src/main/java/org/jenkinsci/plugins/docker/workflow/DockerDSL.groovy
        scriptObj.echo "FORTIFY_ROOT of the host: \"${scriptObj.env.FORTIFY_ROOT}\""
        String fortifyRoot = Shell.quickShell(scriptObj, 'echo "${FORTIFY_ROOT}"', unix).trim()
        scriptObj.echo "FORTIFY_ROOT inside the container: \"${fortifyRoot}\""
        // scriptObj.sh "pwd; ls -al /; ls -al /proc; cat /proc/1/cpuset; ls -al; set"
        if (fortifyRoot) {
            return fortifyRoot
        }
        if (unix) {
            return "${scriptObj.env.HOME}/Home/workspace/fortify" //MacOS
        }
        return "e:/fortify"
    }
    def init() {
        if (!scriptObj || !localBranchName || !pipelineParams) {
            scriptObj.error('FortifyRunner requires named arguments scriptObj, localBranchName and pipelineParams in its constructor')

        } else {
            jobBaseName = scriptObj.env.JOB_BASE_NAME
            fortifyApp = (pipelineParams.fortifyApp ?:
            pipelineParams.projectName ?:
            pipelineParams.sonarQubeProjectKey ?:
            jobBaseName)
            fortifyVer = pipelineParams.fortifyVer ?: localBranchName
            unix = scriptObj.isUnix()
            fortifyRoot = obtainFortifyRoot(scriptObj, unix)
            fortifyAppDescr = pipelineParams.fortifyAppDescr ?: "${scriptObj.env.JOB_BASE_NAME}"
            fortifySSC = pipelineParams.fortifyServer
            scriptWeb = pipelineParams.fortifyScriptWeb
        }
    }
    def translateOnly(def opts = null) {
        FortifyResult fortifyResult = new FortifyResult()
        fortifyResult.message = " the project wasn't scanned with Fortify"
        fortifyResult.codeSecurityGatePassed = false
        try {
            def scriptResult = runScript("-t" + (opts ? " " + opts : ""))
            validateCodeSecurityGate(scriptResult, false, fortifyResult)
        } catch (hudson.AbortException e) {
            scriptObj.echo "Fortify execution was aborted"
            fortifyResult.message = "Prject Status UNKNOWN. fortify was unable to access if the project 
            is compliant with code Security Governance becauswe its execution was aborted"
            
        } catch (Exception e) {
            scriptObj.echo "Unexpected error: ${e}"
            fortifyResult.message = " Project Status UNKNOWN. fortify was unable to assess if the project
            is compliant with code security governance due to unexpected error(s)."
        }
        return fortifyResult
    }
    def run(def opts = null) {
        FortifyResult fortifyResult = new FortifyResult()
        fortifyResult.message = "The project wasn't scanned with fortify."
        fortifyResult.codeSecurityGatePassed = false

        try {
            def scriptResult = runScript(opts)
            validateCodeSecurityGate(scriptResult, true, fortifyResult)
        } catch (hudson.AbortException e) {
            scriptObj.echo "Fortify executin was aborted."
            fortifyResult.message = "Project status UNKONWN. Fortify was unable to assess if the project 
            is compliant with code Security Governance because its execution was aborted"
        } catch (Exception e) {
            scriptObj.echo "Unexpected error: ${e}"
            fortifyResult.message = "Project status UNKNOWN. Fortify was unable to assess if the project
            is compliant with code security Governance due to unexpected error(s)."
        }
        return fortifyResult
    }
    private def runScript(def runOpts = null) {
        // Avoid a slinet exit on calling global steps such as echo (println),
        // sh, bat and error, by resolving them against the script object.
        // https://stackoverflow.com/questions/42149652/println-in-call-method-of-vars-foo-groovy-works-but-not-in-method-in-class
        scriptObj.echo "Please wait, the Fortify scan may take between 10 minutes and 2 hours (60 times the build time)..."

        def curlAuth = ""
        if (scriptObj.env.GITLAB_API_TOKEN != null) {
            curlAuth = "-H \"PRIVATE-TOKEN: ${scriptObj.env.GITLAB_API_TOKEN}\""
        }
        // Re-evaluate isUnix() in case the build changed its node
        unix = scriptObj.isUnix()
        fortifyRoot = obtainFortifyRoot(scriptObj, unix)

        def fortifyOpts = ""
        if(buildId != null) {
            fortifyOpts += " -b \"${buildId}\""

        }
        if(opts != null) {
            fortifyOpts += " ${opts}"

        }
        if(runOpts != null) {
            fortifyOpts += " ${runOpts}"
        }

        def shellScript = """scriptweb="${scriptWeb}"
        curlauth=(${curlAuth})
        e=\$(curl "\${curlauth[@]}" -s -o fortify.sh --write-out "%{http_code}" "\${scriptweb}fortify.sh")
        (( e == 200 ))
        e=\$(curl "\${curlauth[@]}" -s -o fortify.sh --write-out "%{http_code}" "\${scriptweb}fortify-ssc.py")
        (( e == 200 ))
        source fortify.sh \\
        "${fortifySSC}" \\
        "${fortifyApp}" "${fortifyVer}" "${fortifyAppDescr}" \\
        "${fortifyRoot}"${fortifyOpts}
        """

        // Call fortify
        def platformScript
        int status
        def scriptResult = ""
        if (unix) {
            platformScript = ("""export PATH="/bin:/usr/bin"
            export HOME="\${WORKSPACE}"
            """ + shellScript).replaceAll(/\n\s*/, "\n ")
            status = scriptObj.sh(returnStatus: true, script: platformScript)
        } else {
            // protect line breaks and special characters such as CMD pipe | against the greedy interpretation by CMD.
            //
            // Avoid wrapping the CMD script with double quotes to satisfy
            // CMD's failure to follow a quoted string across multiple line
            //
            // use quoted strings for uniform escape rules (applied once 
            // against Groovy interpolation and another against the RegEx
            // interpreter). this seems easier than slashy groovy Strings that 
            // do not seem to allow visible representation of newlines.
            platformScript = "\"%cygbinslash%bash.exe\" -exc '" +
            String.escapeForCmdFoldInSingleQuotes("""windir="\$(/usr/bin/cygpath -u "\${WINDIR}")"
            export PATH="/bin:/usr/bin:\${windir}/System32:\${windir}/System32/WindowsPowerShell/v1.0"
            """ + shellScript) + "'"
            status = scriptObj.bat(returnStatus: true, script: platformScript)
        }

        if (status) {
            scriptObj.echo "Error: ${status}"
            scriptResult = "Exit code ${status} executing the following script. \n${platformScript}"

            if(scriptObj.env.GITLAB_API_TOKEN != null) {
                scriptResult = scriptResult.replace(scriptObj.env.GITLAB_API_TOKEN, "****")
            }

        }
        return scriptResult

    }
    private def validateCodeSecurityGate(scriptResult, checkResultFile, fortifyResult) {
        if(scriptResult) {
            fortifyResult.message = "Project status UNKNOWN. Fortify was unable to assess if the project is compliant with code Security Governance due to unexpected error(s)."
            fortifyResult.codeSecurityGatePassed = false
        } else if(checkResultFile) {
            int numHighs = 0
            String firstCategory = null
            String firstIssue = null
            String issuesOutput =scriptObj.readFile("${scriptObj.env.WORKSPACE}/fortify-issues.txt")
            def issuesOutputLines = issuesOutput.split('\n')

            for (def line in issuesOutputLines) {
                line = line.trim()

                if(!line) {
                    continue
                }
                def m = (line =~ /(\d+) issues of (\d+) matched search query.*/)

                if(m) {
                    numHighs = m[0][1].toInteger()
                } else if (line.startsWith("Issue counts")) {
                } else if (!firstCategory) {
                    firstCategory = line
                    def c = (firstCategory =~ /"([^\"]+)".*/)
                    if(c) {
                        firstCategory = c[0][1]
                    }
                } else {
                    firstIssue = line
                    break
                }
            }
            if (numHighs > 0) {
                fortifyResult.message = "Project FAILED Code Security Gate. Fortify detected ${numHighs} high or critical issues such as \"${firstCategory}\" in ${firstIssue}"
                fortifyResult.codeSecurityGatePassed = false
            } else {
                fortifyResult.message = "Project PASSED Code Security Gate!"
                fortifyResult.codeSecurityGatePassed = true
            }
        } else {
            // Keep the original "the project wan't scanned" / false result/
        }
    }

    static boolean isRequestd(def scriptObj, def forceFullScan, def fortifyTriggers, def localBranchName) {
        if (forceFullScan) {
            return true
        }
        return Conditions.isToolAllowed(scriptObj, "fortify", fortifyTriggers, localBranchName)
    }
}