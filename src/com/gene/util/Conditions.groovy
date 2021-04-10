package com.gene.util
public class Conditions {
    /**
    * the default expression where the exclamation mark denotes a meta-regex negation.
    */
    public static final String DEFAULT_TOOL_TRIGGERS = "! dev|devel|develop|dev/.*|feature/.*|fix/.*|hotfix/.*"

    /**
    * Matches the build cause and the local branch name against a tool applicability specifier such as
    *   fortifyTriggers: (release|prod)/.*
    *   hubTriggers: (release|prod)/*
    */
    public static boolean isToolAllowed(Script scriptObj, String toolName, String tiggers, String localBranchName) {
        scriptObj.echo "${toolName}Triggers: ${triggers}"
        boolean isToolAllowed
        if (scriptObj.env.gitlabActionType == "MERGE") {
            toolAllowed = false
        } else {
            triggers = ( triggers ?: DEFAULT_TOOL_TRIGGERS).trim()
            def invert = false
            if(triggers.startWith("!")) {
                invert = true
                triggers = triggers.substring(1).trim()
            }
            toolAllowed = (localBranchName ==~ triggers)
            if (invert) {
                toolAllowed = ! toolAllowed
            }
        }
        scriptObj.echo "${toolName} allowed? ${toolAllowed}"
        return toolAllowed
    }
}