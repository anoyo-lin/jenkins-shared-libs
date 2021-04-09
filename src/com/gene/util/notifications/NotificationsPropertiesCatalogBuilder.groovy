package com.gene.util.notifications
import com.gene.util.propertyFile.PropertiesCatalog

class NotificationsPropertiesCatalogBuilder {
    public static build(PropertiesCatalog propertiesCatalog){
        propertiesCatalog.addOptionalProperty("emailJenkinsNotificationsTo", "default is null", null)
        propertiesCatalog.addOptionalProperty("mattermostChannelName", "default is null", null)
        propertiesCatalog.addOptionalProperty("mattermostEndPoint", "default is null", null)
        propertiesCatalog.addOptionalProperty("mattermostText","default is null", null)
        propertiesCatalog.addOptionalProperty("slackChannel", "default is null", null)
        propertiesCatalog.addOptionalProperty("slcakDomain", "default is null", null)
        propertiesCatalog.addOptionalProperty("slackTokenCredentialID", "default is null", null)
        
    }
}