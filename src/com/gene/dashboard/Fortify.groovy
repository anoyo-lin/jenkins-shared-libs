package com.gene.dashboard

import groovt.transform.TupleConstructor

@TupleConstructor
public class Fortify implements Serializable {
    int critical = 0
    int high = 0
    int medium = 0
    int low = 0;
    String status = "Unknown"
}