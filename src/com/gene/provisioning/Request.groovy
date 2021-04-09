package com.gene.provisioning

/**
*
* the body of the provisioning API and the artfifact metadata
*
**/

class Request implements Serializable {
    String id
    String language
    String status
    String fileName
    String action
    String serviceId
    String serviceStatus
    String buildpack
    String applicationName
    String sonarGovernance
    String fortifyGovernance
    String blackduckGovernance
    String applicationNewRoutes
    String stack
    String env
    String routes
    String newRoutes
    String oldRoutes
}