package com.gene.parameters

import com.gene.ap.gitflow.GitUtil
import com.gene.logger.*
import com.gene.util.notification.NotificationPropertiesCatalogBuilder
import com.gene.util.propertyFile.PropertiesCatalog
import com.gene.util.propertyFile.PropertiesFileReader

class ParametersReaderAks extends ParametersReader implements Serialzable {
    ParametersReaderAks(Script scriptObj) {
        super(scriptObj)
    }
    @Override
    public PropertiesCatalog buildPropertiesCatalog(PropertiesCatalog propertiesCatalog) {
        propertiesCatalog.addMandatoryProperty("AZURE_SERVICE_PRINCIPAL_ID", "Missing the Azure service Principal ID")
        propertiesCatalog.addMandatoryProperty("AZURE_TENANT_ID", "Missing the Azure Tenant ID")
        propertiesCatalog.addMandatoryProperty("AZURE_SUBSCRIPTION_ID", "Missing the Azure Subscription ID")
        propertiesCatalog.addMandatoryProperty("AZURE_RESOURCE_GROUP", "Missing the Azure resource group")
        propertiesCatalog.addMandatoryProperty("AZURE_AKS_CLUSTER_NAME", "Missing the Azure cluseter name") 
        propertiesCatalog.addMandatoryProperty("AZURE_KEYVAULT_NAME", "Missing the Azure key Vault name") 
        propertiesCatalog.addMandatoryProperty("EMAIL_RECIPIENT_NAME", "Missing the email recipient name") 
        propertiesCatalog.addMandatoryProperty("K8S_TARGET_NAMESPACE", "Missing k8s target namespace") 
        propertiesCatalog.addMandatoryProperty("K8S_PATH", "Missing the k8s path", "k8s")
        propertiesCatalog.addMandatoryProperty("HELM_CHART_VERSION", "Missing helm chart version")
        propertiesCatalog.addMandatoryProperty("HELM_CHART_PATH", "Missing the helm chart path", "helm")
        propertiesCatalog.addMandatoryProperty("HELM_TEMPLATE_REPO", "Missing the helm template repo", "ssh://git.git.ap.gene.com:8080/rsf/rsf-helm-template.git")
        propertiesCatalog.addMandatoryProperty("HELM_TEMPLATE_BRANCH", "Missing the helm template branch", "master")
        return propertiesCatalog
       
    }

}