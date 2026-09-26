package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.InventoryItem
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryDao {
    @Query("SELECT * FROM inventory_items ORDER BY id DESC")
    fun getAllItems(): Flow<List<InventoryItem>>

    @Query("SELECT MAX(itemNo) FROM inventory_items")
    suspend fun getMaxItemNo(): Long?

    @Query("SELECT COUNT(*) FROM inventory_items")
    fun getItemCount(): Flow<Int>

    @Query("SELECT * FROM inventory_items WHERE syncedToSheet = 0")
    suspend fun getUnsyncedItems(): List<InventoryItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: InventoryItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<InventoryItem>): List<Long>

    @Update
    suspend fun updateItem(item: InventoryItem)

    @Query("UPDATE inventory_items SET syncedToSheet = 1 WHERE id = :id")
    suspend fun markAsSynced(id: Long)

    @Delete
    suspend fun deleteItem(item: InventoryItem)

    @Query("DELETE FROM inventory_items")
    suspend fun clearAll()
}
