package com.example.ui.screens

import android.app.DownloadManager
import android.text.format.Formatter
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddToHomeScreen
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.OpenCatalogData
import com.example.downloads.DownloadStartResult
import com.example.downloads.DownloadedItem
import com.example.downloads.enqueueFileDownload
import com.example.downloads.openDownloadedFile
import com.example.downloads.openSystemDownloadsApp
import com.example.downloads.pinGameOrAppToHomeScreen
import com.example.downloads.pinPlayHubToHomeScreen
import com.example.downloads.queryPlayHubDownloads
import com.example.downloads.removeTrackedDownload
import com.example.model.StoreApp
import kotlinx.coroutines.delay

@Composable
fun DownloadsDialog(
    targetApp: StoreApp?,
    onOpenInBrowser: (String) -> Unit,
    onShowMessage: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var downloads by remember { mutableStateOf(queryPlayHubDownloads(context)) }
    var directUrlInput by remember { mutableStateOf("") }
    var catalogSearch by remember { mutableStateOf("") }

    val catalogItems = remember { OpenCatalogData.starterApps() }
    val featuredQuickApp = remember(targetApp, catalogItems) {
        targetApp ?: catalogItems.first()
    }
    val filteredCatalog = remember(catalogSearch, catalogItems) {
        val q = catalogSearch.trim()
        if (q.isEmpty()) {
            catalogItems.take(18)
        } else {
            catalogItems.filter {
                it.title.contains(q, ignoreCase = true) ||
                    it.creatorName.contains(q, ignoreCase = true) ||
                    it.category.contains(q, ignoreCase = true)
            }.take(24)
        }
    }

    fun refreshDownloads() {
        downloads = queryPlayHubDownloads(context)
    }

    fun triggerAppDownload(app: StoreApp) {
        when (val result = enqueueFileDownload(
            context = context,
            url = app.downloadUrl,
            title = app.title
        )) {
            is DownloadStartResult.Started -> {
                refreshDownloads()
                onShowMessage("Downloading ${result.fileName} to device Downloads...")
            }
            is DownloadStartResult.Failed -> {
                onShowMessage(result.message)
            }
        }
    }

    fun triggerUrlDownload(rawUrl: String) {
        val normalized = rawUrl.trim().let {
            if (it.isNotEmpty() && !it.startsWith("http://") && !it.startsWith("https://")) {
                "https://$it"
            } else {
                it
            }
        }
        if (normalized.isEmpty()) {
            onShowMessage("Enter any download URL first, or tap any app below.")
            return
        }
        when (val result = enqueueFileDownload(
            context = context,
            url = normalized,
            title = normalized.substringAfter("://").substringBefore("/")
        )) {
            is DownloadStartResult.Started -> {
                directUrlInput = ""
                refreshDownloads()
                onShowMessage("Downloading ${result.fileName} to Downloads...")
            }
            is DownloadStartResult.Failed -> {
                onShowMessage(result.message)
            }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            refreshDownloads()
            delay(1000L)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Download,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = targetApp?.let { "Download & Install • ${it.title}" } ?: "Play Hub Downloads",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Tap any item to download to phone or add to Home Screen",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = { refreshDownloads() }) {
                            Icon(Icons.Rounded.Refresh, contentDescription = "Refresh Downloads")
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("close_downloads_dialog")
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close Downloads")
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (targetApp != null) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        PlayHubAppIconBadge(app = targetApp, size = 48.dp)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = targetApp.title,
                                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                            Text(
                                                text = "${targetApp.creatorName} • ${targetApp.category} • ${targetApp.versionName}",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Install this app icon directly onto your phone's Home Screen, download its file/package to your Downloads folder, or open its official download page.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                                    )

                                    Button(
                                        onClick = { triggerAppDownload(targetApp) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("start_device_download_button")
                                    ) {
                                        Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Download to Device Storage")
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            val pinned = pinGameOrAppToHomeScreen(context, targetApp)
                                            onShowMessage(
                                                if (pinned) {
                                                    "Home Screen install prompt sent for ${targetApp.title}!"
                                                } else {
                                                    "Your launcher doesn't support pinned shortcuts. Use Download or Play directly."
                                                }
                                            )
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("pin_home_shortcut_button")
                                    ) {
                                        Icon(Icons.Rounded.AddToHomeScreen, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Install App Icon on Home Screen")
                                    }

                                    TextButton(
                                        onClick = {
                                            onDismiss()
                                            onOpenInBrowser(targetApp.downloadUrl)
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Rounded.Language, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Open Official Download Page in Browser")
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Downloaded Files (${downloads.size})",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(
                                    onClick = {
                                        val pinned = pinPlayHubToHomeScreen(context)
                                        onShowMessage(
                                            if (pinned) {
                                                "Play Hub icon sent to your phone Home Screen!"
                                            } else {
                                                "Launcher didn't allow shortcut prompt; use Chrome ⋮ -> Install and create shortcut."
                                            }
                                        )
                                    },
                                    modifier = Modifier.testTag("pin_playhub_home_button")
                                ) {
                                    Icon(Icons.Rounded.AddToHomeScreen, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Home Icon")
                                }
                                TextButton(
                                    onClick = {
                                        if (!openSystemDownloadsApp(context)) {
                                            onShowMessage("Open your phone's Files / Downloads app to view saved files.")
                                        }
                                    }
                                ) {
                                    Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Device Folder")
                                }
                            }
                        }
                    }

                    if (downloads.isEmpty()) {
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                shape = RoundedCornerShape(16.dp),
                                tonalElevation = 2.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { triggerAppDownload(featuredQuickApp) }
                                    .testTag("empty_downloads_tap_card")
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Download,
                                        contentDescription = "Tap to download",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(38.dp)
                                    )
                                    Text(
                                        text = "Tap here to start your first download",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "Tap this box or choose any game/app below to save it right to your phone Downloads or add its icon to your Home Screen.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { triggerAppDownload(featuredQuickApp) },
                                            modifier = Modifier
                                                .weight(1f)
                                                .minimumInteractiveComponentSize()
                                                .testTag("empty_state_download_now_button")
                                        ) {
                                            Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Download ${featuredQuickApp.title}",
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        FilledTonalButton(
                                            onClick = {
                                                val pinned = pinGameOrAppToHomeScreen(context, featuredQuickApp)
                                                onShowMessage(
                                                    if (pinned) {
                                                        "Home Screen icon added for ${featuredQuickApp.title}!"
                                                    } else {
                                                        "Launcher shortcut not supported on this screen."
                                                    }
                                                )
                                            },
                                            modifier = Modifier
                                                .minimumInteractiveComponentSize()
                                                .testTag("empty_state_pin_button")
                                        ) {
                                            Icon(Icons.Rounded.AddToHomeScreen, contentDescription = "Add to Home Screen", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        items(downloads, key = { "dl_${it.id}" }) { item ->
                            DownloadItemRow(
                                item = item,
                                onOpen = {
                                    if (!openDownloadedFile(context, item)) {
                                        onShowMessage("Check your phone's Files / Downloads folder to open this file.")
                                    }
                                },
                                onRemove = {
                                    removeTrackedDownload(context, item.id)
                                    refreshDownloads()
                                }
                            )
                        }
                    }

                    // Direct Link / URL Download Bar
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.Link,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Download From Any Direct Link",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = directUrlInput,
                                        onValueChange = { directUrlInput = it },
                                        placeholder = { Text("Paste https://... link") },
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("direct_download_url_input")
                                    )
                                    Button(
                                        onClick = { triggerUrlDownload(directUrlInput) },
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .testTag("direct_download_url_button")
                                    ) {
                                        Text("Get")
                                    }
                                }
                            }
                        }
                    }

                    // One-Tap Download & Home Screen Installer Catalog
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "One-Tap Download & Home Screen Installer",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            OutlinedTextField(
                                value = catalogSearch,
                                onValueChange = { catalogSearch = it },
                                placeholder = { Text("Search Free Fire, BGMI, Roblox, Spotify, JioSaavn...") },
                                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Search apps to download") },
                                singleLine = true,
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("downloads_catalog_search")
                            )
                        }
                    }

                    items(filteredCatalog, key = { "quick_dl_${it.id}" }) { app ->
                        QuickDownloadCatalogRow(
                            app = app,
                            onTapDownload = { triggerAppDownload(app) },
                            onPinHome = {
                                val pinned = pinGameOrAppToHomeScreen(context, app)
                                onShowMessage(
                                    if (pinned) {
                                        "Home Screen install prompt sent for ${app.title}!"
                                    } else {
                                        "Launcher didn't support pinned shortcuts."
                                    }
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickDownloadCatalogRow(
    app: StoreApp,
    onTapDownload: () -> Unit,
    onPinHome: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTapDownload)
            .testTag("quick_download_row_${app.id}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlayHubAppIconBadge(app = app, size = 44.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${app.creatorName} • ${app.category}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(
                onClick = onPinHome,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("quick_pin_home_${app.id}")
            ) {
                Icon(
                    imageVector = Icons.Rounded.AddToHomeScreen,
                    contentDescription = "Add ${app.title} to Home Screen",
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
            FilledTonalButton(
                onClick = onTapDownload,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("quick_download_btn_${app.id}")
            ) {
                Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Download", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun DownloadItemRow(
    item: DownloadedItem,
    onOpen: () -> Unit,
    onRemove: () -> Unit
) {
    val context = LocalContext.current
    val isSuccess = item.status == DownloadManager.STATUS_SUCCESSFUL
    val isFailed = item.status == DownloadManager.STATUS_FAILED
    val isRunning = item.status == DownloadManager.STATUS_RUNNING || item.status == DownloadManager.STATUS_PENDING

    val sizeLabel = remember(item.bytesDownloaded, item.bytesTotal) {
        val dl = Formatter.formatShortFileSize(context, item.bytesDownloaded.coerceAtLeast(0L))
        if (item.bytesTotal > 0L) {
            val tot = Formatter.formatShortFileSize(context, item.bytesTotal)
            "$dl / $tot"
        } else {
            dl
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isSuccess, onClick = onOpen)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = when {
                        isSuccess -> Icons.Rounded.CheckCircle
                        isFailed -> Icons.Rounded.ErrorOutline
                        else -> Icons.Rounded.Download
                    },
                    contentDescription = null,
                    tint = when {
                        isSuccess -> MaterialTheme.colorScheme.primary
                        isFailed -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.secondary
                    },
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title.ifBlank { "Download #${item.id}" },
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = when {
                            isSuccess -> "Downloaded • $sizeLabel"
                            isFailed -> "Download failed (Reason ${item.reason})"
                            isRunning -> "Downloading ${item.progressPercent}% • $sizeLabel"
                            else -> "Paused • $sizeLabel"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (isSuccess) {
                    OutlinedButton(
                        onClick = onOpen,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Text("Open", style = MaterialTheme.typography.labelMedium)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
                IconButton(onClick = onRemove) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Remove download",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (isRunning) {
                Spacer(modifier = Modifier.height(8.dp))
                if (item.bytesTotal > 0L) {
                    LinearProgressIndicator(
                        progress = { item.progressPercent / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                    )
                } else {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                    )
                }
            }
        }
    }
}
