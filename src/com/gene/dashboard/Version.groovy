package com.gene.dashboard

import groovy.transform.TupleConstructor

@TupleConstructor

public class Version implements Serializable {
    String current = ""
    String release = ""
}