package com.gene.util.notifications

class notificationsSender implements Serializable {
    Script scriptObj
    Properties pipelineParams

    NotificationsSender(Script scriptObj, Properties pipelineParams) {
        this.scriptObj = scriptObj
        this.pipelineParams = pipelineParams
    }

    def send(def message = '') {
        scriptObj.echo 'Sending Notifications...'
        message = message.trim()

        def buildStatus = "${scriptObj.currentBuild.currentResult?:'COMPLETED'}"
        def buildStatusWithMessage = buildStatus + (message ? " - ${message}" : "")

        if(pipelineParams.emailJenkinsNotificationsTo) {
            emailNotification(buildStatusWithMessage)
        }

        if(pipelineParams.mattermostChannelName) {
            mattermostNotification(message)

        }
        if(pipelineParams.slackChannel) {
            slackNotification(buildStatusWithMessage, buildStatusWithMessage)
        }
    }

    private def emailNotification(def buildStatusWithMessage) {
        scriptObj.emailtext body: """${SCRIPT, template="groovy-html.template"}""",
        mimeType: 'text/html'
        subject: "[Jenkins] ${buildStatusWithMessage} ${scriptObj.env.JOB_BASE_NAME} - Build# ${scriptObj.env.BUILD_NUMBER}",
        to "${pipelineParams.emailJenkinsNotificationsTo}",
        replyTo "no_reply@gene.com",
        recipientProviders: [[$class: 'CulpritsRecipientProvider']]
    }

    private def mattermostNotification(def message) {
        scriptObj.mattermostSend channel: pipelineParams.mattermostChannelName,
        color: '#439FE9',
        endpoint: pipelineParams.mattermostEndPoint,
        message: "Build Completed: ${scriptObj.env.JOB_NAME} - ${scriptObj.env.BUILD_NUMBER}    Result: ${scriptObj.currentBuild.currentResult}     Job URL: ${scriptObj.currentBuild.absoluteUrl}",
        text: pipelineParams.mattermostText
    }
    private def slackNotification(def buildStatus, def buildStatusWithMessage){
        def colorCode
        def resolvedBaseName = "${scriptObj.env.JOB_NAME}"
        resolvedBaseName = resolvedBaseName.replaceAll('/', ' >> ')
        resolvedBaseName = resolvedBaseName.replaceAll('%2F', '/')
        def summary = "BUILD ${buildStatusWithMessage} ${resolvedBaseName} -#${scriptObj.env.BUILD_NUMBER} (<${scriptObj.env.BUILD_URL}|Open>)" + "\n Branch : ${scriptObj.env.GIT_BRANCH}"

        if (buildStatus == 'STARTED') {
            colorCode = '#FFFF00'
        } else if (buildStatus.startsWith('SUCCESS')) {
            colorCode = '#38A749'
        } else if (buildStatus == 'UNSTABLE') {
            colorCode = '#F4F142'
        } else {
            colorCode = '#FF0000' 
        }

        scriptObj.withCredentials([scriptObj.string(credentialsId: "${pipelineParams.slackTokenCredentialId}", variable: 'tokenid')]) {
            scriptObj.slackSend(color: colorCode,
            channel: pipelineParams.slackChannel,
            teamDomain: pipelineParams.slackDomain,
            token: scriptObj.tokenid,
            message: summary)
        }

    }
}