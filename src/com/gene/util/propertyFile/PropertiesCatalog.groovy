package com.gene.util.propertyFile
import com.cloudbees.groovy.cps.*

/*
* contains the definition of the all properties supproted by a pipeline
*/

class PropertiesCatalog implements java.io.Serializable {
    private def propertiesCatalog = [:]

    @NonCPS
    public void addMandatoryProperty(String name, String missingMessage) {
        propertiesCatalog[name] = new MandatoryProperty(name, missingMessage);
    }
    @NonCPS
    public void addOptionalProperty(String name, String missingMessage, String defaultValue) {
        propertiesCatalog[name] = new OptionalProperty(name, missingMessage, defaultValue);
    }
    @NonCPS
    public def getPropertyDefinition(String name) {
        return propertiesCatalog[name]
    }
    @NonCPS
    public def getPropertyDefinitions() {
        return propertiesCatalog.values()
    }
    @NonCPS
    public int size() {
        propertiesCatalog.size()
    }
}