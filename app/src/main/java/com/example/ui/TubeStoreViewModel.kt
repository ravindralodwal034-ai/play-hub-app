package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.OpenCatalogData
import com.example.data.TubeStoreRepository
import com.example.model.AppCategory
import com.example.model.LiveAppConfig
import com.example.model.ModerationReport
import com.example.model.StoreApp
import com.example.model.TubeVideo
import com.example.model.UserBookmark
import com.example.model.UserProfile
import com.example.model.VideoCategory
import com.example.model.isVerifiedOwnerEmail
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class TubeStoreFilterState(
    val appSearch: String = "",
    val selectedAppCategory: String = "All",
    val videoSearch: String = "",
    val selectedVideoCategory: String = "All",
    val selectedLanguage: String = "All",
    val selectedChannel: String = "All",
    val statusMessage: String? = null,
    val isSubmitting: Boolean = false
)

class TubeStoreViewModel(
    private val repository: TubeStoreRepository,
    private val currentUserId: String,
    defaultDisplayName: String,
    userEmail: String? = null,
    isEmailVerified: Boolean = false
) : ViewModel() {

    val isOwnerAdmin: Boolean = isVerifiedOwnerEmail(userEmail, isEmailVerified)

    private val _filterState = MutableStateFlow(TubeStoreFilterState())
    val filterState: StateFlow<TubeStoreFilterState> = _filterState.asStateFlow()

    val liveConfig: StateFlow<LiveAppConfig> = repository.observeLiveAppConfig()
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing live app config", firestoreError ?: error)
            emit(LiveAppConfig())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = LiveAppConfig()
        )

    private val rawPublicAppsFlow = repository.observePublicApps()
        .map<List<StoreApp>, UiState<List<StoreApp>>> { cloudApps ->
            val existingTitles = cloudApps.map { it.title.lowercase() }.toSet()
            val extras = OpenCatalogData.starterApps().filterNot { it.title.lowercase() in existingTitles }
            UiState.Success(cloudApps + extras)
        }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val root = firestoreError ?: error
            Log.w(TAG, "Error observing public apps", root)
            emit(UiState.Error(root.message ?: "Failed to load Tube Store apps"))
        }

    val publicApps: StateFlow<UiState<List<StoreApp>>> = combine(rawPublicAppsFlow, liveConfig) { state, cfg ->
        val blocked = cfg.blockedIds.toSet()
        when (state) {
            is UiState.Success -> UiState.Success(state.data.filterNot { it.id in blocked })
            else -> state
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Success(OpenCatalogData.starterApps())
    )

    val myApps: StateFlow<UiState<List<StoreApp>>> = repository.observeMyApps(currentUserId)
        .map<List<StoreApp>, UiState<List<StoreApp>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val root = firestoreError ?: error
            Log.w(TAG, "Error observing creator apps", root)
            emit(UiState.Error(root.message ?: "Failed to load your apps"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    private val rawPublicVideosFlow = repository.observePublicVideos()
        .map<List<TubeVideo>, UiState<List<TubeVideo>>> { cloudVideos ->
            val existingTitles = cloudVideos.map { it.title.lowercase() }.toSet()
            val extras = OpenCatalogData.starterVideos().filterNot { it.title.lowercase() in existingTitles }
            UiState.Success(cloudVideos + extras)
        }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val root = firestoreError ?: error
            Log.w(TAG, "Error observing public videos", root)
            emit(UiState.Error(root.message ?: "Failed to load Tube channels"))
        }

    val publicVideos: StateFlow<UiState<List<TubeVideo>>> = combine(rawPublicVideosFlow, liveConfig) { state, cfg ->
        val blocked = cfg.blockedIds.toSet()
        when (state) {
            is UiState.Success -> UiState.Success(state.data.filterNot { it.id in blocked })
            else -> state
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Success(OpenCatalogData.starterVideos())
    )

    val myVideos: StateFlow<UiState<List<TubeVideo>>> = repository.observeMyVideos(currentUserId)
        .map<List<TubeVideo>, UiState<List<TubeVideo>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val root = firestoreError ?: error
            Log.w(TAG, "Error observing creator videos", root)
            emit(UiState.Error(root.message ?: "Failed to load your videos"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val userProfile: StateFlow<UserProfile?> = repository.observeUserProfile(currentUserId)
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing user profile", firestoreError ?: error)
            emit(null)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = null
        )

    val bookmarks: StateFlow<List<UserBookmark>> = repository.observeBookmarks(currentUserId)
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing bookmarks", firestoreError ?: error)
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList()
        )

    val moderationReports: StateFlow<List<ModerationReport>> = (
        if (isOwnerAdmin) {
            repository.observeModerationReports()
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    Log.w(TAG, "Error observing moderation reports", firestoreError ?: error)
                    emit(emptyList())
                }
        } else {
            flowOf(emptyList())
        }
        ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = emptyList()
    )

    init {
        viewModelScope.launch {
            repository.ensureUserProfile(currentUserId, defaultDisplayName)
        }
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

    fun publishApp(
        creatorName: String,
        title: String,
        tagline: String,
        description: String,
        category: AppCategory,
        downloadUrl: String,
        versionName: String,
        visibility: String,
        accentIndex: Long,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _filterState.update { it.copy(isSubmitting = true) }
            val result = repository.publishApp(
                creatorName = creatorName,
                title = title,
                tagline = tagline,
                description = description,
                category = category,
                downloadUrl = downloadUrl,
                versionName = versionName,
                visibility = visibility,
                accentIndex = accentIndex
            )
            _filterState.update {
                it.copy(
                    isSubmitting = false,
                    statusMessage = result.fold(
                        onSuccess = { "App published to Tube Store!" },
                        onFailure = { err -> err.localizedMessage ?: "Could not publish app" }
                    )
                )
            }
            if (result.isSuccess) onSuccess()
        }
    }

    fun publishVideo(
        channelName: String,
        title: String,
        description: String,
        category: VideoCategory,
        language: String,
        videoUrl: String,
        durationLabel: String,
        visibility: String,
        accentIndex: Long,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _filterState.update { it.copy(isSubmitting = true) }
            val result = repository.publishVideo(
                channelName = channelName,
                title = title,
                description = description,
                category = category,
                language = language,
                videoUrl = videoUrl,
                durationLabel = durationLabel,
                visibility = visibility,
                accentIndex = accentIndex
            )
            _filterState.update {
                it.copy(
                    isSubmitting = false,
                    statusMessage = result.fold(
                        onSuccess = { "Video/Channel published live!" },
                        onFailure = { err -> err.localizedMessage ?: "Could not publish video" }
                    )
                )
            }
            if (result.isSuccess) onSuccess()
        }
    }

    fun recordAppDownload(appId: String) {
        if (appId.startsWith("open_")) return
        viewModelScope.launch {
            repository.incrementAppDownload(appId)
        }
    }

    fun recordVideoView(videoId: String) {
        if (videoId.startsWith("vid_")) return
        viewModelScope.launch {
            repository.incrementVideoView(videoId)
        }
    }

    fun toggleLike(targetId: String, targetType: String, currentlyLiked: Boolean) {
        if (targetId.startsWith("open_") || targetId.startsWith("vid_")) {
            _filterState.update { it.copy(statusMessage = "Saved to your favorites!") }
            return
        }
        viewModelScope.launch {
            val result = repository.toggleBookmarkAndLike(
                userId = currentUserId,
                targetId = targetId,
                targetType = targetType,
                currentlyLiked = currentlyLiked
            )
            if (result.isFailure) {
                _filterState.update {
                    it.copy(statusMessage = result.exceptionOrNull()?.localizedMessage ?: "Action failed")
                }
            }
        }
    }

    fun updateMyApp(
        appId: String,
        creatorName: String,
        title: String,
        tagline: String,
        description: String,
        category: AppCategory,
        downloadUrl: String,
        versionName: String,
        visibility: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _filterState.update { it.copy(isSubmitting = true) }
            val result = repository.updateApp(
                appId = appId,
                creatorName = creatorName,
                title = title,
                tagline = tagline,
                description = description,
                category = category,
                downloadUrl = downloadUrl,
                versionName = versionName,
                visibility = visibility
            )
            _filterState.update {
                it.copy(
                    isSubmitting = false,
                    statusMessage = result.fold(
                        onSuccess = { "App updated!" },
                        onFailure = { err -> err.localizedMessage ?: "Update failed" }
                    )
                )
            }
            if (result.isSuccess) onSuccess()
        }
    }

    fun updateMyVideo(
        videoId: String,
        channelName: String,
        title: String,
        description: String,
        category: VideoCategory,
        language: String,
        videoUrl: String,
        durationLabel: String,
        visibility: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _filterState.update { it.copy(isSubmitting = true) }
            val result = repository.updateVideo(
                videoId = videoId,
                channelName = channelName,
                title = title,
                description = description,
                category = category,
                language = language,
                videoUrl = videoUrl,
                durationLabel = durationLabel,
                visibility = visibility
            )
            _filterState.update {
                it.copy(
                    isSubmitting = false,
                    statusMessage = result.fold(
                        onSuccess = { "Video updated!" },
                        onFailure = { err -> err.localizedMessage ?: "Update failed" }
                    )
                )
            }
            if (result.isSuccess) onSuccess()
        }
    }

    fun deleteMyApp(appId: String) {
        viewModelScope.launch {
            val result = repository.deleteApp(appId)
            _filterState.update {
                it.copy(
                    statusMessage = result.fold(
                        onSuccess = { "App removed from Tube Store" },
                        onFailure = { err -> err.localizedMessage ?: "Delete failed" }
                    )
                )
            }
        }
    }

    fun deleteMyVideo(videoId: String) {
        viewModelScope.launch {
            val result = repository.deleteVideo(videoId)
            _filterState.update {
                it.copy(
                    statusMessage = result.fold(
                        onSuccess = { "Video removed from channel" },
                        onFailure = { err -> err.localizedMessage ?: "Delete failed" }
                    )
                )
            }
        }
    }

    fun updateProfile(displayName: String, studioName: String, bio: String) {
        viewModelScope.launch {
            _filterState.update { it.copy(isSubmitting = true) }
            val result = repository.updateUserProfile(currentUserId, displayName, studioName, bio)
            _filterState.update {
                it.copy(
                    isSubmitting = false,
                    statusMessage = result.fold(
                        onSuccess = { "Creator profile saved!" },
                        onFailure = { err -> err.localizedMessage ?: "Could not update profile" }
                    )
                )
            }
        }
    }

    fun seedStarterCatalog(creatorName: String) {
        viewModelScope.launch {
            _filterState.update { it.copy(isSubmitting = true) }
            val result = repository.seedStarterCatalog(creatorName)
            _filterState.update {
                it.copy(
                    isSubmitting = false,
                    statusMessage = result.fold(
                        onSuccess = { "Global apps, games, music & channels synced!" },
                        onFailure = { err -> err.localizedMessage ?: "Failed to sync catalog" }
                    )
                )
            }
        }
    }

    fun submitCopyReport(
        targetId: String,
        targetType: String,
        targetTitle: String,
        reason: String
    ) {
        viewModelScope.launch {
            _filterState.update { it.copy(isSubmitting = true) }
            val reporter = userProfile.value?.displayName?.ifBlank { "Community Member" } ?: "Community Member"
            val result = repository.submitCopyReport(
                reporterName = reporter,
                targetId = targetId,
                targetType = targetType,
                targetTitle = targetTitle,
                reason = reason
            )
            _filterState.update {
                it.copy(
                    isSubmitting = false,
                    statusMessage = result.fold(
                        onSuccess = { "Copy report sent to app owner for review." },
                        onFailure = { err -> err.localizedMessage ?: "Could not submit report" }
                    )
                )
            }
        }
    }

    fun saveLiveAppConfig(
        bannerText: String,
        bannerEnabled: Boolean,
        maintenanceMode: Boolean,
        maintenanceMessage: String,
        featuredLabel: String,
        blockedIds: List<String>
    ) {
        viewModelScope.launch {
            _filterState.update { it.copy(isSubmitting = true) }
            val result = repository.saveLiveAppConfig(
                bannerText = bannerText,
                bannerEnabled = bannerEnabled,
                maintenanceMode = maintenanceMode,
                maintenanceMessage = maintenanceMessage,
                featuredLabel = featuredLabel,
                blockedIds = blockedIds
            )
            _filterState.update {
                it.copy(
                    isSubmitting = false,
                    statusMessage = result.fold(
                        onSuccess = { "Live update pushed to all users!" },
                        onFailure = { err -> err.localizedMessage ?: "Failed to update live config" }
                    )
                )
            }
        }
    }

    fun resolveModerationReport(reportId: String, status: String, note: String) {
        viewModelScope.launch {
            _filterState.update { it.copy(isSubmitting = true) }
            val result = repository.resolveModerationReport(reportId, status, note)
            _filterState.update {
                it.copy(
                    isSubmitting = false,
                    statusMessage = result.fold(
                        onSuccess = { "Report marked $status." },
                        onFailure = { err -> err.localizedMessage ?: "Failed to update report" }
                    )
                )
            }
        }
    }

    fun banAndRemoveTarget(targetType: String, targetId: String, reportId: String? = null) {
        viewModelScope.launch {
            _filterState.update { it.copy(isSubmitting = true) }
            val result = repository.adminBanAndRemoveTarget(
                targetType = targetType,
                targetId = targetId,
                currentConfig = liveConfig.value,
                reportId = reportId
            )
            _filterState.update {
                it.copy(
                    isSubmitting = false,
                    statusMessage = result.fold(
                        onSuccess = { "Removed & banned across the app!" },
                        onFailure = { err -> err.localizedMessage ?: "Failed to ban item" }
                    )
                )
            }
        }
    }

    fun unblockTargetId(targetId: String) {
        val current = liveConfig.value
        saveLiveAppConfig(
            bannerText = current.bannerText,
            bannerEnabled = current.bannerEnabled,
            maintenanceMode = current.maintenanceMode,
            maintenanceMessage = current.maintenanceMessage,
            featuredLabel = current.featuredLabel,
            blockedIds = current.blockedIds.filterNot { it == targetId }
        )
    }

    private companion object {
        const val TAG = "TubeStoreVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
