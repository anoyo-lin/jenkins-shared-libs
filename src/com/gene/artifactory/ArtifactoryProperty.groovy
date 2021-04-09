package com.gene.artifactory

class ArtifactoryProperty {
    // when adding properties on a artifact in artifactory we have to make sure
    // the properties value doesn't contain some "special" characters.
    // this method replaces the forbidden characters by valid one.
    public static fixValue(String value) {
        if (value == null) {
            return "null"
        } 
        return value.replace("/", "_")
        .replace("[","(").replace("]",")")
        .replaceAll("(?s)[\r\n]", "")
    }
}