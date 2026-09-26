package com.example

import com.example.util.CsvImporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvImporterTest {

    @Test
    fun testParseCsv_standardExportedFormat() {
        val csv = """
            Timestamp,Item No,Item Name,Item Quantity,Item Price (Birr),Total Item Price (Birr),Sync Status
            2026-09-26 10:00:00,101,Ergonomic Office Chair,2,250.00,500.00,Synced
            2026-09-26 10:05:00,102,"Cable, USB-C (2m)",5,75.50,377.50,Pending
        """.trimIndent()

        val result = CsvImporter.parseCsv(csv)

        assertEquals(2, result.successCount)
        assertEquals(0, result.skippedCount)
        assertEquals(2, result.items.size)

        val item1 = result.items[0]
        assertEquals(101L, item1.itemNo)
        assertEquals("Ergonomic Office Chair", item1.itemName)
        assertEquals(2, item1.itemQuantity)
        assertEquals(250.00, item1.itemPrice, 0.001)
        assertEquals(500.00, item1.totalPrice, 0.001)
        assertTrue(item1.syncedToSheet)

        val item2 = result.items[1]
        assertEquals(102L, item2.itemNo)
        assertEquals("Cable, USB-C (2m)", item2.itemName)
        assertEquals(5, item2.itemQuantity)
        assertEquals(75.50, item2.itemPrice, 0.001)
        assertEquals(377.50, item2.totalPrice, 0.001)
        assertEquals(false, item2.syncedToSheet)
    }

    @Test
    fun testParseCsv_simpleFormatWithoutHeaderNumbers() {
        val csv = """
            Product,Qty,Price
            Dell Monitor 27 inch,3,12000 ETB
            Wireless Keyboard,10,850 ብር
        """.trimIndent()

        val result = CsvImporter.parseCsv(csv, startingItemNo = 200L)

        assertEquals(2, result.successCount)
        assertEquals(200L, result.items[0].itemNo)
        assertEquals(12000.0, result.items[0].itemPrice, 0.001)
        assertEquals(36000.0, result.items[0].totalPrice, 0.001)

        assertEquals(201L, result.items[1].itemNo)
        assertEquals(850.0, result.items[1].itemPrice, 0.001)
        assertEquals(8500.0, result.items[1].totalPrice, 0.001)
    }

    @Test
    fun testParseCsv_handlesEmptyAndMalformedRowsGracefully() {
        val csv = """
            Item Name,Item Quantity,Item Price
            
            Valid Item A,2,100
            ,3,50
            Valid Item B,1,200
        """.trimIndent()

        val result = CsvImporter.parseCsv(csv)

        assertEquals(2, result.successCount)
        assertEquals(1, result.skippedCount)
        assertEquals("Valid Item A", result.items[0].itemName)
        assertEquals("Valid Item B", result.items[1].itemName)
    }
}
