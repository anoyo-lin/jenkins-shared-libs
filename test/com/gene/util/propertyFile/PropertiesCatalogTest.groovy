package com.gene.util.propertyFile

import static org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

import com.gene.util.propertyFile.AbstractProperty
import com.gene.util.propertyFile.OptionalProperty
import com.gene.util.propertyFile.PropertiesCatalog

class PropertiesCatalogTest {
    PropertiesCatalog catalog = new PropertiesCatalog()

    @Test 
    void testMandatoryPropertyAddandRetrieve() {
        catalog.addMandatoryProperty("name", "message")
        assertEquals("name", catalog.getPropertyDefinition("name").getName())
        assertEquals("message", catalog.getPropertyDefinition("name").getMissingMessage())
    }

    @Test
    void testOptionalPropertyAddAndRetrieve() {
        catalog.addOptionalProperty("name", "message", "defaultValue")
        assertEquals("name", catalog.getPropertyDefinition("name").getName())
        assertEquals("message", catalog.getPropertyDefinition("name").getMissingMessage())
        assertEquals("defaultValue", ((OptionalProperty)catalog.getPropertyDefinition("name")).getDefualtValue())

    }
    @Test
    void testPropertyDefinition() {
        catalog.addOptionalProperty("optionalPropertyName", "optionalMessage", "defaultValue")
        catalog.addMandatoryProperty("mandatoryPropertyName", "mandatoryMessage")
        def properties = catalog.getPropertyDefinition();
        assertEquals(2, catalog.getPropertyDefinition().size())
        assertEquals("optionalPropertyName", properties[0].getName())
        assertEquals("mandatoryPropertyName", properties[1].getName())
    }
    @Test 
    void testSize() {
        catalog.addOptionalProperty("optionalPropertyName", "optionalMessage", "defaultValue")
        catalog.addMandatoryProperty("mandatoryPropertyName", "mandatoryMessage")
        assertEquals(2, catalog.size())
        
    }
}