package com.example.ui.screens

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.TubeVideo
import com.example.model.UserBookmark
import com.example.model.VideoCategory
import com.example.ui.TubeStoreFilterState
import com.example.ui.UiState
import com.example.ui.theme.StoreCardAccents

@Composable
fun TubeScreen(
    videosState: UiState<List<TubeVideo>>,
    filterState: TubeStoreFilterState,
    bookmarks: List<UserBookmark>,
    defaultChannelName: String,
    onSearchChange: (String) -> Unit,
    onCategorySelect: (String) -> Unit,
    onLanguageSelect: (String) -> Unit,
    onRecordView: (String) -> Unit,
    onToggleLike: (String, Boolean) -> Unit,
    onOpenInBrowser: (String) -> Unit,
    onPublishVideo: (
        channelName: String,
        title: String,
        description: String,
        category: VideoCategory,
        language: String,
        videoUrl: String,
        durationLabel: String,
        visibility: String,
        accentIndex: Long,
        onDone: () -> Unit
    ) -> Unit,
    onSeedStarterCatalog: () -> Unit,
    onChannelSelect: (String) -> Unit = {},
    onReportCopy: ((targetId: String, targetTitle: String, reason: String) -> Unit)? = null
) {
    val context = LocalContext.current
    var showUploadDialog by remember { mutableStateOf(false) }
    var showAiMusicDialog by remember { mutableStateOf(false) }
    var showNativeMediaStudio by remember { mutableStateOf(false) }
    var activePlayerVideo by remember { mutableStateOf<TubeVideo?>(null) }
    var reportingVideo by remember { mutableStateOf<TubeVideo?>(null) }

    val likedVideoIds = remember(bookmarks) {
        bookmarks.filter { it.targetType == "video" }.map { it.targetId }.toSet()
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showUploadDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("upload_video_fab")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Upload Video")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Pro Channels & Streams Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Channels & Creators",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Dedicated channels • Music, Esports, NASA+ & Global",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = { showUploadDialog = true },
                    modifier = Modifier.testTag("header_upload_video_button")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Upload")
                }
            }

            // World & Marwadi Music Quick Highlight Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = "World & Marwadi Music",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Rajasthan Marwadi & World AI Music",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Compose AI songs, live beat synth & world radio",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )
                    }
                    Button(
                        onClick = { showAiMusicDialog = true },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("open_ai_music_button")
                    ) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("AI Music")
                    }
                    TextButton(onClick = { onOpenInBrowser("https://radio.garden") }) {
                        Text("Radio")
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Search Bar
            OutlinedTextField(
                value = filterState.videoSearch,
                onValueChange = onSearchChange,
                placeholder = { Text("Search cartoons, Marwadi songs, channels...") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Search videos") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("tube_search_input")
            )

            // Category Filter Chips
            val categories = listOf("All") + VideoCategory.entries.map { it.label }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    FilterChip(
                        selected = filterState.selectedVideoCategory == cat,
                        onClick = { onCategorySelect(cat) },
                        label = { Text(cat) }
                    )
                }
            }

            // Language Filter Chips (Marwadi/Rajasthani + World Languages)
            val languages = listOf(
                "All",
                "Marwadi / Rajasthani",
                "Hindi",
                "English",
                "World Languages",
                "Global"
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                languages.forEach { lang ->
                    FilterChip(
                        selected = filterState.selectedLanguage == lang,
                        onClick = { onLanguageSelect(lang) },
                        leadingIcon = if (lang == "All") null else {
                            { Icon(Icons.Rounded.Language, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        },
                        label = { Text(lang) }
                    )
                }
            }

            when (videosState) {
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is UiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(videosState.message, color = MaterialTheme.colorScheme.error)
                    }
                }
                is UiState.Success -> {
                    val channelCounts = remember(videosState.data) {
                        videosState.data.groupingBy { it.channelName }.eachCount()
                            .entries.sortedByDescending { it.value }
                    }

                    // Separated Channels Directory Strip
                    if (channelCounts.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = filterState.selectedChannel == "All",
                                onClick = { onChannelSelect("All") },
                                leadingIcon = {
                                    Icon(Icons.Rounded.Tv, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                label = { Text("All Channels (${channelCounts.size})") },
                                modifier = Modifier.testTag("channel_chip_all")
                            )
                            channelCounts.forEach { (chName, count) ->
                                FilterChip(
                                    selected = filterState.selectedChannel == chName,
                                    onClick = { onChannelSelect(chName) },
                                    label = { Text("$chName ($count)") },
                                    modifier = Modifier.testTag("channel_chip_${chName.lowercase().replace(" ", "_")}")
                                )
                            }
                        }
                    }

                    if (filterState.selectedChannel != "All") {
                        val channelItems = remember(videosState.data, filterState.selectedChannel) {
                            videosState.data.filter { it.channelName == filterState.selectedChannel }
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = filterState.selectedChannel,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        text = "${channelItems.size} release(s) • Independent Channel Feed",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                                    )
                                }
                                TextButton(onClick = { onChannelSelect("All") }) {
                                    Text("All Channels")
                                }
                            }
                        }
                    }

                    val filtered = remember(
                        videosState.data,
                        filterState.videoSearch,
                        filterState.selectedVideoCategory,
                        filterState.selectedLanguage,
                        filterState.selectedChannel
                    ) {
                        val q = filterState.videoSearch.trim().lowercase()
                        videosState.data.filter { vid ->
                            val matchesCh = filterState.selectedChannel == "All" ||
                                vid.channelName == filterState.selectedChannel
                            val matchesCat = filterState.selectedVideoCategory == "All" ||
                                vid.category == filterState.selectedVideoCategory
                            val matchesLang = filterState.selectedLanguage == "All" ||
                                vid.language.contains(filterState.selectedLanguage.substringBefore(" /"), ignoreCase = true)
                            val matchesQ = q.isEmpty() ||
                                vid.title.lowercase().contains(q) ||
                                vid.channelName.lowercase().contains(q) ||
                                vid.language.lowercase().contains(q) ||
                                vid.description.lowercase().contains(q)
                            matchesCh && matchesCat && matchesLang && matchesQ
                        }
                    }

                    if (filtered.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Rounded.PlayCircle,
                                contentDescription = "No channels",
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (videosState.data.isEmpty()) "No channels uploaded yet" else "No matching channels",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Upload your cartoon, Rajasthani Marwadi song, or world music link to reach viewers.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                            if (videosState.data.isEmpty()) {
                                Button(
                                    onClick = onSeedStarterCatalog,
                                    enabled = !filterState.isSubmitting
                                ) {
                                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Load Cartoons & World Music")
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            OutlinedButton(onClick = { showUploadDialog = true }) {
                                Text("Upload Video / Music")
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(filtered, key = { it.id }) { video ->
                                val isLiked = likedVideoIds.contains(video.id)
                                TubeVideoCard(
                                    video = video,
                                    isLiked = isLiked,
                                    onWatchClick = {
                                        onRecordView(video.id)
                                        activePlayerVideo = video
                                    },
                                    onToggleLike = { onToggleLike(video.id, isLiked) },
                                    onChannelClick = { onChannelSelect(video.channelName) },
                                    onReportCopyClick = if (onReportCopy != null) {
                                        { reportingVideo = video }
                                    } else {
                                        null
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showUploadDialog) {
        val prefilledChannel = if (filterState.selectedChannel != "All") {
            filterState.selectedChannel
        } else {
            defaultChannelName
        }
        PublishVideoDialog(
            defaultChannelName = prefilledChannel,
            isSubmitting = filterState.isSubmitting,
            onOpenNativeLab = {
                showUploadDialog = false
                showNativeMediaStudio = true
            },
            onOpenEditorTool = { url ->
                showUploadDialog = false
                onOpenInBrowser(url)
            },
            onDismiss = { showUploadDialog = false },
            onSubmit = { channel, title, desc, cat, lang, url, dur, vis, accent ->
                onPublishVideo(channel, title, desc, cat, lang, url, dur, vis, accent) {
                    showUploadDialog = false
                }
            }
        )
    }

    if (showNativeMediaStudio) {
        InstantStudioDialog(
            onDismiss = { showNativeMediaStudio = false },
            onShowMessage = { msg ->
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        )
    }

    activePlayerVideo?.let { video ->
        InAppStreamPlayerDialog(
            video = video,
            onClose = { activePlayerVideo = null }
        )
    }

    if (showAiMusicDialog) {
        AiMusicStudioDialog(
            onDismiss = { showAiMusicDialog = false },
            onOpenSongMaker = {
                onOpenInBrowser("https://musiclab.chromeexperiments.com/Song-Maker/")
            },
            onOpenAudioMass = {
                onOpenInBrowser("https://audiomass.co/")
            }
        )
    }

    reportingVideo?.let { target ->
        if (onReportCopy != null) {
            ReportCopyDialog(
                targetTitle = target.title,
                targetKind = "Channel Video",
                onDismiss = { reportingVideo = null },
                onSubmit = { reason ->
                    onReportCopy(target.id, target.title, reason)
                    reportingVideo = null
                }
            )
        }
    }
}

@Composable
private fun TubeVideoCard(
    video: TubeVideo,
    isLiked: Boolean,
    onWatchClick: () -> Unit,
    onToggleLike: () -> Unit,
    onChannelClick: () -> Unit = {},
    onReportCopyClick: (() -> Unit)? = null
) {
    val pair = StoreCardAccents[(video.accentIndex.toInt().coerceAtLeast(0)) % StoreCardAccents.size]
    Card(
        onClick = onWatchClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("video_card_${video.id}")
    ) {
        Column {
            // Gradient Player Thumbnail
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(148.dp)
                    .background(Brush.linearGradient(listOf(pair.first, pair.second)))
                    .padding(14.dp)
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Text(
                        text = "${video.category} • ${video.language}",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    color = Color.Black.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .size(54.dp)
                        .align(Alignment.Center)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "Play ${video.title}",
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.align(Alignment.BottomEnd)
                ) {
                    Text(
                        text = video.durationLabel,
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = video.title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Surface(
                            onClick = onChannelClick,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "@${video.channelName}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    if (onReportCopyClick != null) {
                        IconButton(onClick = onReportCopyClick) {
                            Icon(
                                imageVector = Icons.Rounded.Flag,
                                contentDescription = "Report copied video",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onToggleLike) {
                        Icon(
                            imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = if (isLiked) "Unlike video" else "Like video",
                            tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = video.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.Visibility,
                                contentDescription = "Views",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${video.viewCount} views",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Favorite,
                                contentDescription = "Likes",
                                modifier = Modifier.size(15.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${video.likeCount}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Button(
                        onClick = onWatchClick,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Play")
                    }
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun InAppStreamPlayerDialog(
    video: TubeVideo,
    onClose: () -> Unit
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = video.title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${video.channelName} • ${video.language}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close player")
                    }
                }

                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.mediaPlaybackRequiresUserGesture = false
                            settings.domStorageEnabled = true
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true
                            webViewClient = WebViewClient()
                            webChromeClient = WebChromeClient()
                            if (video.videoUrl.endsWith(".mp4", ignoreCase = true) ||
                                video.videoUrl.endsWith(".webm", ignoreCase = true)
                            ) {
                                val html = """
                                    <html><body style="margin:0;background:#000;display:flex;align-items:center;justify-content:center;height:100vh;">
                                    <video controls autoplay playsinline style="width:100%;max-height:100%;">
                                      <source src="${video.videoUrl}" type="video/mp4" />
                                    </video></body></html>
                                """.trimIndent()
                                loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
                            } else {
                                loadUrl(video.videoUrl)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(bottomStart = 22.dp, bottomEnd = 22.dp))
                )
            }
        }
    }
}

@Composable
private fun PublishVideoDialog(
    defaultChannelName: String,
    isSubmitting: Boolean,
    onOpenNativeLab: () -> Unit,
    onOpenEditorTool: (String) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: (
        channelName: String,
        title: String,
        description: String,
        category: VideoCategory,
        language: String,
        videoUrl: String,
        durationLabel: String,
        visibility: String,
        accentIndex: Long
    ) -> Unit
) {
    var channelName by remember { mutableStateOf(defaultChannelName) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var language by remember { mutableStateOf("Marwadi / Rajasthani") }
    var videoUrl by remember { mutableStateOf("https://") }
    var durationLabel by remember { mutableStateOf("04:15") }
    var selectedCategory by remember { mutableStateOf(VideoCategory.MUSIC) }
    var isPublic by remember { mutableStateOf(true) }
    var madeForKids by remember { mutableStateOf(false) }
    var allowComments by remember { mutableStateOf(true) }
    var certifiedOriginal by remember { mutableStateOf(false) }
    var accentIndex by remember { mutableLongStateOf(2L) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val quickLanguages = listOf("Marwadi / Rajasthani", "Hindi", "Punjabi", "English", "World Languages", "Global")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Upload & Studio Settings") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Create / Edit Media Before Upload", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(onClick = onOpenNativeLab) {
                        Text("Phone Photo/Video Lab")
                    }
                    OutlinedButton(onClick = { onOpenEditorTool("https://www.photopea.com") }) {
                        Text("Thumbnail Photo")
                    }
                    OutlinedButton(onClick = { onOpenEditorTool("https://audiomass.co/") }) {
                        Text("Edit Audio")
                    }
                    OutlinedButton(onClick = { onOpenEditorTool("https://www.vectorpea.com") }) {
                        Text("Banner Art")
                    }
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Video / Song Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = channelName,
                    onValueChange = { channelName = it },
                    label = { Text("Channel Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (min 5 chars)") },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = videoUrl,
                    onValueChange = { videoUrl = it },
                    label = { Text("Stream / MP4 / Web URL (https://)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = language,
                    onValueChange = { language = it },
                    label = { Text("Language / Region") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickLanguages.forEach { lang ->
                        FilterChip(
                            selected = language == lang,
                            onClick = { language = lang },
                            label = { Text(lang) }
                        )
                    }
                }
                OutlinedTextField(
                    value = durationLabel,
                    onValueChange = { durationLabel = it },
                    label = { Text("Duration (e.g. 04:15)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Category", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    VideoCategory.entries.forEachIndexed { idx, cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = {
                                selectedCategory = cat
                                accentIndex = idx.toLong()
                            },
                            label = { Text(cat.label) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Public Channel Visibility", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = isPublic, onCheckedChange = { isPublic = it })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Made for Kids / Family Safe", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = madeForKids, onCheckedChange = { madeForKids = it })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Allow Viewer Comments", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = allowComments, onCheckedChange = { allowComments = it })
                }
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = certifiedOriginal,
                            onCheckedChange = { certifiedOriginal = it }
                        )
                        Text(
                            text = "Anti-Copy Rule: I confirm this video/song/photo is 100% original. Copying another creator results in immediate ban.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                if (validationError != null) {
                    Text(
                        text = validationError ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isSubmitting && certifiedOriginal,
                onClick = {
                    val cleanTitle = title.trim()
                    val cleanChannel = channelName.trim()
                    val cleanDesc = description.trim()
                    val cleanUrl = videoUrl.trim()
                    when {
                        cleanTitle.length < 2 -> validationError = "Title must be at least 2 characters"
                        cleanChannel.length < 2 -> validationError = "Channel name must be at least 2 characters"
                        cleanDesc.length < 5 -> validationError = "Description must be at least 5 characters"
                        !(cleanUrl.startsWith("https://") || cleanUrl.startsWith("http://")) || cleanUrl.length < 8 ->
                            validationError = "Enter a valid https:// or http:// URL"
                        else -> {
                            validationError = null
                            onSubmit(
                                cleanChannel,
                                cleanTitle,
                                cleanDesc,
                                selectedCategory,
                                language,
                                cleanUrl,
                                durationLabel,
                                if (isPublic) "public" else "private",
                                accentIndex
                            )
                        }
                    }
                }
            ) {
                Text(if (isSubmitting) "Uploading..." else "Publish")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
