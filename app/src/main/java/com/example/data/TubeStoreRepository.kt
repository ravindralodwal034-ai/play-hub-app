package com.example.data

import android.content.Context
import com.example.R
import com.example.model.AppCategory
import com.example.model.LiveAppConfig
import com.example.model.ModerationReport
import com.example.model.StoreApp
import com.example.model.TubeVideo
import com.example.model.UserBookmark
import com.example.model.UserProfile
import com.example.model.VideoCategory
import com.example.model.isVerifiedOwnerEmail
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class TubeStoreRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        val uid = auth.currentUser?.uid
        check(!uid.isNullOrBlank()) { "User must be signed in with Google before accessing Firestore." }
        return uid
    }

    fun observePublicApps(): Flow<List<StoreApp>> = flow {
        requireUserId()
        emitAll(
            db.collection("apps")
                .whereEqualTo("visibility", "public")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        StoreApp::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser != null) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, "apps")
                    }
                    throw error
                }
        )
    }

    fun observeMyApps(userId: String): Flow<List<StoreApp>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Refusing cross-user app query." }
        emitAll(
            db.collection("apps")
                .whereEqualTo("creatorId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        StoreApp::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, "apps")
                    }
                    throw error
                }
        )
    }

    fun observePublicVideos(): Flow<List<TubeVideo>> = flow {
        requireUserId()
        emitAll(
            db.collection("videos")
                .whereEqualTo("visibility", "public")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        TubeVideo::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser != null) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, "videos")
                    }
                    throw error
                }
        )
    }

    fun observeMyVideos(userId: String): Flow<List<TubeVideo>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Refusing cross-user video query." }
        emitAll(
            db.collection("videos")
                .whereEqualTo("creatorId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        TubeVideo::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, "videos")
                    }
                    throw error
                }
        )
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Refusing cross-user profile read." }
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            UserProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, "users/$userId")
                    }
                    throw error
                }
        )
    }

    fun observeBookmarks(userId: String): Flow<List<UserBookmark>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Refusing cross-user bookmarks query." }
        emitAll(
            db.collection("users").document(userId).collection("bookmarks")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        UserBookmark::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, "users/$userId/bookmarks")
                    }
                    throw error
                }
        )
    }

    suspend fun ensureUserProfile(userId: String, defaultDisplayName: String): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            val docRef = db.collection("users").document(uid)
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val safeName = defaultDisplayName.trim().ifBlank { "Creator" }.take(60)
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "studioName" to "$safeName Studio".take(60),
                    "bio" to "Creator & explorer on Tube Store",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updateUserProfile(
        userId: String,
        displayName: String,
        studioName: String,
        bio: String
    ): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            val cleanName = displayName.trim()
            val cleanStudio = studioName.trim()
            val cleanBio = bio.trim()
            require(cleanName.length in 1..60) { "Display name must be 1 to 60 characters" }
            require(cleanStudio.length in 1..60) { "Studio name must be 1 to 60 characters" }
            require(cleanBio.length <= 240) { "Bio must be 240 characters or less" }

            db.collection("users").document(uid).update(
                mapOf(
                    "displayName" to cleanName,
                    "studioName" to cleanStudio,
                    "bio" to cleanBio,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun publishApp(
        creatorName: String,
        title: String,
        tagline: String,
        description: String,
        category: AppCategory,
        downloadUrl: String,
        versionName: String,
        visibility: String,
        accentIndex: Long
    ): Result<Unit> {
        var docPath = "apps"
        return try {
            val uid = requireUserId()
            val cleanTitle = title.trim()
            val cleanTagline = tagline.trim()
            val cleanDesc = description.trim()
            val cleanUrl = downloadUrl.trim()
            val cleanVersion = versionName.trim().ifBlank { "1.0.0" }
            val cleanCreator = creatorName.trim().ifBlank { "Creator" }
            val cleanVisibility = if (visibility == "private") "private" else "public"

            require(cleanCreator.length in 1..60) { "Creator name must be 1–60 characters" }
            require(cleanTitle.length in 2..60) { "App title must be 2–60 characters" }
            require(cleanTagline.length in 2..120) { "Tagline must be 2–120 characters" }
            require(cleanDesc.length in 5..800) { "Description must be 5–800 characters" }
            require(cleanVersion.length in 1..20) { "Version must be 1–20 characters" }
            require(
                cleanUrl.length in 8..500 &&
                    (cleanUrl.startsWith("https://") || cleanUrl.startsWith("http://"))
            ) { "URL must start with https:// or http://" }

            val docRef = db.collection("apps").document()
            docPath = docRef.path
            val payload = mapOf(
                "creatorId" to uid,
                "creatorName" to cleanCreator,
                "title" to cleanTitle,
                "tagline" to cleanTagline,
                "description" to cleanDesc,
                "category" to category.label,
                "downloadUrl" to cleanUrl,
                "versionName" to cleanVersion,
                "visibility" to cleanVisibility,
                "downloadCount" to 0L,
                "likeCount" to 0L,
                "accentIndex" to accentIndex.coerceIn(0L, 5L),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun publishVideo(
        channelName: String,
        title: String,
        description: String,
        category: VideoCategory,
        language: String,
        videoUrl: String,
        durationLabel: String,
        visibility: String,
        accentIndex: Long
    ): Result<Unit> {
        var docPath = "videos"
        return try {
            val uid = requireUserId()
            val cleanChannel = channelName.trim()
            val cleanTitle = title.trim()
            val cleanDesc = description.trim()
            val cleanLang = language.trim().ifBlank { "Global" }.take(40)
            val cleanUrl = videoUrl.trim()
            val cleanDuration = durationLabel.trim().ifBlank { "03:30" }
            val cleanVisibility = if (visibility == "private") "private" else "public"

            require(cleanChannel.length in 2..60) { "Channel name must be 2–60 characters" }
            require(cleanTitle.length in 2..80) { "Video title must be 2–80 characters" }
            require(cleanDesc.length in 5..800) { "Description must be 5–800 characters" }
            require(cleanLang.length in 1..40) { "Language must be 1–40 characters" }
            require(cleanDuration.length in 1..16) { "Duration label must be 1–16 characters" }
            require(
                cleanUrl.length in 8..500 &&
                    (cleanUrl.startsWith("https://") || cleanUrl.startsWith("http://"))
            ) { "Video URL must start with https:// or http://" }

            val docRef = db.collection("videos").document()
            docPath = docRef.path
            val payload = mapOf(
                "creatorId" to uid,
                "channelName" to cleanChannel,
                "title" to cleanTitle,
                "description" to cleanDesc,
                "category" to category.label,
                "language" to cleanLang,
                "videoUrl" to cleanUrl,
                "durationLabel" to cleanDuration,
                "visibility" to cleanVisibility,
                "viewCount" to 0L,
                "likeCount" to 0L,
                "accentIndex" to accentIndex.coerceIn(0L, 5L),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun incrementAppDownload(appId: String): Result<Unit> {
        val path = "apps/$appId"
        return try {
            requireUserId()
            require(appId.isNotBlank()) { "App ID must not be blank" }
            db.collection("apps").document(appId).update(
                mapOf(
                    "downloadCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun incrementVideoView(videoId: String): Result<Unit> {
        val path = "videos/$videoId"
        return try {
            requireUserId()
            require(videoId.isNotBlank()) { "Video ID must not be blank" }
            db.collection("videos").document(videoId).update(
                mapOf(
                    "viewCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleBookmarkAndLike(
        userId: String,
        targetId: String,
        targetType: String,
        currentlyLiked: Boolean
    ): Result<Unit> {
        val bookmarkId = "${targetType}_$targetId"
        val collectionName = if (targetType == "app") "apps" else "videos"
        val bookmarkPath = "users/$userId/bookmarks/$bookmarkId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            val bookmarkRef = db.collection("users").document(uid)
                .collection("bookmarks").document(bookmarkId)
            val targetRef = db.collection(collectionName).document(targetId)

            if (currentlyLiked) {
                bookmarkRef.delete().await()
                targetRef.update(
                    mapOf(
                        "likeCount" to FieldValue.increment(-1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                bookmarkRef.set(
                    mapOf(
                        "userId" to uid,
                        "targetId" to targetId,
                        "targetType" to targetType,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                targetRef.update(
                    mapOf(
                        "likeCount" to FieldValue.increment(1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, bookmarkPath)
            Result.failure(e)
        }
    }

    suspend fun updateApp(
        appId: String,
        creatorName: String,
        title: String,
        tagline: String,
        description: String,
        category: AppCategory,
        downloadUrl: String,
        versionName: String,
        visibility: String
    ): Result<Unit> {
        val path = "apps/$appId"
        return try {
            requireUserId()
            require(appId.isNotBlank()) { "App ID must not be blank" }
            val cleanCreator = creatorName.trim().ifBlank { "Creator" }
            val cleanTitle = title.trim()
            val cleanTagline = tagline.trim()
            val cleanDesc = description.trim()
            val cleanUrl = downloadUrl.trim()
            val cleanVersion = versionName.trim().ifBlank { "1.0.0" }
            val cleanVisibility = if (visibility == "private") "private" else "public"

            require(cleanCreator.length in 1..60) { "Creator name must be 1–60 characters" }
            require(cleanTitle.length in 2..60) { "App title must be 2–60 characters" }
            require(cleanTagline.length in 2..120) { "Tagline must be 2–120 characters" }
            require(cleanDesc.length in 5..800) { "Description must be 5–800 characters" }
            require(cleanVersion.length in 1..20) { "Version must be 1–20 characters" }
            require(
                cleanUrl.length in 8..500 &&
                    (cleanUrl.startsWith("https://") || cleanUrl.startsWith("http://"))
            ) { "URL must start with https:// or http://" }

            db.collection("apps").document(appId).update(
                mapOf(
                    "creatorName" to cleanCreator,
                    "title" to cleanTitle,
                    "tagline" to cleanTagline,
                    "description" to cleanDesc,
                    "category" to category.label,
                    "downloadUrl" to cleanUrl,
                    "versionName" to cleanVersion,
                    "visibility" to cleanVisibility,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateVideo(
        videoId: String,
        channelName: String,
        title: String,
        description: String,
        category: VideoCategory,
        language: String,
        videoUrl: String,
        durationLabel: String,
        visibility: String
    ): Result<Unit> {
        val path = "videos/$videoId"
        return try {
            requireUserId()
            require(videoId.isNotBlank()) { "Video ID must not be blank" }
            val cleanChannel = channelName.trim()
            val cleanTitle = title.trim()
            val cleanDesc = description.trim()
            val cleanLang = language.trim().ifBlank { "Global" }.take(40)
            val cleanUrl = videoUrl.trim()
            val cleanDuration = durationLabel.trim().ifBlank { "03:30" }
            val cleanVisibility = if (visibility == "private") "private" else "public"

            require(cleanChannel.length in 2..60) { "Channel name must be 2–60 characters" }
            require(cleanTitle.length in 2..80) { "Video title must be 2–80 characters" }
            require(cleanDesc.length in 5..800) { "Description must be 5–800 characters" }
            require(cleanLang.length in 1..40) { "Language must be 1–40 characters" }
            require(cleanDuration.length in 1..16) { "Duration label must be 1–16 characters" }
            require(
                cleanUrl.length in 8..500 &&
                    (cleanUrl.startsWith("https://") || cleanUrl.startsWith("http://"))
            ) { "Video URL must start with https:// or http://" }

            db.collection("videos").document(videoId).update(
                mapOf(
                    "channelName" to cleanChannel,
                    "title" to cleanTitle,
                    "description" to cleanDesc,
                    "category" to category.label,
                    "language" to cleanLang,
                    "videoUrl" to cleanUrl,
                    "durationLabel" to cleanDuration,
                    "visibility" to cleanVisibility,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteApp(appId: String): Result<Unit> {
        val path = "apps/$appId"
        return try {
            requireUserId()
            require(appId.isNotBlank())
            db.collection("apps").document(appId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteVideo(videoId: String): Result<Unit> {
        val path = "videos/$videoId"
        return try {
            requireUserId()
            require(videoId.isNotBlank())
            db.collection("videos").document(videoId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun seedStarterCatalog(creatorName: String): Result<Unit> {
        return try {
            val uid = requireUserId()
            val cleanCreator = creatorName.trim().ifBlank { "Tube Studio" }.take(60)
            val batch = db.batch()

            OpenCatalogData.starterApps(creatorId = uid, creatorName = cleanCreator).forEach { app ->
                val payload = mapOf(
                    "creatorId" to uid,
                    "creatorName" to app.creatorName,
                    "title" to app.title,
                    "tagline" to app.tagline,
                    "description" to app.description,
                    "category" to app.category,
                    "downloadUrl" to app.downloadUrl,
                    "versionName" to app.versionName,
                    "visibility" to "public",
                    "downloadCount" to 0L,
                    "likeCount" to 0L,
                    "accentIndex" to app.accentIndex,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                batch.set(db.collection("apps").document(), payload)
            }

            OpenCatalogData.starterVideos(creatorId = uid).forEach { vid ->
                val payload = mapOf(
                    "creatorId" to uid,
                    "channelName" to vid.channelName,
                    "title" to vid.title,
                    "description" to vid.description,
                    "category" to vid.category,
                    "language" to vid.language,
                    "videoUrl" to vid.videoUrl,
                    "durationLabel" to vid.durationLabel,
                    "visibility" to "public",
                    "viewCount" to 0L,
                    "likeCount" to 0L,
                    "accentIndex" to vid.accentIndex,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                batch.set(db.collection("videos").document(), payload)
            }

            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "apps|videos")
            Result.failure(e)
        }
    }

    private fun requireVerifiedAdmin(): String {
        val user = auth.currentUser
        val uid = user?.uid
        check(!uid.isNullOrBlank()) { "User must be signed in." }
        check(isVerifiedOwnerEmail(user.email, user.isEmailVerified)) {
            "Only verified app owner can perform Owner Control actions."
        }
        return uid
    }

    fun observeLiveAppConfig(): Flow<LiveAppConfig> = flow {
        requireUserId()
        emitAll(
            db.collection("app_config").document("live")
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            LiveAppConfig::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        ) ?: LiveAppConfig()
                    } else {
                        LiveAppConfig()
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser != null) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, "app_config/live")
                    }
                    throw error
                }
        )
    }

    suspend fun saveLiveAppConfig(
        bannerText: String,
        bannerEnabled: Boolean,
        maintenanceMode: Boolean,
        maintenanceMessage: String,
        featuredLabel: String,
        blockedIds: List<String>
    ): Result<Unit> {
        val path = "app_config/live"
        return try {
            val uid = requireVerifiedAdmin()
            val cleanBanner = bannerText.trim().take(180)
            val cleanMaintenance = maintenanceMessage.trim().take(220)
            val cleanFeatured = featuredLabel.trim().take(60)
            val cleanBlocked = blockedIds.map { it.trim() }.filter { it.isNotBlank() }.distinct().take(100)

            val payload = mapOf(
                "bannerText" to cleanBanner,
                "bannerEnabled" to bannerEnabled,
                "maintenanceMode" to maintenanceMode,
                "maintenanceMessage" to cleanMaintenance,
                "featuredLabel" to cleanFeatured,
                "blockedIds" to cleanBlocked,
                "updatedBy" to uid,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("app_config").document("live").set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun submitCopyReport(
        reporterName: String,
        targetId: String,
        targetType: String,
        targetTitle: String,
        reason: String
    ): Result<Unit> {
        var path = "moderation_reports"
        return try {
            val uid = requireUserId()
            val cleanReporter = reporterName.trim().ifBlank { "Community Member" }.take(60)
            val cleanTargetId = targetId.trim().take(128)
            val cleanType = if (targetType == "video") "video" else "app"
            val cleanTitle = targetTitle.trim().ifBlank { "Untitled" }.take(80)
            val cleanReason = reason.trim()

            require(cleanTargetId.isNotEmpty()) { "Target ID missing" }
            require(cleanReason.length in 3..300) { "Report reason must be 3–300 characters" }

            val docRef = db.collection("moderation_reports").document()
            path = docRef.path
            val payload = mapOf(
                "reporterId" to uid,
                "reporterName" to cleanReporter,
                "targetId" to cleanTargetId,
                "targetType" to cleanType,
                "targetTitle" to cleanTitle,
                "reason" to cleanReason,
                "status" to "open",
                "resolutionNote" to "",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    fun observeModerationReports(): Flow<List<ModerationReport>> = flow {
        requireVerifiedAdmin()
        emitAll(
            db.collection("moderation_reports")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ModerationReport::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(
                        compareBy<ModerationReport> {
                            when (it.status) {
                                "open" -> 0
                                "resolved" -> 1
                                else -> 2
                            }
                        }.thenByDescending { it.createdAt ?: Timestamp(0, 0) }
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser != null) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, "moderation_reports")
                    }
                    throw error
                }
        )
    }

    suspend fun resolveModerationReport(
        reportId: String,
        status: String,
        resolutionNote: String
    ): Result<Unit> {
        val path = "moderation_reports/$reportId"
        return try {
            requireVerifiedAdmin()
            val cleanStatus = when (status) {
                "resolved", "dismissed", "open" -> status
                else -> "resolved"
            }
            val cleanNote = resolutionNote.trim().take(200)
            db.collection("moderation_reports").document(reportId).update(
                mapOf(
                    "status" to cleanStatus,
                    "resolutionNote" to cleanNote,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun adminBanAndRemoveTarget(
        targetType: String,
        targetId: String,
        currentConfig: LiveAppConfig,
        reportId: String? = null
    ): Result<Unit> {
        val collectionName = if (targetType == "video") "videos" else "apps"
        val path = "$collectionName/$targetId"
        return try {
            val uid = requireVerifiedAdmin()
            require(targetId.isNotBlank()) { "Target ID must not be blank" }

            // Delete target listing if present
            try {
                db.collection(collectionName).document(targetId).delete().await()
            } catch (e: FirebaseFirestoreException) {
                if (e.code != FirebaseFirestoreException.Code.NOT_FOUND) throw e
            }

            // Persist targetId in live blockedIds so any cached/re-referenced copy is hidden globally
            val updatedBlocked = (currentConfig.blockedIds + targetId).distinct().take(100)
            db.collection("app_config").document("live").set(
                mapOf(
                    "bannerText" to currentConfig.bannerText.trim().take(180),
                    "bannerEnabled" to currentConfig.bannerEnabled,
                    "maintenanceMode" to currentConfig.maintenanceMode,
                    "maintenanceMessage" to currentConfig.maintenanceMessage.trim().take(220),
                    "featuredLabel" to currentConfig.featuredLabel.trim().take(60),
                    "blockedIds" to updatedBlocked,
                    "updatedBy" to uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            if (!reportId.isNullOrBlank()) {
                db.collection("moderation_reports").document(reportId).update(
                    mapOf(
                        "status" to "resolved",
                        "resolutionNote" to "Removed and blocked by owner",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }
}
