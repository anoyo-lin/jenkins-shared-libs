package com.gene.util
// vim et:ts=4:sts=4:sw=5:fileencoding=utf-8

import com.gene.util.Strings

public class Shell {
    static String quickShell(Script scriptObj, String command, Boolean unix = null) {
        // rely on boxing and unboxing instead of new Boolean() and booleanValue() which do not show in the whitelist,
        // https://github.com/jenkinsci/script-security-plugin/blob/master/src/main/resources/org/jenkinsci/plugins/scriptsecurity/sandbox/whitelists/generic-whitelist
        if (unix == null) {
            unix = scriptObj.isUnix()
        }
        if (unix) {
            return scriptObj.sh(returnStdout: true, script: command)

        }
        scriptObj.echo("+ @${command}")
        return Strings.removeCarriageReturns(scriptObj.bat(returnStdout: true,
        script: String.escapeForCmd("@" + command)))
    }

    /**
    * obtain a java property such as java.home or user.home from the java on path.
    * one of the blackduck hub-detect install stages uses java from PATH.
    */

    static String getBuildJavaProperty(Script scriptObj, String propertyName, Boolean unix = null) {
        String javaOutput = quickShell(scriptObj, "java -XshowSettins:properties 2>&1 || exit 0", unix)
        String propertyNameRegEx = propertyName.replaceALL(/\./, "\\.")
        for (def line in javaOutput.split("\n")) {
            line = line.trim()
            if (!line) {
                continue
            }
            def m = (line =~ /${propertyNameRegEx} = (.*)/)
            if (m) {
                return m[0][1]
            }
        }
        return null 
    }
    /**
    * check and enable a certification authority.
    */
    static  boolean checkEnableCertAuthority(Script scriptObj, String caFile, String alias, String resource,
    Boolean unix = null, String javaHome = null) {
        if(javaHome == null) {
            javaHome = getBuildJavaProperty(scriptObj, "java.home", unix)

        }
        if(!scriptObj.fileExists(javaHome)) {
            return false
        }
        String kt = "\"${javaHome}/bin/keytool\" -keystore \"${javaHome}/lib/security/cacerts\" -keypass changeit -storepass changeit"
        String checkCertOutput = quickShell(scriptObj, "${kt} -list -alias \"${alias}\" 2>&1|| exit 0 ", unix)
        scriptObj.echo(checkCertOutput)
        if (checkCertOutput =~ /does not exist/) {
            String caBundle = scriptObj.libraryResource(resource)
            scriptObj.writeFile(file "${scriptObj.env.WORKSPACE}/${caFile}", text: caBundle)
            String addCertCommand = "${kt} -import -file \"${caFile}\" -alias \"${alias}\" -v -noprompt 2>&1 || exit 0"
            String addCertWithAdminCommand = addCertCommand
            if (unix){
                addCertWithAdminCommand = "sudo ${addCertCommand}"
            }
            String addCertOutput = quickShell(scriptObj, addCertCommand, unix)
            scriptObj.echo(addCertOutput)
            if (unix && (addCertOutput =~ /sudo: /)) {
                scriptObj.echo("Trying the sudo to elevate the permssion")
                addCertOutput = quickShell(scriptObj, addCertWithAdminCommand, unix)
                scriptObj.echo(addCertOutput)
            }
            if (addCertOutput =~ /Exception:/) {
                scriptObj.echo("exception with keytool error")
                return false
            }
        }
        return true
    }
}