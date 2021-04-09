package com.gene.util.pipeline

public enum PipelineType implements Serializable {
    DONET(0, "DotNet Classic"),
    DONETCORE(1, "DotNetCore"),
    JAVA_MAVEN(2, "Java Maven"),
    AEM_MAVEN(3, "AEM Maven"),
    NODEJS(4, "NodeJS"),
    SWIFT(5, "Swift"),
    SELENIUM(6, "Selenium");

    public final int type;
    public final String desc;

    public PipelineType(final int type, final String desc) {
        this.type = type
        this.desc = desc
    }

    static public PipelineType lookup(String desc) {
        return (values().find{it.desc == desc});
    }
}