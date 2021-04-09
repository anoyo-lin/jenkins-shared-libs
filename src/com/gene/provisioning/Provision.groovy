package com.gene.provisioning

import java.lang.reflect.Field

/**
  *
  * the interface of provisioning API running
  *
  */

class Provision implements Serializable {
    String id 

    String api
    String org
    String space
    String foundation
    String teamEmail
    
    String appName
    String framework
    String buildpack
    String sourcePath
    String services
    String routes
    String environmentVariables
    String stack
    String deleteOldApp
    String createDeploymentMarker
    String healthCheckType
    String healthCheckHttpEndpoint
    String healthCheckTimeout
    String noStart

    String manifestFileName
    String appNewName
    String appOldName
    String newRoutes

    public String toString() {
        StringBuilder result = new StringBuilder()
        String newline = '\n'

        result.append(this.getClass().getName())
        result.append(" Object {")
        result.append(newline)

        Field[] fields = this.getClass().getDeclaredFields()

        for ( Field field : fields ) {
            field.setAccessible(true)
            result.append(" ")
            try {
                result.append(field.getName())
                result.append(": ")
                result.append(field.get(this))
            } catch ( Exception err ) {
                System.out.println(err)
            }
            result.append(newline)
        }
        result.append("}")
        return result.toString()
    }
}