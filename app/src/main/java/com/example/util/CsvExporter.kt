package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.InventoryItem
import java.io.File
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExporter {

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }

    fun generateCsv(items: List<InventoryItem>): String {
        val sb = StringBuilder()
        // Header matching Ethiopian Birr inventory layout
        sb.append("Timestamp,Item No,Item Name,Item Quantity,Item Price (Birr),Total Item Price (Birr),Sync Status\n")

        for (item in items) {
            sb.append(escapeCsv(item.timestamp)).append(",")
            sb.append(item.itemNo).append(",")
            sb.append(escapeCsv(item.itemName)).append(",")
            sb.append(item.itemQuantity).append(",")
            sb.append(String.format(Locale.US, "%.2f", item.itemPrice)).append(",")
            sb.append(String.format(Locale.US, "%.2f", item.totalPrice)).append(",")
            sb.append(if (item.syncedToSheet) "Synced" else "Pending")
            sb.append("\n")
        }
        return sb.toString()
    }

    fun getDefaultFileName(): String {
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        return "inventory_export_${dateFormat.format(Date())}.csv"
    }

    fun writeToOutputStream(outputStream: OutputStream, csvContent: String) {
        outputStream.bufferedWriter().use { writer ->
            writer.write(csvContent)
            writer.flush()
        }
    }

    fun exportToDocumentsFile(context: Context, items: List<InventoryItem>): File? {
        return try {
            val docsDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
            if (!docsDir.exists()) docsDir.mkdirs()
            val file = File(docsDir, getDefaultFileName())
            file.writeText(generateCsv(items))
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun createShareableCsvFile(context: Context, items: List<InventoryItem>): Pair<Uri, Intent>? {
        try {
            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }
            val fileName = getDefaultFileName()
            val file = File(exportDir, fileName)
            file.writeText(generateCsv(items))

            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, file)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Inventory Data Export ($fileName)")
                putExtra(Intent.EXTRA_TEXT, "Exported ${items.size} inventory items in CSV format.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            return Pair(uri, Intent.createChooser(shareIntent, "Share Inventory CSV"))
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}
