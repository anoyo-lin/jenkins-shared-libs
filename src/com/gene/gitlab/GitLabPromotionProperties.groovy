package com.gene.gitlab

import com.gene.util.propertyFile.PropertiesCatalog
import com.gene.pipeline.PipelineType

class GitLabPromotionPropertiesCatalogBuilder {
    public static build(PropertiesCatalog propertiesCatalog, PipelineType pipelineType) {
        propertiesCatalog.addMandatoryProperty("fromBranch", "[ERROR] fromBranch property is mandatory. \
        must contain the name of the branch the pipeline will promote code from")
        propertiesCatalog.addMandatoryProperty("gitLabSSHCredentialsId", "[ERROR] gitLabSSHCredentialsId \
        property is mandatory. must contain the name of the Gitlab ssh credentials entry in the jenkins \
        credential vault")
        propertiesCatalog.addMandatoryProperty("toBranch", "[ERROR] toBranch property is mandatory. Must \
        contain the name of the branch the pipeline will promote code to")

        propertiesCatalog.addOptionalProperty("fromSnapshottoReleaseOnToBranch", "Defaulting from SnapshotToReleaseOnToBranch to false. \
        set to true if you want the promotion to increment the patch version.", "false")
        propertiesCatalog.addOptionalProperty("increaseFromBranchMinorVersion", "Defaulting increaseFromBranchMinorVersion to false. \
        Set to true if you want the promotion to increment the minor Version.", "false")
        propertiesCatalog.addOptionalProperty("increaseToBranchPatchVersion", "Defaulting increaseToBranchPatchVersion to false.\ 
        set to true if you want the promotion to increment the patch version", "false")
        propertiesCatalog.addOptionalProperty("onlyOneReleaseBranch", "Defaulting onlyOneReleaseBranch to false. \
        set to true if your project should have only one release branch", "false")
    }
}