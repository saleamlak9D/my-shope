package com.example.data.repository

import com.example.data.local.InventoryDao
import com.example.data.model.InventoryItem
import com.example.data.network.GoogleSheetsSyncService
import com.example.data.network.SyncResult
import kotlinx.coroutines.flow.Flow

class InventoryRepository(
    private val dao: InventoryDao,
    private val syncService: GoogleSheetsSyncService = GoogleSheetsSyncService()
) {
    val allItems: Flow<List<InventoryItem>> = dao.getAllItems()
    val itemCount: Flow<Int> = dao.getItemCount()

    suspend fun getNextItemNumber(): Long {
        val max = dao.getMaxItemNo() ?: 0L
        return if (max <= 0L) 101L else max + 1L
    }

    suspend fun saveAndSyncItem(
        webAppUrl: String,
        itemNo: Long,
        name: String,
        quantity: Int,
        price: Double,
        totalPrice: Double,
        timestamp: String
    ): Pair<InventoryItem, SyncResult> {
        val initialItem = InventoryItem(
            itemNo = itemNo,
            itemName = name,
            itemQuantity = quantity,
            itemPrice = price,
            totalPrice = totalPrice,
            timestamp = timestamp,
            syncedToSheet = false
        )

        val insertedId = dao.insertItem(initialItem)
        val savedItem = initialItem.copy(id = insertedId)

        if (webAppUrl.isNotBlank()) {
            val syncResult = syncService.sendItemToSheet(webAppUrl, savedItem)
            if (syncResult is SyncResult.Success) {
                dao.markAsSynced(insertedId)
                return Pair(savedItem.copy(syncedToSheet = true), syncResult)
            }
            return Pair(savedItem, syncResult)
        } else {
            return Pair(savedItem, SyncResult.Success("Saved locally (Google Sheets URL not configured)"))
        }
    }

    suspend fun syncPendingItems(webAppUrl: String): Pair<Int, Int> {
        if (webAppUrl.isBlank()) return Pair(0, 0)
        val pending = dao.getUnsyncedItems()
        var successCount = 0
        var failCount = 0

        for (item in pending) {
            val result = syncService.sendItemToSheet(webAppUrl, item)
            if (result is SyncResult.Success) {
                dao.markAsSynced(item.id)
                successCount++
            } else {
                failCount++
            }
        }
        return Pair(successCount, failCount)
    }

    suspend fun testConnection(webAppUrl: String): SyncResult {
        return syncService.testConnection(webAppUrl)
    }

    suspend fun updateItem(item: InventoryItem) {
        dao.updateItem(item)
    }

    suspend fun insertItems(items: List<InventoryItem>): List<Long> {
        return dao.insertItems(items)
    }

    suspend fun deleteItem(item: InventoryItem) {
        dao.deleteItem(item)
    }

    suspend fun clearAll() {
        dao.clearAll()
    }
}
