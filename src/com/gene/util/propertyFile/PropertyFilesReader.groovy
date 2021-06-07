package com.gene.util.propertyFile

class PropertyFilesReader {
    static boolean read(Script scriptObj, String propertiesFileName, PropertiesCatalog propertiesCatalog, String commonPropertiesFileName, Properties properties, String propertiesFolderName = "") {
        scriptObj.echo '**************** Starting to process the Properties Files ****************'
        Properties pipelineProperties = new Properties()

        readFile(scriptObj, commonPropertiesFileName, pipelineProperties, propertiesFolderName)
        readFile(scriptObj, propertiesFileName. pipelineProperties, propertiesFolderName)

        scriptObj.echo '**************** Validating the contetn of the Properties Files ****************'
        PropertiesFileValidator propertiesFileValidator = new PropertiesFileValidator(propertiesCatalog)
        boolean valid = propertiesFileValidator.validProperties(pipelineProperties);

        scriptObj.echo propertiesFileValidator.getReportDetails()

        if (valid) {
            for(String name : pipelineProperties.stringPropertyNames()) {
                properties.setProperty(name, pipelineProperties.getProperty(name))
            }
            scriptObj.echo '**************** it\'s valid ****************'
        }

        scriptObj.echo '**************** Done processing the Properties Files ****************'
        
        return valid
    }
    static def private readFile(Script scriptObj, def fileName, Properties pipelineProperties, String propertiesFolderName = "jenkins") {
        def fileExists = scriptObj.fileExists "${filename}"
        if ( propertiesFolderName == null || propertiesFolderName == "" )
        propertiesFolderName = "jenkins"

        scriptObj.echo "Default Folder : [${propertiesFolderName}]"

        scriptObj.echo "[${fileName}] file found: " + fileExists
        if(fileExists) {
            def fileProperties = scriptObj.readProperties file: "${fileName}"
            fileProperties.each { name, value ->
            pipelineProperties.setProperty(name, value)
            }
        } else {
            fileExists = scriptObj.fileExists "${propertiesFolderName}/${fileName}"
            scriptObj.echo "[${propertiesFolderName}/${fileName}] file found: " + fileExists
            if (fileExists) {
                def fileProperties = scriptObj.readProperties file: "${propertiesFolderName}/${fileName}"
                fileProperties.each { name, value ->
                pipelineProperties.setProperty(name, value)
                }
            }
        }
    }
    static boolean propertiesBoolean(Properties pipelineProperties, String propertiesKeyName) {
        if (pipelineProperties.getProperty(propertiesKeyName).toLowerCase().trim() == 'true') {
            return true
        } else if (pipelineProperties.getProperty(propertiesKeyName).toLowerCase().trim() == 'false') {
            return false
        } else {
            return null
        }
    }
}