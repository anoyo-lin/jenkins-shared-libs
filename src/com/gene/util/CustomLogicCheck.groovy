package com.gene.util

class CustomLogicCheck implements Serializable {
    public static CustomLogicCheck(Script scriptObj) {
        def envParams = [
            'Gene': ['dev', 'sit', 'uat', 'uat-dr', 'preprod-dr', 'preprod', 'prod', 'prod-dr', 'qa'],
            'Gene-Devops': ['dev', 'sit', 'uat', 'uat-dr', 'preprod-dr', 'preprod', 'prod', 'prod-dr', 'qa']
        ]
        scriptObj.env.jenkinsProjectName = scriptObj.env.JOB_URL.tokenize('/')[4]
        def isSpecialPipeline = envParams.containKey(scriptObj.env.jenkinsProjectName)
        scriptObj.echo "isSpecialPipeline is: ${isSpecialPipeline}"
        if(scriptObj.configuration.customSharedLibrary) {
            scriptObj.echo "customeSharedLibrary is ${scriptObj.configuration.customSharedLibrary}"

        } else if(isSpecialPipeline) {
            scriptObj.configuration.customeSharedLibrary = 'special'
        }
        scriptObj.echo "customSharedLibrary: ${scriptObj.configuration.customSharedLibrary}"
    }
}