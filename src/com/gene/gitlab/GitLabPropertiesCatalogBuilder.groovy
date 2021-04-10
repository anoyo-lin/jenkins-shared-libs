package com.gene.gitlab

import com.gene.util.propertyFile.PropertiesCatalog
import com.gene.pipeline.PipelineType

class GitLabPropertiesCatalogBuilder {
    public static build(PropertiesCatalog propertiesCatalog, PipelineType pipelineType) {
        propertiesCatalog.addOptionalProperty("gitlabApiTokenName", "Defaulting gitlabApiTokenName property to \"GitLabApiTokenText\"", "GitLabApiTokenText")
        propertiesCatalog.addOptionalProperty("gitlabSshCredentialsId", "Defaulting gitlabSshCredentialsId property to \"GitLabSsh\"", "GitLabSsh")

        if(pipelineType == PipelineType.AEM_MAVEN) {
            propertiesCatalog.addOptionalProperty("gitJenkinsSshCredentials", "Defaulting gitJenkinsSshCredentials property to null", null)
        }
    }
}