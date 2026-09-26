package com.example

import com.example.data.model.InventoryItem
import com.example.util.CsvExporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvExporterTest {

    @Test
    fun testGenerateCsv_withValidItems() {
        val items = listOf(
            InventoryItem(
                id = 1,
                itemNo = 101,
                itemName = "Ergonomic Chair",
                itemQuantity = 2,
                itemPrice = 149.99,
                totalPrice = 299.98,
                timestamp = "2026-09-26 10:00:00",
                syncedToSheet = true
            ),
            InventoryItem(
                id = 2,
                itemNo = 102,
                itemName = "Cable, USB-C (2m)",
                itemQuantity = 5,
                itemPrice = 9.50,
                totalPrice = 47.50,
                timestamp = "2026-09-26 10:05:00",
                syncedToSheet = false
            )
        )

        val csv = CsvExporter.generateCsv(items)
        val lines = csv.trim().split("\n")

        assertEquals(3, lines.size)
        assertEquals("Timestamp,Item No,Item Name,Item Quantity,Item Price (Birr),Total Item Price (Birr),Sync Status", lines[0])

        // Verify first row
        assertTrue(lines[1].contains("101,Ergonomic Chair,2,149.99,299.98,Synced"))

        // Verify second row with escaped comma in name
        assertTrue(lines[2].contains("102,\"Cable, USB-C (2m)\",5,9.50,47.50,Pending"))
    }
}
