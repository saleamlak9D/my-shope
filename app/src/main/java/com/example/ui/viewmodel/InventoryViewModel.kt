package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.InventoryDatabase
import com.example.data.model.InventoryItem
import com.example.data.network.SyncResult
import com.example.data.repository.InventoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AppTab(val title: String) {
    FORM("Data Entry"),
    INVENTORY("Inventory Log"),
    WEB_CODE("Apps Script & Web"),
    PREVIEW("Web Preview")
}

data class FormUiState(
    val itemNo: String = "101",
    val isAutoIncrement: Boolean = true,
    val itemName: String = "",
    val itemQuantity: String = "1",
    val itemPrice: String = "",
    val sheetsUrl: String = "",
    val isSubmitting: Boolean = false,
    val isTestingUrl: Boolean = false,
    val feedbackMessage: String? = null,
    val isSuccess: Boolean = false,
    val nameError: String? = null,
    val quantityError: String? = null,
    val priceError: String? = null
)

class InventoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: InventoryRepository
    private val prefs = application.getSharedPreferences("inventory_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(FormUiState())
    val uiState: StateFlow<FormUiState> = _uiState.asStateFlow()

    private val _currentTab = MutableStateFlow(AppTab.FORM)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    private val _currentLanguage = MutableStateFlow(
        try {
            com.example.ui.util.AppLanguage.valueOf(
                prefs.getString("app_language", com.example.ui.util.AppLanguage.AMHARIC.name) ?: com.example.ui.util.AppLanguage.AMHARIC.name
            )
        } catch (e: Exception) {
            com.example.ui.util.AppLanguage.AMHARIC
        }
    )
    val currentLanguage: StateFlow<com.example.ui.util.AppLanguage> = _currentLanguage.asStateFlow()

    private val _currentThemeMode = MutableStateFlow(
        try {
            com.example.ui.util.AppThemeMode.valueOf(
                prefs.getString("app_theme_mode", com.example.ui.util.AppThemeMode.SYSTEM.name) ?: com.example.ui.util.AppThemeMode.SYSTEM.name
            )
        } catch (e: Exception) {
            com.example.ui.util.AppThemeMode.SYSTEM
        }
    )
    val currentThemeMode: StateFlow<com.example.ui.util.AppThemeMode> = _currentThemeMode.asStateFlow()

    fun toggleLanguage() {
        val next = if (_currentLanguage.value == com.example.ui.util.AppLanguage.ENGLISH) com.example.ui.util.AppLanguage.AMHARIC else com.example.ui.util.AppLanguage.ENGLISH
        _currentLanguage.value = next
        prefs.edit().putString("app_language", next.name).apply()
    }

    fun setLanguage(lang: com.example.ui.util.AppLanguage) {
        _currentLanguage.value = lang
        prefs.edit().putString("app_language", lang.name).apply()
    }

    fun toggleThemeMode() {
        val next = when (_currentThemeMode.value) {
            com.example.ui.util.AppThemeMode.SYSTEM -> com.example.ui.util.AppThemeMode.LIGHT
            com.example.ui.util.AppThemeMode.LIGHT -> com.example.ui.util.AppThemeMode.DARK
            com.example.ui.util.AppThemeMode.DARK -> com.example.ui.util.AppThemeMode.SYSTEM
        }
        _currentThemeMode.value = next
        prefs.edit().putString("app_theme_mode", next.name).apply()
    }

    fun setThemeMode(mode: com.example.ui.util.AppThemeMode) {
        _currentThemeMode.value = mode
        prefs.edit().putString("app_theme_mode", mode.name).apply()
    }

    val inventoryItems: StateFlow<List<InventoryItem>>

    init {
        val database = InventoryDatabase.getDatabase(application)
        repository = InventoryRepository(database.inventoryDao())

        inventoryItems = repository.allItems.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Load saved URL from preferences
        val savedUrl = prefs.getString("google_sheets_url", "") ?: ""
        _uiState.value = _uiState.value.copy(sheetsUrl = savedUrl)

        // Initialize next item number from database
        loadNextItemNumber()
    }

    private fun loadNextItemNumber() {
        viewModelScope.launch {
            val nextNo = repository.getNextItemNumber()
            if (_uiState.value.isAutoIncrement) {
                _uiState.value = _uiState.value.copy(itemNo = nextNo.toString())
            }
        }
    }

    fun selectTab(tab: AppTab) {
        _currentTab.value = tab
    }

    fun onItemNoChange(newNo: String) {
        if (!_uiState.value.isAutoIncrement) {
            _uiState.value = _uiState.value.copy(itemNo = newNo.filter { it.isDigit() })
        }
    }

    fun toggleAutoIncrement(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isAutoIncrement = enabled)
        if (enabled) {
            loadNextItemNumber()
        }
    }

    fun onItemNameChange(name: String) {
        _uiState.value = _uiState.value.copy(
            itemName = name,
            nameError = if (name.isBlank()) "Item name is required" else null
        )
    }

    fun onQuantityChange(quantity: String) {
        val cleaned = quantity.filter { it.isDigit() }
        _uiState.value = _uiState.value.copy(
            itemQuantity = cleaned,
            quantityError = if (cleaned.toIntOrNull() == null || cleaned.toInt() <= 0) "Must be at least 1" else null
        )
    }

    fun adjustQuantity(delta: Int) {
        val current = _uiState.value.itemQuantity.toIntOrNull() ?: 1
        val updated = (current + delta).coerceAtLeast(1)
        _uiState.value = _uiState.value.copy(
            itemQuantity = updated.toString(),
            quantityError = null
        )
    }

    fun onPriceChange(price: String) {
        // Allow numbers and at most one decimal point
        val cleaned = price.filter { it.isDigit() || it == '.' }
        if (cleaned.count { it == '.' } <= 1) {
            _uiState.value = _uiState.value.copy(
                itemPrice = cleaned,
                priceError = if (cleaned.toDoubleOrNull() == null || cleaned.toDouble() < 0) "Enter valid price" else null
            )
        }
    }

    fun onSheetsUrlChange(url: String) {
        _uiState.value = _uiState.value.copy(sheetsUrl = url)
        prefs.edit().putString("google_sheets_url", url.trim()).apply()
    }

    fun testConnection() {
        val url = _uiState.value.sheetsUrl.trim()
        if (url.isBlank()) {
            _uiState.value = _uiState.value.copy(
                feedbackMessage = "Please enter a Web App URL first",
                isSuccess = false
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTestingUrl = true)
            val result = repository.testConnection(url)
            when (result) {
                is SyncResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isTestingUrl = false,
                        feedbackMessage = "Connection Verified: Web App responded successfully!",
                        isSuccess = true
                    )
                }
                is SyncResult.Failure -> {
                    _uiState.value = _uiState.value.copy(
                        isTestingUrl = false,
                        feedbackMessage = "Connection failed: ${result.error}",
                        isSuccess = false
                    )
                }
            }
        }
    }

    fun clearFeedback() {
        _uiState.value = _uiState.value.copy(feedbackMessage = null)
    }

    fun submitCurrentItem() {
        val state = _uiState.value

        // Validate
        val name = state.itemName.trim()
        val qty = state.itemQuantity.toIntOrNull()
        val price = state.itemPrice.toDoubleOrNull()

        var hasError = false
        var nameErr: String? = null
        var qtyErr: String? = null
        var priceErr: String? = null

        if (name.isBlank()) {
            nameErr = "Please enter an item name"
            hasError = true
        }
        if (qty == null || qty <= 0) {
            qtyErr = "Quantity must be at least 1"
            hasError = true
        }
        if (price == null || price < 0) {
            priceErr = "Please enter a valid price"
            hasError = true
        }

        if (hasError) {
            _uiState.value = state.copy(
                nameError = nameErr,
                quantityError = qtyErr,
                priceError = priceErr
            )
            return
        }

        val itemNoLong = state.itemNo.toLongOrNull() ?: 101L
        val totalPrice = (qty ?: 1) * (price ?: 0.0)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val timestamp = dateFormat.format(Date())

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true)

            val (savedItem, syncResult) = repository.saveAndSyncItem(
                webAppUrl = state.sheetsUrl.trim(),
                itemNo = itemNoLong,
                name = name,
                quantity = qty ?: 1,
                price = price ?: 0.0,
                totalPrice = totalPrice,
                timestamp = timestamp
            )

            val nextItemNo = if (state.isAutoIncrement) {
                (itemNoLong + 1).toString()
            } else {
                state.itemNo
            }

            val successMsg = when (syncResult) {
                is SyncResult.Success -> {
                    "✓ Item #$itemNoLong ($name) saved & synced! Total: $${String.format(Locale.US, "%.2f", totalPrice)}"
                }
                is SyncResult.Failure -> {
                    "✓ Saved locally! (Sheet sync pending: ${syncResult.error})"
                }
            }

            // Clear form fields for the next entry and increment Item No
            _uiState.value = _uiState.value.copy(
                itemNo = nextItemNo,
                itemName = "",
                itemQuantity = "1",
                itemPrice = "",
                isSubmitting = false,
                isSuccess = true,
                feedbackMessage = successMsg,
                nameError = null,
                quantityError = null,
                priceError = null
            )
        }
    }

    fun syncAllPending() {
        val url = _uiState.value.sheetsUrl.trim()
        if (url.isBlank()) {
            _uiState.value = _uiState.value.copy(
                feedbackMessage = "Please enter a Web App URL in settings to sync",
                isSuccess = false
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true)
            val (successCount, failCount) = repository.syncPendingItems(url)
            _uiState.value = _uiState.value.copy(
                isSubmitting = false,
                feedbackMessage = "Synced $successCount items to Google Sheet ($failCount failed)",
                isSuccess = successCount > 0
            )
        }
    }

    fun updateItem(updatedItem: InventoryItem) {
        viewModelScope.launch {
            repository.updateItem(updatedItem)
            _uiState.value = _uiState.value.copy(
                feedbackMessage = "Updated Item #${updatedItem.itemNo} (${updatedItem.itemName})",
                isSuccess = true
            )
        }
    }

    fun deleteItem(item: InventoryItem) {
        viewModelScope.launch {
            repository.deleteItem(item)
        }
    }

    fun clearAllInventory() {
        viewModelScope.launch {
            repository.clearAll()
            loadNextItemNumber()
        }
    }

    fun exportCsvToStream(outputStream: java.io.OutputStream): Boolean {
        return try {
            val items = inventoryItems.value
            val csvText = com.example.util.CsvExporter.generateCsv(items)
            com.example.util.CsvExporter.writeToOutputStream(outputStream, csvText)
            _uiState.value = _uiState.value.copy(
                feedbackMessage = "Successfully exported ${items.size} items to CSV!",
                isSuccess = true
            )
            true
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                feedbackMessage = "Failed to export CSV: ${e.localizedMessage}",
                isSuccess = false
            )
            false
        }
    }

    fun getCsvContent(): String {
        return com.example.util.CsvExporter.generateCsv(inventoryItems.value)
    }

    suspend fun parseCsvFromStream(inputStream: java.io.InputStream): com.example.util.CsvImportResult {
        val nextNo = repository.getNextItemNumber()
        return com.example.util.CsvImporter.parseCsvStream(inputStream, nextNo)
    }

    fun importParsedItems(
        items: List<InventoryItem>,
        replaceExisting: Boolean = false,
        onComplete: (Int) -> Unit = {}
    ) {
        viewModelScope.launch {
            if (replaceExisting) {
                repository.clearAll()
            }
            repository.insertItems(items)
            loadNextItemNumber()
            val msg = if (currentLanguage.value == com.example.ui.util.AppLanguage.AMHARIC) {
                "${items.size} ዕቃዎች በስኬት ከCSV ተመዝግበዋል!"
            } else {
                "Successfully imported ${items.size} items from CSV!"
            }
            _uiState.value = _uiState.value.copy(
                feedbackMessage = msg,
                isSuccess = true
            )
            onComplete(items.size)
        }
    }
}
