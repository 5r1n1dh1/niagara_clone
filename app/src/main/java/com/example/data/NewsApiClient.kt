package com.example.data

import android.content.Context
import android.util.Log
import com.example.domain.RemoteConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

data class NewsFetchResult(
    val stories: List<NewsStory>,
    val rateLimitRemaining: Int,
    val rateLimitResetEpoch: Long
)

class NewsApiClient(private val context: Context, private val preferences: NewsPreferences) {
    private val tag = "NewsApiClient"

    suspend fun fetch(page: Int = 1): NewsFetchResult = withContext(Dispatchers.IO) {
        val prefs = preferences.load()
        val key = prefs.apiKey
        if (key.isBlank()) throw NewsApiException(-1, NewsError.InvalidKey("API key is blank"))

        val useSearch = prefs.keywords.isNotBlank() || prefs.query.isNotBlank() || prefs.useDateWindow
        val endpoint = if (useSearch) RemoteConfig.CURRENTS_SEARCH_PATH else RemoteConfig.CURRENTS_LATEST_NEWS_PATH
        val urlString = buildUrl(endpoint, prefs, page)

        val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15000
            readTimeout = 20000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", RemoteConfig.CURRENTS_API_USER_AGENT)
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Authorization", "Bearer $key")
        }

        try {
            val code = conn.responseCode
            val remaining = conn.getHeaderField("X-RateLimit-Remaining")?.toIntOrNull() ?: 100
            val reset = conn.getHeaderField("X-RateLimit-Reset")?.toLongOrNull() ?: (System.currentTimeMillis() / 1000 + 3600)

            if (code == 401) throw NewsApiException(401, NewsError.InvalidKey("API key rejected"))
            if (code == 429) {
                val retryAfter = conn.getHeaderField("Retry-After")?.toLongOrNull() ?: 3600L
                throw NewsApiException(429, NewsError.QuotaExceeded(retryAfter))
            }
            if (code == 400) {
                val errorBody = try { conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "" } catch (_: Throwable) { "" }
                throw NewsApiException(400, NewsError.BadRequest(null, errorBody.ifBlank { "Bad request" }))
            }
            if (code !in 200..299) {
                throw NewsApiException(code, NewsError.Server(code))
            }

            val responseBody = conn.inputStream.bufferedReader().use { it.readText() }
            val stories = parseStories(responseBody)
            NewsFetchResult(stories, remaining, reset)
        } catch (e: NewsApiException) {
            throw e
        } catch (t: Throwable) {
            Log.e(tag, "Network error fetching news: ${t.message}", t)
            throw NewsApiException(-1, NewsError.Offline)
        } finally {
            try {
                conn.disconnect()
            } catch (_: Throwable) {}
        }
    }

    private fun buildUrl(endpoint: String, prefs: NewsPreferencesState, page: Int): String {
        val sb = StringBuilder(RemoteConfig.CURRENTS_API_BASE).append(endpoint)
        sb.append("?language=").append(prefs.language)
        if (prefs.country.isNotBlank()) sb.append("&country=").append(prefs.country)
        if (prefs.categories.isNotEmpty()) {
            sb.append("&category=").append(prefs.categories.joinToString(","))
        }
        sb.append("&page_number=").append(page)
        sb.append("&page_size=").append(prefs.pageSize)

        if (endpoint == RemoteConfig.CURRENTS_SEARCH_PATH) {
            if (prefs.keywords.isNotBlank()) sb.append("&keywords=").append(android.net.Uri.encode(prefs.keywords))
            if (prefs.query.isNotBlank()) sb.append("&query=").append(android.net.Uri.encode(prefs.query))
            if (prefs.requireImages) sb.append("&has_image=true")
        }
        return sb.toString()
    }

    private fun parseStories(jsonStr: String): List<NewsStory> {
        val list = mutableListOf<NewsStory>()
        try {
            val root = JSONObject(jsonStr)
            val arr = root.optJSONArray("news") ?: return emptyList()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US).apply { isLenient = false }

            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val id = obj.optString("id", "")
                val title = obj.optString("title", "").trim()
                if (title.isBlank()) continue
                val description = obj.optString("description", "")
                val url = obj.optString("url", "")
                if (!url.startsWith("http")) continue

                val author = obj.optString("author", "")
                val publisher = try {
                    val host = URL(url).host.removePrefix("www.")
                    if (host.isNotBlank()) host else author.ifBlank { "News" }
                } catch (_: Throwable) {
                    author.ifBlank { "News" }
                }

                val image = obj.optString("image", "")
                val imageUrl = if (image.startsWith("http")) image else null

                val catArr = obj.optJSONArray("category")
                val category = if (catArr != null && catArr.length() > 0) catArr.optString(0, "general") else "general"

                val publishedStr = obj.optString("published", "")
                val publishedEpoch = try {
                    dateFormat.parse(publishedStr)?.time ?: System.currentTimeMillis()
                } catch (_: Throwable) {
                    System.currentTimeMillis()
                }

                list.add(NewsStory(id, title, description, url, publisher, imageUrl, category, publishedEpoch))
            }
        } catch (t: Throwable) {
            Log.e(tag, "Error parsing news JSON: ${t.message}", t)
        }
        return list.distinctBy { it.id }
    }
}

class NewsApiException(val code: Int, val newsError: NewsError) : Exception(newsError.toString())
