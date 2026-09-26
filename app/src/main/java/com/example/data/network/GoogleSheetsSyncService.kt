package com.example.data.network

import com.example.data.model.InventoryItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed class SyncResult {
    data class Success(val message: String) : SyncResult()
    data class Failure(val error: String) : SyncResult()
}

class GoogleSheetsSyncService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun sendItemToSheet(webAppUrl: String, item: InventoryItem): SyncResult = withContext(Dispatchers.IO) {
        if (webAppUrl.isBlank()) {
            return@withContext SyncResult.Failure("Google Sheets Web App URL is empty")
        }

        try {
            val jsonObject = JSONObject().apply {
                put("timestamp", item.timestamp)
                put("itemNo", item.itemNo)
                put("itemName", item.itemName)
                put("itemQuantity", item.itemQuantity)
                put("itemPrice", item.itemPrice)
                put("totalItemPrice", item.totalPrice)
            }

            val requestBody = jsonObject.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(webAppUrl.trim())
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyString = response.body?.string() ?: ""
                    SyncResult.Success(if (bodyString.isNotBlank()) bodyString else "Row added successfully")
                } else {
                    SyncResult.Failure("HTTP ${response.code}: ${response.message}")
                }
            }
        } catch (e: Exception) {
            SyncResult.Failure(e.localizedMessage ?: "Network connection error")
        }
    }

    suspend fun testConnection(webAppUrl: String): SyncResult = withContext(Dispatchers.IO) {
        if (webAppUrl.isBlank()) {
            return@withContext SyncResult.Failure("Please enter a valid Web App URL")
        }

        try {
            val request = Request.Builder()
                .url(webAppUrl.trim())
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    SyncResult.Success(if (body.isNotBlank()) "Connected! Response: $body" else "Connected successfully!")
                } else {
                    SyncResult.Failure("HTTP ${response.code}: ${response.message}")
                }
            }
        } catch (e: Exception) {
            SyncResult.Failure(e.localizedMessage ?: "Failed to connect to Web App URL")
        }
    }
}
