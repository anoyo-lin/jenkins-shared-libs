package com.gene.dashboard

import groovy.transform.TupleConstructor

@TupleConstructor
public class Sonarqube implements Serializable {
    int codeSmell = 0
    Float coverage = 0
    String status = "Unknown"
}