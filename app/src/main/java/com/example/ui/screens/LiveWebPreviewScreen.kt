package com.example.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.View
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import com.example.ui.theme.SheetsForestGreen
import com.example.util.WebTemplates
import java.io.File
import java.util.Locale

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LiveWebPreviewScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedViewMode by remember { mutableIntStateOf(0) } // 0: Interactive Web Simulation, 1: Embedded WebView
    var isRendererCrashed by remember { mutableStateOf(false) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    BackHandler {
        onBack()
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Top Toolbar
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = androidx.compose.ui.graphics.RectangleShape
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("preview_back_button")) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Web Data-Entry Preview",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = SheetsForestGreen
                    )
                    Text(
                        text = "Mobile Bootstrap 5 Inventory Layout",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Open in External Browser
                IconButton(
                    onClick = {
                        openHtmlInExternalBrowser(context)
                    },
                    modifier = Modifier.testTag("open_external_browser_button")
                ) {
                    Icon(
                        Icons.Default.OpenInBrowser,
                        contentDescription = "Open in External Browser",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // View Mode Switcher
        TabRow(
            selectedTabIndex = selectedViewMode,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = SheetsForestGreen
        ) {
            Tab(
                selected = selectedViewMode == 0,
                onClick = { selectedViewMode = 0 },
                text = { Text("Web Simulation", fontSize = 13.sp, fontWeight = if (selectedViewMode == 0) FontWeight.Bold else FontWeight.Normal) }
            )
            Tab(
                selected = selectedViewMode == 1,
                onClick = { selectedViewMode = 1 },
                text = { Text("Embedded WebView", fontSize = 13.sp, fontWeight = if (selectedViewMode == 1) FontWeight.Bold else FontWeight.Normal) }
            )
        }

        if (selectedViewMode == 0) {
            // Interactive, crash-immune Compose Web Simulation
            BootstrapWebSimulation(context = context)
        } else {
            // Safe Embedded WebView with Software Layer and onRenderProcessGone crash protection
            if (isRendererCrashed) {
                RendererCrashFallback(
                    onRetry = {
                        isRendererCrashed = false
                    },
                    onSwitchToSimulation = {
                        selectedViewMode = 0
                    }
                )
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    AndroidView(
                        factory = { ctx ->
                            try {
                                WebView(ctx).apply {
                                    // Disable hardware acceleration on view to avoid MESA rendernode GPU crash in cloud emulator
                                    setLayerType(View.LAYER_TYPE_SOFTWARE, null)

                                    settings.apply {
                                        javaScriptEnabled = true
                                        domStorageEnabled = true
                                        useWideViewPort = true
                                        loadWithOverviewMode = true
                                        cacheMode = WebSettings.LOAD_NO_CACHE
                                    }

                                    webViewClient = object : WebViewClient() {
                                        override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                                            Log.w("LiveWebPreview", "Chromium render process crash caught safely.")
                                            isRendererCrashed = true
                                            try {
                                                view?.destroy()
                                            } catch (t: Throwable) {
                                                Log.e("LiveWebPreview", "Error destroying webview", t)
                                            }
                                            // Returning TRUE prevents the host application from terminating
                                            return true
                                        }
                                    }

                                    webChromeClient = WebChromeClient()

                                    try {
                                        loadDataWithBaseURL(
                                            "https://script.google.com",
                                            WebTemplates.HTML_BOOTSTRAP_CODE,
                                            "text/html",
                                            "UTF-8",
                                            null
                                        )
                                    } catch (e: Exception) {
                                        Log.e("LiveWebPreview", "Failed to load HTML into WebView", e)
                                    }
                                    webViewInstance = this
                                }
                            } catch (e: Throwable) {
                                Log.e("LiveWebPreview", "Failed to initialize WebView in emulator environment", e)
                                isRendererCrashed = true
                                View(ctx)
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("html_webview")
                    )
                }
            }
        }
    }
}

@Composable
private fun RendererCrashFallback(
    onRetry: () -> Unit,
    onSwitchToSimulation: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Warning,
            contentDescription = null,
            tint = Color(0xFFE65100),
            modifier = Modifier.size(54.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Virtual Emulator Graphics Notice",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "The cloud container emulator environment does not provide a direct hardware GPU rendernode for Chromium. Your app was protected from terminating by our crash handler.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = onSwitchToSimulation,
                colors = ButtonDefaults.buttonColors(containerColor = SheetsForestGreen)
            ) {
                Text("Use Web Simulation")
            }
            OutlinedButton(onClick = onRetry) {
                Text("Retry WebView")
            }
        }
    }
}

data class WebSubmission(
    val no: String,
    val name: String,
    val qty: Int,
    val price: Double,
    val total: Double
)

@Composable
private fun BootstrapWebSimulation(context: Context) {
    val scroll = rememberScrollState()

    var itemNo by remember { mutableStateOf("101") }
    var autoIncrement by remember { mutableStateOf(true) }
    var itemName by remember { mutableStateOf("") }
    var itemQty by remember { mutableIntStateOf(1) }
    var itemPrice by remember { mutableStateOf("") }
    var webAppUrl by remember { mutableStateOf("") }
    var showSuccess by remember { mutableStateOf(false) }

    val recentSubmissions = remember {
        mutableStateListOf(
            WebSubmission("100", "Sample Packaging Boxes", 10, 3.50, 35.00)
        )
    }

    val priceVal = itemPrice.toDoubleOrNull() ?: 0.0
    val totalCalculated = itemQty * priceVal

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF3F6F4))
            .verticalScroll(scroll)
            .padding(bottom = 32.dp)
    ) {
        // Bootstrap Green Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF0F9D58), Color(0xFF0B8043))
                    ),
                    shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
                )
                .padding(horizontal = 16.dp, vertical = 24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Web, contentDescription = null, tint = Color.White)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Inventory Data Entry",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                )
                Text(
                    text = "Interactive Bootstrap 5 & JavaScript Web Layout",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.8f))
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Success alert
            if (showSuccess) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFD4EDDA)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF155724))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Item submitted! Form cleared and ready for next entry.",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium, color = Color(0xFF155724)),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Main Web Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Item No
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "1. Item No",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Auto-increment", fontSize = 12.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.width(4.dp))
                            Switch(
                                checked = autoIncrement,
                                onCheckedChange = { autoIncrement = it },
                                colors = SwitchDefaults.colors(checkedTrackColor = SheetsForestGreen)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = itemNo,
                        onValueChange = { if (!autoIncrement) itemNo = it },
                        readOnly = autoIncrement,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    // Item Name
                    Text("2. Item Name", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                    OutlinedTextField(
                        value = itemName,
                        onValueChange = { itemName = it },
                        placeholder = { Text("e.g. Ergonomic Office Chair") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    // Quantity and Price
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("3. Quantity", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedButton(
                                    onClick = { itemQty = (itemQty - 1).coerceAtLeast(1) },
                                    modifier = Modifier.size(36.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(14.dp))
                                }
                                Text(
                                    text = "$itemQty",
                                    modifier = Modifier.weight(1f),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    fontWeight = FontWeight.Bold
                                )
                                OutlinedButton(
                                    onClick = { itemQty++ },
                                    modifier = Modifier.size(36.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                }
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("4. Price ($)", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = itemPrice,
                                onValueChange = { itemPrice = it },
                                placeholder = { Text("0.00") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }

                    // Live Total Item Price Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Total Item Price", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                Text("$itemQty units × $${String.format(Locale.US, "%.2f", priceVal)}", fontSize = 11.sp, color = Color.Gray)
                            }
                            Text(
                                text = "$${String.format(Locale.US, "%.2f", totalCalculated)}",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = SheetsForestGreen
                                )
                            )
                        }
                    }

                    // Submit Button
                    Button(
                        onClick = {
                            if (itemName.isNotBlank()) {
                                recentSubmissions.add(
                                    0,
                                    WebSubmission(
                                        no = itemNo,
                                        name = itemName,
                                        qty = itemQty,
                                        price = priceVal,
                                        total = totalCalculated
                                    )
                                )
                                showSuccess = true
                                itemName = ""
                                itemQty = 1
                                itemPrice = ""
                                if (autoIncrement) {
                                    itemNo = ((itemNo.toIntOrNull() ?: 100) + 1).toString()
                                }
                            } else {
                                Toast.makeText(context, "Please enter an item name", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SheetsForestGreen),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Submit to Google Sheet", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Submissions Log Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Recent Submissions (${recentSubmissions.size})",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Color.Gray)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    recentSubmissions.forEach { sub ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("#${sub.no} ${sub.name}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Text("$${String.format(Locale.US, "%.2f", sub.total)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SheetsForestGreen)
                        }
                    }
                }
            }
        }
    }
}

private fun openHtmlInExternalBrowser(context: Context) {
    try {
        val file = File(context.cacheDir, "inventory_form.html")
        file.writeText(WebTemplates.HTML_BOOTSTRAP_CODE)
        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, file)

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "text/html")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Open HTML Form"))
    } catch (e: Exception) {
        Toast.makeText(context, "Could not open browser: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
