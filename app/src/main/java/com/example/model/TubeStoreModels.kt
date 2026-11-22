package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

const val OWNER_ADMIN_EMAIL = "ravindralodwal034@gmail.com"

fun isVerifiedOwnerEmail(email: String?, isEmailVerified: Boolean): Boolean {
    return isEmailVerified && email?.trim().equals(OWNER_ADMIN_EMAIL, ignoreCase = true)
}

enum class AppCategory(val label: String) {
    TOOLS("Tools"),
    ENTERTAINMENT("Entertainment"),
    SOCIAL("Social"),
    GAMES("Games"),
    EDUCATION("Education"),
    THEMES("Themes")
}

enum class VideoCategory(val label: String) {
    CARTOONS("Cartoons"),
    MUSIC("Music"),
    GAMING("Gaming"),
    TECH("Tech"),
    COMEDY("Comedy"),
    EDUCATION("Education")
}

enum class TubeThemePreset(val label: String, val subtitle: String) {
    CRIMSON("Tube Crimson", "Bold coral-red & midnight navy"),
    CYBER("Cyber Neon", "Electric cyan & ultraviolet"),
    SAFFRON("Royal Saffron", "Vibrant marigold & obsidian"),
    EMERALD("Emerald Pulse", "Vivid green & deep forest")
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val studioName: String = "",
    val bio: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class UserBookmark(
    @DocumentId val id: String = "",
    val userId: String = "",
    val targetId: String = "",
    val targetType: String = "app",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class StoreApp(
    @DocumentId val id: String = "",
    val creatorId: String = "",
    val creatorName: String = "",
    val title: String = "",
    val tagline: String = "",
    val description: String = "",
    val category: String = AppCategory.TOOLS.label,
    val downloadUrl: String = "",
    val versionName: String = "1.0.0",
    val visibility: String = "public",
    val downloadCount: Long = 0L,
    val likeCount: Long = 0L,
    val accentIndex: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class TubeVideo(
    @DocumentId val id: String = "",
    val creatorId: String = "",
    val channelName: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = VideoCategory.CARTOONS.label,
    val language: String = "Global",
    val videoUrl: String = "",
    val durationLabel: String = "03:30",
    val visibility: String = "public",
    val viewCount: Long = 0L,
    val likeCount: Long = 0L,
    val accentIndex: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class LiveAppConfig(
    @DocumentId val id: String = "",
    val bannerText: String = "",
    val bannerEnabled: Boolean = false,
    val maintenanceMode: Boolean = false,
    val maintenanceMessage: String = "Tube Store is being updated by the owner. Please check back shortly.",
    val featuredLabel: String = "",
    val blockedIds: List<String> = emptyList(),
    val updatedBy: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ModerationReport(
    @DocumentId val id: String = "",
    val reporterId: String = "",
    val reporterName: String = "",
    val targetId: String = "",
    val targetType: String = "app",
    val targetTitle: String = "",
    val reason: String = "",
    val status: String = "open",
    val resolutionNote: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class InstalledAppInfo(
    val label: String,
    val packageName: String
)
