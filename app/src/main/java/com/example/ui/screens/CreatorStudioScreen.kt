package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.AppCategory
import com.example.model.StoreApp
import com.example.model.TubeVideo
import com.example.model.UserProfile
import com.example.model.VideoCategory
import com.example.ui.UiState

@Composable
fun CreatorStudioScreen(
    profile: UserProfile?,
    myAppsState: UiState<List<StoreApp>>,
    myVideosState: UiState<List<TubeVideo>>,
    isSubmitting: Boolean,
    onUpdateProfile: (displayName: String, studioName: String, bio: String) -> Unit,
    onUpdateApp: (
        appId: String,
        creatorName: String,
        title: String,
        tagline: String,
        description: String,
        category: AppCategory,
        downloadUrl: String,
        versionName: String,
        visibility: String,
        onDone: () -> Unit
    ) -> Unit,
    onUpdateVideo: (
        videoId: String,
        channelName: String,
        title: String,
        description: String,
        category: VideoCategory,
        language: String,
        videoUrl: String,
        durationLabel: String,
        visibility: String,
        onDone: () -> Unit
    ) -> Unit,
    onDeleteApp: (String) -> Unit,
    onDeleteVideo: (String) -> Unit,
    onSignOut: () -> Unit,
    isOwnerAdmin: Boolean = false,
    openReportsCount: Int = 0,
    onOpenAdminControl: () -> Unit = {}
) {
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var editingApp by remember { mutableStateOf<StoreApp?>(null) }
    var editingVideo by remember { mutableStateOf<TubeVideo?>(null) }

    val myApps = (myAppsState as? UiState.Success)?.data ?: emptyList()
    val myVideos = (myVideosState as? UiState.Success)?.data ?: emptyList()

    val totalViews = myVideos.sumOf { it.viewCount }
    val totalDownloads = myApps.sumOf { it.downloadCount }
    val totalLikes = myApps.sumOf { it.likeCount } + myVideos.sumOf { it.likeCount }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (isOwnerAdmin) {
            item {
                Card(
                    onClick = onOpenAdminControl,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_admin_control_button")
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AdminPanelSettings,
                            contentDescription = "Owner Control Center",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(34.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Owner Live Control Center",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = if (openReportsCount > 0) {
                                    "$openReportsCount pending copy report(s) • Live banner & ban controls"
                                } else {
                                    "Live banner, maintenance mode & instant anti-copy moderation"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                            )
                        }
                        Button(onClick = onOpenAdminControl) {
                            Text("Manage")
                        }
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = profile?.studioName?.ifBlank { "Creator Studio" } ?: "Creator Studio",
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Text(
                                text = profile?.displayName ?: "Authenticated Creator",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(
                            onClick = { showEditProfileDialog = true },
                            modifier = Modifier.testTag("edit_profile_button")
                        ) {
                            Icon(Icons.Rounded.Edit, contentDescription = "Edit Studio Profile")
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = profile?.bio?.ifBlank { "Publish free apps, cartoons & world music to reach viewers." }
                            ?: "Publish free apps, cartoons & world music to reach viewers.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatBadge(label = "Views", value = totalViews.toString(), modifier = Modifier.weight(1f))
                        StatBadge(label = "Opens", value = totalDownloads.toString(), modifier = Modifier.weight(1f))
                        StatBadge(label = "Likes", value = totalLikes.toString(), modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onSignOut,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sign_out_button")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sign Out")
                    }
                }
            }
        }

        item {
            Text(
                text = "My Published Apps (${myApps.size})",
                style = MaterialTheme.typography.titleMedium
            )
        }

        if (myApps.isEmpty()) {
            item {
                Text(
                    text = "No apps published by your studio yet. Use the Apps tab to publish!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(myApps, key = { "my_app_${it.id}" }) { app ->
                StudioManagedItemCard(
                    icon = Icons.Rounded.Apps,
                    title = app.title,
                    subtitle = "${app.category} • ${app.visibility} • ${app.downloadCount} opens • ${app.likeCount} likes",
                    onEdit = { editingApp = app },
                    onDelete = { onDeleteApp(app.id) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "My Tube Videos & Music (${myVideos.size})",
                style = MaterialTheme.typography.titleMedium
            )
        }

        if (myVideos.isEmpty()) {
            item {
                Text(
                    text = "No videos or music channels uploaded yet. Use the Tube tab to upload!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(myVideos, key = { "my_vid_${it.id}" }) { vid ->
                StudioManagedItemCard(
                    icon = Icons.Rounded.PlayCircle,
                    title = vid.title,
                    subtitle = "${vid.language} • ${vid.visibility} • ${vid.viewCount} views • ${vid.likeCount} likes",
                    onEdit = { editingVideo = vid },
                    onDelete = { onDeleteVideo(vid.id) }
                )
            }
        }
    }

    if (showEditProfileDialog) {
        var displayName by remember { mutableStateOf(profile?.displayName ?: "") }
        var studioName by remember { mutableStateOf(profile?.studioName ?: "") }
        var bio by remember { mutableStateOf(profile?.bio ?: "") }

        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text("Edit Creator Studio") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text("Display Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = studioName,
                        onValueChange = { studioName = it },
                        label = { Text("Studio / Channel Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = bio,
                        onValueChange = { bio = it },
                        label = { Text("Creator Bio") },
                        minLines = 2,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = !isSubmitting && displayName.trim().isNotEmpty() && studioName.trim().isNotEmpty(),
                    onClick = {
                        onUpdateProfile(displayName, studioName, bio)
                        showEditProfileDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) { Text("Cancel") }
            }
        )
    }

    editingApp?.let { app ->
        EditAppDialog(
            app = app,
            isSubmitting = isSubmitting,
            onDismiss = { editingApp = null },
            onSave = { creator, title, tagline, desc, cat, url, ver, vis ->
                onUpdateApp(app.id, creator, title, tagline, desc, cat, url, ver, vis) {
                    editingApp = null
                }
            }
        )
    }

    editingVideo?.let { vid ->
        EditVideoDialog(
            video = vid,
            isSubmitting = isSubmitting,
            onDismiss = { editingVideo = null },
            onSave = { channel, title, desc, cat, lang, url, dur, vis ->
                onUpdateVideo(vid.id, channel, title, desc, cat, lang, url, dur, vis) {
                    editingVideo = null
                }
            }
        )
    }
}

@Composable
private fun EditAppDialog(
    app: StoreApp,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSave: (
        creatorName: String,
        title: String,
        tagline: String,
        description: String,
        category: AppCategory,
        downloadUrl: String,
        versionName: String,
        visibility: String
    ) -> Unit
) {
    var creatorName by remember { mutableStateOf(app.creatorName) }
    var title by remember { mutableStateOf(app.title) }
    var tagline by remember { mutableStateOf(app.tagline) }
    var description by remember { mutableStateOf(app.description) }
    var downloadUrl by remember { mutableStateOf(app.downloadUrl) }
    var versionName by remember { mutableStateOf(app.versionName) }
    var selectedCategory by remember {
        mutableStateOf(AppCategory.entries.firstOrNull { it.label == app.category } ?: AppCategory.TOOLS)
    }
    var isPublic by remember { mutableStateOf(app.visibility == "public") }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit App Listing") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("App Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = creatorName,
                    onValueChange = { creatorName = it },
                    label = { Text("Studio Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = tagline,
                    onValueChange = { tagline = it },
                    label = { Text("Tagline") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = downloadUrl,
                    onValueChange = { downloadUrl = it },
                    label = { Text("Official URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = versionName,
                    onValueChange = { versionName = it },
                    label = { Text("Version") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AppCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat.label) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Public on Tube Store", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = isPublic, onCheckedChange = { isPublic = it })
                }
                if (validationError != null) {
                    Text(validationError ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isSubmitting,
                onClick = {
                    if (title.trim().length < 2 || tagline.trim().length < 2 || description.trim().length < 5) {
                        validationError = "Fill in valid title, tagline, and description"
                    } else {
                        onSave(
                            creatorName,
                            title,
                            tagline,
                            description,
                            selectedCategory,
                            downloadUrl,
                            versionName,
                            if (isPublic) "public" else "private"
                        )
                    }
                }
            ) { Text("Save Changes") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun EditVideoDialog(
    video: TubeVideo,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSave: (
        channelName: String,
        title: String,
        description: String,
        category: VideoCategory,
        language: String,
        videoUrl: String,
        durationLabel: String,
        visibility: String
    ) -> Unit
) {
    var channelName by remember { mutableStateOf(video.channelName) }
    var title by remember { mutableStateOf(video.title) }
    var description by remember { mutableStateOf(video.description) }
    var language by remember { mutableStateOf(video.language) }
    var videoUrl by remember { mutableStateOf(video.videoUrl) }
    var durationLabel by remember { mutableStateOf(video.durationLabel) }
    var selectedCategory by remember {
        mutableStateOf(VideoCategory.entries.firstOrNull { it.label == video.category } ?: VideoCategory.MUSIC)
    }
    var isPublic by remember { mutableStateOf(video.visibility == "public") }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Video / Music Channel") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
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
                    label = { Text("Description") },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = language,
                    onValueChange = { language = it },
                    label = { Text("Language / Region") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = videoUrl,
                    onValueChange = { videoUrl = it },
                    label = { Text("Stream / Video URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = durationLabel,
                    onValueChange = { durationLabel = it },
                    label = { Text("Duration") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    VideoCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat.label) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Public Channel", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = isPublic, onCheckedChange = { isPublic = it })
                }
                if (validationError != null) {
                    Text(validationError ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isSubmitting,
                onClick = {
                    if (title.trim().length < 2 || channelName.trim().length < 2 || description.trim().length < 5) {
                        validationError = "Fill in valid title, channel name, and description"
                    } else {
                        onSave(
                            channelName,
                            title,
                            description,
                            selectedCategory,
                            language,
                            videoUrl,
                            durationLabel,
                            if (isPublic) "public" else "private"
                        )
                    }
                }
            ) { Text("Save Changes") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun StatBadge(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun StudioManagedItemCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleSmall)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Visibility,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onEdit) {
                Icon(
                    Icons.Rounded.Edit,
                    contentDescription = "Edit $title",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Rounded.Delete,
                    contentDescription = "Delete $title",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
