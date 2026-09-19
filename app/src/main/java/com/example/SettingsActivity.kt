package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.data.LauncherRepository
import com.example.ui.components.SettingsContent
import com.example.ui.theme.NiagaraTheme
import com.example.util.WidgetIntegrationsManager

class SettingsActivity : ComponentActivity() {

    private lateinit var repository: LauncherRepository
    private lateinit var widgetIntegrations: WidgetIntegrationsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        repository = LauncherRepository(applicationContext)
        widgetIntegrations = WidgetIntegrationsManager(application)

        setContent {
            val settings by repository.settings.collectAsState()
            val allApps by repository.allApps.collectAsState()

            NiagaraTheme(
                preset = settings.themePreset,
                fontFamily = settings.customFont
            ) {
                Scaffold(
                    topBar = {
                        @OptIn(ExperimentalMaterial3Api::class)
                        TopAppBar(
                            title = { Text("Flux Launcher Settings") },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back"
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background
                            )
                        )
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(innerPadding)
                    ) {
                        SettingsContent(
                            settings = settings,
                            allApps = allApps,
                            widgetIntegrations = widgetIntegrations,
                            onUpdateSettings = { repository.updateSettings(it) },
                            onExportBackup = { repository.exportBackupJson() },
                            onImportBackup = { repository.importBackupJson(it) },
                            onResetDefaults = { repository.resetToDefaults() },
                            onClose = { finish() },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}
