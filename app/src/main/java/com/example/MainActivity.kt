package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ai.configureAppCheck
import com.example.auth.signOutUser
import com.example.data.LauncherThemeSettings
import com.example.data.PreferencesManager
import com.example.data.TubeStoreRepository
import com.example.downloads.DownloadStartResult
import com.example.downloads.EXTRA_LAUNCH_APP_TITLE
import com.example.downloads.EXTRA_LAUNCH_APP_URL
import com.example.downloads.enqueueFileDownload
import com.example.downloads.isDirectDownloadUrl
import com.example.model.StoreApp
import com.example.ui.TubeStoreViewModel
import com.example.ui.UiState
import com.example.ui.screens.AdminControlDialog
import com.example.ui.screens.AiMusicStudioDialog
import com.example.ui.screens.DownloadsDialog
import com.example.ui.screens.EducationNasaDialog
import com.example.ui.screens.GuestBrowseShell
import com.example.ui.screens.InstantAppRunnerDialog
import com.example.ui.screens.PlayConsoleScreen
import com.example.ui.screens.PrivateBrowserScreen
import com.example.ui.screens.StoreScreen
import com.example.ui.screens.StoreViewMode
import com.example.ui.screens.instantAppFromUrl
import com.example.ui.theme.TubeStoreTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

private const val ROUTE_GAMES = "games"
private const val ROUTE_APPS = "apps"
private const val ROUTE_BROWSER = "browser"
private const val ROUTE_CONSOLE = "console"

private data class PlayNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val playNavItems = listOf(
    PlayNavItem(ROUTE_GAMES, "Games", Icons.Rounded.SportsEsports, Icons.Outlined.SportsEsports),
    PlayNavItem(ROUTE_APPS, "Apps", Icons.Rounded.Apps, Icons.Outlined.Apps),
    PlayNavItem(ROUTE_BROWSER, "Browser", Icons.Rounded.Language, Icons.Outlined.Language),
    PlayNavItem(ROUTE_CONSOLE, "Console", Icons.Rounded.Person, Icons.Outlined.Person)
)

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

class MainActivity : ComponentActivity() {
    private var pendingShortcutApp by mutableStateOf<StoreApp?>(null)

    private fun extractShortcutApp(intent: Intent?) {
        val url = intent?.getStringExtra(EXTRA_LAUNCH_APP_URL)
        val title = intent?.getStringExtra(EXTRA_LAUNCH_APP_TITLE)
        if (!url.isNullOrBlank()) {
            pendingShortcutApp = instantAppFromUrl(title ?: "Play Hub App", url)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        extractShortcutApp(intent)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val prefsManager = remember(context) { PreferencesManager(context.applicationContext) }
            val themeSettings by prefsManager.themeSettings.collectAsStateWithLifecycle(
                initialValue = LauncherThemeSettings()
            )

            TubeStoreTheme(
                preset = themeSettings.themePreset,
                darkTheme = themeSettings.darkMode
            ) {
                TubeStoreRoot(
                    themeSettings = themeSettings,
                    prefsManager = prefsManager,
                    initialShortcutApp = pendingShortcutApp,
                    onConsumeShortcutApp = { pendingShortcutApp = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractShortcutApp(intent)
    }
}

@Composable
private fun TubeStoreRoot(
    themeSettings: LauncherThemeSettings,
    prefsManager: PreferencesManager,
    initialShortcutApp: StoreApp?,
    onConsumeShortcutApp: () -> Unit,
    auth: FirebaseAuth = Firebase.auth
) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        GuestBrowseShell(
            themeSettings = themeSettings,
            prefsManager = prefsManager,
            initialShortcutApp = initialShortcutApp,
            onConsumeShortcutApp = onConsumeShortcutApp
        )
    } else {
        key(user.uid) {
            TubeStoreMainShell(
                currentUserId = user.uid,
                defaultDisplayName = user.displayName ?: "Developer",
                userEmail = user.email,
                isEmailVerified = user.isEmailVerified,
                themeSettings = themeSettings,
                prefsManager = prefsManager,
                initialShortcutApp = initialShortcutApp,
                onConsumeShortcutApp = onConsumeShortcutApp
            )
        }
    }
}

@Composable
private fun TubeStoreMainShell(
    currentUserId: String,
    defaultDisplayName: String,
    userEmail: String?,
    isEmailVerified: Boolean,
    themeSettings: LauncherThemeSettings,
    prefsManager: PreferencesManager,
    initialShortcutApp: StoreApp?,
    onConsumeShortcutApp: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: TubeStoreViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                TubeStoreViewModel(
                    repository = TubeStoreRepository(db),
                    currentUserId = currentUserId,
                    defaultDisplayName = defaultDisplayName,
                    userEmail = userEmail,
                    isEmailVerified = isEmailVerified
                )
            }
        }
    )

    val appsState by viewModel.publicApps.collectAsStateWithLifecycle()
    val myAppsState by viewModel.myApps.collectAsStateWithLifecycle()
    val videosState by viewModel.publicVideos.collectAsStateWithLifecycle()
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
    val filterState by viewModel.filterState.collectAsStateWithLifecycle()
    val liveConfig by viewModel.liveConfig.collectAsStateWithLifecycle()
    val moderationReports by viewModel.moderationReports.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_GAMES

    var browserRequestedUrl by remember { mutableStateOf<String?>(null) }
    var activeInstantApp by remember { mutableStateOf<StoreApp?>(null) }
    var showStudyNasa by remember { mutableStateOf(false) }
    var showAiMusicDialog by remember { mutableStateOf(false) }
    var showDownloadsDialog by remember { mutableStateOf(false) }
    var downloadTargetApp by remember { mutableStateOf<StoreApp?>(null) }
    var showAdminControl by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(initialShortcutApp) {
        if (initialShortcutApp != null) {
            activeInstantApp = initialShortcutApp
            onConsumeShortcutApp()
        }
    }

    LaunchedEffect(filterState.statusMessage) {
        val msg = filterState.statusMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    fun openDownloadFlow(app: StoreApp?) {
        if (app != null && isDirectDownloadUrl(app.downloadUrl)) {
            when (val res = enqueueFileDownload(context, app.downloadUrl, app.title)) {
                is DownloadStartResult.Started -> {
                    scope.launch { snackbarHostState.showSnackbar("Downloading ${res.fileName}...") }
                }
                is DownloadStartResult.Failed -> {
                    scope.launch { snackbarHostState.showSnackbar(res.message) }
                }
            }
        }
        downloadTargetApp = app
        showDownloadsDialog = true
    }

    fun openUrlInPrivateBrowser(url: String) {
        browserRequestedUrl = url
        navController.navigate(ROUTE_BROWSER) {
            popUpTo(ROUTE_GAMES) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    if (liveConfig.maintenanceMode && !viewModel.isOwnerAdmin) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Build,
                    contentDescription = "Maintenance Mode",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Play Hub Maintenance",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = liveConfig.maintenanceMessage.ifBlank {
                        "Play Hub is being updated live by the owner. Please check back shortly."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
                OutlinedButton(
                    onClick = {
                        signOutUser(
                            credentialManager = credentialManager,
                            scope = scope
                        )
                    }
                ) {
                    Text("Sign Out")
                }
            }
        }
        return
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!isWideScreen) {
                    NavigationBar {
                        playNavItems.forEach { item ->
                            val selected = currentRoute == item.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(item.route) {
                                        popUpTo(ROUTE_GAMES) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label
                                    )
                                },
                                label = { Text(item.label) },
                                modifier = Modifier.testTag("nav_${item.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isWideScreen) {
                    NavigationRail {
                        playNavItems.forEach { item ->
                            val selected = currentRoute == item.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(item.route) {
                                        popUpTo(ROUTE_GAMES) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label
                                    )
                                },
                                label = { Text(item.label) }
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    if (liveConfig.bannerEnabled && liveConfig.bannerText.isNotBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("live_announcement_banner")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Campaign,
                                    contentDescription = "Live Announcement",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = liveConfig.bannerText,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                if (viewModel.isOwnerAdmin) {
                                    TextButton(onClick = { showAdminControl = true }) {
                                        Icon(Icons.Rounded.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Edit")
                                    }
                                }
                            }
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = ROUTE_GAMES,
                        modifier = Modifier.weight(1f)
                    ) {
                        composable(ROUTE_GAMES) {
                            StoreScreen(
                                storeMode = StoreViewMode.GAMES,
                                appsState = appsState,
                                filterState = filterState,
                                bookmarks = bookmarks,
                                defaultCreatorName = profile?.studioName?.ifBlank { defaultDisplayName }
                                    ?: defaultDisplayName,
                                onSearchChange = viewModel::updateAppSearch,
                                onCategorySelect = viewModel::selectAppCategory,
                                onOpenInBrowser = ::openUrlInPrivateBrowser,
                                onRunInstant = { app ->
                                    viewModel.recordAppDownload(app.id)
                                    activeInstantApp = app
                                },
                                onOpenStudyNasa = { showStudyNasa = true },
                                onOpenAiMusic = { showAiMusicDialog = true },
                                onDownloadApp = { openDownloadFlow(it) },
                                onOpenDownloads = { openDownloadFlow(null) },
                                onRecordDownload = viewModel::recordAppDownload,
                                onToggleLike = { id, liked -> viewModel.toggleLike(id, "app", liked) },
                                onPublishApp = { creator, title, tagline, desc, cat, url, ver, vis, accent, onDone ->
                                    viewModel.publishApp(
                                        creatorName = creator,
                                        title = title,
                                        tagline = tagline,
                                        description = desc,
                                        category = cat,
                                        downloadUrl = url,
                                        versionName = ver,
                                        visibility = vis,
                                        accentIndex = accent,
                                        onSuccess = onDone
                                    )
                                },
                                onSeedStarterCatalog = {
                                    viewModel.seedStarterCatalog(profile?.studioName ?: defaultDisplayName)
                                },
                                featuredLabel = liveConfig.featuredLabel,
                                onReportCopy = { id, title, reason ->
                                    viewModel.submitCopyReport(
                                        targetId = id,
                                        targetType = "app",
                                        targetTitle = title,
                                        reason = reason
                                    )
                                }
                            )
                        }

                        composable(ROUTE_APPS) {
                            StoreScreen(
                                storeMode = StoreViewMode.APPS,
                                appsState = appsState,
                                filterState = filterState,
                                bookmarks = bookmarks,
                                defaultCreatorName = profile?.studioName?.ifBlank { defaultDisplayName }
                                    ?: defaultDisplayName,
                                onSearchChange = viewModel::updateAppSearch,
                                onCategorySelect = viewModel::selectAppCategory,
                                onOpenInBrowser = ::openUrlInPrivateBrowser,
                                onRunInstant = { app ->
                                    viewModel.recordAppDownload(app.id)
                                    activeInstantApp = app
                                },
                                onOpenStudyNasa = { showStudyNasa = true },
                                onOpenAiMusic = { showAiMusicDialog = true },
                                onDownloadApp = { openDownloadFlow(it) },
                                onOpenDownloads = { openDownloadFlow(null) },
                                onRecordDownload = viewModel::recordAppDownload,
                                onToggleLike = { id, liked -> viewModel.toggleLike(id, "app", liked) },
                                onPublishApp = { creator, title, tagline, desc, cat, url, ver, vis, accent, onDone ->
                                    viewModel.publishApp(
                                        creatorName = creator,
                                        title = title,
                                        tagline = tagline,
                                        description = desc,
                                        category = cat,
                                        downloadUrl = url,
                                        versionName = ver,
                                        visibility = vis,
                                        accentIndex = accent,
                                        onSuccess = onDone
                                    )
                                },
                                onSeedStarterCatalog = {
                                    viewModel.seedStarterCatalog(profile?.studioName ?: defaultDisplayName)
                                },
                                featuredLabel = liveConfig.featuredLabel,
                                onReportCopy = { id, title, reason ->
                                    viewModel.submitCopyReport(
                                        targetId = id,
                                        targetType = "app",
                                        targetTitle = title,
                                        reason = reason
                                    )
                                }
                            )
                        }

                        composable(ROUTE_BROWSER) {
                            PrivateBrowserScreen(
                                requestedUrl = browserRequestedUrl,
                                onConsumeRequestedUrl = { browserRequestedUrl = null },
                                onShowMessage = { msg ->
                                    scope.launch { snackbarHostState.showSnackbar(msg) }
                                },
                                onOpenDownloads = { openDownloadFlow(null) }
                            )
                        }

                        composable(ROUTE_CONSOLE) {
                            PlayConsoleScreen(
                                isSignedIn = true,
                                profile = profile,
                                appsState = myAppsState,
                                themeSettings = themeSettings,
                                isSubmitting = filterState.isSubmitting,
                                isSigningIn = false,
                                isOwnerAdmin = viewModel.isOwnerAdmin,
                                openReportsCount = moderationReports.count { it.status == "open" },
                                onOpenAdminControl = { showAdminControl = true },
                                onSignOutClick = {
                                    signOutUser(
                                        credentialManager = credentialManager,
                                        scope = scope
                                    )
                                },
                                onUpdateProfile = viewModel::updateProfile,
                                onPublishApp = { creator, title, tagline, desc, cat, url, ver, vis, accent, onDone ->
                                    viewModel.publishApp(
                                        creatorName = creator,
                                        title = title,
                                        tagline = tagline,
                                        description = desc,
                                        category = cat,
                                        downloadUrl = url,
                                        versionName = ver,
                                        visibility = vis,
                                        accentIndex = accent,
                                        onSuccess = onDone
                                    )
                                },
                                onUpdateApp = { appId, creator, title, tagline, desc, cat, url, ver, vis, onDone ->
                                    viewModel.updateMyApp(
                                        appId = appId,
                                        creatorName = creator,
                                        title = title,
                                        tagline = tagline,
                                        description = desc,
                                        category = cat,
                                        downloadUrl = url,
                                        versionName = ver,
                                        visibility = vis,
                                        onSuccess = onDone
                                    )
                                },
                                onDeleteApp = viewModel::deleteMyApp,
                                onRunApp = { app ->
                                    viewModel.recordAppDownload(app.id)
                                    activeInstantApp = app
                                },
                                onOpenDownloads = { openDownloadFlow(null) },
                                onSelectThemePreset = { preset ->
                                    scope.launch { prefsManager.setThemePreset(preset) }
                                },
                                onToggleDarkMode = { enabled ->
                                    scope.launch { prefsManager.setDarkMode(enabled) }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    activeInstantApp?.let { app ->
        InstantAppRunnerDialog(
            app = app,
            onDismiss = { activeInstantApp = null },
            onOpenInPrivateBrowser = ::openUrlInPrivateBrowser,
            onDownloadApp = { openDownloadFlow(it) },
            onShowMessage = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } }
        )
    }

    if (showDownloadsDialog) {
        DownloadsDialog(
            targetApp = downloadTargetApp,
            onOpenInBrowser = ::openUrlInPrivateBrowser,
            onShowMessage = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } },
            onDismiss = {
                showDownloadsDialog = false
                downloadTargetApp = null
            }
        )
    }

    if (showStudyNasa) {
        EducationNasaDialog(
            onDismiss = { showStudyNasa = false },
            onRunInstantUrl = { title, url ->
                activeInstantApp = instantAppFromUrl(title, url)
            }
        )
    }

    if (showAiMusicDialog) {
        AiMusicStudioDialog(
            onDismiss = { showAiMusicDialog = false },
            onOpenSongMaker = {
                activeInstantApp = instantAppFromUrl(
                    "Song Maker",
                    "https://musiclab.chromeexperiments.com/Song-Maker/"
                )
            },
            onOpenAudioMass = {
                activeInstantApp = instantAppFromUrl(
                    "AudioMass Studio",
                    "https://audiomass.co/"
                )
            }
        )
    }

    if (showAdminControl && viewModel.isOwnerAdmin) {
        val currentPublicApps = (appsState as? UiState.Success)?.data ?: emptyList()
        val currentPublicVideos = (videosState as? UiState.Success)?.data ?: emptyList()
        AdminControlDialog(
            liveConfig = liveConfig,
            reports = moderationReports,
            publicApps = currentPublicApps,
            publicVideos = currentPublicVideos,
            isSubmitting = filterState.isSubmitting,
            onSaveLiveConfig = viewModel::saveLiveAppConfig,
            onBanAndRemove = { type, id, reportId ->
                viewModel.banAndRemoveTarget(type, id, reportId)
            },
            onDismissReport = { reportId ->
                viewModel.resolveModerationReport(reportId, "dismissed", "Dismissed by owner")
            },
            onUnblockId = viewModel::unblockTargetId,
            onDismiss = { showAdminControl = false }
        )
    }
}
