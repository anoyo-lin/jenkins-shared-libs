import com.gene.jenkins.JenkinsUtil
import com.gene.workflow.drivers.*
import com.gene.fortify.fortifyResult
import com.gene.parameters.ParametersReader
import com.gene.provisioning.*
import com.gene.snyk.snykResult
import com.gene.sonarqube.SonarQubeResult
import com.gene.util.QualityGateCheck

import com.gene.gitflow.GitFlowUtil
import com.gene.util.notification.NotificationPropertiesCatalogBuilder
import com.gene.util.propertyFile.PropertiesCatalog 
import com.gene.util.propertyFile.PropertyFilesReader
import com.gene.cloudfoundry.ConcourseUtil

import com.gene.snyk.SnykRunner


public Map configuration
public Properties pipelineParams
private ParamstersReader paramsReader
public Provision provisionObj
public SonarQubeResult SonarQubeResult
public FortifyResult fortifyResult
public SnykResult snykResult
private Boolean statusCodeScan
private String pipelineSettingsParams

def call(Map configuration) {
    this.configuration = configuration

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
            GIT_BRANCH_TAG = ""

            pipelineName = "release"
            framework = "java"
            flyVars = ""
            statusCodeScan = true
        }


        stages {
            stage("Initialization") {
                steps {
                    script {
                        // sonarQubeResult = new SonarQubeResult()
                        // fortifyResult = new FortifyResult()
                        // snykResult = new SnykResult()
                        pipelineParams = new Properties()
                        paramsReader = new ParametersReader(this)

                        this.configuration.sourceLanguage = "java"

                        env.jenkinsProjectName = JOB_URL.tokenize('/')[4]
                        if (this.configuration.customSharedLibrary) {
                            echo "custom Library is ${this.configuration.customSharedLibrary}"
                        }  else {
                            this.configuration.customSharedLibrary = "deveops"
                        }
                        echo "customLibrary : ${this.configuration.customSharedLibrary}"

                        def paramsDriverObj = new PipelineParamsReaderDriver(this)
                        paramsDriverObj.main()
                    }
                }
            }
            // stage("Unit Test") {
                // when {
                    // expression { return !paramsReader.readPipelineParams('skipUnitTest')}

                // }
                // steps {
                    // script {
                        // unitTestDriverObj = new UnitTestDriver(this)
                        // unitTestDriverObj.main()
                    // }
                // }
            // }

            // stage ("Code Scan") {
                // parallel {
                    // stage ("Code Quality Scan") {
                        // when {
                            // expression { return !paramsReader.readPipelineParams('skipCodeQualityScan')}

                        // }
                        // stages {
                            // stage("Code Scan Operations") {
                                // steps {
                                    // script {
                                        // codeScanDriverObj = new CodeScanDriver(this, 'CodeScanner')
                                    // }
                                // }
                            // }
                        // }
                    // }
                    // stage ("Fortify Scan") {
                        // when {
                            // expression { return !paramsReader.readPipelineParams('skipFortifyScan')}

                        // }
                        // stages {
                            // stage ("Fortify Scan Operations") {
                                // steps {
                                    // script {
                                        // fortifyScanDriverObj = new FortifyScanDriver(this)
                                        // fortifyScanDriverObj.main()
                                    // }
                                // }
                            // }
                        // }
                    // }
                    // stage ("Snyk Scan") {
                        // when {
                            // expression { return !paramsReader.readPipelineParams('skipSnykScan')}

                        // }
                        // stages {
                            // stage ("Snyk Scan Operations") {
                                // steps {
                                    // script {
                                        // snykScanDriverObj = new SnykScanDriver(this)
                                        // snykScanDriverObj.main()
                                    // }
                                // }
                            // }
                        // }
                    // }
                // }
            // }
            // stage ("Upload to artifactory") {
                // when {
                    // allOf {
                        // allOf {
                            // expression { !paramsReader.readPipelineParams('skipUploaddArtifactory')}
                        // }
                        // anyOf { branch 'master'; branch 'develop*'; branch 'hotfix*'; branch 'release'}
                    // }
                // }
                // steps {
                    // script {
                        // artifactoryUploadDriverObj = new ArtifactoryUploadDriver(this)
                        // artifactoryUploadDriverObj.main()
                    // }
                // }
            // }

            // stage ("Donwload from Artifactory") {
                // steps {
                    // script {
                        // artifactoryDownloadObj = new ArtifactoryDownloadDriver(this)
                        // artifactoryDownloadObj.main()
                    // }
                // }
            // }

            // stage ("Deploy to PCF by ProvisioningCli") {
                // when {
                    // allOf {
                        // expression { return paramsReader.readPipelineParams('useProvisioningCli')} 
                        // expression { return !paramsReader.readPipelineParams('skipDeploy')} 
                        // expression { return !paramsReader.readPipelineParams('isLibrary')} 
                        // anyOf { branch 'master'; branch 'develop*'; branch 'hotfix*'; branch 'release*'}
                    // }
                // }
                // stages {
                    // stage("provisioningCli push Operations") {
                        // steps {
                            // script {
                                // provisioningCliPushObj = new ProvisioningCliPushDriver(this)
                                // provisioningCliPushObj.main()
                            // }
                        // }
                    // }
                // }
            // }

            // stage ("Deploy to PCF by Concourse") {
                // when {
                    // allOf {
                        // expression { return !paramsReader.readPipelineParams('useProvisioningCli')} 
                        // expression { return !paramsReader.readPipelineParams('skipDeploy')} 
                        // expression { return !paramsReader.readPipelineParams('isLibrary')} 
                        // anyOf { branch 'master'; branch 'develop*'; branch 'hotfix*'; branch 'release*'}
                    // }
                // }
                // stages {
                    // stage("Concourse push Operations") {
                        // steps {
                            // script {
                                // concourseDeploymentDriverObj = new ConcourseDeploymentDriver(this)
                                // concourseDeploymentDriverObj.main()
                            // }
                        // }
                    // }
                // }
            // }

            // stage ("NewRelic Performance Analysis") {
                // when {
                    // allOf {
                        // expression { return !paramsReader.readPipelineParams('isLibrary')}
                        // expression { return paramsReader.readPipelineParams('enableNewRelicAnalysis')}
                        // expression { return QualityGateCheck.isCodeScanPassed(this)}
                        // anyOf {
                            // branch 'master'; branch 'develop*'; branch 'release*'; branch 'hotfix*'
                        // }
                    // }
                // }
                // stages {
                    // stage("NewRelic main Operations") {
                        // steps {
                            // script {
                                // newRelicDriverObj = new NewRelicDriver(this)
                                // newRelicDriverObj.main()
                            // }
                        // }
                    // }
                // }
            // }

            // stage ("Smoke Test") {
                // when { 
                    // allOf {
                        // expression { return !paramsReader.readPipelineParams('skipSmokeTest') && !paramsReader.readPipelineParams('isLibrary') && !paramsReader.readPipelineParams('skipDeploy')}
                        // expression { return QualityGateCheck.isCodeScanPassed(this)}
                    // }
                    // anyOf { branch 'master'; branch 'develop*'; branch 'release*'; branch 'hotfix*' }
                // }
                // steps {
                    // script {
                        // smoketestDriverObj = new SmokeTestDriver(this)
                        // smokeTestDriverObj.main()
                    // }
                // }
            // }

            stage("Update and Release Branches") {
                when {
                    allOf {
                        expression { return !paramsReader.readPipelineParams('skipUpdateBranches')}
                        expression { return QualityGateCheck.isCodeScanPassed(this)}
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
                    expression { return paramsReader.readPipelineParams('triggerNextPipeline') && QualityGateCheck.isCodeScanPassed(this)}
                    anyOf { branch 'master'; branch 'develop*'; branch 'hotfix*'; branch 'release';}
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
