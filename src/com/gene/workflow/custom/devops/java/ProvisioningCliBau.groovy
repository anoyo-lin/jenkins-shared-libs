package com.gene.workflow..custom.devops.java

import com.gene.provisioning.*
import com.gene.workflow.interfaces.ProvisioningCliInterface
import com.gene.artifactory.*

class ProvisioningCliBau extends ProvisioningCliPush implements ProvisioningCliInterface {
    public ProvisioningCliBau(Script scriptObj){
        super(scriptObj)
    }
    protected void replaceWorkSpace(String pipelineRepository, String pipelineRepoBranches) {
        scriptObj.sh "rm -fr golden_pipeline"
        GitUitl.cloneRepository(
            scriptObj,
            pipelineRepository,
            scriptObj.scm.getUserRemoteConfigs()[0].getCredentialsId(),
            pipelineRepoBranches,
            pipelinePropertiesFolder = 'golden_pipeline'
        )
    }
    protected List getMicroServicesList() {
        def microServicesLst = new ArrayList()
        def yaml = scriptObj.readFile file: "./${scriptObj.pipelineParams.microServicesLstYaml}"
        def micorServicesYaml = scriptObj.readYaml text: "${yaml}"
        if (microServicesLstYaml.microServices){
            for (Map service: microServicesLstYaml.microServices) {
                microServicesLst.add(service)
            }
            return microServicesLst
        } else {
            throw new Exception("no valid microServicesLstYaml's content.")
        }
    }
    protected void bulkPushing() {
        def microServicesLst = getMicroServicesList()
        for ( def microService : microServicesLst) {
            replaceWorkSpace(microService.repoUrl, microService.branch)
            super.provisionObjInit()
            super.startAppOnPcf()
        }
    }
    protected void buldRestarting() {
        def microServicesLst = getMicroServicesList()
        for (def microService : microServicesLst) {
            super.restartAppOnPcf(microService.appName)
        }
    }
    @Override
    public void provisioningCliMainOperations() {
        if ( Utils.checkIfProd(provisionObj.foundation, provisionObj.org)) {
            def chosenRef = null
            scriptObj.timeout(time: 5, unit: "MINUTES") {
                chosenRef = scriptObj.input message: "please input change ticket or incident ticket to proceed", ok: "proceed", paramters: [
                    scriptObj.string(
                        description: "this is change Ticket input field",
                        name: "changeTicket"
                    ),
                    scriptObj.string(
                        description: "this is incident ticket input field",
                        name: "incidentTicket"
                    )
                ]
            }
            scriptObj.env.CHAGNE_TICKET_NO = chosenRef['changeTicket']
            scriptObj.env.INCIDENT_TICKET_NO = chosenRef['incidentTicket']
        }
        if (scriptObj.params.PROVISIONING_BAU_CONTROL) {
            if ( scriptObj.params.PROVISIONING_BAU_CONTROL == 'restartApp') {
                super.restartAppOnPcf()
            } else if ( scriptObj.params.PROVISIONING_BAU_CONTROL == 'bulkPushing') {
                bulkPushing()
            } else if ( scriptObj.params.PROVISIONING_BAU_CONTROL == 'bulkRestarting') {
                bulkRestarting()
            } else if ( scriptObj.params.PROVISIONING_BAU_CONTROL == 'rollbackApp') {
                super.rollBackAppOnPcf()
            } else if ( scriptObj.params.PROVISIONING_BAU_CONTROL == 'proxyUpsert') {
                super.upsertAppOnPcf()
            } else if ( scriptObj.params.PROVISIONING_BAU_CONTROL == 'enableAutoScale') {
                super.enableAutoScale()
            } else if ( scriptObj.params.PROVISIONING_BAU_CONTROL == 'createServices') {
                super.createServices()
            } else if ( scriptObj.params.PROVISIONING_BAU_CONTROL == 'deleteService') {
                scriptObj.timeout(time: 20, unit: "MINUTES") {
                    def serviceName = scriptObj.input(
                        message: "please specified the service name you want to delete",
                        ok: "continue"
                    )
                }
                super.deleteServiceOnPcf(serviceName)
            } else if ( scriptObj.params.PROVISIONING_BAU_CONTROL == "startApp" ) {
                super.startAppOnPcf()
            } else if ( scriptObj.params.PROVISIONING_BAU_CONTROL == 'stopApp') {
                super.stopAppOnPcf()
            } else if ( scriptObj.params.PROVISIONING_BAU_CONTROL == 'deleteApp') {
                super.deleteAppOnPcf()
            } else if ( scriptObj.params.PROVISIONING_BAU_CONTROL == 'pushApp') {
                if (
                    provisionObj.framework.contains('java')
                ) {
                    def fileName = ArtifactoryUtil.downloadArtifact(scriptObj)
                    scriptObj.sh """ if [[ ! -d target ]]; then mkdir target; fi
                    cp ${fileName} target/
                    """
                    super.packageApplication()
                    super.pushAppOnPcf()
                }
            } else {
                throw new Exception("invalid parameters for Provisioning BAU tasks")
            }
        }
    }
}