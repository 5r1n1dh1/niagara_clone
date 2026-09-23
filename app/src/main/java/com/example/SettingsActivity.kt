package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.data.LauncherRepository
import com.example.ui.components.SettingsStudioSheet
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
            val dockPackageNames by repository.dockPackageNames.collectAsState()
            val userCategories by repository.userCategories.collectAsState()

            val dockApps = remember(dockPackageNames, allApps) {
                dockPackageNames.mapNotNull { pkg -> allApps.find { it.packageName == pkg } }
            }

            NiagaraTheme(
                preset = settings.themePreset,
                fontFamily = settings.customFont
            ) {
                SettingsStudioSheet(
                    settings = settings,
                    allApps = allApps,
                    dockApps = dockApps,
                    widgetIntegrations = widgetIntegrations,
                    onUpdateSettings = { repository.updateSettings(it) },
                    onSelectDockSlot = null,
                    onClearDockSlot = { slot -> repository.clearDockSlot(slot) },
                    onResetDockDefaults = { repository.resetDockToDefaults() },
                    onExportBackup = { repository.exportBackupJson() },
                    onImportBackup = { repository.importBackupJson(it) },
                    onResetDefaults = { repository.resetToDefaults() },
                    onOpenSetupGuide = {},
                    onOpenSystemWidgetPicker = {},
                    userCategories = userCategories,
                    onAddCategory = { repository.addUserCategory(it) },
                    onRenameCategory = { old, new -> repository.updateUserCategoryName(old, new) },
                    onRemoveCategory = { repository.removeUserCategory(it) },
                    onAssignCategory = { pkg, cat -> repository.setAppCategory(pkg, cat) },
                    onDismiss = { finish() },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
