package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "inventory_items")
data class InventoryItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val itemNo: Long,
    val itemName: String,
    val itemQuantity: Int,
    val itemPrice: Double,
    val totalPrice: Double,
    val timestamp: String,
    val syncedToSheet: Boolean = false
)
