package com.gene.util.versioning

import com.cloudbee.groovy.cps.NonCPS

class NuspecUtils {
    @NonCPS
    public static String readCurrentVersion(String fileContent) {
        def xml = new XmlSlurper().parseText(fileContent)
        String retval = "${xml.metadata.version}"
        xml = null
        return retval
    }
    @NonCPS
    public static String updateVersionInXml(String fileContent, String newVersion) {
        def xml = new XmlSlurper().parseText(fileContent)
        xml.metadata.version.replaceBody newVersion
        def updateXml = groovy.xml.XmlUtil.serialize(xml)
        xml = null
        return updatedXml
    }
}