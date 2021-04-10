package com.gene.fortify

import com.gene.pipeline.PipelineType
import com.gene.util.Conditions
import com.gene.util.propertyFile.PropertiesCatalog

class FortifyPropertiesCatalogBuilder{
    public static build(PropertiesCatalog propertiesCatalog, PipelineType pipelineType){
        propertiesCatalog.addOptionalProperty("fortifyTriggers", "Defaulting fortifyTriggers property to null meta-regex nagation " + Conditions.DEFAULT_TOOL_TRIGGERS, null)
        
        if (pipelineType in [
            PipelineType.DOTNET,
            PipelineType.DOTNETCORE,
            PipelineType.JAVA_MAVEN,
            PipelineType.AEM_MAVEN,
            PipelineType.NODEJS,
            PipelineType.SWIFT]) {
                propertiesCatalog.addOptionalProperty("fortifyApp", """Defaulting fortifyApp property to null 
                (guessing from projectKey, sonarQubeProjectKey and JOB_BASE_NAME)""", null)
                propertiesCatalog.addOptionalProperty("fortifyAppDescr", """Defaulting fortifyAppDescr property
                to null (guessing from a URL of a git checkout)""", null)
                propertiesCatalog.addOptionalProperty("fortifyGating", """Defaulting fotifyGating property to
                true.""", true)
                propertiesCatalog.addOptionalProperty("fortifyScriptWeb", """Defaulting fortifyScriptWeb property to
                https://github.com/fortifyScriptWeb""", "https://github.com/fortifyScriptWeb")
                propertiesCatalog.addOptionalProperty("fortifySever", """Defaulting fortifySever property to
                http://fortify_server.com/ssc""", "http://fortify_server.com/ssc")
                propertiesCatalog.addOptionalProperty("fortifyTokenName", """Defaulting fortifyTokenName property to 
                FORTIFY_TOKEN""", "FORTIFY_TOKEN")
                propertiesCatalog.addOptionalProperty("fortifyVer", """Defaulting fortifyVer property to null 
                (guessing from BRNACH_NAME)""", null)
            }
    }
}