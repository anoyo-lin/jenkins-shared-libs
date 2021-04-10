package com.gene.propertyFile

import static org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import com.gene.util.propertyFile.PropertiesFileReader

class PropertiesFileReaderTest {
    @Test 
    void test() {
        Properties properties = PropertiesFileReader.readPropertyFiles("test/files", "dev-ci.properties", "common-ci.properties")
        assertNull(properties.getProperty("thatDoesntExist"))
        assertEquals(properties.getProperty("env"), "DEV")
        assertEquals(properties.getProperty("test"), "True")
    }
}