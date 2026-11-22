package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TubeStoreDao {

    @Query("SELECT * FROM local_channels ORDER BY createdAt DESC")
    fun observeChannels(): Flow<List<LocalChannelEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertChannel(channel: LocalChannelEntity)

    @Query("DELETE FROM local_channels WHERE id = :channelId")
    suspend fun deleteChannelById(channelId: String)

    @Query("SELECT * FROM roadmap_milestones WHERE channelId = :channelId ORDER BY weekNumber ASC")
    fun observeMilestonesForChannel(channelId: String): Flow<List<RoadmapMilestoneEntity>>

    @Query("SELECT * FROM roadmap_milestones ORDER BY weekNumber ASC")
    fun observeAllMilestones(): Flow<List<RoadmapMilestoneEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMilestones(milestones: List<RoadmapMilestoneEntity>)

    @Update
    suspend fun updateMilestone(milestone: RoadmapMilestoneEntity)

    @Query("DELETE FROM roadmap_milestones WHERE channelId = :channelId")
    suspend fun deleteMilestonesForChannel(channelId: String)

    @Transaction
    suspend fun createChannelWithRoadmap(
        channel: LocalChannelEntity,
        milestones: List<RoadmapMilestoneEntity>
    ) {
        upsertChannel(channel)
        deleteMilestonesForChannel(channel.id)
        insertMilestones(milestones)
    }

    @Transaction
    suspend fun deleteChannelWithRoadmap(channelId: String) {
        deleteMilestonesForChannel(channelId)
        deleteChannelById(channelId)
    }

    @Query("SELECT * FROM local_apps ORDER BY createdAt DESC")
    fun observeLocalApps(): Flow<List<LocalAppEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLocalApp(app: LocalAppEntity)

    @Query("DELETE FROM local_apps WHERE id = :appId")
    suspend fun deleteLocalAppById(appId: String)

    @Query("UPDATE local_apps SET launchCount = launchCount + 1 WHERE id = :appId")
    suspend fun incrementAppLaunch(appId: String)

    @Query("SELECT * FROM local_videos ORDER BY createdAt DESC")
    fun observeLocalVideos(): Flow<List<LocalVideoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLocalVideo(video: LocalVideoEntity)

    @Query("DELETE FROM local_videos WHERE id = :videoId")
    suspend fun deleteLocalVideoById(videoId: String)

    @Query("UPDATE local_videos SET viewCount = viewCount + 1 WHERE id = :videoId")
    suspend fun incrementVideoView(videoId: String)

    @Query("SELECT * FROM local_bookmarks ORDER BY createdAt DESC")
    fun observeBookmarks(): Flow<List<LocalBookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBookmark(bookmark: LocalBookmarkEntity)

    @Query("DELETE FROM local_bookmarks WHERE bookmarkKey = :bookmarkKey")
    suspend fun deleteBookmark(bookmarkKey: String)
}
