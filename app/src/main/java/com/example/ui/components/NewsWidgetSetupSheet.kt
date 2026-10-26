package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.NewsPreferencesState
import com.example.domain.RemoteConfig
import com.example.ui.theme.GlassStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsWidgetSetupSheet(
    initial: NewsPreferencesState,
    glass: GlassStyle = GlassStyle(),
    onSave: (NewsPreferencesState) -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var step by remember { mutableIntStateOf(0) }
    val totalSteps = 6

    var apiKey by remember { mutableStateOf(initial.apiKey) }
    var showPassword by remember { mutableStateOf(false) }
    var selectedLanguage by remember { mutableStateOf(initial.language) }
    var selectedCountry by remember { mutableStateOf(initial.country) }
    var selectedCategories by remember { mutableStateOf(initial.categories) }
    var keywords by remember { mutableStateOf(initial.keywords) }
    var query by remember { mutableStateOf(initial.query) }
    var requireImages by remember { mutableStateOf(initial.requireImages) }
    var useDateWindow by remember { mutableStateOf(initial.useDateWindow) }
    var windowHours by remember { mutableIntStateOf(initial.windowHours) }
    var pageSize by remember { mutableIntStateOf(initial.pageSize) }
    var minRefreshMinutes by remember { mutableIntStateOf(initial.minRefreshMinutes) }
    var configured by remember { mutableStateOf(initial.configured) }

    var validationMsg by remember { mutableStateOf<String?>(null) }
    var isValidating by remember { mutableStateOf(false) }

    BackHandler(enabled = true) {
        if (step > 0) step-- else onDismiss()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.Transparent,
        scrimColor = Color.Black.copy(alpha = 0.45f)
    ) {
        FrostedGlassPanel(
            cornerRadius = 28.dp,
            glass = glass,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(bottom = 20.dp)
                .testTag("news_widget_setup_sheet")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "Currents News Setup", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(text = "Step ${step + 1} of $totalSteps", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }

                // Step content
                when (step) {
                    0 -> StepWelcome(context = context)
                    1 -> StepApiKey(
                        apiKey = apiKey,
                        showPassword = showPassword,
                        validationMsg = validationMsg,
                        isValidating = isValidating,
                        onKeyChange = { apiKey = it.trim() },
                        onToggleShow = { showPassword = !showPassword },
                        onValidate = {
                            scope.launch {
                                isValidating = true
                                validationMsg = null
                                val trimmed = apiKey.trim()
                                if (trimmed.isBlank() || trimmed.length < 8) {
                                    validationMsg = "Key too short (min 8 chars)"
                                    isValidating = false
                                    return@launch
                                }
                                if (trimmed.contains(" ")) {
                                    validationMsg = "Key contains whitespace"
                                    isValidating = false
                                    return@launch
                                }
                                val stripped = trimmed.removePrefix("Bearer ").trim()
                                val urlStr = "${RemoteConfig.CURRENTS_API_BASE}/available/categories"
                                val conn = try {
                                    (URL(urlStr).openConnection() as HttpURLConnection).apply {
                                        connectTimeout = 5000
                                        readTimeout = 5000
                                        setRequestProperty("User-Agent", RemoteConfig.CURRENTS_API_USER_AGENT)
                                        setRequestProperty("Accept", "application/json")
                                        setRequestProperty("Authorization", "Bearer $stripped")
                                    }
                                } catch (e: Exception) {
                                    validationMsg = "Network setup error"
                                    isValidating = false
                                    return@launch
                                }
                                try {
                                    val code = conn.responseCode
                                    val remaining = conn.getHeaderField("X-RateLimit-Remaining")?.toIntOrNull() ?: 100
                                    val retryAfter = conn.getHeaderField("Retry-After")?.toLongOrNull()
                                    when {
                                        code == 200 -> validationMsg = "Key valid — Quota: $remaining left"
                                        code == 401 -> validationMsg = "This key was rejected by Currents."
                                        code == 429 -> validationMsg = "Quota exhausted. Retry after ${retryAfter ?: "unknown"}s"
                                        else -> validationMsg = "Server error ($code)"
                                    }
                                } catch (e: Exception) {
                                    validationMsg = "Validation error: ${e.message}"
                                } finally {
                                    try { conn.disconnect() } catch (_: Throwable) {}
                                }
                                isValidating = false
                            }
                        },
                        onPaste = {
                            clipboardManager.getText()?.text?.let { clip ->
                                apiKey = clip.removePrefix("Bearer ").trim()
                            }
                        },
                        clipboardManager = clipboardManager
                    )
                    2 -> StepLanguageRegion(
                        selectedLanguage = selectedLanguage,
                        selectedCountry = selectedCountry,
                        onLanguageChange = { selectedLanguage = it },
                        onCountryChange = { selectedCountry = it }
                    )
                    3 -> StepCategories(
                        selectedCategories = selectedCategories,
                        onToggleCategory = { cat ->
                            selectedCategories = if (cat in selectedCategories) {
                                if (selectedCategories.size > 1) selectedCategories - cat else selectedCategories
                            } else {
                                selectedCategories + cat
                            }
                        },
                        onPreset = { preset -> selectedCategories = preset }
                    )
                    4 -> StepKeywords(
                        keywords = keywords,
                        query = query,
                        requireImages = requireImages,
                        useDateWindow = useDateWindow,
                        windowHours = windowHours,
                        onKeywordsChange = { keywords = it },
                        onQueryChange = { query = it },
                        onImagesToggle = { requireImages = !requireImages },
                        onDateWindowToggle = { useDateWindow = !useDateWindow },
                        onWindowHoursChange = { windowHours = it.coerceIn(1, RemoteConfig.CURRENTS_FREE_MAX_WINDOW_HOURS) }
                    )
                    5 -> StepCadence(
                        keywords = keywords,
                        query = query,
                        pageSize = pageSize,
                        minRefreshMinutes = minRefreshMinutes,
                        onPageSizeChange = { pageSize = it.coerceIn(1, RemoteConfig.CURRENTS_FREE_MAX_PAGE_SIZE) },
                        onCadenceChange = { minRefreshMinutes = it }
                    )
                }

                // Footer navigation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (step > 0) {
                        TextButton(onClick = { step-- }) {
                            Text("Back")
                        }
                    } else {
                        Box(modifier = Modifier.width(0.dp))
                    }

                    if (step == totalSteps - 1) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (initial.configured) {
                                TextButton(onClick = {
                                    configured = false
                                    onSave(initial.copy(apiKey = "", configured = false, categories = selectedCategories))
                                }) {
                                    Text("Remove key")
                                }
                            }
                            Button(
                                onClick = {
                                    configured = apiKey.isNotBlank()
                                    onSave(
                                        initial.copy(
                                            apiKey = apiKey,
                                            configured = configured,
                                            language = selectedLanguage,
                                            country = selectedCountry,
                                            categories = selectedCategories,
                                            keywords = keywords,
                                            query = query,
                                            useDateWindow = useDateWindow,
                                            windowHours = windowHours,
                                            requireImages = requireImages,
                                            pageSize = pageSize,
                                            minRefreshMinutes = minRefreshMinutes
                                        )
                                    )
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Save preferences")
                            }
                        }
                    } else {
                        Button(
                            onClick = {
                                when (step) {
                                    0 -> step++
                                    1 -> if (apiKey.trim().isNotEmpty() && validationMsg?.startsWith("Key valid") == true) step++
                                    2 -> if (selectedLanguage.isNotBlank()) step++
                                    3 -> if (selectedCategories.isNotEmpty()) step++
                                    4 -> step++
                                }
                            },
                            enabled = when (step) {
                                0 -> true
                                1 -> apiKey.trim().isNotEmpty() && (validationMsg?.startsWith("Key valid") == true || !isValidating)
                                2 -> selectedLanguage.isNotBlank()
                                3 -> selectedCategories.isNotEmpty()
                                4 -> true
                                else -> false
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Next")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepWelcome(context: android.content.Context) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("What is Currents?", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(
            "Currents provides real-time news headlines. You must create a free account and copy your API key. The key stays only on this device.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row {
            TextButton(onClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://currentsapi.services")))
            }) {
                Text("Open currentsapi.services")
            }
        }
    }
}

@Composable
private fun StepApiKey(
    apiKey: String,
    showPassword: Boolean,
    validationMsg: String?,
    isValidating: Boolean,
    onKeyChange: (String) -> Unit,
    onToggleShow: () -> Unit,
    onValidate: () -> Unit,
    onPaste: () -> Unit,
    clipboardManager: androidx.compose.ui.platform.ClipboardManager
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("API Key", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = apiKey,
            onValueChange = onKeyChange,
            label = { Text("Currents API Key") },
            singleLine = true,
            visualTransformation = if (showPassword) VisualTransformation { text ->
                androidx.compose.ui.text.input.TransformedText(
                    androidx.compose.ui.text.AnnotatedString(text.text),
                    object : androidx.compose.ui.text.input.OffsetMapping {
                        override fun originalToTransformed(offset: Int): Int = offset
                        override fun transformedToOriginal(offset: Int): Int = offset
                    }
                )
            } else PasswordVisualTransformation(),
            trailingIcon = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onToggleShow, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = "Toggle visibility",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = {
                clipboardManager.getText()?.text?.let { clip ->
                    onPaste()
                }
            }) {
                Text("Paste")
            }
            Button(onClick = onValidate, enabled = !isValidating && apiKey.trim().isNotEmpty(), shape = RoundedCornerShape(8.dp)) {
                if (isValidating) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text("Validate")
            }
        }
        validationMsg?.let { msg ->
            val isOk = msg.startsWith("Key valid")
            Text(
                text = msg,
                color = if (isOk) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                fontSize = 12.sp
            )
        }
        Text(
            text = "Paste the full key. If you copied a curl line, the Bearer prefix is stripped automatically.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
private fun StepLanguageRegion(
    selectedLanguage: String,
    selectedCountry: String,
    onLanguageChange: (String) -> Unit,
    onCountryChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Language", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            RemoteConfig.CURRENTS_LANGUAGES.forEach { lang ->
                FilterChip(
                    selected = selectedLanguage == lang,
                    onClick = { onLanguageChange(lang) },
                    label = { Text(lang.uppercase()) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text("Region (optional)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val regionLabels = mapOf(
                "" to "Worldwide",
                "US" to "United States", "GB" to "United Kingdom", "CA" to "Canada",
                "AU" to "Australia", "DE" to "Germany", "FR" to "France",
                "IN" to "India", "JP" to "Japan", "BR" to "Brazil",
                "MX" to "Mexico", "IT" to "Italy", "ES" to "Spain",
                "NL" to "Netherlands", "RU" to "Russia", "CN" to "China"
            )
            (listOf("") + RemoteConfig.CURRENTS_REGIONS).distinct().forEach { region ->
                FilterChip(
                    selected = selectedCountry == region,
                    onClick = { onCountryChange(region) },
                    label = { Text(regionLabels[region] ?: region) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun StepCategories(
    selectedCategories: Set<String>,
    onToggleCategory: (String) -> Unit,
    onPreset: (Set<String>) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Categories", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            FilterChip(
                selected = selectedCategories.containsAll(setOf("general", "world")),
                onClick = { onPreset(setOf("general", "world")) },
                label = { Text("Top stories", fontSize = 11.sp) }
            )
            FilterChip(
                selected = selectedCategories.containsAll(setOf("technology", "programming")),
                onClick = { onPreset(setOf("technology", "programming")) },
                label = { Text("Tech", fontSize = 11.sp) }
            )
            FilterChip(
                selected = selectedCategories.containsAll(setOf("finance", "business")),
                onClick = { onPreset(setOf("finance", "business")) },
                label = { Text("Money", fontSize = 11.sp) }
            )
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            RemoteConfig.CURRENTS_CATEGORIES.forEach { cat ->
                val selected = cat in selectedCategories
                FilterChip(
                    selected = selected,
                    onClick = { onToggleCategory(cat) },
                    label = { Text(cat.replaceFirstChar { it.uppercase() }, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }
        if (selectedCategories.isEmpty()) {
            Text("Select at least one category", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
        }
    }
}

@Composable
private fun StepKeywords(
    keywords: String,
    query: String,
    requireImages: Boolean,
    useDateWindow: Boolean,
    windowHours: Int,
    onKeywordsChange: (String) -> Unit,
    onQueryChange: (String) -> Unit,
    onImagesToggle: () -> Unit,
    onDateWindowToggle: () -> Unit,
    onWindowHoursChange: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Keywords (optional)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = keywords,
            onValueChange = onKeywordsChange,
            label = { Text("e.g. Bitcoin, AI") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(
                selected = requireImages,
                onClick = onImagesToggle,
                label = { Text("Only with images", fontSize = 11.sp) }
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Advanced query", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
            Switch(checked = useDateWindow, onCheckedChange = { onDateWindowToggle() })
        }
        if (useDateWindow) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                label = { Text("Advanced boolean query") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(1, 6, 12, 24, 168).forEach { h ->
                    FilterChip(
                        selected = windowHours == h,
                        onClick = { onWindowHoursChange(h) },
                        label = { Text("${h}h", fontSize = 11.sp) }
                    )
                }
            }
            Text(
                "Search is limited to the last 7 days on the free plan.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (keywords.isNotBlank() || query.isNotBlank()) {
            Text(
                "Using Currents search — free plan searches at most the last 7 days.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun StepCadence(
    keywords: String,
    query: String,
    pageSize: Int,
    minRefreshMinutes: Int,
    onPageSizeChange: (Int) -> Unit,
    onCadenceChange: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Refresh cadence", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(Pair(0, "Manual"), Pair(360, "6h"), Pair(60, "1h"), Pair(15, "15m")).forEach { (min, label) ->
                FilterChip(
                    selected = minRefreshMinutes == min,
                    onClick = { onCadenceChange(min) },
                    label = { Text(label, fontSize = 11.sp) }
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text("Articles per refresh", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(5, 10, 20).forEach { sz ->
                FilterChip(
                    selected = pageSize == sz,
                    onClick = { onPageSizeChange(sz) },
                    label = { Text("$sz (max 20)", fontSize = 11.sp) }
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text("Review request", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        val endpoint = if (keywords.isNotBlank() || query.isNotBlank()) "search" else "latest-news"
        val reviewUrl = "GET /v1/$endpoint?language=${RemoteConfig.CURRENTS_LANGUAGES.first()}&category=general&page_size=$pageSize"
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = reviewUrl,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}


