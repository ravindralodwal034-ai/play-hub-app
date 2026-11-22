package com.example.ui.screens

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
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
import com.example.auth.onGoogleSignInClicked
import com.example.data.LauncherThemeSettings
import com.example.data.PreferencesManager
import com.example.data.local.LocalGuestRepository
import com.example.data.local.TubeStoreDatabase
import com.example.downloads.DownloadStartResult
import com.example.downloads.enqueueFileDownload
import com.example.downloads.isDirectDownloadUrl
import com.example.model.StoreApp
import com.example.ui.LocalStudioViewModel
import com.example.ui.UiState
import kotlinx.coroutines.launch

private const val GUEST_ROUTE_GAMES = "games"
private const val GUEST_ROUTE_APPS = "apps"
private const val GUEST_ROUTE_BROWSER = "browser"
private const val GUEST_ROUTE_CONSOLE = "console"

private data class GuestNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val guestNavItems = listOf(
    GuestNavItem(GUEST_ROUTE_GAMES, "Games", Icons.Rounded.SportsEsports, Icons.Outlined.SportsEsports),
    GuestNavItem(GUEST_ROUTE_APPS, "Apps", Icons.Rounded.Apps, Icons.Outlined.Apps),
    GuestNavItem(GUEST_ROUTE_BROWSER, "Browser", Icons.Rounded.Language, Icons.Outlined.Language),
    GuestNavItem(GUEST_ROUTE_CONSOLE, "Console", Icons.Rounded.Person, Icons.Outlined.Person)
)

@Composable
fun GuestBrowseShell(
    themeSettings: LauncherThemeSettings,
    prefsManager: PreferencesManager,
    initialShortcutApp: StoreApp? = null,
    onConsumeShortcutApp: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val localViewModel: LocalStudioViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val dao = TubeStoreDatabase.getInstance(app).tubeStoreDao()
                LocalStudioViewModel(LocalGuestRepository(dao))
            }
        }
    )

    val combinedApps by localViewModel.combinedApps.collectAsStateWithLifecycle()
    val localApps by localViewModel.localApps.collectAsStateWithLifecycle()
    val bookmarks by localViewModel.bookmarks.collectAsStateWithLifecycle()
    val filterState by localViewModel.filterState.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: GUEST_ROUTE_GAMES

    var browserRequestedUrl by remember { mutableStateOf<String?>(null) }
    var activeInstantApp by remember { mutableStateOf<StoreApp?>(null) }
    var showStudyNasa by remember { mutableStateOf(false) }
    var showAiMusicDialog by remember { mutableStateOf(false) }
    var showDownloadsDialog by remember { mutableStateOf(false) }
    var downloadTargetApp by remember { mutableStateOf<StoreApp?>(null) }
    var isSigningIn by remember { mutableStateOf(false) }
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
            localViewModel.clearStatusMessage()
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
        navController.navigate(GUEST_ROUTE_BROWSER) {
            popUpTo(GUEST_ROUTE_GAMES) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!isWideScreen) {
                    NavigationBar {
                        guestNavItems.forEach { item ->
                            val selected = currentRoute == item.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(item.route) {
                                        popUpTo(GUEST_ROUTE_GAMES) { saveState = true }
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
                                modifier = Modifier.testTag("guest_nav_${item.route}")
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
                        guestNavItems.forEach { item ->
                            val selected = currentRoute == item.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(item.route) {
                                        popUpTo(GUEST_ROUTE_GAMES) { saveState = true }
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

                NavHost(
                    navController = navController,
                    startDestination = GUEST_ROUTE_GAMES,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(GUEST_ROUTE_GAMES) {
                        StoreScreen(
                            storeMode = StoreViewMode.GAMES,
                            appsState = UiState.Success(combinedApps),
                            filterState = filterState,
                            bookmarks = bookmarks,
                            defaultCreatorName = "Indie India Studio",
                            onSearchChange = localViewModel::updateAppSearch,
                            onCategorySelect = localViewModel::selectAppCategory,
                            onOpenInBrowser = ::openUrlInPrivateBrowser,
                            onRunInstant = { app ->
                                localViewModel.recordAppLaunch(app.id)
                                activeInstantApp = app
                            },
                            onOpenStudyNasa = { showStudyNasa = true },
                            onOpenAiMusic = { showAiMusicDialog = true },
                            onDownloadApp = { openDownloadFlow(it) },
                            onOpenDownloads = { openDownloadFlow(null) },
                            onRecordDownload = localViewModel::recordAppLaunch,
                            onToggleLike = { id, liked ->
                                localViewModel.toggleBookmark(id, "app", liked)
                            },
                            onPublishApp = { creator, title, tagline, desc, cat, url, ver, _, accent, onDone ->
                                localViewModel.saveApp(
                                    creatorName = creator,
                                    title = title,
                                    tagline = tagline,
                                    description = desc,
                                    category = cat,
                                    downloadUrl = url,
                                    versionName = ver,
                                    accentIndex = accent,
                                    onDone = onDone
                                )
                            },
                            onSeedStarterCatalog = {}
                        )
                    }

                    composable(GUEST_ROUTE_APPS) {
                        StoreScreen(
                            storeMode = StoreViewMode.APPS,
                            appsState = UiState.Success(combinedApps),
                            filterState = filterState,
                            bookmarks = bookmarks,
                            defaultCreatorName = "My Studio",
                            onSearchChange = localViewModel::updateAppSearch,
                            onCategorySelect = localViewModel::selectAppCategory,
                            onOpenInBrowser = ::openUrlInPrivateBrowser,
                            onRunInstant = { app ->
                                localViewModel.recordAppLaunch(app.id)
                                activeInstantApp = app
                            },
                            onOpenStudyNasa = { showStudyNasa = true },
                            onOpenAiMusic = { showAiMusicDialog = true },
                            onDownloadApp = { openDownloadFlow(it) },
                            onOpenDownloads = { openDownloadFlow(null) },
                            onRecordDownload = localViewModel::recordAppLaunch,
                            onToggleLike = { id, liked ->
                                localViewModel.toggleBookmark(id, "app", liked)
                            },
                            onPublishApp = { creator, title, tagline, desc, cat, url, ver, _, accent, onDone ->
                                localViewModel.saveApp(
                                    creatorName = creator,
                                    title = title,
                                    tagline = tagline,
                                    description = desc,
                                    category = cat,
                                    downloadUrl = url,
                                    versionName = ver,
                                    accentIndex = accent,
                                    onDone = onDone
                                )
                            },
                            onSeedStarterCatalog = {}
                        )
                    }

                    composable(GUEST_ROUTE_BROWSER) {
                        PrivateBrowserScreen(
                            requestedUrl = browserRequestedUrl,
                            onConsumeRequestedUrl = { browserRequestedUrl = null },
                            onShowMessage = { msg ->
                                scope.launch { snackbarHostState.showSnackbar(msg) }
                            },
                            onOpenDownloads = { openDownloadFlow(null) }
                        )
                    }

                    composable(GUEST_ROUTE_CONSOLE) {
                        PlayConsoleScreen(
                            isSignedIn = false,
                            profile = null,
                            appsState = UiState.Success(localApps.map { it.toStoreApp() }),
                            themeSettings = themeSettings,
                            isSubmitting = false,
                            isSigningIn = isSigningIn,
                            onSignInClick = {
                                isSigningIn = true
                                onGoogleSignInClicked(
                                    context = context,
                                    credentialManager = credentialManager,
                                    onAuthSuccess = { isSigningIn = false },
                                    onAuthError = { msg ->
                                        isSigningIn = false
                                        scope.launch { snackbarHostState.showSnackbar(msg) }
                                    },
                                    scope = scope,
                                    onAuthCancelled = { isSigningIn = false }
                                )
                            },
                            onPublishApp = { creator, title, tagline, desc, cat, url, ver, _, accent, onDone ->
                                localViewModel.saveApp(
                                    creatorName = creator,
                                    title = title,
                                    tagline = tagline,
                                    description = desc,
                                    category = cat,
                                    downloadUrl = url,
                                    versionName = ver,
                                    accentIndex = accent,
                                    onDone = onDone
                                )
                            },
                            onUpdateApp = { appId, creator, title, tagline, desc, cat, url, ver, _, onDone ->
                                localViewModel.saveApp(
                                    id = appId,
                                    creatorName = creator,
                                    title = title,
                                    tagline = tagline,
                                    description = desc,
                                    category = cat,
                                    downloadUrl = url,
                                    versionName = ver,
                                    accentIndex = localApps.firstOrNull { it.id == appId }?.accentIndex ?: 0L,
                                    onDone = onDone
                                )
                            },
                            onDeleteApp = localViewModel::deleteApp,
                            onRunApp = { app ->
                                localViewModel.recordAppLaunch(app.id)
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
}
