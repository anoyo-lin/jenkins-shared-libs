import com.gene.jenkins.JenkinsUtil
import com.gene.workflow.driver.*
import com.gene.parameters.ParametersReader

public Map configuration
public Properties pipelineParams
private ParametersReader paramsReader 

def call(Map configuration) {
   this.configuration = configuration

   defaultPath = JenkinsUtil.getNodeVersionPath(configuration) + ":" + JenkinsUtil.getDefaultPath()

   pipeline {
       agent {
           docker {
               image JenkinsUtil.getAgentDockerImage()
               args "-u deveops:devops --privileged --e PATH=${defaultPath} -v /app/maven/.m2:/home/devops/.m2 -v /var/run/docker.sock:/var/run/docker.sock"
           }
       }
       options {
           timestamps()
           timeout(
               time: configuration.timeoutTime ? configuration.timeoutTime : 6,
               unit: configuration.timeoutUnit ? configuration.timeoutUnit : "HOURS"

           )
           disableConcurrentBuilds()
           buildDiscarder(
               logRotator(
                   numToKeepStr: configuration.logRotatorNumToKeep ? configuration.logRotatorNumToKeep : "30"
                   artifactNumToKeepStr: configuration.logRotatorArtifactNumToKeepStr ? configuration.logRotatorArtifactNumToKeepStr : "3"
               )
           )

        
       }

       environment {
           pipelineName = "operations"
       }
       parameters {
           choice(
               choices:[
                   'pushApp',
                   'startApp',
                   'stopApp',
                   'restartApp',
                   'proxyUpsert',
                   'enableAutoscale',
                   'flyway',
                   'others',
                   'createServices'
               ], description: "create pushApp/startApp/stopApp/restartApp/proxyUpsert/enableAutoScale/flyway/others/createServices action for BAU tasks",
               name: "PROVISIONING_BAU_CONTROL"
           )
       }

       stages {
           stage('Initialization') {
               steps {
                   script {
                       echo "customLibrary: ${this.configuration.customSharedLibrary}"
                       this.pipelineParams = new Properties()
                       this.paramsReader  = new ParametersReader(this)
                       paramsDriverObj = new PipelineParamsDriver(this)
                       paramsDriverObj.main()
                   }
               }
           }
           stage ("ProvisioningCLi Bau Task") {
               when {
                   allOf {
                       expression { return paramsReader.readPipelineParams('useProvisioningCli') }
                       expression { return !paramsReader.readPipelineParams('skipDeploy') }
                       expression { return !paramsReader.readPipelineParams('isLibrary') }
                       anyOf {
                           branch 'master'; branch 'develop*'; branch 'hotfix*'; branch 'release*';
                       }
                   }
               }
               steps {
                   script {
                       provisioningCliBauObj = new ProvisioningCliBauDriver(this)
                       provisioningCliBauObj.main()
                   }
               }
           }
           stage ("concourse Bau Task") {
               when {
                   allOf {
                       expression { return !paramsReader.readPipelineParams('useProvisioningCli') }
                       expression { return !paramsReader.readPipelineParams('skipDeploy') }
                       expression { return !paramsReader.readPipelineParams('isLibrary') }
                       anyOf {
                           branch 'master'; branch 'develop*'; branch 'hotfix*'; branch 'release*';
                       }
                   }
               }
               steps {
                   script {
                       concourseDeploymentDriverObj = new ConcourseDeploymentDriver(this)
                       concourseDeploymentDriverObj.main()
                   }
               }
           }
       }
       post {
           always {
               logger.info('==============Post Notification=============')

               script {
                   postProcessDriverObj = new PostProcessDriver(this)
                   portProcessDriverObj.main()
               }
           }
       }
   }
}