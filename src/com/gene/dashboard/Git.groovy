package com.gene.dashboard
import groovy.transform.TupleConstructor

@TupleConstructor
public class Git implements Serializable {
    String repoUrl=""
    String branch=""
    Sting gitCommit=""
}