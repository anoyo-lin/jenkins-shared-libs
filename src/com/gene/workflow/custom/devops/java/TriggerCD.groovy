package com.gene.workflow.custom.devops.java

import com.gene.workflow.interfaces.TriggerCDInterface
import com.gene.logger.*
import com.gene.provisioning.Utils

public class TriggerCD implements TriggerCDInterface {
   protected Script scriptObj
   protected Logger logger

   protected String language

   protected downStreamBranch
   protected urlEncodedBranch

   TriggerCD(Script scriptObj) {
       this.scriptObj = scriptObj
       this.logger = new Logger(scriptObj, Level.INFO)
   } 

   public void triggerCDPreOperations() {
       this.language = Utils.getSourceFramework(scriptObj)
       scriptObj.stageStats["${scriptObj.env.STAGE_NAME}"] = 'failed'
       logger.info("Triggering job for ${scriptObj.configuration.downStreamBranch}")
       // when we use jgitflow to merge the current branch to nominal 'master', we need to define masterBranchName.
       // then if we don't hardcode the downStreamBranch in jenkinsFile, we can use the masterBranchName in jenkinsFile
       this.downStreamBranch = scriptObj.configuration.downStreamBranch ? scriptObj.configuration.downStreamBranch : scriptObj.configuration.masterBranchName
       if(this.downStreamBranch.contains('/')) {
           this.urlEncodedBranch = this.downStreamBranch.replace('/', "%2F")
       } else {
           this.urlEncodedBranch = this.downStreamBranch
       }
   }
   public void triggerCDMainOperations() {
       if (scriptObj.JOB_URL.contains('Microservice')) {
           try {
               scriptObj.build job: "../../../${scriptObj.configuration.downStreamEnv}/Microservice/${scriptObj.configuration.downStreamJob}/${this.urlEncodedBranch}", wait: false, parameters: [scriptObj.string(name: 'GIT_BRNACH_TAG', value: "${this.downStreamBranch}")]
           } catch ( Exception err ) {
               scriptObj.build job: "../../../${scriptObj.configuration.downStreamEnv}/${scriptObj.configuration.downStreamJob}/${this.urlEncodedBranch}", wait: false, parameters: [scriptObj.string(name: 'GIT_BRNACH_TAG', value: "${this.downStreamBranch}")]
           }
       } else {
               scriptObj.build job: "../../../${scriptObj.configuration.downStreamEnv}/${scriptObj.configuration.downStreamJob}/${this.urlEncodedBranch}", wait: false, parameters: [scriptObj.string(name: 'GIT_BRNACH_TAG', value: "${this.downStreamBranch}")]
       }
   }
   public void triggerCDPostOperations() {
       logger.info("Trigger CD Complete")
       scriptObj.stageStats["${scriptObj.env.STAGE_NAME}"] = 'successful'
   }
}