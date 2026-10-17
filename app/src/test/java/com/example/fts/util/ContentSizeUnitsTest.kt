package com.example.fts.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ContentSizeUnitsTest {
    @Test
    fun convertsKilobytesUsingBinaryUnits() {
        assertEquals(1024L, ContentSizeUnits.toBytes(1, "KB"))
        assertEquals(200L * 1024L, ContentSizeUnits.toBytes(200, "KB"))
    }

    @Test
    fun convertsMegabytesUsingBinaryUnits() {
        assertEquals(1024L * 1024L, ContentSizeUnits.toBytes(1, "MB"))
        assertEquals(2L * 1024L * 1024L, ContentSizeUnits.toBytes(2, "MB"))
    }

    @Test
    fun rejectsNonPositiveValuesAndUnknownUnits() {
        assertThrows(IllegalArgumentException::class.java) { ContentSizeUnits.toBytes(0, "KB") }
        assertThrows(IllegalArgumentException::class.java) { ContentSizeUnits.toBytes(-1, "MB") }
        assertThrows(IllegalArgumentException::class.java) { ContentSizeUnits.toBytes(1, "GB") }
    }
}