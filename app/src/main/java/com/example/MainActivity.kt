package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.InventoryFormScreen
import com.example.ui.screens.InventoryListScreen
import com.example.ui.screens.LiveWebPreviewScreen
import com.example.ui.screens.WebCodeGuideScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SheetsForestGreen
import com.example.ui.util.AppLanguage
import com.example.ui.util.AppStrings
import com.example.ui.util.AppThemeMode
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.InventoryViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: InventoryViewModel = viewModel()
            val themeMode by vm.currentThemeMode.collectAsState()
            val systemDark = isSystemInDarkTheme()
            val isDark = when (themeMode) {
                AppThemeMode.SYSTEM -> systemDark
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
            }

            MyApplicationTheme(darkTheme = isDark) {
                InventoryApp(vm)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryApp(
    viewModel: InventoryViewModel = viewModel()
) {
    val currentTab by viewModel.currentTab.collectAsState()
    val currentLang by viewModel.currentLanguage.collectAsState()
    val currentTheme by viewModel.currentThemeMode.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (currentTab != AppTab.PREVIEW) {
                CenterAlignedTopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(SheetsForestGreen),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.TableChart,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = AppStrings.appTitle(currentLang),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    actions = {
                        // Language Switcher (EN <-> አማ)
                        FilledTonalButton(
                            onClick = { viewModel.toggleLanguage() },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .testTag("toggle_language_button")
                        ) {
                            Icon(
                                Icons.Default.Language,
                                contentDescription = "Language",
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (currentLang == AppLanguage.ENGLISH) "አማ" else "EN",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }

                        // Theme Mode Switcher (System -> Light -> Dark)
                        IconButton(
                            onClick = { viewModel.toggleThemeMode() },
                            modifier = Modifier.testTag("toggle_theme_button")
                        ) {
                            val icon = when (currentTheme) {
                                AppThemeMode.LIGHT -> Icons.Default.LightMode
                                AppThemeMode.DARK -> Icons.Default.DarkMode
                                AppThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
                            }
                            Icon(
                                imageVector = icon,
                                contentDescription = "Theme: ${currentTheme.name}",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == AppTab.FORM,
                    onClick = { viewModel.selectTab(AppTab.FORM) },
                    icon = { Icon(Icons.Default.EditNote, contentDescription = "Form") },
                    label = { Text(AppStrings.tabForm(currentLang), fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_tab_form"),
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
                NavigationBarItem(
                    selected = currentTab == AppTab.INVENTORY,
                    onClick = { viewModel.selectTab(AppTab.INVENTORY) },
                    icon = { Icon(Icons.Default.TableChart, contentDescription = "Inventory") },
                    label = { Text(AppStrings.tabInventory(currentLang), fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_tab_inventory"),
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
                NavigationBarItem(
                    selected = currentTab == AppTab.WEB_CODE,
                    onClick = { viewModel.selectTab(AppTab.WEB_CODE) },
                    icon = { Icon(Icons.Default.Code, contentDescription = "Web & Script") },
                    label = { Text(AppStrings.tabWebCode(currentLang), fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_tab_web_code"),
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
                NavigationBarItem(
                    selected = currentTab == AppTab.PREVIEW,
                    onClick = { viewModel.selectTab(AppTab.PREVIEW) },
                    icon = { Icon(Icons.Default.OpenInBrowser, contentDescription = "Web Preview") },
                    label = { Text(AppStrings.tabPreview(currentLang), fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_tab_preview"),
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        }
    ) { innerPadding ->
        when (currentTab) {
            AppTab.FORM -> InventoryFormScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
            )
            AppTab.INVENTORY -> InventoryListScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
            )
            AppTab.WEB_CODE -> WebCodeGuideScreen(
                onOpenPreview = { viewModel.selectTab(AppTab.PREVIEW) },
                modifier = Modifier.padding(innerPadding)
            )
            AppTab.PREVIEW -> LiveWebPreviewScreen(
                onBack = { viewModel.selectTab(AppTab.FORM) },
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}
