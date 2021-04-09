package com.gene.gitflow

public class GitUtil {
    public static String getNameFromRepository(String repository) {
        return (repository =~ /.*\/(.+).git/)[0][1]
    }

    public static void cloneRepositories(
        Script scriptObj,
        List repositories,
        String credentialsId,
        List branches
    ) {
        for ( repo in repositories) {
            this.cloneRepository(scriptObj, repo, credentialsId, branches)
        }
    }
    public static void cloneRepository(
        Script scriptObj,
        String repository,
        String credentialsId,
        List branches = [[name: "master"]],
        String targetDir = null
    ) {
        def repoName = this.getNameFromRepository(repository)
        if (targetDir == null ) 
        targetDir = repoName
        scriptObj.checkout([
            $class: "GitSCM",
            branches: branches,
            extensions: [
                [$class: "WipeWorkspace"],
                [$class: "RelativeTargetDirectory", relativeTargetDir: targetDir],
                [$class: "CloneOption", noTags: false, reference: "", shallow: true]
            ],
            userRemoteConfigs: [
                ["credentialsId": credentialsId, url: repository]
            ]
        ])
    }
    public static void cloneRepositoryGroovy(
        Script scriptObj,
        String repository,
        String credentialsId,
        List branches=[[name: "master"]],
        String targetDir = null
    ) {
        def repoName = this.getNameFromRepository(repository)
        if ( targetDir == null)
        targetDir = repoName
        def jenkinsCredentials = com.cloudbees.plugins.credentials.CredentialsProvider.lookCredentials(
            com.cloudbees.plugins.credentials.Credentials.class,
            Jenkins.instance,
            null,
            null
        )
        for (creds in jenkinsCredentials) {
            if(creds.id == credentialsId) {
                def key = new File('/tmp/id_rsa')
                key.createNewFile()
                key << creds.getPrivateKey()
            }
        }
        def sout = new StringBuilder(), serr = new StringBuilder()
        def proc = 'env'.execute()
        proc.consumeProcessOutput(sout, serr)
        proc.waitForOrKill(1000)
        scriptObj.echo "out> ${sout}\nerr> ${serr}"
    }
}