package com.gene.dashboard

import groovy.transform.TupleConstructor

@TupleConstructor
public class Pcf implements Serializable {
    String org = ""
    String space = ""
    String app = ""
    String gitCommits = ""
    String version = ""
    String artifactUrl = ""

}