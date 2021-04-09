package com.gene.util.propertyFile

import java.util.List
import java.util.Properties
import com.cloudbees.groovy.cps.*

/*
* Validates and fixes the properties provided to a pipeling (against what the pipeline defined as supported properties).
*/
class PropertiesFileValidator implements java.io.Serializable {
    final private List<String> report = new ArrayList<String>();
    final private PropertiesCatalog catalog;

    public PropertiesFileValidator(PropertiesCatalog catalog) {
        this.catalog = catalog;
    }
    @NonCPS
    public boolean ValidateProperties(PropertiesCatalog properties) {
        final def infos = []
        final def errors = []
        // Validate that all mandatory values are provided and default missing values when possible
        for (def propertyDefinition : catalog.getPropertyDefinition()) {
            String value = properties.getProperty(propertyDefinition.getName())

            if(value == null || value.trim().isEmpty()) {
                if(propertyDefinition.mandatory) {
                    errors.add("[ERROR]:" + propertyDefinition.getMissingMessage());
                } else { // Optional, set to default value
                if (propertyDefinition.defaultValue != null){
                    properties.setProperty(propertyDefinition.name, propertyDefinition.defaultValue)

                }
                infos.add(propertyDefinition.getMissingMessage())
                }
            } else {
                infos.add("Provided value for " + propertyDefinition.getName() + "=" + value);
            }
        }
        for (String propertyName : properties.stringPropertyNames()) {
            if(!catalog.getPropertyDefinition(propertyName)) {
                infos.add("[WARNING]: " + propertyName + " property is unknown by this pipeline. This is probaly a deprecated property or there is a typo in ther property name.");
            }
        }
        report.addAll(errors);
        report.addAll(infos);

        return !(errors.size() > 0);
    }
    @NonCPS
    public String getReportDetails() {
        def retval = "--------------------------- Validation Report ---------------------------"
        for (def reportLine : report) {
            retval = retval + "\n" + reportLine
        }
        return retval
    }

}