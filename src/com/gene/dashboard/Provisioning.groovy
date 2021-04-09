package com.gene.dashboard

import groovy.transform.TupleConstructor
@TupleConstructor

public class Provisioning implements Serializable {
    String foundation = ""
    String org = ""
    String space = ""
    String appName = ""
    String gitCommits = ""
    String version = ""
    String artifactUrl = ""
}