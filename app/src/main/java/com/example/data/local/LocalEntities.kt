package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.StoreApp
import com.example.model.TubeVideo
import com.example.model.UserBookmark

@Entity(tableName = "local_channels")
data class LocalChannelEntity(
    @PrimaryKey val id: String,
    val name: String,
    val handle: String,
    val niche: String,
    val language: String,
    val weeklyVideoTarget: Int = 2,
    val weeklyShortsTarget: Int = 3,
    val bio: String = "",
    val accentIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "roadmap_milestones")
data class RoadmapMilestoneEntity(
    @PrimaryKey val id: String,
    val channelId: String,
    val monthNumber: Int,
    val weekNumber: Int,
    val title: String,
    val guide: String,
    val targetUploads: Int,
    val completedUploads: Int = 0,
    val isCompleted: Boolean = false
)

@Entity(tableName = "local_apps")
data class LocalAppEntity(
    @PrimaryKey val id: String,
    val creatorName: String,
    val title: String,
    val tagline: String,
    val description: String,
    val category: String,
    val downloadUrl: String,
    val versionName: String,
    val accentIndex: Long = 0L,
    val launchCount: Long = 0L,
    val likeCount: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toStoreApp(): StoreApp = StoreApp(
        id = id,
        creatorId = "local_guest",
        creatorName = creatorName,
        title = title,
        tagline = tagline,
        description = description,
        category = category,
        downloadUrl = downloadUrl,
        versionName = versionName,
        visibility = "public",
        accentIndex = accentIndex,
        downloadCount = launchCount,
        likeCount = likeCount
    )
}

@Entity(tableName = "local_videos")
data class LocalVideoEntity(
    @PrimaryKey val id: String,
    val channelName: String,
    val title: String,
    val description: String,
    val category: String,
    val language: String,
    val videoUrl: String,
    val durationLabel: String,
    val accentIndex: Long = 0L,
    val viewCount: Long = 0L,
    val likeCount: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toTubeVideo(): TubeVideo = TubeVideo(
        id = id,
        creatorId = "local_guest",
        channelName = channelName,
        title = title,
        description = description,
        category = category,
        language = language,
        videoUrl = videoUrl,
        durationLabel = durationLabel,
        visibility = "public",
        accentIndex = accentIndex,
        viewCount = viewCount,
        likeCount = likeCount
    )
}

@Entity(tableName = "local_bookmarks")
data class LocalBookmarkEntity(
    @PrimaryKey val bookmarkKey: String,
    val targetId: String,
    val targetType: String,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toUserBookmark(): UserBookmark = UserBookmark(
        id = bookmarkKey,
        userId = "local_guest",
        targetId = targetId,
        targetType = targetType
    )
}
