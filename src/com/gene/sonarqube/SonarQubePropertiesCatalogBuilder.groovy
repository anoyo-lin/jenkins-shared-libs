package com.gene.sonarqube

import com.gene.util.propertyFile.PropertiesCatalog
import com.gene.pipeline.PipelineType

class SonarQubePropertiesCatalogBuilder {
    public static build(PropertiesCatalog propertiesCatalog , PipelineType pipelineType ) {
        propertiesCatalog.addOptionalProperty("sonarQubeFailPipelineOnFailedQualityGate", "Default is true", "true")
        propertiesCatalog.addOptionalProperty("sonarQubeLogLevel", "Default is True", "true")

        if(pipelineType == PipelineType.DOTNET || pipelineType == PipelineType.DOTNETCORE) {
            propertiesCatalog.addMandatoryProperty("sonarQubeProjectKey", "must set sonarQubeProjectKey")
            propertiesCatalog.addMandatoryProperty("sonarQubeProjectVersion", "must set sonerQubeProjectVersion")
        }
    }
}