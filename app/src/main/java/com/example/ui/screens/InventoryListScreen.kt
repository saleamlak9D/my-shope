package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.InventoryItem
import com.example.ui.theme.SheetsForestGreen
import com.example.ui.util.AppLanguage
import com.example.ui.util.AppStrings
import com.example.ui.viewmodel.InventoryViewModel
import com.example.util.CsvExporter
import java.util.Locale

@Composable
fun InventoryListScreen(
    viewModel: InventoryViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val items by viewModel.inventoryItems.collectAsState()
    val state by viewModel.uiState.collectAsState()
    val lang by viewModel.currentLanguage.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var filterSyncStatus by remember { mutableStateOf<Boolean?>(null) } // null: all, true: synced, false: pending
    var itemToEdit by remember { mutableStateOf<InventoryItem?>(null) }
    var itemToDelete by remember { mutableStateOf<InventoryItem?>(null) }
    var showClearAllDialog by remember { mutableStateOf(false) }
    var showCsvPreviewDialog by remember { mutableStateOf(false) }
    var pendingImportResult by remember { mutableStateOf<com.example.util.CsvImportResult?>(null) }
    var showImportPreviewDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val totalCount = items.size
    val totalQuantity = items.sumOf { it.itemQuantity }
    val totalInventoryValue = items.sumOf { it.totalPrice }
    val unsyncedCount = items.count { !it.syncedToSheet }

    // Filter items based on search query (name, itemNo, or database ID) and sync status
    val filteredItems = remember(items, searchQuery, filterSyncStatus) {
        val baseList = when (filterSyncStatus) {
            true -> items.filter { it.syncedToSheet }
            false -> items.filter { !it.syncedToSheet }
            null -> items
        }
        if (searchQuery.isBlank()) {
            baseList
        } else {
            val q = searchQuery.trim().lowercase()
            val numericQ = q.removePrefix("#").trim()
            baseList.filter { item ->
                item.itemName.lowercase().contains(q) ||
                item.itemNo.toString().contains(numericQ) ||
                item.id.toString() == numericQ
            }
        }
    }

    // Launcher for saving CSV via Storage Access Framework
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    viewModel.exportCsvToStream(outputStream)
                }
                Toast.makeText(context, "CSV exported successfully!", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Launcher for selecting and importing a CSV file
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        val result = viewModel.parseCsvFromStream(inputStream)
                        if (result.items.isNotEmpty()) {
                            pendingImportResult = result
                            showImportPreviewDialog = true
                        } else {
                            val emptyMsg = result.errorMessage ?: (if (lang == AppLanguage.AMHARIC)
                                "በCSV ፋይሉ ውስጥ ምንም ትክክለኛ ዕቃ አልተገኘም"
                            else
                                "No valid inventory records found in the selected CSV")
                            Toast.makeText(context, emptyMsg, Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(
                        context,
                        if (lang == AppLanguage.AMHARIC) "ፋይሉን ማንበብ አልተቻለም፡ ${e.message}" else "Failed to open CSV: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        // Grand Total Stock Monetary Value Summary Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("total_monetary_value_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(SheetsForestGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = AppStrings.totalStockValueTitle(lang),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = AppStrings.totalStockValueSubtitle(totalCount, totalQuantity, lang),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "${String.format(Locale.US, "%,.2f", totalInventoryValue)} ${AppStrings.currencySymbol(lang)}",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = SheetsForestGreen
                        ),
                        modifier = Modifier.testTag("total_stock_value_text")
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "$totalCount ${if (lang == AppLanguage.AMHARIC) "ዕቃዎች" else "Items"}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "$totalQuantity ${AppStrings.unitsLabel(lang)}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }
                }
            }
        }

        // Search Bar & Filter Section
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(AppStrings.searchPlaceholder(lang)) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.primary)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { searchQuery = "" },
                            modifier = Modifier.testTag("clear_search_button")
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("inventory_search_bar")
                    .testTag("inventory_search_input")
            )

            // Quick Filter Chips (All, Synced, Pending)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = filterSyncStatus == null,
                    onClick = { filterSyncStatus = null },
                    label = { Text("${AppStrings.filterAll(lang)} (${items.size})", fontSize = 11.sp) },
                    modifier = Modifier.testTag("filter_chip_all")
                )
                FilterChip(
                    selected = filterSyncStatus == true,
                    onClick = { filterSyncStatus = if (filterSyncStatus == true) null else true },
                    label = { Text("${AppStrings.filterSynced(lang)} (${items.count { it.syncedToSheet }})", fontSize = 11.sp) },
                    modifier = Modifier.testTag("filter_chip_synced")
                )
                FilterChip(
                    selected = filterSyncStatus == false,
                    onClick = { filterSyncStatus = if (filterSyncStatus == false) null else false },
                    label = { Text("${AppStrings.filterPending(lang)} (${items.count { !it.syncedToSheet }})", fontSize = 11.sp) },
                    modifier = Modifier.testTag("filter_chip_pending")
                )
            }

            // Results Counter
            if (searchQuery.isNotBlank() || filterSyncStatus != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (lang == AppLanguage.AMHARIC) "የተገኙ ዕቃዎች፡ ${filteredItems.size} ከ ${items.size}" else "Found ${filteredItems.size} of ${items.size} items",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = SheetsForestGreen
                    )
                    TextButton(
                        onClick = {
                            searchQuery = ""
                            filterSyncStatus = null
                        },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(AppStrings.resetFilters(lang), fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // CSV Export & Backup Actions Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.FileDownload,
                            contentDescription = null,
                            tint = SheetsForestGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (lang == AppLanguage.AMHARIC) "የCSV ምትኬ እና ማውረጃ" else "CSV Backup & Export",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (items.isNotEmpty()) {
                        IconButton(
                            onClick = { showCsvPreviewDialog = true },
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("preview_csv_button")
                        ) {
                            Icon(
                                Icons.Default.Visibility,
                                contentDescription = "Preview CSV Text",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val defaultName = CsvExporter.getDefaultFileName()
                            createDocumentLauncher.launch(defaultName)
                        },
                        enabled = items.isNotEmpty(),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("export_csv_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = SheetsForestGreen),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(vertical = 8.dp, horizontal = 10.dp)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(AppStrings.saveCsv(lang), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = {
                            val sharePair = CsvExporter.createShareableCsvFile(context, items)
                            if (sharePair != null) {
                                context.startActivity(sharePair.second)
                            } else {
                                Toast.makeText(context, if (lang == AppLanguage.AMHARIC) "CSV ማዘጋጀት አልተቻለም" else "Failed to prepare CSV for sharing", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = items.isNotEmpty(),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_csv_button"),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(vertical = 8.dp, horizontal = 10.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(AppStrings.shareCsv(lang), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                // Quick direct export to app storage
                OutlinedButton(
                    onClick = {
                        val exportedFile = CsvExporter.exportToDocumentsFile(context, items)
                        if (exportedFile != null && exportedFile.exists()) {
                            Toast.makeText(
                                context,
                                if (lang == AppLanguage.AMHARIC) "CSV ተቀምጧል፡ ${exportedFile.name}" else "CSV saved to Documents: ${exportedFile.name}",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            Toast.makeText(context, "Export error", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = items.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("direct_save_csv_button"),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    Icon(Icons.Default.SaveAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (lang == AppLanguage.AMHARIC) "ፈጣን የCSV ማስቀመጫ (ወደ መሣሪያው)" else "Quick Save to Device Storage",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Bulk Import CSV Button
                FilledTonalButton(
                    onClick = {
                        openDocumentLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "text/plain", "*/*"))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("import_csv_button"),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp), tint = SheetsForestGreen)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (lang == AppLanguage.AMHARIC) "ዕቃዎችን ከCSV አስገባ (Import CSV)" else "Bulk Import Inventory from CSV",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Sync & Actions Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (unsyncedCount > 0) {
                FilledTonalButton(
                    onClick = { viewModel.syncAllPending() },
                    enabled = !state.isSubmitting,
                    modifier = Modifier.testTag("sync_pending_button")
                ) {
                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(AppStrings.syncPending(lang, unsyncedCount), fontSize = 12.sp)
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = SheetsForestGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (items.isNotEmpty()) AppStrings.allSynced(lang) else "Ready",
                        style = MaterialTheme.typography.bodySmall,
                        color = SheetsForestGreen
                    )
                }
            }

            if (items.isNotEmpty()) {
                TextButton(
                    onClick = { showClearAllDialog = true }
                ) {
                    Text(AppStrings.clearAll(lang), color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
        }

        // List of Items from Room Database
        if (filteredItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Default.Inventory2,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = if (searchQuery.isNotEmpty() || filterSyncStatus != null) AppStrings.noItemsMatch(lang) else AppStrings.noItemsYet(lang),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (searchQuery.isNotEmpty()) {
                            if (lang == AppLanguage.AMHARIC) "ከ\"$searchQuery\" ጋር የሚዛመድ ዕቃ የለም።" else "No items match \"$searchQuery\"."
                        } else if (filterSyncStatus != null) {
                            if (lang == AppLanguage.AMHARIC) "በዚህ ምድብ ውስጥ የተገኘ ዕቃ የለም።" else "No items found in this category."
                        } else {
                            if (lang == AppLanguage.AMHARIC) "አዲስ ዕቃ ለመመዝገብ ወደ መረጃ ማስገቢያ ትር ይሂዱ።" else "Go to the Data Entry tab to submit your first item."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (searchQuery.isNotEmpty() || filterSyncStatus != null) {
                        OutlinedButton(
                            onClick = {
                                searchQuery = ""
                                filterSyncStatus = null
                            },
                            modifier = Modifier.testTag("reset_search_empty_button")
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(AppStrings.resetFilters(lang))
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("inventory_lazy_column"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredItems, key = { it.id }) { item ->
                    InventoryItemCard(
                        item = item,
                        lang = lang,
                        onEditClick = { itemToEdit = item },
                        onDeleteClick = { itemToDelete = item }
                    )
                }
            }
        }
    }

    // Edit Item Dialog
    itemToEdit?.let { item ->
        EditItemDialog(
            item = item,
            lang = lang,
            onDismiss = { itemToEdit = null },
            onSave = { updatedItem ->
                viewModel.updateItem(updatedItem)
                itemToEdit = null
            }
        )
    }

    // Delete Confirmation Dialog
    itemToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = {
                Text(if (lang == AppLanguage.AMHARIC) "ዕቃ #${item.itemNo} ይሰረዝ?" else "Delete Item #${item.itemNo}?")
            },
            text = {
                Text(
                    if (lang == AppLanguage.AMHARIC)
                        "እርግጠኛ ነዎት '${item.itemName}' ከመሣሪያው መዝገብ እንዲሰረዝ ይፈልጋሉ?"
                    else
                        "Are you sure you want to delete '${item.itemName}' from local inventory?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteItem(item)
                        itemToDelete = null
                    },
                    modifier = Modifier.testTag("confirm_delete_button")
                ) {
                    Text(AppStrings.delete(lang), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text(AppStrings.cancel(lang))
                }
            }
        )
    }

    // Clear All Dialog
    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = {
                Text(if (lang == AppLanguage.AMHARIC) "ሁሉንም መዝገቦች አጽዳ?" else "Clear All Records?")
            },
            text = {
                Text(
                    if (lang == AppLanguage.AMHARIC)
                        "ይህ በዚህ መሣሪያ ላይ የተመዘገቡትን ዕቃዎች በሙሉ ያጠፋል። በጉግል ሉህ ላይ ያሉ መረጃዎች አይነኩም።"
                    else
                        "This will remove all local inventory logs from this device. Google Sheet rows will remain intact."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllInventory()
                        showClearAllDialog = false
                    }
                ) {
                    Text(AppStrings.clearAll(lang), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) {
                    Text(AppStrings.cancel(lang))
                }
            }
        )
    }

    // CSV Preview Dialog
    if (showCsvPreviewDialog) {
        val csvText = viewModel.getCsvContent()
        val horizScroll = rememberScrollState()

        AlertDialog(
            onDismissRequest = { showCsvPreviewDialog = false },
            title = {
                Text("CSV Preview (${items.size} rows)")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Standard RFC 4180 CSV formatted output:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2124)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .horizontalScroll(horizScroll)
                                .padding(10.dp)
                        ) {
                            Text(
                                text = csvText,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                                color = Color(0xFFE0E0E0)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Inventory CSV", csvText)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "CSV copied to clipboard!", Toast.LENGTH_SHORT).show()
                        showCsvPreviewDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SheetsForestGreen)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy CSV")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCsvPreviewDialog = false }) {
                    Text(AppStrings.cancel(lang))
                }
            }
        )
    }

    // CSV Bulk Import Preview & Confirmation Dialog
    pendingImportResult?.let { result ->
        if (showImportPreviewDialog) {
            ImportPreviewDialog(
                result = result,
                lang = lang,
                onDismiss = {
                    showImportPreviewDialog = false
                    pendingImportResult = null
                },
                onConfirm = { replaceExisting ->
                    viewModel.importParsedItems(
                        items = result.items,
                        replaceExisting = replaceExisting
                    ) { count ->
                        showImportPreviewDialog = false
                        pendingImportResult = null
                        Toast.makeText(
                            context,
                            if (lang == AppLanguage.AMHARIC)
                                "$count ዕቃዎች በስኬት ተመዝግበዋል!"
                            else
                                "Successfully imported $count items!",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            )
        }
    }
}

@Composable
fun ImportPreviewDialog(
    result: com.example.util.CsvImportResult,
    lang: AppLanguage,
    onDismiss: () -> Unit,
    onConfirm: (replaceExisting: Boolean) -> Unit
) {
    var replaceExisting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.FileUpload, contentDescription = null, tint = SheetsForestGreen)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = AppStrings.importCsvDialogTitle(lang),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Summary Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = if (lang == AppLanguage.AMHARIC)
                                "የተገኙ ዕቃዎች፡ ${result.successCount} (ከ ${result.totalRows} ረድፎች)"
                            else
                                "Ready to import: ${result.successCount} items (from ${result.totalRows} rows)",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        if (result.skippedCount > 0) {
                            Text(
                                text = if (lang == AppLanguage.AMHARIC)
                                    "የተዘለሉ/ስህተት የነበራቸው፡ ${result.skippedCount}"
                                else
                                    "Skipped invalid rows: ${result.skippedCount}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                // Table preview of first 4 items
                Text(
                    text = if (lang == AppLanguage.AMHARIC) "የዕቃዎች ቅድመ እይታ (ናሙና)፦" else "Preview of items to import:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    result.items.take(4).forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "#${item.itemNo} ${item.itemName}",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                                Text(
                                    text = "${item.itemQuantity} ${AppStrings.unitsLabel(lang)} × ${String.format(Locale.US, "%.2f", item.itemPrice)} ${AppStrings.currencySymbol(lang)}",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "${String.format(Locale.US, "%.2f", item.totalPrice)} ${AppStrings.currencySymbol(lang)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = SheetsForestGreen
                            )
                        }
                        if (item != result.items.take(4).last()) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                        }
                    }
                    if (result.items.size > 4) {
                        Text(
                            text = if (lang == AppLanguage.AMHARIC) "+ ተጨማሪ ${result.items.size - 4} ዕቃዎች..." else "+ ${result.items.size - 4} more items...",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                // Import Mode Choice
                Text(
                    text = if (lang == AppLanguage.AMHARIC) "የማስገቢያ ዘዴ፦" else "Import Mode:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RadioButton(
                        selected = !replaceExisting,
                        onClick = { replaceExisting = false },
                        colors = RadioButtonDefaults.colors(selectedColor = SheetsForestGreen)
                    )
                    Text(
                        text = AppStrings.importModeAppend(lang),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RadioButton(
                        selected = replaceExisting,
                        onClick = { replaceExisting = true },
                        colors = RadioButtonDefaults.colors(selectedColor = SheetsForestGreen)
                    )
                    Text(
                        text = AppStrings.importModeReplace(lang),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(replaceExisting) },
                colors = ButtonDefaults.buttonColors(containerColor = SheetsForestGreen),
                modifier = Modifier.testTag("confirm_import_csv_button")
            ) {
                Text(AppStrings.confirmImportButton(result.items.size, lang))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(AppStrings.cancel(lang))
            }
        }
    )
}

@Composable
fun EditItemDialog(
    item: InventoryItem,
    lang: AppLanguage,
    onDismiss: () -> Unit,
    onSave: (InventoryItem) -> Unit
) {
    var editNo by remember { mutableStateOf(item.itemNo.toString()) }
    var editName by remember { mutableStateOf(item.itemName) }
    var editQty by remember { mutableStateOf(item.itemQuantity.toString()) }
    var editPrice by remember { mutableStateOf(String.format(Locale.US, "%.2f", item.itemPrice)) }
    var nameError by remember { mutableStateOf(false) }

    val qtyInt = editQty.toIntOrNull() ?: 1
    val priceDbl = editPrice.toDoubleOrNull() ?: 0.0
    val totalCalc = qtyInt * priceDbl

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = SheetsForestGreen)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (lang == AppLanguage.AMHARIC) "ዕቃ #${item.itemNo}ን አስተካክል" else "Edit Item #${item.itemNo}",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Item No
                OutlinedTextField(
                    value = editNo,
                    onValueChange = { editNo = it.filter { c -> c.isDigit() } },
                    label = { Text(AppStrings.itemNo(lang)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_item_no_input"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(8.dp)
                )

                // Item Name
                OutlinedTextField(
                    value = editName,
                    onValueChange = {
                        editName = it
                        nameError = it.isBlank()
                    },
                    label = { Text(AppStrings.itemName(lang)) },
                    isError = nameError,
                    supportingText = {
                        if (nameError) Text(
                            if (lang == AppLanguage.AMHARIC) "ስም ባዶ መሆን አይችልም" else "Name cannot be empty",
                            color = MaterialTheme.colorScheme.error
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_item_name_input"),
                    shape = RoundedCornerShape(8.dp)
                )

                // Quantity with Stepper
                Text(AppStrings.itemQuantity(lang), style = MaterialTheme.typography.labelMedium)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            val current = editQty.toIntOrNull() ?: 1
                            editQty = (current - 1).coerceAtLeast(1).toString()
                        },
                        modifier = Modifier.size(36.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(16.dp))
                    }
                    OutlinedTextField(
                        value = editQty,
                        onValueChange = { editQty = it.filter { c -> c.isDigit() } },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("edit_item_qty_input"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
                        shape = RoundedCornerShape(8.dp)
                    )
                    FilledTonalButton(
                        onClick = {
                            val current = editQty.toIntOrNull() ?: 1
                            editQty = (current + 1).toString()
                        },
                        modifier = Modifier.size(36.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(16.dp))
                    }
                }

                // Price
                OutlinedTextField(
                    value = editPrice,
                    onValueChange = { input ->
                        val cleaned = input.filter { it.isDigit() || it == '.' }
                        if (cleaned.count { it == '.' } <= 1) {
                            editPrice = cleaned
                        }
                    },
                    label = { Text(AppStrings.itemPrice(lang)) },
                    leadingIcon = { Text(AppStrings.currencySymbol(lang), fontWeight = FontWeight.Bold, color = SheetsForestGreen) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_item_price_input"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(8.dp)
                )

                // Recalculated Total
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(AppStrings.totalItemPrice(lang), style = MaterialTheme.typography.labelMedium)
                        Text(
                            "${String.format(Locale.US, "%.2f", totalCalc)} ${AppStrings.currencySymbol(lang)}",
                            fontWeight = FontWeight.Bold,
                            color = SheetsForestGreen,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (editName.isBlank()) {
                        nameError = true
                        return@Button
                    }
                    val updatedNo = editNo.toLongOrNull() ?: item.itemNo
                    val updatedQty = (editQty.toIntOrNull() ?: item.itemQuantity).coerceAtLeast(1)
                    val updatedPrice = editPrice.toDoubleOrNull() ?: item.itemPrice
                    val updatedTotal = updatedQty * updatedPrice

                    onSave(
                        item.copy(
                            itemNo = updatedNo,
                            itemName = editName.trim(),
                            itemQuantity = updatedQty,
                            itemPrice = updatedPrice,
                            totalPrice = updatedTotal
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = SheetsForestGreen),
                modifier = Modifier.testTag("save_edit_item_button")
            ) {
                Text(AppStrings.saveChanges(lang))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(AppStrings.cancel(lang))
            }
        }
    )
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    highlight: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (highlight) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = if (highlight) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (highlight) SheetsForestGreen else MaterialTheme.colorScheme.onSurface
                ),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun InventoryItemCard(
    item: InventoryItem,
    lang: AppLanguage,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Item No Badge
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "#${item.itemNo}",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.itemName,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    if (item.syncedToSheet) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFE8F5E9))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                if (lang == AppLanguage.AMHARIC) "የተላከ" else "Synced",
                                fontSize = 10.sp,
                                color = Color(0xFF2E7D32),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFFFF3E0))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                if (lang == AppLanguage.AMHARIC) "በስልኩ ላይ" else "Local",
                                fontSize = 10.sp,
                                color = Color(0xFFE65100),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${item.itemQuantity} ${AppStrings.unitsLabel(lang)} × ${String.format(Locale.US, "%.2f", item.itemPrice)} ${AppStrings.currencySymbol(lang)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = item.timestamp,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            // Total Price and Edit/Delete Actions
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "${String.format(Locale.US, "%.2f", item.totalPrice)} ${AppStrings.currencySymbol(lang)}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = SheetsForestGreen
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row {
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("edit_item_${item.id}")
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit item",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("delete_item_${item.id}")
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete item",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
