public Map configuration
public Properties pipelineParams
private Boolean statusCodeScan
private Boolean isSpecialPipeline

def call(Map configuration) {
    this.configuration = configuration
    this.configuration.sourceLanguage = 'python'


    if (configuration.skipBranchIndexing == null || configuration.skipBranchIndexing == true) {
                // execute this before anything else, including requesting any time on an agent
        print currentBuild.rawBuild.getCauses().toString()
        if (currentBuild.rawBuild.getCauses().toString().contains("BranchIndexingCause")) {
            print "[INFO] Build skipped due to trigger being Branch Indexing"
            currentBuild.result = 'ABORTED' // optional, gives a better hint to the user that it's been skipped, rather then the default which shows it's successful
            return
        }
    }

    pipeline {
        agent {
            docker {
                // docker helm alicloud cli 
                image devops-ci-image:0.1
                args "-u devops:devops --privileged -v /var/run/docker.sock:/var/run/docker.sock"
            }
        }
        options {
            timestamps()
            disableConcurrentBuilds()
            timeout(
                time: configuration.timeoutTime ? configuration.timeoutTime : 6,
                unit: configuration.timeoutUnit ? configuration.timeoutUnit : "HOURS"
            )
            buildDiscarder(logRotator(
                numToKeepStr: configuration.logRotatorNumToKeep ? configuration.logRotatorNumToKeep : "30",
                artifactNumToKeepStr: configuration.logRotatorArtifactNumToKeepStr ? configuration.logRotatorArtifactNumToKeepStr : "3"
            ))
        }

        environment {
            pipelineName = "deployment"
            framework = "python"
        }

        stages {
            stage("build docker image & upload to registry") {
                steps {
                    script {

                    }
                }
            }
            stage("login to public cloud") {
                steps {
                    script {

                    }
                }
            }
            stage("clone Helm Chart & Upgrade") {
                steps {
                    script {

                    }
                }
            }
        }

        post {
            always{
                script {
                    echo "building successful"
                }
            }
        }
    }
}