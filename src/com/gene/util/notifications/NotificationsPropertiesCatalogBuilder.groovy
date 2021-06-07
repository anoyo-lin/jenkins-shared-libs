package com.gene.util.notifications
import com.gene.util.propertyFile.PropertiesCatalog

class NotificationsPropertiesCatalogBuilder {
    public static build(PropertiesCatalog propertiesCatalog){
        propertiesCatalog.addOptionalProperty("emailJenkinsNotificationsTo", "default emailJenkinsNotificationsTo is null", null)
        propertiesCatalog.addOptionalProperty("mattermostChannelName", "default mattermostCHannelName is null", null)
        propertiesCatalog.addOptionalProperty("mattermostEndPoint", "default mattermostEndPoint is null", null)
        propertiesCatalog.addOptionalProperty("mattermostText","default mattermostText is null", null)
        propertiesCatalog.addOptionalProperty("slackChannel", "default slackChannel is null", null)
        propertiesCatalog.addOptionalProperty("slackDomain", "default slackDomain is null", null)
        propertiesCatalog.addOptionalProperty("slackTokenCredentialID", "default slackTokenCredentialID is null", null)
        
    }
}