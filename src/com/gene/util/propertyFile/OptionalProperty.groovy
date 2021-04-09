package com.gene.util.propertyFile
/*
* property that may be provided in the .properties files.
* if not provided. the pipelisn will use the default value defined on this class.
*/

public class OptionalProperty implements java.io.Serializable {
    String name
    String missingMessage
    String defaultValue;
    boolean mandatory = false

    public OptionalProperty(String name, String missingMessage, String defaultValue) {
        this.name = name
        this.missingMessage = missingMessage
        this.defaultValue = defaultValue
    }
}