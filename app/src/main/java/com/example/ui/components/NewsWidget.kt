package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.NewsError
import com.example.data.NewsFeedState
import com.example.data.NewsPreferencesState
import com.example.data.NewsStory
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun NewsWidget(
    state: NewsFeedState,
    prefs: NewsPreferencesState,
    size: String = "4x2",
    blurRadius: Float = 15f,
    translucency: Float = 0.30f,
    glassEffect: Float = 0.30f,
    isEmbeddedInCard: Boolean = false,
    onOpenArticle: (String) -> Unit,
    onOpenSetup: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val content = @Composable {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(12.dp)
                .testTag("news_widget_$size"),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("news_widget_header"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Newspaper,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "NEWS FEED",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (state is NewsFeedState.Ready && state.remainingQuota <= 20) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "${state.remainingQuota} left",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    IconButton(onClick = onRefresh, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onOpenSetup, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Body State
            when (state) {
                is NewsFeedState.NeedsApiKey -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Connect Currents API",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Add your free Currents API key to view personalised news headlines.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Button(
                            onClick = onOpenSetup,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Set up feed")
                        }
                    }
                }
                is NewsFeedState.Loading -> {
                    if (state.cached.isNotEmpty()) {
                        StoryList(stories = state.cached, size = size, onOpenArticle = onOpenArticle)
                    } else {
                        Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(28.dp))
                        }
                    }
                }
                is NewsFeedState.Ready -> {
                    if (state.stories.isEmpty()) {
                        EmptyStateMessage(onOpenSetup)
                    } else {
                        StoryList(stories = state.stories, size = size, onOpenArticle = onOpenArticle)
                    }
                }
                is NewsFeedState.Stale -> {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(12.dp))
                            Text(text = "Showing offline cache", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                        }
                        if (state.stories.isNotEmpty()) {
                            StoryList(stories = state.stories, size = size, onOpenArticle = onOpenArticle)
                        } else {
                            EmptyStateMessage(onOpenSetup)
                        }
                    }
                }
                is NewsFeedState.Failed -> {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val message = when (val err = state.error) {
                            is NewsError.Offline -> "No internet connection"
                            is NewsError.InvalidKey -> "API key rejected"
                            is NewsError.QuotaExceeded -> "Daily quota exhausted"
                            is NewsError.BadRequest -> err.msg
                            is NewsError.Server -> "Server error (${err.code})"
                            is NewsError.Unknown -> err.msg
                        }
                        Text(text = message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                        Button(onClick = onOpenSetup, shape = RoundedCornerShape(8.dp)) {
                            Text("Configure API Key")
                        }
                    }
                }
            }
        }
    }

    if (isEmbeddedInCard) {
        content()
    } else {
        Surface(
            modifier = modifier,
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f),
            border = BorderStroke(1.dp, Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.28f), MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f))))
        ) {
            content()
        }
    }
}

@Composable
private fun StoryList(stories: List<NewsStory>, size: String, onOpenArticle: (String) -> Unit) {
    val maxItems = when (size) {
        "4x1" -> 1
        "4x2" -> 2
        "4x3" -> 4
        else -> 6
    }
    val visibleStories = stories.take(maxItems)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        visibleStories.forEachIndexed { index, story ->
            StoryRow(story = story, size = size, index = index, onOpenArticle = onOpenArticle)
            if (index < visibleStories.size - 1) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), thickness = 0.5.dp)
            }
        }
    }
}

@Composable
private fun StoryRow(story: NewsStory, size: String, index: Int, onOpenArticle: (String) -> Unit) {
    val maxLines = if (size == "4x1") 1 else 2
    val timeAgo = formatTimeAgo(story.publishedAtEpochMillis)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onOpenArticle(story.url) }
            .padding(vertical = 6.dp)
            .testTag("news_widget_row_$index"),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (!story.imageUrl.isNullOrBlank() && size != "4x1") {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                AsyncImage(
                    model = story.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().testTag("news_widget_image_$index")
                )
            }
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = story.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = maxLines,
                overflow = TextOverflow.Ellipsis
            )
            if (story.description.isNotBlank() && size != "4x1") {
                Text(
                    text = story.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                ) {
                    Text(
                        text = story.category.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
                Text(
                    text = story.publisher,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(text = "•", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = timeAgo,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmptyStateMessage(onOpenSetup: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = "No articles found", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick = onOpenSetup) {
            Text("Adjust filters")
        }
    }
}

private fun formatTimeAgo(epochMillis: Long): String {
    val diff = System.currentTimeMillis() - epochMillis
    val minutes = diff / (1000 * 60)
    val hours = minutes / 60
    val days = hours / 24
    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days < 7 -> "${days}d ago"
        else -> SimpleDateFormat("MMM d", Locale.US).format(Date(epochMillis))
    }
}
