package com.gene.dashboard

import groovy.transform.TupleConstructor

@TupleConstructor
public class UnitTest implements Serializable {
    int test = 0
    int passed = 0
    int failed = 0
    int skipped = 0
    int rate = 0
}