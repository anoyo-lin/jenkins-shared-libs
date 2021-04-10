package com.gene.dashboard

import groovy.transform.TupleConstructor
@TupleConstructor
public class Status implements Serializable {
    Map status = new HashMap<> ()
}