package com.gene.artifactory

public class ArtifactoryUtil {
    
    public static String deployFile(scriptObj, String file) {
        def pom = scriptObj.readMavenPom file: "pom.xml"

        def repoURL = pom.getVersion().endsWith("-SNAPSHOT") ? pom.getDistributionManagement().getSnapshotRepository().getUrl() : pom.getDistributionManagement().getRepository().getUrl()
        def repoId = pom.getVersion().endsWith("-SNAPSHOT") ? pom.getDistributionManagement().getSnapshotRepository().getId() : pom.getDistributionManagement().getRepository().getUrl()
        return this.deployFile(scriptObj, repoUrl, pom.getGroupId(), pom.getArtifactId(), pom.getVersion(), pom.getPackaging(), file, repoId)
    }

    public static void deployFile(
        Script scriptObj,
        String url,
        String groupId,
        String artifactId,
        String version,
        String packaging,
        String file,
        String repoId = "") {
        def appendParams =""
        if (repo) {
            appendParams = "-DrepositoryId=${repoId}"
        }
        scriptObj.sh "mvn --settings settings.xml deploy:deploy-file ${appendParams} -DgroupId=${groupId} -DartifactId=${artifactId} -Dversion=${version} -Dpackaging=${packaging} -DgeneratePom=true -Dfile=${file} -Durl=${url}"
    }
    public static String downloadArtifact(Script scriptObj, String file=null) {
        def pom = scriptObj.readMavenPom file: "pom.xml"
        def artifactId = pom.getArtifactId()
        def version = pom.getVersion()
        def packaging = pom.getPackaging()
        if ( file == null ) {
            file = "${artifactId}-${version}.${packaging}"
        }
        def repoUrl = pom.getVersion().endsWith("-SNAPSHOT") ? pom.getDistributionManagement().getSnapshotRepository().getUrl() : pom.getDistributionManagement().getRepository().getUrl()
        return this.downloadArtifact(scriptObj, repoUrl, pom.getGroupId(), artifactId, version, packaging, file)
    }

    public static String downloadArtifact(
        Script scriptObj,
        String url,
        String groupId,
        String artifactId,
        String version,
        String packaging,
        String file=null
    ) {
        def groupPath = groupId.replace(".", "/")
        if (file == null) {
            file = "${artifactId}-${version}.${packaging}"
        }
        scriptObj.sh "/usr/bin/curl -x GET --fail ${url}/${groupPath}/${artifactId}/${version}/${file} -o ${file}"
        return file
    }

    public static String wrapNodePackageName(
        String artifactId,
        String artifactVersion,
        String profilesActive,
        String branchName = ""
    ) {
        def packagingName = ""
        def packaging = "tar.gz"
        if (branchName && branchName != "" ) {
            branchName = branchName.replace("/", "-")
            packagingName = "${branchName}-"
        }
        packagingName = "${packagingName}${artifactId}-${artifactVersion}"
        if (profilesActive) {
            packagingName = "${packagingName}-${profilesActive}"
        }
        return "${packagingName}.${packaging}"
    }
    public static void deployNodePackage(
        Script scriptObj,
        String groupId,
        String artifactId,
        String branchName,
        String version,
        String profilesActive,
        String repoUrl
    ) {
        def groupPath = groupId.replace(".","/")
        def packagingName = wrapNodePackageName(artifactId, version, profilesActive, branchName)
        scriptObj.sh "/usr/bin/curl -u user:password -X PUT \"${repoUrl}/${groupPath}/${artifactId}/${version}/\" -T ${packagingName}"
    }
    public static void downloadNodePackage(
        Script scriptObj,
        String repoUrl
        String groupID,
        String artifactId,
        String version,
        String packagingName
    ) {
        def groupPath = groupId.replace(".", "/")
        scriptObj.sh "/usr/bin/curl -u user:password -X PUT \"${repoUrl}/${groupPath}/${artifactId}/${version}/\" -T ${packagingName}"
    }
    public static String downloadNodePackage(
        Script scriptObj,
        String groupID,
        String artifactId,
        String branchName,
        String version,
        String profilesActive,
        String repoUrl
    ) {
        def groupPath = groupId.replace(".", "/")
        def packagingName = wrapNodePackageName(artifactId, version, profilesActive, branchName)
        scriptObj.sh "/usr/bin/curl -X GET \"${repoUrl}/${groupPath}/${artifactId}/${version}/${packagingName}\" -T ${packagingName}"
    }


    
}