package com.gene.util.pipeline

public class PipelineUtils {
    public static String buildCause(gitlabActionType) {
        return gitlabActionType ?: "JENKINS_MANUAL"
    }
}