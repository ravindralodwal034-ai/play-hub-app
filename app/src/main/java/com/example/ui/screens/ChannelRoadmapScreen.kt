package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.LocalAppEntity
import com.example.data.local.LocalChannelEntity
import com.example.data.local.LocalVideoEntity
import com.example.data.local.RoadmapMilestoneEntity
import com.example.model.AppCategory
import com.example.model.StoreApp
import com.example.model.VideoCategory

@Composable
fun ChannelRoadmapScreen(
    channels: List<LocalChannelEntity>,
    selectedChannelId: String?,
    allMilestones: List<RoadmapMilestoneEntity>,
    localApps: List<LocalAppEntity>,
    localVideos: List<LocalVideoEntity>,
    showGuestAuthCard: Boolean,
    isSigningIn: Boolean,
    onSelectChannel: (String) -> Unit,
    onCreateChannel: (
        name: String,
        handle: String,
        niche: String,
        language: String,
        weeklyVideos: Int,
        weeklyShorts: Int,
        bio: String,
        accentIndex: Int,
        onDone: () -> Unit
    ) -> Unit,
    onDeleteChannel: (String) -> Unit,
    onToggleMilestone: (RoadmapMilestoneEntity) -> Unit,
    onAdjustUploads: (RoadmapMilestoneEntity, Int) -> Unit,
    onEditApp: (LocalAppEntity, String, String, String, String, AppCategory, String, String, () -> Unit) -> Unit,
    onDeleteApp: (String) -> Unit,
    onEditVideo: (LocalVideoEntity, String, String, String, VideoCategory, String, String, String, () -> Unit) -> Unit,
    onDeleteVideo: (String) -> Unit,
    onRunApp: (StoreApp) -> Unit,
    onSignInClick: () -> Unit
) {
    var showCreateChannelDialog by remember { mutableStateOf(false) }
    var selectedMonth by remember { mutableIntStateOf(1) }
    var expandedMilestoneId by remember { mutableStateOf<String?>(null) }
    var editingLocalApp by remember { mutableStateOf<LocalAppEntity?>(null) }
    var editingLocalVideo by remember { mutableStateOf<LocalVideoEntity?>(null) }

    val activeChannel = remember(channels, selectedChannelId) {
        channels.firstOrNull { it.id == selectedChannelId } ?: channels.firstOrNull()
    }

    val channelMilestones = remember(allMilestones, activeChannel) {
        if (activeChannel == null) emptyList()
        else allMilestones.filter { it.channelId == activeChannel.id }.sortedBy { it.weekNumber }
    }

    val monthMilestones = remember(channelMilestones, selectedMonth) {
        channelMilestones.filter { it.monthNumber == selectedMonth }
    }

    val completedWeeks = channelMilestones.count { it.isCompleted }
    val totalUploads = channelMilestones.sumOf { it.completedUploads }
    val targetUploads = channelMilestones.sumOf { it.targetUploads }.coerceAtLeast(1)
    val progressFraction = if (channelMilestones.isEmpty()) 0f
    else (completedWeeks.toFloat() / channelMilestones.size.toFloat()).coerceIn(0f, 1f)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (showGuestAuthCard) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Free No-Login Mode Active",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "All channels, 90-day plans & apps save free on your phone. Want cloud backup across devices?",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Button(
                            onClick = onSignInClick,
                            enabled = !isSigningIn,
                            modifier = Modifier.testTag("studio_google_sign_in_button")
                        ) {
                            Text(if (isSigningIn) "Signing..." else "Sign In")
                        }
                    }
                }
            }
        }

        // 3-Month Channel Builder Hero
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
                                text = "3-Month Channel Builder",
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Launch your channel free & follow a 90-day growth plan",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = { showCreateChannelDialog = true },
                            modifier = Modifier.testTag("create_channel_button")
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New")
                        }
                    }

                    if (channels.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            channels.forEach { ch ->
                                FilterChip(
                                    selected = ch.id == activeChannel?.id,
                                    onClick = { onSelectChannel(ch.id) },
                                    label = { Text(ch.name) }
                                )
                            }
                        }
                    }

                    if (activeChannel == null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Rounded.RocketLaunch,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Start Your 90-Day Tube Channel",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Pick your niche (Marwadi Music, World Songs, Cartoons, Tech, Education), set your weekly upload target, and unlock 12 step-by-step weekly missions.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = { showCreateChannelDialog = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Create Free Channel & 90-Day Plan")
                                }
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${activeChannel.name} (${activeChannel.handle})",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${activeChannel.niche} • ${activeChannel.language} • Goal: ${activeChannel.weeklyVideoTarget} Videos + ${activeChannel.weeklyShortsTarget} Shorts/wk",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { onDeleteChannel(activeChannel.id) }) {
                                Icon(
                                    Icons.Rounded.Delete,
                                    contentDescription = "Delete Channel",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "90-Day Roadmap: ${(progressFraction * 100).toInt()}%",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                text = "$completedWeeks/12 weeks • $totalUploads/$targetUploads uploads",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { progressFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                        )
                    }
                }
            }
        }

        // Month 1, Month 2, Month 3 Filter & Weekly Cards
        if (activeChannel != null) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        1 to "Month 1: Launch",
                        2 to "Month 2: Growth",
                        3 to "Month 3: Scale"
                    ).forEach { (month, label) ->
                        FilterChip(
                            selected = selectedMonth == month,
                            onClick = { selectedMonth = month },
                            label = { Text(label) }
                        )
                    }
                }
            }

            items(monthMilestones, key = { it.id }) { milestone ->
                val isExpanded = expandedMilestoneId == milestone.id
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (milestone.isCompleted) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerLow
                        }
                    ),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            expandedMilestoneId = if (isExpanded) null else milestone.id
                        }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { onToggleMilestone(milestone) }) {
                                Icon(
                                    imageVector = if (milestone.isCompleted) {
                                        Icons.Rounded.CheckCircle
                                    } else {
                                        Icons.Rounded.RadioButtonUnchecked
                                    },
                                    contentDescription = "Toggle Week ${milestone.weekNumber}",
                                    tint = if (milestone.isCompleted) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Week ${milestone.weekNumber}: ${milestone.title}",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = "Uploads logged: ${milestone.completedUploads} / ${milestone.targetUploads}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = {
                                    expandedMilestoneId = if (isExpanded) null else milestone.id
                                }
                            ) {
                                Icon(
                                    imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                    contentDescription = "Details"
                                )
                            }
                        }

                        AnimatedVisibility(visible = isExpanded) {
                            Column(modifier = Modifier.padding(start = 12.dp, top = 6.dp, end = 8.dp)) {
                                Text(
                                    text = milestone.guide,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Weekly Uploads:",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                    OutlinedButton(
                                        onClick = { onAdjustUploads(milestone, -1) },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Rounded.Remove, contentDescription = "Decrease", modifier = Modifier.size(16.dp))
                                    }
                                    Text(
                                        text = "${milestone.completedUploads}/${milestone.targetUploads}",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Button(
                                        onClick = { onAdjustUploads(milestone, 1) },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Rounded.Add, contentDescription = "Log Upload", modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Log")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Local Apps & Videos Management (Free Edit / Delete / Instant Run)
        if (showGuestAuthCard) {
            item {
                Text(
                    text = "My Instant Apps (${localApps.size})",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            if (localApps.isEmpty()) {
                item {
                    Text(
                        text = "No custom apps added yet. Publish any website or editor from the Apps tab!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(localApps, key = { "l_app_${it.id}" }) { app ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Apps, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(app.title, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "${app.category} • ${app.launchCount} instant runs",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { onRunApp(app.toStoreApp()) }) {
                                Icon(Icons.Rounded.Bolt, contentDescription = "Run Instant", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { editingLocalApp = app }) {
                                Icon(Icons.Rounded.Edit, contentDescription = "Edit App")
                            }
                            IconButton(onClick = { onDeleteApp(app.id) }) {
                                Icon(Icons.Rounded.Delete, contentDescription = "Delete App", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "My Tube Uploads (${localVideos.size})",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            if (localVideos.isEmpty()) {
                item {
                    Text(
                        text = "No videos or music channels uploaded yet. Tap Upload in the Tube tab!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(localVideos, key = { "l_vid_${it.id}" }) { vid ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.PlayCircle, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(vid.title, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "${vid.language} • ${vid.viewCount} plays",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { editingLocalVideo = vid }) {
                                Icon(Icons.Rounded.Edit, contentDescription = "Edit Video")
                            }
                            IconButton(onClick = { onDeleteVideo(vid.id) }) {
                                Icon(Icons.Rounded.Delete, contentDescription = "Delete Video", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateChannelDialog) {
        CreateChannelDialog(
            onDismiss = { showCreateChannelDialog = false },
            onSubmit = { name, handle, niche, lang, vTarget, sTarget, bio ->
                onCreateChannel(name, handle, niche, lang, vTarget, sTarget, bio, 0) {
                    showCreateChannelDialog = false
                }
            }
        )
    }

    editingLocalApp?.let { app ->
        EditLocalAppDialog(
            app = app,
            onDismiss = { editingLocalApp = null },
            onSave = { creator, title, tagline, desc, cat, url, ver ->
                onEditApp(app, creator, title, tagline, desc, cat, url, ver) {
                    editingLocalApp = null
                }
            }
        )
    }

    editingLocalVideo?.let { vid ->
        EditLocalVideoDialog(
            video = vid,
            onDismiss = { editingLocalVideo = null },
            onSave = { ch, title, desc, cat, lang, url, dur ->
                onEditVideo(vid, ch, title, desc, cat, lang, url, dur) {
                    editingLocalVideo = null
                }
            }
        )
    }
}

@Composable
private fun CreateChannelDialog(
    onDismiss: () -> Unit,
    onSubmit: (
        name: String,
        handle: String,
        niche: String,
        language: String,
        weeklyVideos: Int,
        weeklyShorts: Int,
        bio: String
    ) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var handle by remember { mutableStateOf("") }
    var selectedNiche by remember { mutableStateOf("Marwadi Music") }
    var selectedLang by remember { mutableStateOf("Marwadi / Rajasthani") }
    var weeklyVideos by remember { mutableIntStateOf(2) }
    var weeklyShorts by remember { mutableIntStateOf(3) }
    var bio by remember { mutableStateOf("") }

    val niches = listOf("Marwadi Music", "World Music", "Cartoons", "Tech & Editing", "Education", "Gaming")
    val languages = listOf("Marwadi / Rajasthani", "Hindi", "English", "World Languages")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Tube Channel (90-Day Plan)") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Channel Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = handle,
                    onValueChange = { handle = it },
                    label = { Text("@handle (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Niche", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    niches.forEach { n ->
                        FilterChip(selected = selectedNiche == n, onClick = { selectedNiche = n }, label = { Text(n) })
                    }
                }
                Text("Primary Language", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    languages.forEach { l ->
                        FilterChip(selected = selectedLang == l, onClick = { selectedLang = l }, label = { Text(l) })
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Videos / week: $weeklyVideos", style = MaterialTheme.typography.bodyMedium)
                    Row {
                        IconButton(onClick = { if (weeklyVideos > 1) weeklyVideos-- }) {
                            Icon(Icons.Rounded.Remove, contentDescription = "Fewer videos")
                        }
                        IconButton(onClick = { if (weeklyVideos < 7) weeklyVideos++ }) {
                            Icon(Icons.Rounded.Add, contentDescription = "More videos")
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Shorts / week: $weeklyShorts", style = MaterialTheme.typography.bodyMedium)
                    Row {
                        IconButton(onClick = { if (weeklyShorts > 0) weeklyShorts-- }) {
                            Icon(Icons.Rounded.Remove, contentDescription = "Fewer shorts")
                        }
                        IconButton(onClick = { if (weeklyShorts < 14) weeklyShorts++ }) {
                            Icon(Icons.Rounded.Add, contentDescription = "More shorts")
                        }
                    }
                }
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Channel Description") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank(),
                onClick = { onSubmit(name, handle, selectedNiche, selectedLang, weeklyVideos, weeklyShorts, bio) }
            ) {
                Text("Launch 90-Day Plan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun EditLocalAppDialog(
    app: LocalAppEntity,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, AppCategory, String, String) -> Unit
) {
    var creator by remember { mutableStateOf(app.creatorName) }
    var title by remember { mutableStateOf(app.title) }
    var tagline by remember { mutableStateOf(app.tagline) }
    var description by remember { mutableStateOf(app.description) }
    var url by remember { mutableStateOf(app.downloadUrl) }
    var version by remember { mutableStateOf(app.versionName) }
    var category by remember {
        mutableStateOf(AppCategory.entries.firstOrNull { it.label == app.category } ?: AppCategory.TOOLS)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Instant App") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("App Name") }, singleLine = true)
                OutlinedTextField(value = creator, onValueChange = { creator = it }, label = { Text("Creator") }, singleLine = true)
                OutlinedTextField(value = tagline, onValueChange = { tagline = it }, label = { Text("Tagline") }, singleLine = true)
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, maxLines = 3)
                OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("Instant Web URL") }, singleLine = true)
                OutlinedTextField(value = version, onValueChange = { version = it }, label = { Text("Version") }, singleLine = true)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AppCategory.entries.forEach { cat ->
                        FilterChip(selected = category == cat, onClick = { category = cat }, label = { Text(cat.label) })
                    }
                }
            }
        },
        confirmButton = {
            Button(enabled = title.isNotBlank(), onClick = { onSave(creator, title, tagline, description, category, url, version) }) {
                Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun EditLocalVideoDialog(
    video: LocalVideoEntity,
    onDismiss: () -> Unit,
    onSave: (String, String, String, VideoCategory, String, String, String) -> Unit
) {
    var channel by remember { mutableStateOf(video.channelName) }
    var title by remember { mutableStateOf(video.title) }
    var description by remember { mutableStateOf(video.description) }
    var language by remember { mutableStateOf(video.language) }
    var url by remember { mutableStateOf(video.videoUrl) }
    var duration by remember { mutableStateOf(video.durationLabel) }
    var category by remember {
        mutableStateOf(VideoCategory.entries.firstOrNull { it.label == video.category } ?: VideoCategory.MUSIC)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Tube Upload") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, singleLine = true)
                OutlinedTextField(value = channel, onValueChange = { channel = it }, label = { Text("Channel") }, singleLine = true)
                OutlinedTextField(value = language, onValueChange = { language = it }, label = { Text("Language") }, singleLine = true)
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, maxLines = 3)
                OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("Stream URL") }, singleLine = true)
                OutlinedTextField(value = duration, onValueChange = { duration = it }, label = { Text("Duration") }, singleLine = true)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    VideoCategory.entries.forEach { cat ->
                        FilterChip(selected = category == cat, onClick = { category = cat }, label = { Text(cat.label) })
                    }
                }
            }
        },
        confirmButton = {
            Button(enabled = title.isNotBlank(), onClick = { onSave(channel, title, description, category, language, url, duration) }) {
                Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
