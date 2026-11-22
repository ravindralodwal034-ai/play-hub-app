package com.example.data.local

import com.example.model.AppCategory
import com.example.model.VideoCategory
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class LocalGuestRepository(private val dao: TubeStoreDao) {

    val channels: Flow<List<LocalChannelEntity>> = dao.observeChannels()
    val allMilestones: Flow<List<RoadmapMilestoneEntity>> = dao.observeAllMilestones()
    val localApps: Flow<List<LocalAppEntity>> = dao.observeLocalApps()
    val localVideos: Flow<List<LocalVideoEntity>> = dao.observeLocalVideos()
    val bookmarks: Flow<List<LocalBookmarkEntity>> = dao.observeBookmarks()

    suspend fun createChannel(
        name: String,
        handle: String,
        niche: String,
        language: String,
        weeklyVideoTarget: Int,
        weeklyShortsTarget: Int,
        bio: String,
        accentIndex: Int
    ): String {
        val channelId = "ch_${UUID.randomUUID().toString().take(10)}"
        val cleanHandle = handle.trim().let {
            when {
                it.isEmpty() -> "@${name.trim().lowercase().replace(Regex("[^a-z0-9]"), "")}"
                it.startsWith("@") -> it
                else -> "@$it"
            }
        }
        val channel = LocalChannelEntity(
            id = channelId,
            name = name.trim(),
            handle = cleanHandle,
            niche = niche.trim().ifBlank { "Marwadi Music & Shorts" },
            language = language.trim().ifBlank { "Marwadi / Rajasthani" },
            weeklyVideoTarget = weeklyVideoTarget.coerceIn(1, 14),
            weeklyShortsTarget = weeklyShortsTarget.coerceIn(0, 21),
            bio = bio.trim(),
            accentIndex = accentIndex
        )
        val milestones = LocalChannelRoadmapGenerator.generate12WeekRoadmap(
            channelId = channelId,
            channelName = channel.name,
            niche = channel.niche,
            language = channel.language,
            weeklyVideoTarget = channel.weeklyVideoTarget,
            weeklyShortsTarget = channel.weeklyShortsTarget
        )
        dao.createChannelWithRoadmap(channel, milestones)
        return channelId
    }

    suspend fun deleteChannel(channelId: String) {
        dao.deleteChannelWithRoadmap(channelId)
    }

    suspend fun toggleMilestoneCompleted(milestone: RoadmapMilestoneEntity) {
        dao.updateMilestone(milestone.copy(isCompleted = !milestone.isCompleted))
    }

    suspend fun adjustMilestoneUploads(milestone: RoadmapMilestoneEntity, delta: Int) {
        val updatedCount = (milestone.completedUploads + delta).coerceIn(0, 50)
        val autoDone = if (updatedCount >= milestone.targetUploads) true else milestone.isCompleted
        dao.updateMilestone(
            milestone.copy(
                completedUploads = updatedCount,
                isCompleted = autoDone
            )
        )
    }

    suspend fun saveLocalApp(
        id: String? = null,
        creatorName: String,
        title: String,
        tagline: String,
        description: String,
        category: AppCategory,
        downloadUrl: String,
        versionName: String,
        accentIndex: Long
    ) {
        val entity = LocalAppEntity(
            id = id ?: "local_app_${UUID.randomUUID().toString().take(10)}",
            creatorName = creatorName.trim().ifBlank { "Tube Creator" },
            title = title.trim(),
            tagline = tagline.trim().ifBlank { "Free instant web tool" },
            description = description.trim().ifBlank { tagline.trim() },
            category = category.label,
            downloadUrl = normalizeHttps(downloadUrl),
            versionName = versionName.trim().ifBlank { "1.0" },
            accentIndex = accentIndex
        )
        dao.upsertLocalApp(entity)
    }

    suspend fun deleteLocalApp(appId: String) {
        dao.deleteLocalAppById(appId)
    }

    suspend fun recordLocalAppLaunch(appId: String) {
        dao.incrementAppLaunch(appId)
    }

    suspend fun saveLocalVideo(
        id: String? = null,
        channelName: String,
        title: String,
        description: String,
        category: VideoCategory,
        language: String,
        videoUrl: String,
        durationLabel: String,
        accentIndex: Long
    ) {
        val entity = LocalVideoEntity(
            id = id ?: "local_vid_${UUID.randomUUID().toString().take(10)}",
            channelName = channelName.trim().ifBlank { "Tube Creator" },
            title = title.trim(),
            description = description.trim().ifBlank { "Free community video & audio stream." },
            category = category.label,
            language = language.trim().ifBlank { "Marwadi / Rajasthani" },
            videoUrl = normalizeHttps(videoUrl),
            durationLabel = durationLabel.trim().ifBlank { "05:00" },
            accentIndex = accentIndex
        )
        dao.upsertLocalVideo(entity)
    }

    suspend fun deleteLocalVideo(videoId: String) {
        dao.deleteLocalVideoById(videoId)
    }

    suspend fun recordLocalVideoView(videoId: String) {
        dao.incrementVideoView(videoId)
    }

    suspend fun toggleBookmark(targetId: String, targetType: String, currentlyBookmarked: Boolean) {
        val key = "${targetType}_$targetId"
        if (currentlyBookmarked) {
            dao.deleteBookmark(key)
        } else {
            dao.upsertBookmark(
                LocalBookmarkEntity(
                    bookmarkKey = key,
                    targetId = targetId,
                    targetType = targetType
                )
            )
        }
    }

    private fun normalizeHttps(raw: String): String {
        val trimmed = raw.trim()
        return when {
            trimmed.isBlank() -> "https://duckduckgo.com"
            trimmed.startsWith("http://", ignoreCase = true) ||
                trimmed.startsWith("https://", ignoreCase = true) -> trimmed
            else -> "https://$trimmed"
        }
    }
}
