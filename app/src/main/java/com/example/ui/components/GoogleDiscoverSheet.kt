package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class DiscoverArticle(
    val id: String,
    val title: String,
    val publisher: String,
    val category: String,
    val timeAgo: String,
    val readMinutes: Int,
    val emoji: String,
    val url: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoogleDiscoverSheet(
    onDismiss: () -> Unit,
    blurRadius: Float = 15f,
    translucency: Float = 0.30f,
    glassEffect: Float = 0.30f,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val articles = listOf(
        DiscoverArticle(
            "1",
            "Gemini 2.5 and Next-Gen Multimodal Models Reshape Real-Time Mobile OS Workflows",
            "TechCrunch",
            "Artificial Intelligence",
            "2h ago",
            4,
            "🤖",
            "https://news.google.com"
        ),
        DiscoverArticle(
            "2",
            "James Webb Telescope Reveals Early Galaxy Clusters Defying Standard Cosmological Models",
            "Nature Astronomy",
            "Science",
            "3h ago",
            5,
            "🔭",
            "https://news.google.com"
        ),
        DiscoverArticle(
            "3",
            "Android 16 Unveils Revolutionary Predictive Ergonomic Gesture System",
            "Android Authority",
            "Android",
            "5h ago",
            3,
            "📱",
            "https://news.google.com"
        ),
        DiscoverArticle(
            "4",
            "Minimalist Homescreen Architecture: Why Less Distraction Sparks Greater Daily Focus",
            "Wired",
            "Digital Wellbeing",
            "7h ago",
            6,
            "🧘",
            "https://news.google.com"
        )
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.Transparent,
        scrimColor = Color.Black.copy(alpha = 0.45f),
        modifier = modifier.testTag("google_discover_sheet")
    ) {
        FrostedGlassPanel(
            cornerRadius = 28.dp,
            blurRadius = blurRadius.dp,
            translucency = translucency,
            glassEffect = glassEffect,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(bottom = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 18.dp)
            ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("G", fontWeight = FontWeight.Black, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Google Discover", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Personalized stories & updates", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                FilledTonalButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://google.com"))
                        context.startActivity(intent)
                    },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text("Open Feed", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Trending topics
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf("Tech", "AI", "Space", "Android", "Design").forEach { tag ->
                    SuggestionChip(
                        onClick = {},
                        label = { Text(tag, fontSize = 11.sp) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(articles, key = { it.id }) { article ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(article.url))
                                context.startActivity(intent)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                modifier = Modifier.size(52.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(article.emoji, fontSize = 28.sp)
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = article.category.uppercase(),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = article.title,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    lineHeight = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${article.publisher} • ${article.timeAgo} • ${article.readMinutes}m read",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
}
