package com.gene.cloudfoundry

import com.gene.artifactory.ArtifactoryUtil

public class CloudFoundry {
    public static void generateKeyPair(Script scriptObj, String privKey) {
        def keyPair = scriptObj.sh (returnStdout: true,
        script: """#!/bin/bash -e
        set +x
        echo -e ${privKey} | \
        sed -e 's,-----END RSA PRIVATE KEY-----,,' -e 's,-----BEGIN RSA PRIVATE KEY-----,,'
        """).trim().replace(" ", '\\\\n')
        return keyPair
    }
    public static void createBackingServices(Script scriptObj, String apiEndpoint, String cfUserName, String cfPassword, String cfOrg, String cfSpace, String keyPair, String environment, String jenkinsDir) {
        scriptObj.sh """#!/bin/bash -e
        |cf login -a ${apiEndpoint} -u ${cfUserName} -p ${cfPassword} -o ${cfOrg} -s ${cfSpace}
        |source ${jenkinsDir}/jenkins/cf-${environment}.conf
        |source ${jenkinsDir}/cf_scripts/assemble-services-all.sh -k ${keyPair} -e ${environment}
        |""".stripMargin()
    }
    public static void cfTask(Script scriptObj, String jenkinsDir, String apiEndpoint, String cfUserName, String cfPassword, String cfOrg, String cfSpace, String targetEnvironment, String tasks, String manifestFile) {
        def pom = scriptObj.readMavenPom file: "pom.xml"
        def groupId = pom.getGroupId()
        def artifactId = pom.getArtifactId()
        def artifactPackaging = pom.getPackaging()
        def releaseUrl = pom.getDistributionManagement().getRepository().getUrl()
        def artifactVersion = ''
        def jarFileLoc = jenkinsDir

        if (targetEnvironment.contains('dev')) {
            artifactVersion = pom.getVersion()
            jarFileLoc = jarFileLoc + '/target'
        } else {
            artifactVersion = pom.getVersion().replace("-SNAPSHOT", "")
            ArtifactoryUtil.downloadArtifact(scriptObj, releaseUrl, groupId, artifactId, artifactVersion, artifactPackaging, artifactId + '-' + artifactVersion + '-' + artifactPackaging)
        }

        scriptObj.sh """
        #!/bin/bash -e
        echo 'Connecting to CloudFoundry
        cf login -a ${apiEndpoint} -u ${cfUserName} -p ${cfPassword} -o ${cfOrg} -s ${cfSpace}
        if [[ ${task} == 'deployment' ]]; then
        source ${jenkinsDir}/cf_scripts/deploy.sh -c ${manifestFile} -d ${jarFileLoc} -a ${artifactId} -v ${artifactVersion} -p ${artifactPackaging} -e ${targetEnvironment}
        else 
        source ${jenkinsDir}/cf_scripts/autoscale.sh -c ${manifestFile} -a ${artifactId}
        fi
        """
    }
}