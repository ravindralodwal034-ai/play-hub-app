package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.OpenCatalogData
import com.example.data.local.LocalAppEntity
import com.example.data.local.LocalChannelEntity
import com.example.data.local.LocalGuestRepository
import com.example.data.local.LocalVideoEntity
import com.example.data.local.RoadmapMilestoneEntity
import com.example.model.AppCategory
import com.example.model.StoreApp
import com.example.model.TubeVideo
import com.example.model.UserBookmark
import com.example.model.VideoCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LocalStudioViewModel(
    private val repository: LocalGuestRepository
) : ViewModel() {

    val channels: StateFlow<List<LocalChannelEntity>> = repository.channels
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMilestones: StateFlow<List<RoadmapMilestoneEntity>> = repository.allMilestones
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val localApps: StateFlow<List<LocalAppEntity>> = repository.localApps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val localVideos: StateFlow<List<LocalVideoEntity>> = repository.localVideos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val combinedApps: StateFlow<List<StoreApp>> = repository.localApps
        .map { custom ->
            custom.map { it.toStoreApp() } + OpenCatalogData.starterApps()
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            OpenCatalogData.starterApps()
        )

    val combinedVideos: StateFlow<List<TubeVideo>> = repository.localVideos
        .map { custom ->
            custom.map { it.toTubeVideo() } + OpenCatalogData.starterVideos()
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            OpenCatalogData.starterVideos()
        )

    val bookmarks: StateFlow<List<UserBookmark>> = repository.bookmarks
        .map { list -> list.map { it.toUserBookmark() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedChannelId = MutableStateFlow<String?>(null)
    val selectedChannelId: StateFlow<String?> = _selectedChannelId.asStateFlow()

    private val _filterState = MutableStateFlow(TubeStoreFilterState())
    val filterState: StateFlow<TubeStoreFilterState> = _filterState.asStateFlow()

    fun selectChannel(channelId: String) {
        _selectedChannelId.value = channelId
    }

    fun updateAppSearch(query: String) {
        _filterState.update { it.copy(appSearch = query) }
    }

    fun selectAppCategory(category: String) {
        _filterState.update { it.copy(selectedAppCategory = category) }
    }

    fun updateVideoSearch(query: String) {
        _filterState.update { it.copy(videoSearch = query) }
    }

    fun selectVideoCategory(category: String) {
        _filterState.update { it.copy(selectedVideoCategory = category) }
    }

    fun selectLanguageFilter(language: String) {
        _filterState.update { it.copy(selectedLanguage = language) }
    }

    fun selectChannelFilter(channelName: String) {
        _filterState.update { it.copy(selectedChannel = channelName) }
    }

    fun clearStatusMessage() {
        _filterState.update { it.copy(statusMessage = null) }
    }

    fun createChannel(
        name: String,
        handle: String,
        niche: String,
        language: String,
        weeklyVideoTarget: Int,
        weeklyShortsTarget: Int,
        bio: String,
        accentIndex: Int,
        onDone: () -> Unit = {}
    ) {
        if (name.isBlank()) {
            _filterState.update { it.copy(statusMessage = "Please enter a channel name") }
            return
        }
        viewModelScope.launch {
            val newId = repository.createChannel(
                name = name,
                handle = handle,
                niche = niche,
                language = language,
                weeklyVideoTarget = weeklyVideoTarget,
                weeklyShortsTarget = weeklyShortsTarget,
                bio = bio,
                accentIndex = accentIndex
            )
            _selectedChannelId.value = newId
            _filterState.update {
                it.copy(statusMessage = "Channel '$name' launched with a 90-day roadmap!")
            }
            onDone()
        }
    }

    fun deleteChannel(channelId: String) {
        viewModelScope.launch {
            repository.deleteChannel(channelId)
            if (_selectedChannelId.value == channelId) {
                _selectedChannelId.value = null
            }
            _filterState.update { it.copy(statusMessage = "Channel deleted") }
        }
    }

    fun toggleMilestone(milestone: RoadmapMilestoneEntity) {
        viewModelScope.launch {
            repository.toggleMilestoneCompleted(milestone)
        }
    }

    fun adjustMilestoneUploads(milestone: RoadmapMilestoneEntity, delta: Int) {
        viewModelScope.launch {
            repository.adjustMilestoneUploads(milestone, delta)
        }
    }

    fun saveApp(
        id: String? = null,
        creatorName: String,
        title: String,
        tagline: String,
        description: String,
        category: AppCategory,
        downloadUrl: String,
        versionName: String,
        accentIndex: Long,
        onDone: () -> Unit = {}
    ) {
        if (title.isBlank()) {
            _filterState.update { it.copy(statusMessage = "Please enter an app title") }
            return
        }
        viewModelScope.launch {
            repository.saveLocalApp(
                id = id,
                creatorName = creatorName,
                title = title,
                tagline = tagline,
                description = description,
                category = category,
                downloadUrl = downloadUrl,
                versionName = versionName,
                accentIndex = accentIndex
            )
            _filterState.update {
                it.copy(statusMessage = if (id == null) "App '$title' added for instant launch!" else "Updated '$title'")
            }
            onDone()
        }
    }

    fun deleteApp(appId: String) {
        viewModelScope.launch {
            repository.deleteLocalApp(appId)
            _filterState.update { it.copy(statusMessage = "App removed from your studio") }
        }
    }

    fun recordAppLaunch(appId: String) {
        if (!appId.startsWith("local_app_")) return
        viewModelScope.launch {
            repository.recordLocalAppLaunch(appId)
        }
    }

    fun saveVideo(
        id: String? = null,
        channelName: String,
        title: String,
        description: String,
        category: VideoCategory,
        language: String,
        videoUrl: String,
        durationLabel: String,
        accentIndex: Long,
        onDone: () -> Unit = {}
    ) {
        if (title.isBlank()) {
            _filterState.update { it.copy(statusMessage = "Please enter a video or channel title") }
            return
        }
        viewModelScope.launch {
            repository.saveLocalVideo(
                id = id,
                channelName = channelName,
                title = title,
                description = description,
                category = category,
                language = language,
                videoUrl = videoUrl,
                durationLabel = durationLabel,
                accentIndex = accentIndex
            )
            _filterState.update {
                it.copy(statusMessage = if (id == null) "Published '$title' to Tube!" else "Updated '$title'")
            }
            onDone()
        }
    }

    fun deleteVideo(videoId: String) {
        viewModelScope.launch {
            repository.deleteLocalVideo(videoId)
            _filterState.update { it.copy(statusMessage = "Video removed from your studio") }
        }
    }

    fun recordVideoView(videoId: String) {
        if (!videoId.startsWith("local_vid_")) return
        viewModelScope.launch {
            repository.recordLocalVideoView(videoId)
        }
    }

    fun toggleBookmark(targetId: String, targetType: String, currentlyBookmarked: Boolean) {
        viewModelScope.launch {
            repository.toggleBookmark(targetId, targetType, currentlyBookmarked)
        }
    }
}
