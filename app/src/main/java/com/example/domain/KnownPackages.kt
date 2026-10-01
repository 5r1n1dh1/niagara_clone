package com.example.domain

/**
 * Centralized Known Packages registry adhering to Phase 8 architectural hardening.
 */
object KnownPackages {
    const val YOUTUBE_MUSIC_OG = "com.google.android.apps.youtube.music"
    const val YOUTUBE_MUSIC_REVANCED = "app.revanced.android.apps.youtube.music"
    const val YOUTUBE_MUSIC_RVX = "app.rvx.android.apps.youtube.music"
    const val YOUTUBE_MUSIC_VANCED = "com.vanced.android.apps.youtube.music"
    const val INSTAGRAM = "com.instagram.android"
    const val REDDIT = "com.reddit.frontpage"
    const val AMAZON_SHOPPING = "com.amazon.mShop.android.shopping"

    fun isYouTubeMusic(packageName: String): Boolean {
        val pkg = packageName.lowercase()
        return pkg.contains("youtube.music") ||
                pkg == YOUTUBE_MUSIC_REVANCED ||
                pkg == YOUTUBE_MUSIC_RVX ||
                pkg == YOUTUBE_MUSIC_VANCED
    }
}
