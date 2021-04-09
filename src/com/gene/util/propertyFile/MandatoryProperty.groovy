package com.gene.util.propertyFile

/*
* Property that must be provided in the .properties fiels
*/

public class MandatoryProperty implements java.io.Serializable {
    String name
    String missingMessage
    boolean mandatroy = true

    public MandatoryProperty(String name, String missingMessage) {
        this.name = name
        this.missingMessage = missingMessage
    }
}