package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.GppGood
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.ReportProblem
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.LiveAppConfig
import com.example.model.ModerationReport
import com.example.model.OWNER_ADMIN_EMAIL
import com.example.model.StoreApp
import com.example.model.TubeVideo

@Composable
fun AdminControlDialog(
    liveConfig: LiveAppConfig,
    reports: List<ModerationReport>,
    publicApps: List<StoreApp>,
    publicVideos: List<TubeVideo>,
    isSubmitting: Boolean,
    onSaveLiveConfig: (
        bannerText: String,
        bannerEnabled: Boolean,
        maintenanceMode: Boolean,
        maintenanceMessage: String,
        featuredLabel: String,
        blockedIds: List<String>
    ) -> Unit,
    onBanAndRemove: (targetType: String, targetId: String, reportId: String?) -> Unit,
    onDismissReport: (reportId: String) -> Unit,
    onUnblockId: (targetId: String) -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler { onDismiss() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            var activeTab by remember { mutableIntStateOf(0) }

            var bannerText by remember(liveConfig.bannerText) { mutableStateOf(liveConfig.bannerText) }
            var bannerEnabled by remember(liveConfig.bannerEnabled) { mutableStateOf(liveConfig.bannerEnabled) }
            var maintenanceMode by remember(liveConfig.maintenanceMode) { mutableStateOf(liveConfig.maintenanceMode) }
            var maintenanceMessage by remember(liveConfig.maintenanceMessage) {
                mutableStateOf(
                    liveConfig.maintenanceMessage.ifBlank {
                        "Tube Store is being updated by the owner. Please check back shortly."
                    }
                )
            }
            var featuredLabel by remember(liveConfig.featuredLabel) { mutableStateOf(liveConfig.featuredLabel) }

            LaunchedEffect(liveConfig) {
                bannerText = liveConfig.bannerText
                bannerEnabled = liveConfig.bannerEnabled
                maintenanceMode = liveConfig.maintenanceMode
                if (liveConfig.maintenanceMessage.isNotBlank()) {
                    maintenanceMessage = liveConfig.maintenanceMessage
                }
                featuredLabel = liveConfig.featuredLabel
            }

            val openReportsCount = remember(reports) { reports.count { it.status == "open" } }

            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AdminPanelSettings,
                            contentDescription = "Owner Admin Control Center",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(30.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Owner Control Center",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Verified Owner: $OWNER_ADMIN_EMAIL",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("close_admin_control_button")
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close Owner Control Center")
                        }
                    }
                }

                // Live Stats Strip
                val gamesCount = remember(publicApps) { publicApps.count { it.category == "Games" } }
                val appsOnlyCount = (publicApps.size - gamesCount).coerceAtLeast(0)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AdminStatChip("Games", gamesCount.toString(), Modifier.weight(1f))
                    AdminStatChip("Apps", appsOnlyCount.toString(), Modifier.weight(1f))
                    AdminStatChip("Reports", openReportsCount.toString(), Modifier.weight(1f))
                    AdminStatChip("Blocked", liveConfig.blockedIds.size.toString(), Modifier.weight(1f))
                }

                TabRow(selectedTabIndex = activeTab) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = { Text("Live Config") }
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = { Text("Reports ($openReportsCount)") }
                    )
                    Tab(
                        selected = activeTab == 2,
                        onClick = { activeTab = 2 },
                        text = { Text("Moderate") }
                    )
                }

                when (activeTab) {
                    0 -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                    shape = MaterialTheme.shapes.large
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Rounded.Campaign,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Live Announcement & Spotlight",
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Show Live Announcement Banner",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Switch(
                                                checked = bannerEnabled,
                                                onCheckedChange = { bannerEnabled = it },
                                                modifier = Modifier.testTag("admin_banner_switch")
                                            )
                                        }
                                        OutlinedTextField(
                                            value = bannerText,
                                            onValueChange = { if (it.length <= 180) bannerText = it },
                                            label = { Text("Announcement Banner Message (max 180)") },
                                            placeholder = { Text("e.g. New Rajasthani Channels & Free Games live now!") },
                                            minLines = 2,
                                            maxLines = 3,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("admin_banner_input")
                                        )
                                        OutlinedTextField(
                                            value = featuredLabel,
                                            onValueChange = { if (it.length <= 60) featuredLabel = it },
                                            label = { Text("Featured Spotlight Tag (max 60)") },
                                            placeholder = { Text("e.g. Editor's Choice • Free Arena") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }

                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                    shape = MaterialTheme.shapes.large
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Rounded.Build,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Maintenance & Public Access Guard",
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Maintenance Mode",
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                                Text(
                                                    text = "Pauses public browsing while owner stays signed in",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Switch(
                                                checked = maintenanceMode,
                                                onCheckedChange = { maintenanceMode = it },
                                                modifier = Modifier.testTag("admin_maintenance_switch")
                                            )
                                        }
                                        OutlinedTextField(
                                            value = maintenanceMessage,
                                            onValueChange = { if (it.length <= 220) maintenanceMessage = it },
                                            label = { Text("Maintenance Screen Message") },
                                            minLines = 2,
                                            maxLines = 3,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }

                            item {
                                Button(
                                    enabled = !isSubmitting,
                                    onClick = {
                                        onSaveLiveConfig(
                                            bannerText,
                                            bannerEnabled,
                                            maintenanceMode,
                                            maintenanceMessage,
                                            featuredLabel,
                                            liveConfig.blockedIds
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("admin_save_live_config_button")
                                ) {
                                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(if (isSubmitting) "Pushing Update..." else "Save & Push Live Update")
                                }
                            }

                            if (liveConfig.blockedIds.isNotEmpty()) {
                                item {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                                        shape = MaterialTheme.shapes.large
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = "Blocked Content IDs (${liveConfig.blockedIds.size}) — Tap to Unblock",
                                                style = MaterialTheme.typography.titleSmall
                                            )
                                            Row(
                                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                liveConfig.blockedIds.forEach { blockedId ->
                                                    FilterChip(
                                                        selected = true,
                                                        onClick = { onUnblockId(blockedId) },
                                                        leadingIcon = {
                                                            Icon(Icons.Rounded.Block, contentDescription = null, modifier = Modifier.size(16.dp))
                                                        },
                                                        label = { Text("$blockedId • Unblock") }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        if (reports.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Rounded.GppGood,
                                    contentDescription = "No reports",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("No Copy Reports Pending", style = MaterialTheme.typography.titleLarge)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "When users report a copied app, video, or channel, it appears here for 1-tap takedown.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(reports, key = { it.id }) { report ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                        shape = MaterialTheme.shapes.large,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(14.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        Icons.Rounded.ReportProblem,
                                                        contentDescription = null,
                                                        tint = if (report.status == "open") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = report.targetTitle,
                                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                                    )
                                                }
                                                Surface(
                                                    color = if (report.status == "open") {
                                                        MaterialTheme.colorScheme.errorContainer
                                                    } else {
                                                        MaterialTheme.colorScheme.secondaryContainer
                                                    },
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text(
                                                        text = "${report.targetType.uppercase()} • ${report.status.uppercase()}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = report.reason,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Text(
                                                text = "Reported by: ${report.reporterName} • ID: ${report.targetId}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (report.status == "open") {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Button(
                                                        enabled = !isSubmitting,
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = MaterialTheme.colorScheme.error,
                                                            contentColor = MaterialTheme.colorScheme.onError
                                                        ),
                                                        onClick = {
                                                            onBanAndRemove(report.targetType, report.targetId, report.id)
                                                        },
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Icon(Icons.Rounded.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Ban & Remove")
                                                    }
                                                    OutlinedButton(
                                                        enabled = !isSubmitting,
                                                        onClick = { onDismissReport(report.id) },
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Text("Dismiss")
                                                    }
                                                }
                                            } else if (report.resolutionNote.isNotBlank()) {
                                                Text(
                                                    text = "Resolution: ${report.resolutionNote}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            item {
                                Text(
                                    text = "Public Apps & Games (${publicApps.size})",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            items(publicApps, key = { "adm_app_${it.id}" }) { app ->
                                AdminModerationRow(
                                    icon = Icons.Rounded.Apps,
                                    title = app.title,
                                    subtitle = "${app.creatorName} • ${app.category} • ${app.versionName}",
                                    isSubmitting = isSubmitting,
                                    onBanClick = { onBanAndRemove("app", app.id, null) }
                                )
                            }
                            if (publicVideos.isNotEmpty()) {
                                item {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Legacy Cloud Items (${publicVideos.size})",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                                items(publicVideos, key = { "adm_vid_${it.id}" }) { vid ->
                                    AdminModerationRow(
                                        icon = Icons.Rounded.PlayCircle,
                                        title = vid.title,
                                        subtitle = "${vid.channelName} • ${vid.category}",
                                        isSubmitting = isSubmitting,
                                        onBanClick = { onBanAndRemove("video", vid.id, null) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminStatChip(
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
private fun AdminModerationRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    isSubmitting: Boolean,
    onBanClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            OutlinedButton(
                enabled = !isSubmitting,
                onClick = onBanClick,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Rounded.Block, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Ban", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
