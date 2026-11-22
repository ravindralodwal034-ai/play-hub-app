package com.example.ui.screens

import android.net.Uri
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.SportsCricket
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.ViewInAr
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.model.AppCategory
import com.example.model.StoreApp
import com.example.model.UserBookmark
import com.example.ui.TubeStoreFilterState
import com.example.ui.UiState
import com.example.ui.theme.StoreCardAccents

enum class StoreViewMode {
    GAMES,
    APPS
}

@Composable
fun StoreScreen(
    storeMode: StoreViewMode = StoreViewMode.APPS,
    appsState: UiState<List<StoreApp>>,
    filterState: TubeStoreFilterState,
    bookmarks: List<UserBookmark>,
    defaultCreatorName: String,
    onSearchChange: (String) -> Unit,
    onCategorySelect: (String) -> Unit,
    onOpenInBrowser: (String) -> Unit,
    onRunInstant: (StoreApp) -> Unit = { onOpenInBrowser(it.downloadUrl) },
    onOpenStudyNasa: () -> Unit = {},
    onOpenAiMusic: () -> Unit = {},
    onDownloadApp: (StoreApp) -> Unit = { onOpenInBrowser(it.downloadUrl) },
    onOpenDownloads: () -> Unit = {},
    onRecordDownload: (String) -> Unit,
    onToggleLike: (String, Boolean) -> Unit,
    onPublishApp: (
        creatorName: String,
        title: String,
        tagline: String,
        description: String,
        category: AppCategory,
        downloadUrl: String,
        versionName: String,
        visibility: String,
        accentIndex: Long,
        onDone: () -> Unit
    ) -> Unit,
    onSeedStarterCatalog: () -> Unit,
    featuredLabel: String = "",
    onReportCopy: ((targetId: String, targetTitle: String, reason: String) -> Unit)? = null
) {
    var showPublishDialog by remember { mutableStateOf(false) }
    var selectedAppDetail by remember { mutableStateOf<StoreApp?>(null) }
    var reportingApp by remember { mutableStateOf<StoreApp?>(null) }
    var gameFilter by remember { mutableStateOf("All Games") }

    val likedAppIds = remember(bookmarks) {
        bookmarks.filter { it.targetType == "app" }.map { it.targetId }.toSet()
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showPublishDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("publish_app_fab")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = if (storeMode == StoreViewMode.GAMES) "Publish Game" else "Publish App")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Play Store Top Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.app_icon_tubestore),
                    contentDescription = "Play Hub Logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (storeMode == StoreViewMode.GAMES) "Play Hub Games" else "Play Hub Apps",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = when {
                            featuredLabel.isNotBlank() -> "$featuredLabel • Instant Zero-Download"
                            storeMode == StoreViewMode.GAMES -> "India & Global Hits • Zero Download"
                            else -> "Free Apps, Music DAWs, Dev & Space Tools"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = if (featuredLabel.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = onOpenDownloads,
                    modifier = Modifier.testTag("store_downloads_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Download,
                        contentDescription = "Open Downloads Manager",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                OutlinedButton(
                    onClick = { showPublishDialog = true },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("header_publish_app_button")
                ) {
                    Text(if (storeMode == StoreViewMode.GAMES) "+ Game" else "+ App")
                }
            }

            // Play Store Rounded Search Pill
            OutlinedTextField(
                value = filterState.appSearch,
                onValueChange = onSearchChange,
                placeholder = {
                    Text(
                        if (storeMode == StoreViewMode.GAMES) {
                            "Search Free Fire, BGMI, Ludo, Cricket..."
                        } else {
                            "Search Spotify, JioSaavn, Canva, VS Code..."
                        }
                    )
                },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Search") },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("store_search_input")
            )

            // Filter Chips Row
            if (storeMode == StoreViewMode.GAMES) {
                val gameFilters = listOf("All Games", "India Games", "Action & Battle", "Cricket", "Board & Chess", "3D & Multiplayer")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    gameFilters.forEach { gf ->
                        FilterChip(
                            selected = gameFilter == gf,
                            onClick = { gameFilter = gf },
                            label = { Text(gf) },
                            modifier = Modifier.testTag("game_filter_$gf")
                        )
                    }
                }
            } else {
                val appCategories = listOf("All") + AppCategory.entries
                    .filterNot { it == AppCategory.GAMES }
                    .map { it.label }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    appCategories.forEach { cat ->
                        val labelText = when (cat) {
                            "Entertainment" -> "Music & Audio"
                            else -> cat
                        }
                        FilterChip(
                            selected = filterState.selectedAppCategory == cat,
                            onClick = { onCategorySelect(cat) },
                            label = { Text(labelText) },
                            modifier = Modifier.testTag("app_cat_$cat")
                        )
                    }
                }
            }

            when (appsState) {
                is UiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
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
                        Text(
                            text = appsState.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                is UiState.Success -> {
                    val modeItems = remember(appsState.data, storeMode) {
                        if (storeMode == StoreViewMode.GAMES) {
                            appsState.data.filter { it.category == AppCategory.GAMES.label }
                        } else {
                            appsState.data.filterNot { it.category == AppCategory.GAMES.label }
                        }
                    }

                    val indiaGames = remember(modeItems, storeMode) {
                        if (storeMode == StoreViewMode.GAMES) {
                            modeItems.filter {
                                it.creatorName.contains("India", ignoreCase = true) ||
                                    it.versionName.contains("India", ignoreCase = true) ||
                                    it.tagline.contains("India", ignoreCase = true)
                            }
                        } else {
                            emptyList()
                        }
                    }

                    val filtered = remember(
                        modeItems,
                        filterState.appSearch,
                        filterState.selectedAppCategory,
                        gameFilter,
                        storeMode
                    ) {
                        val q = filterState.appSearch.trim().lowercase()
                        modeItems.filter { app ->
                            val matchesFilter = if (storeMode == StoreViewMode.GAMES) {
                                when (gameFilter) {
                                    "India Games" -> app.creatorName.contains("India", true) ||
                                        app.versionName.contains("India", true) ||
                                        app.tagline.contains("India", true)
                                    "Action & Battle" -> app.title.contains("Free Fire", true) ||
                                        app.title.contains("BGMI", true) ||
                                        app.title.contains("Indus", true) ||
                                        app.title.contains("Call of Duty", true) ||
                                        app.title.contains("Brawl", true) ||
                                        app.title.contains("Clash", true) ||
                                        app.title.contains("Roblox", true) ||
                                        app.title.contains("Krunker", true) ||
                                        app.title.contains("1v1", true) ||
                                        app.title.contains("Smash", true) ||
                                        app.title.contains("Shell", true) ||
                                        app.title.contains("Subway", true) ||
                                        app.title.contains("Temple", true) ||
                                        app.title.contains("Minecraft", true)
                                    "Cricket" -> app.title.contains("Cricket", true) ||
                                        app.tagline.contains("Cricket", true)
                                    "Board & Chess" -> app.title.contains("Ludo", true) ||
                                        app.title.contains("Carrom", true) ||
                                        app.title.contains("Chess", true) ||
                                        app.title.contains("Board", true) ||
                                        app.title.contains("2048", true)
                                    "3D & Multiplayer" -> app.title.contains("3D", true) ||
                                        app.tagline.contains("Multiplayer", true) ||
                                        app.title.contains("Poki", true) ||
                                        app.title.contains("CrazyGames", true) ||
                                        app.title.contains("itch", true) ||
                                        app.title.contains("Miniclip", true)
                                    else -> true
                                }
                            } else {
                                filterState.selectedAppCategory == "All" ||
                                    app.category == filterState.selectedAppCategory
                            }

                            val matchesQ = q.isEmpty() ||
                                app.title.lowercase().contains(q) ||
                                app.tagline.lowercase().contains(q) ||
                                app.creatorName.lowercase().contains(q)
                            matchesFilter && matchesQ
                        }
                    }

                    if (filtered.isEmpty()) {
                        EmptyStoreState(
                            hasAnyApps = modeItems.isNotEmpty(),
                            isGamesMode = storeMode == StoreViewMode.GAMES,
                            isSubmitting = filterState.isSubmitting,
                            onSeedCatalog = onSeedStarterCatalog,
                            onPublishClick = { showPublishDialog = true }
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (storeMode == StoreViewMode.GAMES && indiaGames.isNotEmpty() && filterState.appSearch.isBlank() && gameFilter == "All Games") {
                                item {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "India Top Games • Instant Play",
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                            Text(
                                                text = "${indiaGames.size} Games",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            items(indiaGames, key = { "india_shelf_${it.id}" }) { game ->
                                                IndiaFeaturedGameCard(
                                                    app = game,
                                                    onPlayClick = {
                                                        onRecordDownload(game.id)
                                                        onRunInstant(game)
                                                    },
                                                    onCardClick = { selectedAppDetail = game }
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }
                                }
                            }

                            if (storeMode == StoreViewMode.APPS && filterState.appSearch.isBlank()) {
                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            onClick = onOpenStudyNasa,
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            shape = MaterialTheme.shapes.medium,
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("open_study_nasa_button")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Rounded.RocketLaunch,
                                                    contentDescription = "Study & NASA",
                                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text(
                                                        text = "Study & NASA",
                                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                        maxLines = 1
                                                    )
                                                    Text(
                                                        text = "Live Rockets & Classes",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }
                                        Surface(
                                            onClick = onOpenAiMusic,
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            shape = MaterialTheme.shapes.medium,
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("open_ai_music_button")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Rounded.MusicNote,
                                                    contentDescription = "AI Music Studio",
                                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text(
                                                        text = "AI Music Lab",
                                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                        maxLines = 1
                                                    )
                                                    Text(
                                                        text = "Live Beat & Song Synth",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            item {
                                Text(
                                    text = if (storeMode == StoreViewMode.GAMES) {
                                        "Top Charts • All Free Games (${filtered.size})"
                                    } else {
                                        "Top Free Apps & Tools (${filtered.size})"
                                    },
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)
                                )
                            }

                            itemsIndexed(filtered, key = { _, app -> "${storeMode.name}_${app.id}" }) { index, app ->
                                val isLiked = likedAppIds.contains(app.id)
                                PlayStoreChartRow(
                                    rank = index + 1,
                                    app = app,
                                    isGamesMode = storeMode == StoreViewMode.GAMES,
                                    isLiked = isLiked,
                                    onCardClick = { selectedAppDetail = app },
                                    onRunClick = {
                                        onRecordDownload(app.id)
                                        onRunInstant(app)
                                    },
                                    onDownloadClick = {
                                        onRecordDownload(app.id)
                                        onDownloadApp(app)
                                    },
                                    onOpenInPrivateBrowser = {
                                        onRecordDownload(app.id)
                                        onOpenInBrowser(app.downloadUrl)
                                    },
                                    onToggleLike = { onToggleLike(app.id, isLiked) },
                                    onReportCopyClick = if (onReportCopy != null) {
                                        { reportingApp = app }
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

    if (showPublishDialog) {
        PublishAppDialog(
            defaultCreatorName = defaultCreatorName,
            defaultCategory = if (storeMode == StoreViewMode.GAMES) AppCategory.GAMES else AppCategory.TOOLS,
            isSubmitting = filterState.isSubmitting,
            onDismiss = { showPublishDialog = false },
            onSubmit = { creator, title, tagline, desc, cat, url, ver, vis, accent ->
                onPublishApp(creator, title, tagline, desc, cat, url, ver, vis, accent) {
                    showPublishDialog = false
                }
            }
        )
    }

    selectedAppDetail?.let { app ->
        AlertDialog(
            onDismissRequest = { selectedAppDetail = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PlayHubAppIconBadge(app = app, size = 40.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(app.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "${app.creatorName} • ${app.versionName} • ${app.category}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(text = app.description, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "Official URL: ${app.downloadUrl}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        onRecordDownload(app.id)
                        onDownloadApp(app)
                        selectedAppDetail = null
                    }) {
                        Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Download")
                    }
                    Button(onClick = {
                        onRecordDownload(app.id)
                        onRunInstant(app)
                        selectedAppDetail = null
                    }) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (app.category == AppCategory.GAMES.label) "Play" else "Open")
                    }
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (onReportCopy != null) {
                        TextButton(onClick = {
                            reportingApp = app
                            selectedAppDetail = null
                        }) {
                            Text("Report Copy", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    TextButton(onClick = { selectedAppDetail = null }) {
                        Text("Close")
                    }
                }
            }
        )
    }

    reportingApp?.let { target ->
        if (onReportCopy != null) {
            ReportCopyDialog(
                targetTitle = target.title,
                targetKind = if (target.category == AppCategory.GAMES.label) "Game" else "App",
                onDismiss = { reportingApp = null },
                onSubmit = { reason ->
                    onReportCopy(target.id, target.title, reason)
                    reportingApp = null
                }
            )
        }
    }
}

@Composable
private fun IndiaFeaturedGameCard(
    app: StoreApp,
    onPlayClick: () -> Unit,
    onCardClick: () -> Unit
) {
    Card(
        onClick = onCardClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.width(156.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                PlayHubAppIconBadge(app = app, size = 48.dp)
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "INDIA",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = app.title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = app.tagline,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onPlayClick,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Play", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun PlayStoreChartRow(
    rank: Int,
    app: StoreApp,
    isGamesMode: Boolean,
    isLiked: Boolean,
    onCardClick: () -> Unit,
    onRunClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onOpenInPrivateBrowser: () -> Unit,
    onToggleLike: () -> Unit,
    onReportCopyClick: (() -> Unit)? = null
) {
    Card(
        onClick = onCardClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("app_card_${app.id}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = rank.toString(),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(24.dp)
                )
                PlayHubAppIconBadge(app = app, size = 52.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = app.title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (app.creatorName.contains("India", true) || app.versionName.contains("India", true)) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.Rounded.Verified,
                                contentDescription = "Verified India Hit",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                    Text(
                        text = "${app.creatorName} • ${app.versionName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = app.tagline,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (onReportCopyClick != null) {
                    IconButton(onClick = onReportCopyClick) {
                        Icon(
                            imageVector = Icons.Rounded.Flag,
                            contentDescription = "Report copy",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                IconButton(
                    onClick = onToggleLike,
                    modifier = Modifier.testTag("like_app_${app.id}")
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = if (isLiked) "Unsave" else "Save",
                        tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "${app.category} • Free",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onOpenInPrivateBrowser,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Public,
                            contentDescription = "Open ${app.title} in Web Browser",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    OutlinedButton(
                        onClick = onDownloadClick,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("download_app_${app.id}")
                    ) {
                        Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Download", style = MaterialTheme.typography.labelMedium)
                    }
                    Button(
                        onClick = onRunClick,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("get_app_${app.id}")
                    ) {
                        Icon(
                            imageVector = if (isGamesMode) Icons.Rounded.PlayArrow else Icons.Rounded.Bolt,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isGamesMode) "Play" else "Open", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun PlayHubAppIconBadge(
    app: StoreApp,
    size: Dp = 52.dp
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val pair = StoreCardAccents[(app.accentIndex.toInt().coerceAtLeast(0)) % StoreCardAccents.size]
    val t = app.title.lowercase()
    val icon = when {
        t.contains("free fire") -> Icons.Rounded.LocalFireDepartment
        t.contains("bgmi") || t.contains("indus") || t.contains("call of duty") ||
            t.contains("krunker") || t.contains("1v1") || t.contains("shell") -> Icons.Rounded.MyLocation
        t.contains("subway") || t.contains("temple") -> Icons.Rounded.DirectionsRun
        t.contains("minecraft") || t.contains("roblox") || t.contains("tetr") -> Icons.Rounded.ViewInAr
        t.contains("cricket") -> Icons.Rounded.SportsCricket
        t.contains("ludo") || t.contains("carrom") || t.contains("playok") -> Icons.Rounded.Casino
        t.contains("chess") || t.contains("2048") -> Icons.Rounded.Extension
        t.contains("slow roads") || t.contains("kart") -> Icons.Rounded.DirectionsCar
        t.contains("radio") -> Icons.Rounded.Radio
        t.contains("audiomass") || t.contains("audiotool") || t.contains("incredibox") -> Icons.Rounded.GraphicEq
        t.contains("spotify") || t.contains("jiosaavn") || t.contains("gaana") ||
            t.contains("soundcloud") || t.contains("youtube music") -> Icons.Rounded.Headphones
        t.contains("bandlab") || t.contains("song maker") -> Icons.Rounded.MusicNote
        t.contains("photopea") || t.contains("pixlr") || t.contains("capcut") -> Icons.Rounded.AutoFixHigh
        t.contains("canva") || t.contains("figma") || t.contains("vectorpea") || t.contains("excalidraw") -> Icons.Rounded.Palette
        t.contains("vs code") || t.contains("github") || t.contains("codepen") || t.contains("stackblitz") -> Icons.Rounded.Code
        t.contains("f-droid") -> Icons.Rounded.Android
        t.contains("telegram") || t.contains("whatsapp") -> Icons.Rounded.Send
        t.contains("discord") || t.contains("reddit") || t.contains("mastodon") || t.contains("instagram") -> Icons.Rounded.Forum
        t.contains("nasa") || t.contains("isro") || t.contains("geofs") -> Icons.Rounded.RocketLaunch
        t.contains("desmos") || t.contains("wolfram") -> Icons.Rounded.Calculate
        t.contains("khan") || t.contains("duolingo") || t.contains("physics wallah") || t.contains("swayam") -> Icons.Rounded.School
        t.contains("wikipedia") || t.contains("ilovepdf") || t.contains("translate") -> Icons.Rounded.MenuBook
        app.category == AppCategory.GAMES.label -> Icons.Rounded.SportsEsports
        app.category == AppCategory.ENTERTAINMENT.label -> Icons.Rounded.MusicNote
        app.category == AppCategory.THEMES.label -> Icons.Rounded.Brush
        app.category == AppCategory.SOCIAL.label -> Icons.Rounded.Forum
        app.category == AppCategory.EDUCATION.label -> Icons.Rounded.School
        else -> Icons.Rounded.Apps
    }

    val host = remember(app.downloadUrl) {
        runCatching { Uri.parse(app.downloadUrl.trim()).host?.removePrefix("www.") }.getOrNull()
    }
    val realFaviconUrl = remember(host) {
        host?.takeIf { it.isNotBlank() }?.let {
            "https://www.google.com/s2/favicons?domain=$it&sz=128"
        }
    }

    @Composable
    fun FallbackBadge() {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(listOf(pair.first, pair.second))),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = app.title,
                tint = Color.White,
                modifier = Modifier.size(size * 0.54f)
            )
        }
    }

    Surface(
        shape = RoundedCornerShape(size * 0.26f),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        tonalElevation = 1.dp,
        modifier = Modifier.size(size)
    ) {
        if (realFaviconUrl != null) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(realFaviconUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = app.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.92f))
                    .padding(size * 0.12f),
                loading = { FallbackBadge() },
                error = { FallbackBadge() }
            )
        } else {
            FallbackBadge()
        }
    }
}

@Composable
private fun EmptyStoreState(
    hasAnyApps: Boolean,
    isGamesMode: Boolean,
    isSubmitting: Boolean,
    onSeedCatalog: () -> Unit,
    onPublishClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (isGamesMode) Icons.Rounded.SportsEsports else Icons.Rounded.Apps,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = if (hasAnyApps) "No matching results" else "Play Hub is ready!",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Try another keyword or publish a new app/game directly to the store.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(18.dp))
        if (!hasAnyApps) {
            Button(
                onClick = onSeedCatalog,
                enabled = !isSubmitting,
                modifier = Modifier.testTag("seed_catalog_button")
            ) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Load Free Catalog")
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        OutlinedButton(onClick = onPublishClick) {
            Text(if (isGamesMode) "Publish a Game" else "Publish an App")
        }
    }
}

@Composable
fun PublishAppDialog(
    defaultCreatorName: String,
    defaultCategory: AppCategory = AppCategory.TOOLS,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (
        creatorName: String,
        title: String,
        tagline: String,
        description: String,
        category: AppCategory,
        downloadUrl: String,
        versionName: String,
        visibility: String,
        accentIndex: Long
    ) -> Unit
) {
    var creatorName by remember { mutableStateOf(defaultCreatorName) }
    var title by remember { mutableStateOf("") }
    var tagline by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var downloadUrl by remember { mutableStateOf("https://") }
    var versionName by remember { mutableStateOf("1.0.0") }
    var selectedCategory by remember { mutableStateOf(defaultCategory) }
    var isPublic by remember { mutableStateOf(true) }
    var certifiedOriginal by remember { mutableStateOf(false) }
    var accentIndex by remember { mutableLongStateOf(defaultCategory.ordinal.toLong()) }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Publish App / Game to Play Hub") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("App or Game Name (2–60 chars)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = creatorName,
                    onValueChange = { creatorName = it },
                    label = { Text("Developer / Studio Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = tagline,
                    onValueChange = { tagline = it },
                    label = { Text("Short Tagline") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (min 5 chars)") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = downloadUrl,
                    onValueChange = { downloadUrl = it },
                    label = { Text("Official URL (https://)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = versionName,
                    onValueChange = { versionName = it },
                    label = { Text("Version / Region Tag (e.g. India • 1.0)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Category", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AppCategory.entries.forEachIndexed { idx, cat ->
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
                    Text("Public on Play Hub", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = isPublic, onCheckedChange = { isPublic = it })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = certifiedOriginal,
                        onCheckedChange = { certifiedOriginal = it }
                    )
                    Text(
                        text = "Anti-Copy Pledge: I confirm this listing is original or authorized. Copied listings face ban.",
                        style = MaterialTheme.typography.bodySmall
                    )
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
                    val cleanTag = tagline.trim()
                    val cleanDesc = description.trim()
                    val cleanUrl = downloadUrl.trim()
                    when {
                        cleanTitle.length < 2 -> validationError = "Name must be at least 2 characters"
                        cleanTag.length < 2 -> validationError = "Tagline must be at least 2 characters"
                        cleanDesc.length < 5 -> validationError = "Description must be at least 5 characters"
                        !(cleanUrl.startsWith("https://") || cleanUrl.startsWith("http://")) || cleanUrl.length < 8 ->
                            validationError = "Enter a valid https:// or http:// URL"
                        else -> {
                            validationError = null
                            onSubmit(
                                creatorName,
                                cleanTitle,
                                cleanTag,
                                cleanDesc,
                                selectedCategory,
                                cleanUrl,
                                versionName,
                                if (isPublic) "public" else "private",
                                accentIndex
                            )
                        }
                    }
                }
            ) {
                Text(if (isSubmitting) "Publishing..." else "Publish Now")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
internal fun ReportCopyDialog(
    targetTitle: String,
    targetKind: String,
    onDismiss: () -> Unit,
    onSubmit: (reason: String) -> Unit
) {
    var reason by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report Copied $targetKind") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Flag '$targetTitle' for owner review if it copies another developer's app or game.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = { if (it.length <= 300) reason = it },
                    label = { Text("Reason (min 3 chars)") },
                    placeholder = { Text("e.g. Copied original app/game without permission") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Text(error ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val clean = reason.trim()
                if (clean.length < 3) {
                    error = "Enter at least 3 characters"
                } else {
                    onSubmit(clean)
                }
            }) {
                Text("Send Report")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
