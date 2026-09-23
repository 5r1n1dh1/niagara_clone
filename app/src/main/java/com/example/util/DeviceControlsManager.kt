package com.example.util

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DeviceControlsState(
    val isTorchOn: Boolean = false,
    val hasTorch: Boolean = true,
    val isWifiOn: Boolean = true,
    val isBluetoothOn: Boolean = false,
    val ringerMode: Int = AudioManager.RINGER_MODE_NORMAL, // 2: Normal, 1: Vibrate, 0: Silent
    val batteryPercent: Int = 100,
    val isBatteryCharging: Boolean = false
)

object DeviceControlsManager {

    private val _controlsState = MutableStateFlow(DeviceControlsState())
    val controlsState: StateFlow<DeviceControlsState> = _controlsState.asStateFlow()

    private var cameraManager: CameraManager? = null
    private var torchCameraId: String? = null
    private var isInitialized = false

    private val torchCallback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        object : CameraManager.TorchCallback() {
            override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                if (cameraId == torchCameraId) {
                    _controlsState.value = _controlsState.value.copy(isTorchOn = enabled)
                }
            }

            override fun onTorchModeUnavailable(cameraId: String) {
                if (cameraId == torchCameraId) {
                    _controlsState.value = _controlsState.value.copy(isTorchOn = false)
                }
            }
        }
    } else null

    fun initialize(context: Context) {
        if (isInitialized) return
        isInitialized = true

        val appContext = context.applicationContext

        // 1. Setup Camera Torch
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val cm = appContext.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
                cameraManager = cm
                if (cm != null) {
                    for (id in cm.cameraIdList) {
                        val chars = cm.getCameraCharacteristics(id)
                        val flashAvailable = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                        val facing = chars.get(CameraCharacteristics.LENS_FACING)
                        if (flashAvailable && facing == CameraCharacteristics.LENS_FACING_BACK) {
                            torchCameraId = id
                            break
                        }
                    }
                    if (torchCameraId == null && cm.cameraIdList.isNotEmpty()) {
                        torchCameraId = cm.cameraIdList[0]
                    }
                    if (torchCallback != null) {
                        cm.registerTorchCallback(torchCallback, null)
                    }
                }
            } catch (_: Throwable) {
                _controlsState.value = _controlsState.value.copy(hasTorch = false)
            }
        }

        // 2. Initial state sync
        refreshState(appContext)

        // 3. Register Battery & Connectivity Broadcast Receivers
        try {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_BATTERY_CHANGED)
                addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
                addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
                addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            }
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(c: Context?, intent: Intent?) {
                    c?.let { refreshState(it) }
                }
            }
            ContextCompat.registerReceiver(
                appContext,
                receiver,
                filter,
                ContextCompat.RECEIVER_EXPORTED
            )
        } catch (_: Throwable) {
            // Ignore broadcast registration exception if any
        }
    }

    fun refreshState(context: Context) {
        val appContext = context.applicationContext

        // Battery
        val bm = appContext.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val batteryPct = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 85
        val batteryIntent = appContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

        // Audio Ringer
        val am = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val ringer = am?.ringerMode ?: AudioManager.RINGER_MODE_NORMAL

        // Wi-Fi
        val wm = appContext.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val isWifi = wm?.isWifiEnabled ?: true

        // Bluetooth
        val isBt = try {
            val adapter = BluetoothAdapter.getDefaultAdapter()
            adapter?.isEnabled == true
        } catch (_: Throwable) {
            false
        }

        _controlsState.value = _controlsState.value.copy(
            batteryPercent = batteryPct.coerceIn(0, 100),
            isBatteryCharging = isCharging,
            ringerMode = ringer,
            isWifiOn = isWifi,
            isBluetoothOn = isBt
        )
    }

    fun toggleTorch(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val cm = cameraManager ?: return false
            val id = torchCameraId ?: return false
            return try {
                val newState = !_controlsState.value.isTorchOn
                cm.setTorchMode(id, newState)
                _controlsState.value = _controlsState.value.copy(isTorchOn = newState)
                true
            } catch (_: CameraAccessException) {
                false
            } catch (_: Throwable) {
                false
            }
        }
        return false
    }

    fun openWifiSettings(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val intent = Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } else {
                val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        } catch (_: Throwable) {
            try {
                val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Throwable) {}
        }
    }

    fun openBluetoothSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Throwable) {
            try {
                val intent = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Throwable) {}
        }
    }

    fun toggleRingerMode(context: Context) {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        try {
            val nextMode = when (am.ringerMode) {
                AudioManager.RINGER_MODE_NORMAL -> AudioManager.RINGER_MODE_VIBRATE
                AudioManager.RINGER_MODE_VIBRATE -> AudioManager.RINGER_MODE_SILENT
                else -> AudioManager.RINGER_MODE_NORMAL
            }
            am.ringerMode = nextMode
            _controlsState.value = _controlsState.value.copy(ringerMode = nextMode)
        } catch (_: Throwable) {
            // If DND permission prevents direct change, open sound settings
            try {
                val intent = Intent(Settings.ACTION_SOUND_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Throwable) {}
        }
    }

    fun openBatterySettings(context: Context) {
        try {
            val intent = Intent(Intent.ACTION_POWER_USAGE_SUMMARY).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Throwable) {
            try {
                val intent = Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Throwable) {}
        }
    }

    fun openDigitalWellbeing(context: Context) {
        val intents = listOf(
            // 1. Google Pixel / AOSP Digital Wellbeing TopLevelSettingsActivity
            Intent(Intent.ACTION_MAIN).apply {
                component = android.content.ComponentName("com.google.android.apps.wellbeing", "com.google.android.apps.wellbeing.home.TopLevelSettingsActivity")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
            // 2. Google Digital Wellbeing action WELLBEING
            Intent("com.google.android.apps.wellbeing.action.WELLBEING").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
            // 3. Google Digital Wellbeing action SHOW_DASHBOARD
            Intent("com.google.android.apps.wellbeing.action.SHOW_DASHBOARD").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
            // 4. Samsung Digital Wellbeing
            Intent().apply {
                component = android.content.ComponentName("com.samsung.android.forest", "com.samsung.android.forest.home.ui.MainActivity")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
            // 5. Xiaomi / HyperOS Screen Time
            Intent().apply {
                component = android.content.ComponentName("com.miui.securitycenter", "com.miui.powercenter.digitalwellbeing.DigitalWellbeingActivity")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
            // 6. Generic Settings Digital Wellbeing
            Intent("android.settings.DIGITAL_WELLBEING").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )

        for (intent in intents) {
            try {
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                    return
                }
            } catch (_: Throwable) {}
        }

        // Try direct launch by package name
        try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage("com.google.android.apps.wellbeing")
                ?: context.packageManager.getLaunchIntentForPackage("com.samsung.android.forest")
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                return
            }
        } catch (_: Throwable) {}

        // Fallback: Usage Access or Settings
        try {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Throwable) {
            try {
                val intent = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Throwable) {}
        }
    }
}
