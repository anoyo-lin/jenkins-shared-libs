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
            any
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
            ARTIFACTID = 'gene-digits-ocr'
            VERSION = '0.0.1'
            
        }

        stages {
            stage("build docker image & upload to registry") {
                steps {
                    script {
                        sh """
                        #!/bin/bash
                        pip3 install -r requirements.txt
                        python3 manage.py test
                        """
                        def public_ip = sh(command: 'curl http://100.100.100.100/metadata', returnStdout: true)
                        def harbor_port = '30002'
                        docker build -t "${public_ip}:${harbor_port}/library/${ARTIFACTID}:${VERSION}" .
                        docker push "${public_ip}:${harbor_port}/library/${ARTIFACTID}:${VERSION}"
                    }
                }
            }
            stage("login to public cloud") {
                steps {
                    script {
                        withCredentials([usernamePassword(credentialsId: 'ALIYUN_CREDENTIAL', usernameVariable: 'ALIYUN_ACCESS', passwordVariable: 'ALIYUN_SECRET')]) {
                            def cipher = encrypted_cipher
                            sh """
                            git clone kms_client
                            cd kms_client && python3.6 kms_client.py --ak ${env.ALIYUN_ACCESS} --sk ${env.ALIYUN_SECRET} --cipher ${cipher}
                            """
                        }

                    }
                }
            }
            stage("clone Helm Chart & Upgrade") {
                steps {
                    script {
                        GitUtil.cloneRepository(
                            this,
                            configuration.pipelineRepository,
                            scriptObj.scm.getUserRemoteConfigs()[0].getCredentialId(),
                            [[name: "master"]],
                            'helm_chart'
                        )
                        withCredentials([usernamePassword(credentialsId: 'HARBOR_SECRET', usernameVariable: 'HARBOR_NAME', passwordVariable: 'HARBOR_PASSWORD')]) {
                            sh """
                            helm repo add gene-test http://${public_ip}:${harbor_port}/chartrepo/library
                            helm repo update
                            sed docker image name
                            sed chart version
                            helm plugin install https://github.com/chartmuseum/helm-push
                            helm push --username=${env.HARBOR_USER} --password=${env.HARBOR_PASSWOD} gene-test/${ARTIFACTID}-helm-${VERSION}.tgz helm_chart
                            """
                        }
                        sh """
                        helm upgrade -i gene-test gene-test/gene-digits-ocr:0.0.1
                        """


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