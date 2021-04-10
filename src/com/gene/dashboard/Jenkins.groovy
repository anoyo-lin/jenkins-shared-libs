package com.gene.dashboard

import groovy.transform.TupleConstructor

@TupleConstructor
public class Jenkins implements Serializable {
    String status = ""
    String jobUrl = ""
    String buildUrl = ""
    int build = 0

    // Git git
    // UnitTest unitTest
    // Version version
    // SonarQube sonarqube
    // Pcf pcf
    // Fortify fortify
    // SmokeTest smokeTest
    // Pefecto perfecto
}