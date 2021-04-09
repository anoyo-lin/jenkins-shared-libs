package com.gene.workflow.drivers

import com.gene.logger.*
/*
 * import com.gene.parameters.ParametersReader
 * TODO: to understand why we can't import the pipelineParams before the pipelineParamsReader
 */

 class BaseDriver implements Serializable {
     Script scriptObj
     String className
     String framework

     BaseDriver(Script scriptObj, String className) {
         this.scriptObj = scriptObj
         this.className = className
         this.framework = framework
         // this.looger = new Logger(scriptObj, Level.INFO)
     }
     def classReflection() {
         Logger logger = new Logger(scriptObj, Level.INFO)
         // provisioningCliPushDriver = Class.forName("com.gene.workflow.custom.devops.UnitTest")
         def lib_name = ''
         def baseObj = null
         def temp_name = ''
         for (def env_key in scriptObj.env.getEnvironment()) {
             if ( env_key.key.toString().contains("library") ) {
                 logger.info("${env_key.key.toString()}=${env_key.value.toString()}")
                 if (env_key.key.toString().split('\\.')[1] != 'pipeline') {
                     lib_name = env_key.key.toString().split('\\.')[1]
                 } else {
                     temp_name = env_key.key.toString().split('\\.')[1]
                 }
             }
         }
         if ( lib_name == '' ) {
             lib_name = temp_name
         }
         def global_libs = scriptObj.library(lib_name)
         def customSharedLibrary = null

         if (scriptObj.configuration.customSharedLibrary != null || scriptObj.pipelineParams.customSharedLibrary !=null) {
             customSharedLibrary = scriptObj.configuration.customSharedLibrary?scriptObj.configuration.customSharedLibrary:scriptObj.pipelineParams.customSharedLibrary
         }

         if (customSharedLibrary) {
             logger.info("com.gene.workflow.custom.${customSharedLibrary}.${framework}.${className}")
             baseObj = global_libs.com.gene.workflow.custom."${customSharedLibrary}"."${framework}"."${className}".new(scriptObj)
         } else {
             logger.info("com.gene.workflow.custom.devops.${framework}.${className}")
             baseObj = global_libs.com.gene.workflow.custom.devops."${framework}"."${className}".new(scriptObj)
             // def type = baseObj.getClass()
             // logger.info("com.gene.workflow.custom.devops.${className} is : ${type}")
         }
         return baseObj
     }

     private void main() {
         def baseObj = classReflection()
         baseObj.preOperations()
         baseObj.mainOperations()
         baseObj.postOperations()
     }
 }