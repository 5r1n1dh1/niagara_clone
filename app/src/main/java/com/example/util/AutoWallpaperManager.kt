package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import com.example.data.AutoWallpaperConfig
import com.example.data.AutoWallpaperSource
import com.example.data.WallpaperItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import java.net.URLDecoder
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class AutoWallpaperManager(private val context: Context) {

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    private val wallpaperCacheDir by lazy {
        File(context.cacheDir, "auto_wallpapers").apply { if (!exists()) mkdirs() }
    }

    /**
     * Checks if a wallpaper switch is required based on schedule interval or missing file.
     */
    fun shouldSwitchWallpaper(config: AutoWallpaperConfig): Boolean {
        if (!config.isEnabled) return false
        if (config.currentWallpaperUrlOrPath.isEmpty()) return true
        
        val activeFile = File(wallpaperCacheDir, "active_wallpaper.jpg")
        if (!activeFile.exists() || activeFile.length() == 0L) return true

        val intervalMs = config.interval.hours * 3600 * 1000L
        val elapsedMs = System.currentTimeMillis() - config.lastSwitchTimeMs
        return elapsedMs >= intervalMs
    }

    /**
     * Executes wallpaper refresh based on selected Album source.
     */
    suspend fun fetchNextWallpaper(config: AutoWallpaperConfig): AutoWallpaperResult = withContext(Dispatchers.IO) {
        try {
            val items = when (config.source) {
                AutoWallpaperSource.LOCAL_FOLDER -> fetchLocalFolderWallpapers(config)
                AutoWallpaperSource.GIT_REPO -> fetchGitRepoWallpapers(config)
                AutoWallpaperSource.GOOGLE_PHOTOS -> fetchGooglePhotosWallpapers(config)
            }

            if (items.isEmpty()) {
                val fallbackItems = getFallbackWallpapers(config.source)
                val selected = fallbackItems.random()
                return@withContext downloadAndCacheWallpaper(selected, config)
            }

            // Select next wallpaper (filter out current if possible)
            val candidates = items.filter { it.imageUrlOrPath != config.currentWallpaperUrlOrPath }
            val selected = if (candidates.isNotEmpty()) candidates.random() else items.random()

            return@withContext downloadAndCacheWallpaper(selected, config)
        } catch (e: Exception) {
            e.printStackTrace()
            // On network or IO error, fallback safely
            val fallbackItem = getFallbackWallpapers(config.source).random()
            return@withContext downloadAndCacheWallpaper(fallbackItem, config)
        }
    }

    /**
     * Downloads/loads wallpaper image file and caches it locally.
     */
    private suspend fun downloadAndCacheWallpaper(
        item: WallpaperItem,
        config: AutoWallpaperConfig
    ): AutoWallpaperResult = withContext(Dispatchers.IO) {
        val targetFile = File(wallpaperCacheDir, "active_wallpaper.jpg")

        if (item.imageUrlOrPath.startsWith("http://") || item.imageUrlOrPath.startsWith("https://")) {
            val request = Request.Builder()
                .url(item.imageUrlOrPath)
                .header("User-Agent", "FluxLauncher/1.0 (Android)")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful || response.body == null) {
                throw Exception("HTTP request failed: ${response.code}")
            }

            response.body!!.byteStream().use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }
        } else if (item.imageUrlOrPath.startsWith("content://")) {
            context.contentResolver.openInputStream(Uri.parse(item.imageUrlOrPath))?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }
        } else {
            // Local file path
            val srcFile = File(item.imageUrlOrPath)
            if (srcFile.exists()) {
                srcFile.inputStream().use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }
            } else {
                throw Exception("Local wallpaper file does not exist: ${item.imageUrlOrPath}")
            }
        }

        val bitmap = BitmapFactory.decodeFile(targetFile.absolutePath)
            ?: throw Exception("Failed to decode wallpaper bitmap")

        val updatedConfig = config.copy(
            currentWallpaperUrlOrPath = item.imageUrlOrPath,
            currentWallpaperTitle = item.title,
            currentWallpaperSourceInfo = item.authorOrSource,
            lastSwitchTimeMs = System.currentTimeMillis()
        )

        AutoWallpaperResult.Success(
            config = updatedConfig,
            bitmap = bitmap,
            cachedFilePath = targetFile.absolutePath
        )
    }

    /**
     * Local Folder Source Resolver
     */
    private fun fetchLocalFolderWallpapers(config: AutoWallpaperConfig): List<WallpaperItem> {
        val resultList = mutableListOf<WallpaperItem>()

        // 1. Check custom specified folder path
        if (config.localFolderPath.isNotBlank()) {
            val dir = File(config.localFolderPath)
            if (dir.exists() && dir.isDirectory) {
                dir.listFiles { file ->
                    val name = file.name.lowercase()
                    name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".webp")
                }?.forEach { f ->
                    resultList.add(
                        WallpaperItem(
                            id = f.name,
                            title = f.nameWithoutExtension.replace("_", " ").capitalizeWords(),
                            imageUrlOrPath = f.absolutePath,
                            authorOrSource = "Local Folder: ${dir.name}",
                            sourceKind = AutoWallpaperSource.LOCAL_FOLDER
                        )
                    )
                }
            }
        }

        // 2. Check standard Pictures / Wallpapers directories on device
        if (resultList.isEmpty()) {
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val searchDirs = listOfNotNull(
                picturesDir,
                File(picturesDir, "Wallpapers"),
                downloadDir
            )

            for (dir in searchDirs) {
                if (dir.exists() && dir.isDirectory) {
                    dir.listFiles { file ->
                        val name = file.name.lowercase()
                        name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".webp")
                    }?.take(20)?.forEach { f ->
                        resultList.add(
                            WallpaperItem(
                                id = f.name,
                                title = f.nameWithoutExtension.replace("_", " ").capitalizeWords(),
                                imageUrlOrPath = f.absolutePath,
                                authorOrSource = "Local Storage: ${dir.name}",
                                sourceKind = AutoWallpaperSource.LOCAL_FOLDER
                            )
                        )
                    }
                }
            }
        }

        return resultList
    }

    /**
     * Git Repository Source Resolver via GitHub API & Raw Git URLs
     */
    private fun fetchGitRepoWallpapers(config: AutoWallpaperConfig): List<WallpaperItem> {
        val rawUrl = config.gitRepoUrl.trim()
        if (rawUrl.isBlank()) return emptyList()

        // Extract GitHub owner/repo e.g. "dharmx/walls" from "https://github.com/dharmx/walls"
        val (owner, repo) = parseGitHubOwnerRepo(rawUrl) ?: return emptyList()
        val branch = if (config.gitBranch.isBlank()) "main" else config.gitBranch.trim()
        val path = config.gitSubfolder.trim().trim('/')

        val apiUrl = if (path.isEmpty()) {
            "https://api.github.com/repos/$owner/$repo/contents?ref=$branch"
        } else {
            "https://api.github.com/repos/$owner/$repo/contents/$path?ref=$branch"
        }

        val request = Request.Builder()
            .url(apiUrl)
            .header("User-Agent", "FluxLauncher/1.0 (Android)")
            .header("Accept", "application/vnd.github.v3+json")
            .build()

        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful || response.body == null) {
            return emptyList()
        }

        val jsonString = response.body!!.string()
        val jsonArray = JSONArray(jsonString)
        val resultList = mutableListOf<WallpaperItem>()

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.optJSONObject(i) ?: continue
            val type = obj.optString("type")
            val name = obj.optString("name")
            val downloadUrl = obj.optString("download_url")

            val lowerName = name.lowercase()
            val isImage = lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") || 
                          lowerName.endsWith(".png") || lowerName.endsWith(".webp")

            if (type == "file" && isImage && downloadUrl.isNotBlank()) {
                val title = name.substringBeforeLast(".")
                    .replace("-", " ")
                    .replace("_", " ")
                    .capitalizeWords()

                resultList.add(
                    WallpaperItem(
                        id = name,
                        title = title,
                        imageUrlOrPath = downloadUrl,
                        authorOrSource = "Git: $owner/$repo ($branch)",
                        sourceKind = AutoWallpaperSource.GIT_REPO
                    )
                )
            }
        }

        return resultList
    }

    /**
     * Google Photos Album Source Resolver
     */
    private fun fetchGooglePhotosWallpapers(config: AutoWallpaperConfig): List<WallpaperItem> {
        val albumUrl = config.googlePhotosAlbumUrl.trim()
        if (albumUrl.isBlank()) return emptyList()

        val request = Request.Builder()
            .url(albumUrl)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
            .build()

        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful || response.body == null) {
            return emptyList()
        }

        val html = response.body!!.string()
        // Extract Google Photos high-res image URLs matching pattern https://lh3.googleusercontent.com/pw/...
        val pattern = Pattern.compile("https://lh3\\.googleusercontent\\.com/pw/[a-zA-Z0-9_\\-]+")
        val matcher = pattern.matcher(html)

        val urlSet = mutableSetOf<String>()
        while (matcher.find()) {
            val matchedUrl = matcher.group()
            // Append high quality image dimensions parameter =w1920-h1080-no
            urlSet.add("$matchedUrl=w1920-h1080-no")
        }

        val resultList = mutableListOf<WallpaperItem>()
        urlSet.take(30).forEachIndexed { index, imgUrl ->
            resultList.add(
                WallpaperItem(
                    id = "gphoto_$index",
                    title = "Google Photo #${index + 1}",
                    imageUrlOrPath = imgUrl,
                    authorOrSource = "Google Photos Album",
                    sourceKind = AutoWallpaperSource.GOOGLE_PHOTOS
                )
            )
        }

        return resultList
    }

    /**
     * Curated Fallback Collections for Offline/Empty States
     */
    fun getFallbackWallpapers(source: AutoWallpaperSource): List<WallpaperItem> {
        return when (source) {
            AutoWallpaperSource.GIT_REPO -> listOf(
                WallpaperItem(
                    id = "git_1",
                    title = "Emerald Geometric Waves",
                    imageUrlOrPath = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?q=80&w=1920",
                    authorOrSource = "Git: dharmx/walls",
                    sourceKind = AutoWallpaperSource.GIT_REPO
                ),
                WallpaperItem(
                    id = "git_2",
                    title = "Nordic Pine Mountain Peak",
                    imageUrlOrPath = "https://images.unsplash.com/photo-1506744038136-46273834b3fb?q=80&w=1920",
                    authorOrSource = "Git: linuxdotexe/nordic-wallpapers",
                    sourceKind = AutoWallpaperSource.GIT_REPO
                ),
                WallpaperItem(
                    id = "git_3",
                    title = "Minimal Dark Sphere",
                    imageUrlOrPath = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?q=80&w=1920",
                    authorOrSource = "Git: D3vh/Minimal-Wallpapers",
                    sourceKind = AutoWallpaperSource.GIT_REPO
                ),
                WallpaperItem(
                    id = "git_4",
                    title = "Cyberpunk Neo Tokyo Rain",
                    imageUrlOrPath = "https://images.unsplash.com/photo-1519501025264-65ba15a82390?q=80&w=1920",
                    authorOrSource = "Git: dharmx/walls",
                    sourceKind = AutoWallpaperSource.GIT_REPO
                )
            )
            AutoWallpaperSource.GOOGLE_PHOTOS -> listOf(
                WallpaperItem(
                    id = "gphoto_1",
                    title = "Serene Alpine Reflection Lake",
                    imageUrlOrPath = "https://images.unsplash.com/photo-1470071459604-3b5ec3a7fe05?q=80&w=1920",
                    authorOrSource = "Google Photos: Nature Masterpieces",
                    sourceKind = AutoWallpaperSource.GOOGLE_PHOTOS
                ),
                WallpaperItem(
                    id = "gphoto_2",
                    title = "Misty Forest Dawn",
                    imageUrlOrPath = "https://images.unsplash.com/photo-1448375240586-882707db888b?q=80&w=1920",
                    authorOrSource = "Google Photos: Nature Masterpieces",
                    sourceKind = AutoWallpaperSource.GOOGLE_PHOTOS
                ),
                WallpaperItem(
                    id = "gphoto_3",
                    title = "Architectural Glass & Steel",
                    imageUrlOrPath = "https://images.unsplash.com/photo-1486406146926-c627a92ad1ab?q=80&w=1920",
                    authorOrSource = "Google Photos: Modern Minimal",
                    sourceKind = AutoWallpaperSource.GOOGLE_PHOTOS
                )
            )
            AutoWallpaperSource.LOCAL_FOLDER -> listOf(
                WallpaperItem(
                    id = "local_1",
                    title = "Flux Obsidian Minimalist",
                    imageUrlOrPath = "https://images.unsplash.com/photo-1550684848-fac1c5b4e853?q=80&w=1920",
                    authorOrSource = "Local Folder: Dark AMOLED",
                    sourceKind = AutoWallpaperSource.LOCAL_FOLDER
                ),
                WallpaperItem(
                    id = "local_2",
                    title = "Golden Hour Desert Dunes",
                    imageUrlOrPath = "https://images.unsplash.com/photo-1509316975850-ff9c5deb0cd9?q=80&w=1920",
                    authorOrSource = "Local Folder: Nature",
                    sourceKind = AutoWallpaperSource.LOCAL_FOLDER
                )
            )
        }
    }

    private fun parseGitHubOwnerRepo(url: String): Pair<String, String>? {
        var clean = url.trim().removePrefix("https://").removePrefix("http://")
        clean = clean.removePrefix("github.com/").removeSuffix(".git")
        val parts = clean.split('/')
        if (parts.size >= 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
            return Pair(parts[0], parts[1])
        }
        return null
    }

    private fun String.capitalizeWords(): String {
        return this.split(" ")
            .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
    }
}

sealed class AutoWallpaperResult {
    data class Success(
        val config: AutoWallpaperConfig,
        val bitmap: Bitmap,
        val cachedFilePath: String
    ) : AutoWallpaperResult()

    data class Error(val message: String) : AutoWallpaperResult()
}
