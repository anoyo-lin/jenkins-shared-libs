package com.gene.blackduck

import com.gene.pipeline.PipelineType
import com.gene.util.Conditions
import com.gene.util.propertyFile.PropertiesCatalog

class BlackduckPropertiesCatalogBuilder {
    public static build(PropertiesCatalog propertiesCatalog, PipelineType pipelineType) {
        propertiesCatalog.addMandatoryProperty("hubVersionDist", "should be one of: INTERNAL, EXTERNAL, SAAS, OPENSOURCE")
        propertiesCatalog.addMandatoryProperty("hubVersionPhase", "should be one of: PLANNING, DEVELOPMENT, RELEASED, DEPRECATED, ARCHIVED")

        propertiesCatalog.addOptionalProperty("hubExcludedModules", "Default is \"Nothing_To_Exclude\"", "Nothing_To_Exclude")
        propertiesCatalog.addOptionalProperty("hubExclusionPattern", "Default is \"/Nothing/To/Exclude/\"", "/Nothing/To/Exclude/")
        propertiesCatalog.addOptionalProperty("hubFailOnSeverities", "Default is \"ALl\"", "ALL")
        propertiesCatalog.addOptionalProperty("hubFailPipelineFailedOpenSourceGovernance", "Default is \"true\"", "true")
        propertiesCatalog.addOptionalProperty("hubLoggingLevel", "Default is \"WARN\", valid values: ALL|TRACE|DEBUG|INFO|WARN|ERROR|FATAL|OFF", "INFO")
        propertiesCatalog.addOptionalProperty("hubTimeoutMinutes", "Default is 10", "10")
        propertiesCatalog.addOptionalProperty("hubTriggers", "Default is null (meta-regex negation " + Conditions.DEFAULT_TOOL_TRIGGERS, null)
        propertiesCatalog.addOptionalProperty("hubUserPasswordTokenName", "Default is \"jenkins_blackduck\"", "jenkins_blackduck")

    }
}