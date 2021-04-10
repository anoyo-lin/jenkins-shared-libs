package com.gene.jenkins

public class JenkinsUtil {
    public static String getMultibranchJobRealName(
        Script scriptObj,
        String jobName
    ) {
        def jobNameTokens = jobName.tokenize('/') as String[]
        return jobNameTokens[-2]
    }
    public static String getDefaultPath() {
        String PATH="/usr/local/sbin:/usr/local/bin:" +
        "/usr/sbin:/usr/bin:/sbin:/bin:" +
        "/opt/fortify/bin/:/opt/apach-ant-1.9.16/bin"

        return PATH
    }
    public static getNodeVersionPath(Map configuration) {
        String nodeVersion = "11.9.0"
        if ( configuration.nodeVersion != null) {
            if ( configuration.nodeVersion.contains("12")) {
                nodeVersion = "12.16.5"
            } else if ( configuration.nodeVersion.contains("11")) {
                nodeVersion = "11.9.0"
            } else {
                nodeVersion = "10.15.3"
            }
        }
        return "/opt/nvm/versions/node/v" + nodeVersion + "/bin"
    }
    public static getFlyVersionPath(Map configuration) {
        String flyVersion = ''
        if ( configuration.getFlyVersion != null )  {
            flyVersion = configuration.flyVersion
            return "/opt/fly-" + flyVersion
        } else {
            return null
        }
    }
    public static String getAgentDockerImage() {
        return "artifactory.gene.com/docker/jenkins-ci-image:2.0"
    }

    public static String getJavaHomePath(Map configuration) {
        String javaVersion = "1.8.0"
        if (configuration.javaVersion != null)
        javaVersion = configuration.javaVersion

        return "/usr/lib/jvm/java-" + javaVersion
    }
    public static Boolean getBranchesFilter(Script scriptObj) {
        def branchesFilterList
        if (scriptObj.configuration.branchesFilterList == null || scriptObj.configuration.branchesFilterList == '') {
            branchesFilterList = [ 'master', 'hotfix', 'develop', 'release', 'feaure']
        } else {
            branchesFilterList = scriptObj.configuration.branchesFilterList
        }
        def result = false
        for (def item in branchesFilterList) {
            // if (scriptObj.env.BRNACH_NAME.toString().trim().contains("${item}")){
                // result = true
                // break
            def pattern
            if ( item == 'master' || item == 'develop') {
                pattern = /^$item/
            } else {
                pattern = /^$item\//
            }
            def matcher = scriptObj.env.BRNACH_NAME =~ pattern
            if (matcher.find()) {
                result = true
                break
            }
        }
    }
}