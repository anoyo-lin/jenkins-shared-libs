package com.gene.workflow.drivers

public class AksPushDriver extends BaseDriver {
   AksPushDriver(Script scriptObj) {
       super(scriptObj, 'AksPusher')
   } 
   AksPushDriver(Script scriptObj, String className) {
       super(scriptObj, className)
   }
   AksPushDriver(Script scriptObj, String className, String framework) {
       super(scriptObj, className, framework)
   }
   @Override
   public void main() {
       def aksPusherObj = super.classReflection()
       aksPusherObj.aksPushPreOperations()
       aksPusherObj.aksPushMainOperations()
       aksPusherObj.aksPushPostOperations()
   }
}