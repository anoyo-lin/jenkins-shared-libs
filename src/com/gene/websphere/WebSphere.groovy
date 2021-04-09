package com.gene.websphere

import com.gene.gitflow.GitUtil

public class WebSphereUtil {
    public static void deployApplication(
        Script scriptObj,
        String gitCredentialsId,
        String wasHostIP,
        String applicationName,
        String sshUserName,
        String sshKeyFile,
        String wsadminScript,
        String deployFileNameSuffix,
        String artifactFileName
    ) {
        GitUtil.cloneRepository(scriptObj, "ssh://git@git.gene.com/was_ansbile.git", gitCredentialsId)
        scriptObj.dir("was_ansible"){
            scriptObj.sh "/usr/bin/sed -i 's/SERVER/${wasHostIP}/ inventories/host.ini"
            scriptObj.sh """/usr/bin/ansible-playbook -i inventories/host.ini websphere_application.yml --user ${sshUserName} --private-key ${sshKeyFile} \
            -e wsadmin_script=${wsadminScript} -e wsadmin_action=deploy -e application_name=${applicationName} -e artifact_file_path=../${artifactFileName}"""
        }
    }
}