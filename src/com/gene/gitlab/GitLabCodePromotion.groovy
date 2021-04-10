package com.gene.gitlab

import com.gene.pipeline.PipelineType

/**
 * Helper class that contains all the git commands related to code promotions between branches.
 *
 **/

 class GitLabCodePromotion implements Serializable {
     Script scriptObj
     PipelineType PipelineType
     def gitLabSSHCredentialsId
     def gitUrl
     def fromBranch
     def toBranch
     def isUnix

     public GitLabCodePromotion(Script scriptObj, PipelineType pipelineType, def gitUrl, def gitLabSSHCredentialsId, def fromBranch, def toBranch){
         this.scriptObj = scriptObj
         this.pipelineType = pipelineType
         this.gitUrl = gitUrl
         this.gitLabSSHCredentialsId = gitLabSSHCredentialsId
         this.fromBranch = fromBranch
         this.toBranch = toBranch
         this.isUnix = scriptObj.isUnix()
     }

     def checkoutSourceRepo() {
         scriptObj.sshagent([gitLabSSHCredentialsId]) {
             executeGitCmd("checkout ${fromBranch}")
             executeGitCmd("pull")
         }
     }

     def commitSourceRepo(def sourceVersion) {
         scriptObj.sshagent([gitLabSSHCredentialsId]) {
             executeGitCmd("status")
             if (PipelineType.JAVA_MAVEN == pipelineType) {
                 executeGitCmd("add ./\\pom.xml")
                 executeGitCmd("commit -a -m \"Bumped version number to ${sourceVersion} [ci-skip]\"")
             } else if (PipelineType.DOTNETCORE == pipelineType) {
                 executeGitCmd("add ./\\*.nuspec")
                 executeGitCmd("commit -a -m \"Bumped version number to ${sourceVersion} [ci-skip]\"")
             }
             executeGitCmd("push ${gitUrl} HEAD:${fromBranch}")
         }
     }
     def checkoutInNewDestinationBranch(def destinationVersion) {
         scriptObj.sshagent([gitLabSSHCredentialsId]) {
             executeGitCmd("checkout -b ${toBranch}/${destinationVersion} ${fromBranch}")
         }
     }

     def commitAndPushToNewDestinationBranch(boolean commit, def destinationVersion) {
         scriptObj.sshagent([gitLabSSHCredentialsId]) {
             if(commit) {
                 executeGitCmd("commit -a -m \"Changed to ${toBranch} version ${destinationVersion}\"")
             }
             executeGitCmd("push -u ${gitUrl} ${toBranch}/${destinationVersion}")
         }
     }

     def mergeAndTagInExistingDestinationBranch(def destinationVersion) {
         scriptObj.sshagent([gitLabSSHCredentialsId]) {
             // merge any changes in the destination branch into the source branch
             executeGitCmd("checkout ${fromBranch}")
             executeGitCmd("merge -s ours origin/${toBranch}")
             // merge any changes in the source branch into destination branch
             executeGitCmd("checkout ${toBranch}")
             executeGitCmd("tag -a before_merge_in_${toBranch}_${destinationVersion} -m \"Before merge for version ${destinationVersion}\"")
             executeGitCmd("push --force origin before_merge_in_${toBranch}_${destinationVersion}")
             executeGitCmd("merge ${fromBranch}")
         }
     }
     def commitPushAndTagInExistingDestinationBranch(boolean commit, def destinationVersion) {
         scriptObj.sshagent([gitLabSSHCredentialsId]) {
             if (commit) {
                 executeGitCmd("commit -a -m \"Changed to ${toBranch} version ${destinationVersion}\"")
             }
             executeGitCmd("push ${gitUrl} HEAD:${toBranch}")
             executeGitCmd("tag -a after_merge_in_${toBranch}_${destinationVersion} -, \"After merge for version ${destinationVersion}\"")
             executeGitCmd("push origin after_merge_in_${toBranch}_${destinationVersion}")
         }
     }
     private void executeGitCmd(def gitCommand) {
         if(isUnix){
             scriptObj.sh "git ${gitCommand}"
         } else {
             scriptObj.bat "git ${gitCommand}"
         }
     }

 }