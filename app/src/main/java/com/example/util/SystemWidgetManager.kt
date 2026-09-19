package com.example.util

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SystemWidgetProviderItem(
    val providerInfo: AppWidgetProviderInfo,
    val appLabel: String,
    val widgetLabel: String,
    val appPackage: String,
    val appIcon: Drawable?,
    val minWidth: Int,
    val minHeight: Int
)

class FluxAppWidgetHost(context: Context, hostId: Int = 2048) : AppWidgetHost(context, hostId) {
    override fun onCreateView(
        context: Context,
        appWidgetId: Int,
        appWidget: AppWidgetProviderInfo?
    ): AppWidgetHostView {
        return super.onCreateView(context, appWidgetId, appWidget)
    }
}

object SystemWidgetManager {

    private const val HOST_ID = 2048
    private var appWidgetHost: FluxAppWidgetHost? = null
    private var appWidgetManager: AppWidgetManager? = null

    private val _installedWidgetProviders = MutableStateFlow<List<SystemWidgetProviderItem>>(emptyList())
    val installedWidgetProviders: StateFlow<List<SystemWidgetProviderItem>> = _installedWidgetProviders.asStateFlow()

    fun init(context: Context) {
        if (appWidgetHost == null) {
            appWidgetHost = FluxAppWidgetHost(context.applicationContext, HOST_ID)
        }
        if (appWidgetManager == null) {
            appWidgetManager = AppWidgetManager.getInstance(context.applicationContext)
        }
        loadInstalledProviders(context)
    }

    fun startListening() {
        try {
            appWidgetHost?.startListening()
        } catch (e: Exception) {
            // Safe
        }
    }

    fun stopListening() {
        try {
            appWidgetHost?.stopListening()
        } catch (e: Exception) {
            // Safe
        }
    }

    fun allocateAppWidgetId(): Int {
        return try {
            appWidgetHost?.allocateAppWidgetId() ?: -1
        } catch (e: Exception) {
            -1
        }
    }

    fun deleteAppWidgetId(appWidgetId: Int) {
        try {
            if (appWidgetId > 0) {
                appWidgetHost?.deleteAppWidgetId(appWidgetId)
            }
        } catch (e: Exception) {
            // Safe
        }
    }

    fun bindWidgetIfAllowed(context: Context, appWidgetId: Int, provider: ComponentName): Boolean {
        val manager = appWidgetManager ?: AppWidgetManager.getInstance(context)
        return try {
            manager.bindAppWidgetIdIfAllowed(appWidgetId, provider)
        } catch (e: Exception) {
            false
        }
    }

    fun getAppWidgetInfo(context: Context, appWidgetId: Int): AppWidgetProviderInfo? {
        val manager = appWidgetManager ?: AppWidgetManager.getInstance(context)
        return try {
            manager.getAppWidgetInfo(appWidgetId)
        } catch (e: Exception) {
            null
        }
    }

    fun createWidgetHostView(
        context: Context,
        appWidgetId: Int,
        providerInfo: AppWidgetProviderInfo
    ): AppWidgetHostView? {
        return try {
            val host = appWidgetHost ?: FluxAppWidgetHost(context, HOST_ID).also { appWidgetHost = it }
            val view = host.createView(context, appWidgetId, providerInfo)
            view.setAppWidget(appWidgetId, providerInfo)
            view
        } catch (e: Exception) {
            null
        }
    }

    fun loadInstalledProviders(context: Context) {
        try {
            val manager = appWidgetManager ?: AppWidgetManager.getInstance(context)
            val pm = context.packageManager
            val providers = manager.installedProviders ?: emptyList()

            val list = providers.mapNotNull { info ->
                try {
                    val pkg = info.provider.packageName
                    val appInfo = pm.getApplicationInfo(pkg, 0)
                    val appLabel = pm.getApplicationLabel(appInfo).toString()
                    val widgetLabel = info.loadLabel(pm) ?: appLabel
                    val appIcon = try {
                        pm.getApplicationIcon(appInfo)
                    } catch (e: Exception) {
                        null
                    }

                    SystemWidgetProviderItem(
                        providerInfo = info,
                        appLabel = appLabel,
                        widgetLabel = widgetLabel,
                        appPackage = pkg,
                        appIcon = appIcon,
                        minWidth = info.minWidth,
                        minHeight = info.minHeight
                    )
                } catch (e: Exception) {
                    null
                }
            }.sortedWith(compareBy({ it.appLabel.lowercase() }, { it.widgetLabel.lowercase() }))

            _installedWidgetProviders.value = list
        } catch (e: Exception) {
            _installedWidgetProviders.value = emptyList()
        }
    }
}
