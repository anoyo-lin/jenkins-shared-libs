package com.gene.snyk

import com.gene.util.propertyFile.PropertiesCatalog

class SnykPropertiesCatalogBuilder implements Serializable {
    public static build(PropertiesCatalog propertiesCatalog) {
        propertiesCatalog.addOptionalProperty("snykDetectionDepth", "snyk detect depth for big project")
        propertiesCatalog.addOptionalProperty("snykExcludeDir", "snyk scan path exclusion")
        propertiesCatalog.addOptionalProperty("snykOrg", "snyk organization for different project")
        propertiesCatalog.addOptionalProperty("snykFile", "set the custom package file")
        propertiesCatalog.addOptionalProperty("snykSeverityThreshold", "set severity threshold to low/medium/high")

    }
}