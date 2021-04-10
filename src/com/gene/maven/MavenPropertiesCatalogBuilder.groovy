package com.gene.maven

import com.gene.util.propertyFile.PropertiesCatalog
import com.gene.pipeline.PipelineType

class MavenPropertiesCatalogBuilder {
    public static build(PropertiesCatalog propertiesCatalog, PipelineType pipelineType) {
        propertiesCatalog.addMadatoryProperty("mavenHome", "Missing the mavenHome property. It should be set to the path to the mvn executable.")
        propertiesCatalog.addMadatoryProperty("mavenBuildGoal", "Defaulting mavenBuildGoal property to \"clean compile\"", "clean compile")
        propertiesCatalog.addMadatoryProperty("mavenSettingsFileName", "Defaulting mavenSettingsFileName property to \"settings.xml\"", "settings.xml")
        if(pipelineType == Pipeline.SELENIUM) {
            propertiesCatalog.addMadatoryProperty("mavenTestGoal", "Missing mavenTestGoal property")
        } else {
            propertiesCatalog.addMadatoryProperty("mavenTestGoal", "Defaulting mavenTestGoal property to \"-B -f pom.xml test -Dmaven.test.failure.ignore=true\"", "-B -f pom.xml test -Dmaven.test.failure.ignore=true")
        }
    }
}