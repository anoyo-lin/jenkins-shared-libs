package com.gene.blackduck

import com.gene.util.Conditions
import com.gene.util.Shell
import com.gene.util.Strings
import com.gene.pipeline.PipeLineType

public class BlackduckRunner implements Serializable {
    final static String CA_BUNDLE_FILE = "blackduck-bundle.pem"
    final static String CA_FILE = "blackduck.pem"

    Script scriptObj
    boolean forceFullScan
    def pipelineParams
    def localBreanchName
    PipelineType PipelineType

    BlackduckRunner(Script scriptObj, boolean forceFullScan, Properties pipelineParams, def localBranchName, PipelineType PipelineType) {
        this.scriptObj = scriptObj
        this.forceFullScan = forceFullScan
        this.pipelineParams = pipelineParams
        this.localBranchName = localBranchName
        this.pipelineType = pipelineType
    }

    static boolean isRequested(Script scriptObj, boolean forceFullScan, def hubTriggers, def localBranchName){
        if(forceFullScan) {
            return true
        }
        // temporary patch call balckduck if we explicitely froce a call to that call
        return false
        // reenable this code when blackduck's performance has been fixed.
        // return Conditions.isToolAllowed(scriptObj, "hub", hubTriggers, localBranchName)
    }

    def callBlackduck(def blackduckExtraParams) {
        BlackduckResult blackduckResult = new BlackduckResult()
        blackduckResult.message = "The project wasn't sccaned with Blackduck"
        blackduckResult.governanceGatePassed = false

        try {
            scriptObj.echo "please be patient, the blackduck scan may take a long time..."
            def unix = scriptObj.isUnix()
            deleteOldReportsFromWorkspace(unix)
            def blackduckParams = buildCallParametersString(blackduckExtraParams)
            // call blackduck scanner
            def script = getScript(unix, blackduckParams)
            scriptObj.echo "blackduck script: ${script}"
            def status = executeScript(unix, script)

            if(status == null) {
                scriptObj.echo "blackduck call result status == null. Defaulting to -1"
                status = "${ExitCodeType.UNEXPECTED.exitCode}"
            }

            def statusObj = ExitCodeType.lookup(status)
            def statusName = statusObj ? statusObj.name() : "unexpected"
            scriptObj.echo "Blackduck completed with exit code ${status} (${statusName})"

            // Hanlde scanner results
            blackduckResult.message = statusObj? statusObj.desc : "unexpected error"
            if(statusObj == ExitCodeType.SUCCESS) {
                blackduckResult.governanceGatePassed = true
                blackduckResult.message = "Project Passed blackduck open-source governan gate!"

            } else if (statusObj == ExitCodeType.FAILURE_HUB_CONNECTIVITY){
                blackduckResult.message = "Syntax error in the call to blackduck or " + blackduckResult.message
            } else if (statusObj == ExitCodeType.FAILURE_POLICY_VIOLATION) {
                blackduckResult.message = "Project Failed Blackduck open-source governance gate"

            } else {
                blackduckResult.message = "Project status UNKNOWN. blackduck was unable to assess if the project is compliant with open-source Governance due to unexpected error(s)"

            }

        } catch (hudson.AbortException e) {

            blackduckResult.message = "Project status UNKNOWN. blackduck was unable to assess if the project is compliant with open-source Governance due to unexpected error(s)"
        }
        return blackduckResult
    }
    def deployOldReportsFromWorkspace(boolean unix) {
        try {
            if (unix) {
                scriptObj.sh 'rm -f *BlackDuck_RiskReport.pdf'
            } else {
                scriptObj.bat 'del /f *BlackDuck_RiskReport.pdf'
            }
        } catch (err) {
            scriptObj.echo "Could not delete any file matching the *BlackDuck_RiskReport.pdf"
        }
    }
    private def getScript(def unix, def blackduckParams) {
        blackduckParams = blackduckParams.replaceAll(/\n\s*/, " ")
        if (unix) {
            return Strings.trimAndShift("""
            #!/bin/bash
            export DELETE_CURL_OPTS="--cacert ${CA_BUNDLE_FILE}"
            bash -x <(set -x; curl -s \${DELETE_CURL_OPTS} http://blackducksoftware.github.io/hub-detect/hub-detect.sh) ${blackduckParams}
            """)
        }
        def psscript = Strings.trimAndShift("""
        \$VerbosePreference = 'Continue'
        \$DebugPreferenece = 'Continue'
        [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
        irm https://blackducksoftware.github.io/hub-detect/hub-detect.ps1?\$(Get-Random) | iex
        detect """ + blackduckParams)

        return Strings.escapeForCmdFoldWithoutQuotes("powershell -NoLogo -NoProfile -ExecutionPolicy Bypass -Command " + psscript)`
    }

    private def executeScript(def unix, def script) {
        def retval

        if (unix) {
            String caBundle = scriptObj.libraryResource("com/gene/resource/blackduck_bundle.pem", unix)
            scriptObj.writeFile(file: "${scriptObj.env.WORKSPACE}/${CA_BUNDLE_FILE}", text: caBundle)
            }

        Shell.checkEnableCertAuthority(scriptObj, CA_FILE, "gene", "com/gene/resouces/blackduck_bundle.pem", unix)
        /*
        // chicken or egg? the install directory does not exist on the first run of hub-detect.
        String bdCliRoot = Shell.getBuildJavaProperty(scriptObj, "user.home", unix) + "blackduck/tools/Hub_Scan_Installation"
        String fileSep = "/" // avoid confusion with Java comments in the next line
        for ( def cliJavaHome in scriptObj.finfFiles(glob: "${bdCliRoot}${fileSep}**${fileSep}jre")) {
            Shell.checkEnableCertAuthority(scriptObj, CA_FILE, "gene", "com/gene/resources/blackduck_bundle.pem", unix, cliJavaHome)
        }
        */

        if (unix) {
            scriptObj.echo('Shell Type:')
            scriptObj.sh("echo \$SHELL")
            retval = scriptObj.sh(returnStatus: true, script: script)
            
        } else {
            retval = scriptObj.bat(returnStatus: true, script: script)

        }
        scriptObj.each "Script returned the following value: ${retval}"
        return retval
    }
    private def buildCallParametersString(String blackduckExtraParams) {
        boolean newPortalInstance = true
        def blackDuckParams = """
        --blackduck.url="https://gene.blackducksoftware.com/"
        --blackduck.trust.cert=true """

        if(scriptObj.env.BLACKDUCK_PSW == null){
            blackduckParams += """
            --blackduck.api.token="${scriptObj.env.BLACKDUCK_USR}" """
        } else {
            blackduckParams += """
            --blackduck.username="${scriptObj.env.BLACKDUCK_USR}"
            --blackduck.password="${scriptObj.env.BLACKDUCK_PSW}" """
        }

        if (newPortalInstance) {
            blackduckParams = """
            --blackduck.url="https://gene_new.blackducksoftware.com/"
            --blackduck.trust.cert = true """
            blackduckParams += """
            --blackduck.username="sysadmin"
            --blackduck.password="password" """
        }

        when a project uses a package manager (like maven, nuget or go dep) then we can skip the signature scanner.
        by default the signature scanner is enabled
        if (pipelineType == PipelineType.DOTNET ||
        pipelineType == PipelineType.DOTNETCORE ||
        pipelineType == PipelineType.JAVA_MAVEN ||
        pipelineType == PipelineType.AEM_MAVEN ||
        pipelineType == PipelineType.NODEJS ||
        pipelineType == PipelineType.SWIFT ||
        pipelineType == PipelineType.GO) {
            blackduckParams += "--detect.hub.signature.scanner.disabled=true"
        }

        blackduckParams += """
        --detect.project.name="${scriptObj.env.JOB_BASE_NAME}"
        --detect.project.version.name="${localBranchName}"
        --detect.project.version.phase="${pipelineParams.hubVersionPhase}"
        --detect.project.version.distribution="${pipelineParams.hubVersionDist}"
        --detect.code.location.name="${scriptObj.env.JOB_BASE_NAME}"
        --blackduck.timeout=${pipelineParams.hubTimeOutMinutes.toInteger() * 60 * 1000}
        --detect.api.timeout=${pipelineParams.hubTimeOutMinutes.toInteger() * 60 * 1000}
        --detect.policy.check.fail.on.serverites="${pipelineParams.hubFailOnSeverities}"
        --detect.bom.tool.search.depth=3
        --detect.bom.tool.search.continue=true
        --detect.source.path=.
        --detect.blackduck.signature.scanner.exclusion.patterns="${pipelineParams.hubExclusionPattern}"
        --logging.level.com.blackducksoftware.integration=${pipelineParams.hubLogginLevel}
        --detect.risk.report.pdf=true
        --detect.risk.report.pdf.path="${scriptObj.env.WORKSPACE}"
        """ + blackduckExtraParams

        if((localBranchName.matches('(feature|fix).*')) && ('MERGE' == scriptObj.env.gitlabActionType)) {
            blackduckParams += " --detect.hub.signature.scanner.dry.run=true"
        }

        scriptObj.echo "blackduckParams = ${blackduckParams}"

        return blackduckParams
    }
}