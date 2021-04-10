package com.gene.artifactory

import com.gene.util.propertyFile.PropertiesCatalog
import com.gene.pipeline.PipelineType

class ArtifactoryPropertiesCatalogBuilder {
    public static build(PropertiesCatalog propertiesCatalog, PipelineType pipelineType) {
        propertiesCatalog.addOptionalProperty("artifactoryDeploymentPattern", "Defaulting artifactoryDeploymentPattern property to null", null)
        propertiesCatalog.addOptionalProperty("artifactoryInstance", "Defaulting property to \"Artifactory-Production\"", "Artifactory-Production")

        if(pipelineType == PipelineType.DOTNET || pipelineType == PipelineType.DOTNETCORE) {
            propertiesCatalog.addMandatoryProperty("artifactoryCredentialId", "Missing CredentialId property value which must be set to the id of the credential entry to be used to connect to Artifactory.")
            propertiesCatalog.addOptionalProperty("projectDeliverableName", "Defaulting projectDeliverableName property to null", null)

        }
        if(pipelineType == PipelineType.JAVA_MAVEN || pipelineType == PipelineType.SELENIUM){
            propertiesCatalog.addMandatoryProperty("snapshotRepo", "Missing snapshotRepo property value. Should be set to the Artifactory snapshot repo name. An example would be gene-maven-snapshot.")

        }
        if(pipelineType == PipelineType.DOTNET || pipelineType == PipelineType.DOTNETCORE){
            propertiesCatalog.addMandatoryProperty("releaseRepo", "Missing releaseRepo property value. Should be set to the Artifactory release repo name.")

        }
    }
}