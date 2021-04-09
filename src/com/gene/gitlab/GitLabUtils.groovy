package com.gene.gitlab

public class GitLabUtils {
    private static List<String> slice(String[] pieces, int begin, int end) {
        List<String> sliced = new ArrayList<String>()
        int endPos = end
        if(endPos < 0) {
            endPos += pieces.length
        }
        for(int i = begin; i <= endPos; i++) {
            sliced.add(pieces[i])
        }
        return sliced
    }
    public static String getLocalBranchName(Script scriptObj) {
        scriptObj.echo "gitlabActionType = ${scriptObj.env.gitlabActionType?: '???'}"
        scriptObj.echo "gitlabSourceBranch = ${scriptObj.env.gitlabSourceBranch?: '???'}"
        scriptObj.echo "gitlabTargetBranch = ${scriptObj.env.gitlabTargetBranch?: '???'}"
        scriptObj.echo "GIT_BRANCH = ${scriptObj.env.GIT_BRANCH?: '???'}"
        scriptObj.echo "BRANCH_NAME = ${scriptObj.env.BRANCH_Name?: '???'}"

        String branchName = scriptObj.env.BRANCH_NAME ?: scriptObj.env.GIT_BRANCH ?: "master"

        def branchNameParts = branchName.split('/')

        int localStart = 0
        if('origin' == branchNameParts[localStart]){
            localStart += 1
        }

        def localBranchName = slice(branchNameParts, localStart, -1).join('/')
        scriptObj.echo "localBranchName = ${localBranchName}"
        return localBranchName
    }

    public static String buildCause(gitlabActionType) {
        return gitlabActionType ?: "JENKINS_MANUAL"
    }
    // TODO: trigger long-running scans on a GitLab comment "jenkins please run scans"
    // https://github.com/jenkinsci/gitlab-plugin/issue/704
}