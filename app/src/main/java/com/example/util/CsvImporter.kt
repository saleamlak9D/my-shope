package com.example.util

import com.example.data.model.InventoryItem
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CsvImportResult(
    val items: List<InventoryItem>,
    val totalRows: Int,
    val successCount: Int,
    val skippedCount: Int,
    val errors: List<String> = emptyList(),
    val errorMessage: String? = null
)

object CsvImporter {

    fun parseCsvStream(inputStream: InputStream, startingItemNo: Long = 101L): CsvImportResult {
        return try {
            val content = inputStream.bufferedReader().use { it.readText() }
            parseCsv(content, startingItemNo)
        } catch (e: Exception) {
            CsvImportResult(
                items = emptyList(),
                totalRows = 0,
                successCount = 0,
                skippedCount = 0,
                errorMessage = e.message ?: "Failed to read CSV input stream"
            )
        }
    }

    fun parseCsv(csvContent: String, startingItemNo: Long = 101L): CsvImportResult {
        if (csvContent.isBlank()) {
            return CsvImportResult(
                items = emptyList(),
                totalRows = 0,
                successCount = 0,
                skippedCount = 0,
                errorMessage = "The selected CSV file is empty."
            )
        }

        val rows = parseCsvIntoRows(csvContent)
        if (rows.isEmpty()) {
            return CsvImportResult(
                items = emptyList(),
                totalRows = 0,
                successCount = 0,
                skippedCount = 0,
                errorMessage = "No valid data rows found in CSV."
            )
        }

        val firstRow = rows.first()
        val hasHeader = isHeaderRow(firstRow)
        val dataRows = if (hasHeader) rows.drop(1) else rows

        val columnMap = if (hasHeader) {
            resolveColumnIndices(firstRow)
        } else {
            resolveDefaultColumnIndices(if (dataRows.isNotEmpty()) dataRows.first().size else 5)
        }

        val parsedItems = mutableListOf<InventoryItem>()
        val errors = mutableListOf<String>()
        var nextAutoNo = startingItemNo
        val nowFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        for ((index, row) in dataRows.withIndex()) {
            val rowNumber = if (hasHeader) index + 2 else index + 1
            if (row.all { it.isBlank() }) continue

            try {
                // Item Name
                val nameIndex = columnMap[ColumnType.ITEM_NAME] ?: -1
                val rawName = if (nameIndex in row.indices) row[nameIndex].trim() else ""
                if (rawName.isBlank()) {
                    errors.add("Row $rowNumber: Skipped because Item Name is missing.")
                    continue
                }

                // Item No
                val noIndex = columnMap[ColumnType.ITEM_NO] ?: -1
                val rawNo = if (noIndex in row.indices) cleanNumeric(row[noIndex]) else ""
                val itemNo = rawNo.toLongOrNull()?.takeIf { it > 0 } ?: (nextAutoNo++)
                if (itemNo >= nextAutoNo) {
                    nextAutoNo = itemNo + 1
                }

                // Quantity
                val qtyIndex = columnMap[ColumnType.QUANTITY] ?: -1
                val rawQty = if (qtyIndex in row.indices) cleanNumeric(row[qtyIndex]) else ""
                val quantity = (rawQty.toIntOrNull() ?: 1).coerceAtLeast(1)

                // Price
                val priceIndex = columnMap[ColumnType.PRICE] ?: -1
                val rawPrice = if (priceIndex in row.indices) cleanPrice(row[priceIndex]) else ""
                val price = (rawPrice.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)

                // Total Price
                val totalIndex = columnMap[ColumnType.TOTAL] ?: -1
                val rawTotal = if (totalIndex in row.indices) cleanPrice(row[totalIndex]) else ""
                val explicitTotal = rawTotal.toDoubleOrNull()
                val totalPrice = if (explicitTotal != null && explicitTotal > 0.0) {
                    explicitTotal
                } else {
                    quantity * price
                }

                // Timestamp
                val timeIndex = columnMap[ColumnType.TIMESTAMP] ?: -1
                val rawTimestamp = if (timeIndex in row.indices) row[timeIndex].trim() else ""
                val timestamp = if (rawTimestamp.isNotBlank()) rawTimestamp else nowFormatted

                // Sync Status
                val syncIndex = columnMap[ColumnType.SYNC_STATUS] ?: -1
                val rawSync = if (syncIndex in row.indices) row[syncIndex].trim().lowercase() else ""
                val synced = rawSync == "synced" || rawSync == "true" || rawSync == "1"

                parsedItems.add(
                    InventoryItem(
                        id = 0,
                        itemNo = itemNo,
                        itemName = rawName,
                        itemQuantity = quantity,
                        itemPrice = price,
                        totalPrice = totalPrice,
                        timestamp = timestamp,
                        syncedToSheet = synced
                    )
                )
            } catch (e: Exception) {
                errors.add("Row $rowNumber: Failed to parse (${e.message ?: "Invalid data"})")
            }
        }

        return CsvImportResult(
            items = parsedItems,
            totalRows = dataRows.size,
            successCount = parsedItems.size,
            skippedCount = dataRows.size - parsedItems.size,
            errors = errors
        )
    }

    private fun cleanNumeric(value: String): String {
        return value.replace("#", "").trim()
    }

    private fun cleanPrice(value: String): String {
        return value.replace("Birr", "", ignoreCase = true)
            .replace("ብር", "")
            .replace("ETB", "", ignoreCase = true)
            .replace("$", "")
            .replace(",", "")
            .trim()
    }

    private fun isHeaderRow(row: List<String>): Boolean {
        val keywords = listOf("item", "name", "price", "qty", "quantity", "no", "timestamp", "date", "status", "ዕቃ", "ስም", "ዋጋ", "ብዛት")
        return row.any { col ->
            val lower = col.lowercase().trim()
            keywords.any { keyword -> lower.contains(keyword) }
        }
    }

    private enum class ColumnType {
        TIMESTAMP,
        ITEM_NO,
        ITEM_NAME,
        QUANTITY,
        PRICE,
        TOTAL,
        SYNC_STATUS
    }

    private fun resolveColumnIndices(headerRow: List<String>): Map<ColumnType, Int> {
        val map = mutableMapOf<ColumnType, Int>()

        for ((index, col) in headerRow.withIndex()) {
            val lower = col.lowercase().trim()

            when {
                lower.contains("time") || lower.contains("date") || lower.contains("ቀን") -> {
                    map.putIfAbsent(ColumnType.TIMESTAMP, index)
                }
                lower.contains("qty") || lower.contains("quant") || lower.contains("count") ||
                        lower.contains("amount") || lower.contains("units") || lower.contains("ብዛት") -> {
                    map.putIfAbsent(ColumnType.QUANTITY, index)
                }
                lower.contains("total") || lower.contains("ጠቅላላ") || lower.contains("ድምር") -> {
                    map.putIfAbsent(ColumnType.TOTAL, index)
                }
                lower.contains("price") || lower.contains("cost") || lower.contains("rate") || lower.contains("ዋጋ") -> {
                    map.putIfAbsent(ColumnType.PRICE, index)
                }
                lower.contains("item no") || lower.contains("item #") || lower.contains("item_no") ||
                        lower.contains("itemno") || lower == "no" || lower == "#" || lower.contains("ቁጥር") -> {
                    map.putIfAbsent(ColumnType.ITEM_NO, index)
                }
                lower.contains("sync") || lower.contains("status") || lower.contains("ሁኔታ") -> {
                    map.putIfAbsent(ColumnType.SYNC_STATUS, index)
                }
                lower.contains("name") || lower.contains("product") || lower.contains("title") ||
                        lower.contains("description") || lower.contains("item") || lower.contains("ስም") || lower.contains("ዕቃ") -> {
                    if (!map.containsKey(ColumnType.ITEM_NAME)) {
                        map[ColumnType.ITEM_NAME] = index
                    }
                }
            }
        }

        // Fallback for missing crucial columns
        if (!map.containsKey(ColumnType.ITEM_NAME) && headerRow.isNotEmpty()) {
            map[ColumnType.ITEM_NAME] = 0
        }
        return map
    }

    private fun resolveDefaultColumnIndices(columnCount: Int): Map<ColumnType, Int> {
        val map = mutableMapOf<ColumnType, Int>()
        when (columnCount) {
            3 -> {
                map[ColumnType.ITEM_NAME] = 0
                map[ColumnType.QUANTITY] = 1
                map[ColumnType.PRICE] = 2
            }
            4 -> {
                map[ColumnType.ITEM_NO] = 0
                map[ColumnType.ITEM_NAME] = 1
                map[ColumnType.QUANTITY] = 2
                map[ColumnType.PRICE] = 3
            }
            5 -> {
                map[ColumnType.TIMESTAMP] = 0
                map[ColumnType.ITEM_NO] = 1
                map[ColumnType.ITEM_NAME] = 2
                map[ColumnType.QUANTITY] = 3
                map[ColumnType.PRICE] = 4
            }
            6 -> {
                map[ColumnType.TIMESTAMP] = 0
                map[ColumnType.ITEM_NO] = 1
                map[ColumnType.ITEM_NAME] = 2
                map[ColumnType.QUANTITY] = 3
                map[ColumnType.PRICE] = 4
                map[ColumnType.TOTAL] = 5
            }
            else -> { // 7 or more
                map[ColumnType.TIMESTAMP] = 0
                map[ColumnType.ITEM_NO] = 1
                map[ColumnType.ITEM_NAME] = 2
                map[ColumnType.QUANTITY] = 3
                map[ColumnType.PRICE] = 4
                map[ColumnType.TOTAL] = 5
                map[ColumnType.SYNC_STATUS] = 6
            }
        }
        return map
    }

    /**
     * RFC 4180 compliant CSV parser that correctly handles escaped quotes,
     * embedded commas, and CRLF / LF line breaks.
     */
    fun parseCsvIntoRows(csvText: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val currentRow = mutableListOf<String>()
        val currentField = StringBuilder()
        var insideQuotes = false
        var i = 0
        val len = csvText.length

        while (i < len) {
            val c = csvText[i]

            if (insideQuotes) {
                if (c == '"') {
                    if (i + 1 < len && csvText[i + 1] == '"') {
                        currentField.append('"')
                        i++ // Skip escaped quote
                    } else {
                        insideQuotes = false
                    }
                } else {
                    currentField.append(c)
                }
            } else {
                when (c) {
                    '"' -> {
                        insideQuotes = true
                    }
                    ',' -> {
                        currentRow.add(currentField.toString().trim())
                        currentField.clear()
                    }
                    '\r' -> {
                        if (i + 1 < len && csvText[i + 1] == '\n') {
                            i++
                        }
                        currentRow.add(currentField.toString().trim())
                        currentField.clear()
                        if (currentRow.isNotEmpty() && currentRow.any { it.isNotEmpty() }) {
                            rows.add(currentRow.toList())
                        }
                        currentRow.clear()
                    }
                    '\n' -> {
                        currentRow.add(currentField.toString().trim())
                        currentField.clear()
                        if (currentRow.isNotEmpty() && currentRow.any { it.isNotEmpty() }) {
                            rows.add(currentRow.toList())
                        }
                        currentRow.clear()
                    }
                    else -> {
                        currentField.append(c)
                    }
                }
            }
            i++
        }

        // Flush last field if non-empty
        if (currentField.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow.add(currentField.toString().trim())
            if (currentRow.any { it.isNotEmpty() }) {
                rows.add(currentRow.toList())
            }
        }

        return rows
    }
}
