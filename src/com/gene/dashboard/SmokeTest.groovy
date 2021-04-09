package com.gene.dashboard

import groovy.transform.TupleConstructor
@TupleConstructor

public class SmokeTest implements Serializable {
    int tests = 0
    int passed = 0
    int failed = 0
    int skipped = 0
}