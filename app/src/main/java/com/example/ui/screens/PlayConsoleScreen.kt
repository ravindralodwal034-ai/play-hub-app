package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayArrow
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.LauncherThemeSettings
import com.example.downloads.openSystemDownloadsApp
import com.example.model.AppCategory
import com.example.model.StoreApp
import com.example.model.TubeThemePreset
import com.example.model.UserProfile
import com.example.ui.UiState
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.CyberPrimary
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.SaffronPrimary

@Composable
fun PlayConsoleScreen(
    isSignedIn: Boolean,
    profile: UserProfile?,
    appsState: UiState<List<StoreApp>>,
    themeSettings: LauncherThemeSettings,
    isSubmitting: Boolean,
    isSigningIn: Boolean,
    isOwnerAdmin: Boolean = false,
    openReportsCount: Int = 0,
    onOpenAdminControl: () -> Unit = {},
    onSignInClick: () -> Unit = {},
    onSignOutClick: () -> Unit = {},
    onUpdateProfile: (displayName: String, studioName: String, bio: String) -> Unit = { _, _, _ -> },
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
    onDeleteApp: (String) -> Unit,
    onRunApp: (StoreApp) -> Unit,
    onOpenDownloads: () -> Unit = {},
    onSelectThemePreset: (TubeThemePreset) -> Unit,
    onToggleDarkMode: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showPublishDialog by remember { mutableStateOf(false) }
    var editingApp by remember { mutableStateOf<StoreApp?>(null) }

    val myApps = (appsState as? UiState.Success)?.data ?: emptyList()
    val totalOpens = myApps.sumOf { it.downloadCount }
    val totalLikes = myApps.sumOf { it.likeCount }

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

        // Developer Account / Profile Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (isSignedIn) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = profile?.studioName?.ifBlank { "Play Console" } ?: "Play Console",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = profile?.displayName ?: "Verified Developer",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(
                                onClick = { showEditProfileDialog = true },
                                modifier = Modifier.testTag("edit_profile_button")
                            ) {
                                Icon(Icons.Rounded.Edit, contentDescription = "Edit Developer Profile")
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = profile?.bio?.ifBlank { "Publish Indian & global games, music tools, and web apps to Play Hub." }
                                ?: "Publish Indian & global games, music tools, and web apps to Play Hub.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ConsoleStatBadge("Listings", myApps.size.toString(), Modifier.weight(1f))
                            ConsoleStatBadge("Opens", totalOpens.toString(), Modifier.weight(1f))
                            ConsoleStatBadge("Likes", totalLikes.toString(), Modifier.weight(1f))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { showPublishDialog = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Publish App / Game")
                            }
                            OutlinedButton(
                                onClick = onSignOutClick,
                                modifier = Modifier.testTag("sign_out_button")
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Sign Out")
                            }
                        }
                    } else {
                        Text(
                            text = "Play Hub Developer Console",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Publish apps & games directly on your device for free, or sign in with Google for Cloud Sync & Owner Control.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { showPublishDialog = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Publish Local App")
                            }
                            OutlinedButton(
                                enabled = !isSigningIn,
                                onClick = onSignInClick,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("guest_sign_in_button")
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.Login, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isSigningIn) "Signing in..." else "Google Sign-In")
                            }
                        }
                    }
                }
            }
        }

        // My Published Apps & Games
        item {
            Text(
                text = "My Apps & Games (${myApps.size})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        if (myApps.isEmpty()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No custom apps or games published yet. Tap 'Publish' to add your own web app or game!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        } else {
            items(myApps, key = { "console_app_${it.id}" }) { app ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PlayHubAppIconBadge(app = app, size = 38.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = app.title,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${app.category} • ${app.visibility} • ${app.versionName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { onRunApp(app) }) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = "Launch ${app.title}", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { editingApp = app }) {
                            Icon(Icons.Rounded.Edit, contentDescription = "Edit ${app.title}")
                        }
                        IconButton(onClick = { onDeleteApp(app.id) }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Delete ${app.title}", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        // Downloads & Device Settings Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Download, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Downloads & Device Settings",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    Text(
                        text = "Manage files downloaded via Play Hub, install app icons onto your phone's Home Screen, or open your device Downloads folder.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onOpenDownloads,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("console_open_downloads_button")
                        ) {
                            Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Downloads Manager")
                        }
                        OutlinedButton(
                            onClick = { openSystemDownloadsApp(context) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Phone Folder")
                        }
                    }
                }
            }
        }

        // Store Theme & Dark Mode Settings
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Play Store Appearance",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.DarkMode, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Pro Dark Theme", style = MaterialTheme.typography.bodyMedium)
                        }
                        Switch(
                            checked = themeSettings.darkMode,
                            onCheckedChange = onToggleDarkMode,
                            modifier = Modifier.testTag("console_dark_mode_switch")
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TubeThemePreset.entries.forEach { preset ->
                            val swatch = when (preset) {
                                TubeThemePreset.CRIMSON -> CrimsonPrimary
                                TubeThemePreset.CYBER -> CyberPrimary
                                TubeThemePreset.SAFFRON -> SaffronPrimary
                                TubeThemePreset.EMERALD -> EmeraldPrimary
                            }
                            val selected = themeSettings.themePreset == preset
                            Surface(
                                color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.clickable { onSelectThemePreset(preset) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(swatch)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = preset.label.replace("Tube ", ""),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                    if (selected) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPublishDialog) {
        PublishAppDialog(
            defaultCreatorName = profile?.studioName?.ifBlank { "My Studio" } ?: "My Studio",
            defaultCategory = AppCategory.GAMES,
            isSubmitting = isSubmitting,
            onDismiss = { showPublishDialog = false },
            onSubmit = { creator, title, tagline, desc, cat, url, ver, vis, accent ->
                onPublishApp(creator, title, tagline, desc, cat, url, ver, vis, accent) {
                    showPublishDialog = false
                }
            }
        )
    }

    if (showEditProfileDialog && isSignedIn) {
        var displayName by remember { mutableStateOf(profile?.displayName ?: "") }
        var studioName by remember { mutableStateOf(profile?.studioName ?: "") }
        var bio by remember { mutableStateOf(profile?.bio ?: "") }

        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text("Edit Developer Profile") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text("Developer Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = studioName,
                        onValueChange = { studioName = it },
                        label = { Text("Studio / Publisher Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = bio,
                        onValueChange = { bio = it },
                        label = { Text("Studio Bio") },
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
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) { Text("Cancel") }
            }
        )
    }

    editingApp?.let { app ->
        ConsoleEditAppDialog(
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
}

@Composable
private fun ConsoleStatBadge(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ConsoleEditAppDialog(
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
        title = { Text("Edit Listing") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("App / Game Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = creatorName, onValueChange = { creatorName = it }, label = { Text("Publisher") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = tagline, onValueChange = { tagline = it }, label = { Text("Tagline") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, minLines = 2, maxLines = 4, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = downloadUrl, onValueChange = { downloadUrl = it }, label = { Text("Official URL") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = versionName, onValueChange = { versionName = it }, label = { Text("Version") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AppCategory.entries.forEach { cat ->
                        FilterChip(selected = selectedCategory == cat, onClick = { selectedCategory = cat }, label = { Text(cat.label) })
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
                        onSave(creatorName, title, tagline, description, selectedCategory, downloadUrl, versionName, if (isPublic) "public" else "private")
                    }
                }
            ) { Text("Save Changes") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
