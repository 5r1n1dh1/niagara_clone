package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.NewsPreferencesState
import com.example.domain.RemoteConfig
import com.example.ui.theme.GlassStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsWidgetSetupSheet(
    initial: NewsPreferencesState,
    glass: GlassStyle = GlassStyle(),
    onSave: (NewsPreferencesState) -> Unit,
    onDismiss: () -> Unit
) {
    var apiKey by remember { mutableStateOf(initial.apiKey) }
    var selectedLanguage by remember { mutableStateOf(initial.language) }
    var selectedCategories by remember { mutableStateOf(initial.categories) }
    var keywords by remember { mutableStateOf(initial.keywords) }
    var pageSize by remember { mutableIntStateOf(initial.pageSize) }
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

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
                        Text(text = "Configure your personalised feed", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }

                LazyColumn(
                    modifier = Modifier.heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(text = "API Key", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            OutlinedTextField(
                                value = apiKey,
                                onValueChange = { apiKey = it.trim() },
                                label = { Text("Currents API Key") },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = {
                                    val uri = Uri.parse("https://currentsapi.services")
                                    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                }) {
                                    Text("Get free API key")
                                }
                                TextButton(onClick = {
                                    clipboardManager.getText()?.text?.let { clip ->
                                        apiKey = clip.removePrefix("Bearer ").trim()
                                    }
                                }) {
                                    Text("Paste from clipboard")
                                }
                            }
                        }
                    }

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(text = "Language", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("en", "es", "fr", "de").forEach { lang ->
                                    FilterChip(
                                        selected = selectedLanguage == lang,
                                        onClick = { selectedLanguage = lang },
                                        label = { Text(lang.uppercase()) }
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(text = "Categories", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            val popularCategories = listOf("general", "technology", "business", "science", "sports", "entertainment")
                            popularCategories.forEach { cat ->
                                val selected = cat in selectedCategories
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedCategories = if (selected) {
                                                if (selectedCategories.size > 1) selectedCategories - cat else selectedCategories
                                            } else {
                                                selectedCategories + cat
                                            }
                                        }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = cat.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodyMedium)
                                    Checkbox(checked = selected, onCheckedChange = null)
                                }
                            }
                        }
                    }

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(text = "Keywords (Optional)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            OutlinedTextField(
                                value = keywords,
                                onValueChange = { keywords = it },
                                label = { Text("e.g. Bitcoin, AI, SpaceX") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        onSave(
                            initial.copy(
                                apiKey = apiKey,
                                configured = apiKey.isNotBlank(),
                                language = selectedLanguage,
                                categories = selectedCategories,
                                keywords = keywords,
                                pageSize = pageSize
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save preferences")
                }
            }
        }
    }
}
