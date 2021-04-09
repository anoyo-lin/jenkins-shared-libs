package com.gene.workflow.custom.devops.java

import com.gene.workflow.interfaces.ArtifactoryDownloadInterface
import com.gene.logger.*
import com.gene.artifactory.*
import com.gene.provisioning.Utils
import com.gene.dashboard.DashboardUtil

public class ArtifactoryDownload implements ArtifactoryDownloadInterface {
    protected Script scriptObj
    protected Logger logger
    protected String framework

    ArtifactoryDownload(Script scriptObj) {
        this.scriptObj = scriptObj
        this.logger = new Logger(scriptObj, Level.INFO)
    }

    void artifactoryDownloadPreOperations() {
        scriptObj.stageStats["${scriptObj.env.STAGE_NAME}"] = 'failed'
        logger.info("=========== Prepare for downloading from artifactory==============")
        this.framework = Utils.getSourceFramework(scriptObj)
        // def sel_package_ver
        // scriptObj.sh "git checout -B ${scriptObj.env.BRANCH_NAME} --track origin/${scriptObj.env.BRANCH_NAME} && git pull origin ${scriptObj.env.BRANCH_NAME} --allow-unrelated-histories && git fetch --all --tags --prune"
        scriptObj.sh "git checkout -B ${scriptObj.env.BRANCH_NAME} && git pull origin ${scriptObj.env.BRANCH_NAME} --allow-unrelated-histories && git fetch --all --tags --prune"
        if (!scriptObj.params.GIT_BRANCH_TAG.equals('') && scriptObj.params.GIT_BRANCH_TAG != null) {
            scriptObj.sh "git checkout ${scriptObj.params.GIT_BRANCH_TAG}"
            // sel_package_ver = scriptObj.params.GIT_BRANCH_TAG
        } else {
            // def chosenRef
            def git_tags
            scriptObj.sh "git tag --list v* --sort=-version:refname > git-tags.txt"
            git_tags = scriptObj.readFile "git_tags.txt"
            scriptObj.timeout(time: 5, unit: "MINUTES") {
                scriptObj.chosenRef = scriptObj.input message: "Version to deploy", ok: "Deploy", parameters:[
                    scriptObj.choice(choice: git_tags, name: "Git Tags", description: "Select the git tag for deployment."),
                    scriptObj.string(
                        description: "This is going to override the selected tag if provided",
                        name: "Branch name"
                    )
                ]
            }

            if (scriptObj.chosenRef["Branch name"].equals("")) {
                scriptObj.sh "git checkout ${scriptObj.chosenRef['Git Tag']}"
                // sel_package_ver = scriptObj.chosenRef['Git Tag']
            } else {
                scriptObj.sh "git checkout ${scriptObj.chosenRef['Branch name']}"
                scriptObj.sh "git reset --hard origin/${scriptObj.chosenRef['Branch name']}"
                // sel_package_ver = scriptObj.chosenRef['Branch name']
            }
        }
        // scriptObj.env.ARTIFACTID = scriptObj.readMavenPom().getArtifactId()
        // scriptObj.env.VERSION = scriptObj.readMavenPom().getVersion()
        // scriptObj.env.GROUPID = scriptObj.readMavenPom().getGroupId()
        // scriptObj.env.GIT_BRANCH = sel_package_ver

    }
    void artifactoryDownloadMainOperations() {
        if (
            framework == 'java' ||
            framework == 'javaGradle' ||
            framework == 'javaMaven'
        ) {
            def fileName = ArtifactoryUtil.downloadArtifact(scriptObj)
            scriptObj.sh """if [[ ! -d target ]]; then mkdir target; fi
            cp ${fileName} target/
            """
        } else {
            throw new Exception("please check project based on Java?")
        }
    }
    void artifactoryDownloadPostOperations() {
        logger.info("================Complete downloading from Artifactory ================")
        def files = scriptObj.findFiles glob: 'target/*.jar'
        def exists = file.length > 0
        if (exists) {
            def build_commit_lst = scriptObj.sh(script: "for i in \$(ls target/*.jar); do unzip -p \$i \$(unzip -l $i |grep git | awk '{print\$4}') | grep 'git.commit.id=' | awk -F- '{print \$2}'; done", returnStdout: true).trim()
            def build_commit = build_commit_lst.split('\n')
            for ( def item in build_commit) {
                item = item.trim()
                if( item != null && item != '') {
                    scriptObj.env.codeScanResultQuery = item
                    def codeScanPassed = DashboardUtil.getCodeScanResultInfo(scriptObj)
                    scriptObj.codeScanResult = codeScanPassed
                    if ("${codeScanPassed}" == 'false') {
                        logger.info("[ERROR] gitCommit: ${item}, codeScan didn't pass, you can't deploy your artifact.")
                    }
                } else {
                    logger.info("[ERROR] couldn't get the gitCommit infos from artifact. making sure that you used RSF Core, and has git.properties in artifact jar file")
                }
            }
        } else {
            logger.info("[WARNING] don't support artifact other than jar file at this moment")
        }
        scriptObj.stageStats["${scriptObj.env.STAGE_NAME}"] = 'successful'
    }
}