import com.gene.workflow.drivers.*

import com.gene.sonarqube.SonarQubeResult
import com.gene.fortify.fortifyResult
import com.gene.snyk.snykResult

import com.gene.parameters.ParametersReader
import com.gene.jenkins.JenkinsUtil
import com.gene.provisioning.*
import com.gene.util.QualityGateCheck
import com.gene.util.CustomLogicCheck

import com.gene.util.reports.PipelineReport



public Map configuration
public Properties pipelineParams
private ParamstersReader paramsReader
public Provision provisionObj
public SonarQubeResult SonarQubeResult
public FortifyResult fortifyResult
public SnykResult snykResult
private Boolean statusCodeScan
private Boolean isSpecialPipeline
private PipelineReport pipelineReport

def call(Map configuration) {
    this.configuration = configuration
    this.configuration.sourceLanguage = 'java'

    CustomLogicCheck.customLogicCheck(this)

    defaultPath = JenkinsUtil.getNodeVersionPath(configuration) + ":" + JenkinsUtil.getDefaultPath()
    def javaHomePath = JenkinsUtil.getJaveHomePath(configuration)

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
                image JenkinsUtil.getAgentDockerImage()
                args "-u devops:devops --privileged -e PATH=${defaultPath} -v /app/maven/.m2:/home/devops/.m2 -v /var/run/docker.sock:/var/run/docker.sock"
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
            ARTIFACTID = readMavenPom().getArtifactId()
            VERSION = readMavenPom().getVersion()
            GROUPID = readMavenPom().getGroupId()
            ARTIFACT_PACKAGING = readMavenPom().getPackaging()
            JAVA_HOME = "${javaHomePath}"
            pipelineName = "ci"
            framework = "java"
            /* if you define the environment here, and you couldn't define 
            the envVars in external class, for example scriptObj.env.stageTail = 'gene'
            and scriptObj.echo "${scriptObj.env.stageTail}" = null
            */
            // stageTail = ''
        }

        stages {
            stage("Initialization") {
                steps {
                    script {
                        sonarQubeResult = new SonarQubeResult()
                        fortifyResult = new FortifyResult()
                        snykResult = new SnykResult()
                        pipelineReport = new PipelineReport()
                        pipelineParams = new Properties()
                        provisionObj = new Provision()
                        paramsReader = new ParametersReader(this)
                        paramsDriverObj = new PipelineParamsReaderDriver(this)
                        paramsDriverObj.main()
                    }
                }
            }
            stage("Unit Test") {
                when {
                    expression { return !paramsReader.readPipelineParams('skipUnitTest')}
                }
                steps {
                    script {
                        unitTestDriverObj = new UnitTestDriver(this)
                        unitTestDriverObj.main()
                    }
                }
            }

            stage ("Code Scan") {
                parallel {
                    stage ("Code Quality Scan") {
                        when {
                            expression { return !paramsReader.readPipelineParams('skipCodeQualityScan')}
                        }
                        stages {
                            stage("Code Scan Operations") {
                                steps {
                                    script {
                                        codeScanDriverObj = new CodeScanDriver(this, 'CodeScanner')
                                    }
                                }
                            }
                        }
                    }
                    stage ("Fortify Scan") {
                        when {
                            expression { return !paramsReader.readPipelineParams('skipFortifyScan')}
                        }
                        stages {
                            stage ("Fortify Scan Operations") {
                                steps {
                                    script {
                                        fortifyScanDriverObj = new FortifyScanDriver(this)
                                        fortifyScanDriverObj.main()
                                    }
                                }
                            }
                        }
                    }
                    stage ("Snyk Scan") {
                        when {
                            expression { return !paramsReader.readPipelineParams('skipSnykScan')}
                        }
                        stages {
                            stage ("Snyk Scan Operations") {
                                steps {
                                    script {
                                        snykScanDriverObj = new SnykScanDriver(this)
                                        snykScanDriverObj.main()
                                    }
                                }
                            }
                        }
                    }
                }
            }
            stage ("Upload to artifactory") {
                when {
                    allOf {
                        expression { return !paramsReader.readPipelineParams('skipUploaddArtifactory') }
                        expression { return QualityGateCheck.isCodeScanPassed(this) }
                        expression { return JenkinsUtil.getBranchesFilter(this) }
                    }
                }
                steps {
                    script {
                        artifactoryUploadDriverObj = new ArtifactoryUploadDriver(this)
                        artifactoryUploadDriverObj.main()
                    }
                }
            }

            stage ("Deploy to PCF by ProvisioningCli") {
                when {
                    allOf {
                        expression { return paramsReader.readPipelineParams('useProvisioningCli') } 
                        expression { return !paramsReader.readPipelineParams('skipDeploy') } 
                        expression { return !paramsReader.readPipelineParams('isLibrary') } 
                        expression { return QualityGateCheck.isCodeScanPassed(this) }
                        expression { return JenkinsUtil.getBranchesFilter(this) }
                    }
                }
                stages {
                    stage("provisioningCli push Operations") {
                        steps {
                            script {
                                provisioningCliPushObj = new ProvisioningCliPushDriver(this)
                                provisioningCliPushObj.main()
                            }
                        }
                    }
                }
            }

            stage ("Deploy to PCF by Concourse") {
                when {
                    allOf {
                        expression { return !paramsReader.readPipelineParams('useProvisioningCli') } 
                        expression { return !paramsReader.readPipelineParams('skipDeploy') } 
                        expression { return !paramsReader.readPipelineParams('isLibrary') } 
                        expression { return QualityGateCheck.isCodeScanPassed(this) }
                        expression { return JenkinsUtil.getBranchesFilter(this) }
                    }
                }
                stages {
                    stage("Concourse push Operations") {
                        steps {
                            script {
                                concourseDeploymentDriverObj = new ConcourseDeploymentDriver(this)
                                concourseDeploymentDriverObj.main()
                            }
                        }
                    }
                }
            }

            stage ("NewRelic Performance Analysis") {
                when {
                    allOf {
                        expression { return paramsReader.readPipelineParams('enableNewRelicAnalysis')}
                        expression { return !paramsReader.readPipelineParams('skipDeploy') } 
                        expression { return !paramsReader.readPipelineParams('isLibrary') } 
                        expression { return QualityGateCheck.isCodeScanPassed(this) }
                        expression { return JenkinsUtil.getBranchesFilter(this) }
                    }
                }
                stages {
                    stage("NewRelic main Operations") {
                        steps {
                            script {
                                newRelicDriverObj = new NewRelicDriver(this)
                                newRelicDriverObj.main()
                            }
                        }
                    }
                }
            }

            stage ("Smoke Test") {
                when { 
                    allOf {
                        expression { return !paramsReader.readPipelineParams('skipSmokeTest') }
                        expression { return !paramsReader.readPipelineParams('isLibrary') }
                        expression { return !paramsReader.readPipelineParams('skipDeploy') }
                        expression { return QualityGateCheck.isCodeScanPassed(this)}
                        expression { return JenkinsUtil.getBranchesFilter(this) }
                    }
                }
                steps {
                    script {
                        smoketestDriverObj = new SmokeTestDriver(this)
                        smokeTestDriverObj.main()
                    }
                }
            }

            stage("Update Branch and Release Artifacts") {
                when {
                    allOf {
                        expression { return !paramsReader.readPipelineParams('skipUpdateBranches')}
                        expression { return QualityGateCheck.isCodeScanPassed(this)}
                        expression { return JenkinsUtil.getBranchesFilter(this) }
                    }
                }
                steps {
                    script {
                        gitFlowUpdateDriverObj = new GitFlowUpdateDriver(this)
                        gitFlowUpdateDriverObj.main()
                    }
                }
            }

            stage ("Trigger CD") {
                when {
                    allOf {
                        expression { return paramsReader.readPipelineParams('triggerNextPipeline') }
                        expression { return QualityGateCheck.isCodeScanPassed(this) }
                        expression { return JenkinsUtil.getBranchesFilter(this) }
                    }
                }
                steps {
                    script{
                        triggerCDDriverObj = new TriggerCDDriver(this)
                        triggerCDDriverObj.main()
                    }
                }
            }
        }
        post {
            always{
                script {
                    postProcessDriverObj = new PostProcessDriver(this)
                    postProcessDriverObj.main()
                }
            }
        }
    }
}
