package com.example.ui.screens

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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.education.EducationNasaViewModel
import com.example.model.AppCategory
import com.example.model.StoreApp
import com.example.ui.UiState

@Composable
fun EducationNasaDialog(
    onDismiss: () -> Unit,
    onRunInstantUrl: (title: String, url: String) -> Unit,
    eduViewModel: EducationNasaViewModel = viewModel()
) {
    val launchesState by eduViewModel.launchesState.collectAsStateWithLifecycle()
    val apodState by eduViewModel.apodState.collectAsStateWithLifecycle()
    val scientistState by eduViewModel.scientistState.collectAsStateWithLifecycle()
    val tutorState by eduViewModel.tutorAnswerState.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedLevel by remember { mutableStateOf("All") }

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
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            Icons.Rounded.School,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Free Classes (Kids–College) & NASA",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Live Rockets, Scientists & AI Study Mentor",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { eduViewModel.refreshSpaceData() }) {
                            Icon(Icons.Rounded.Refresh, contentDescription = "Refresh Space Data")
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("close_edu_nasa_dialog")
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close")
                        }
                    }
                }

                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Classes") },
                        icon = { Icon(Icons.Rounded.School, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("NASA Live") },
                        icon = { Icon(Icons.Rounded.RocketLaunch, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Scientists") },
                        icon = { Icon(Icons.Rounded.Science, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                }

                when (selectedTab) {
                    0 -> ClassesAndTutorTab(
                        selectedLevel = selectedLevel,
                        onSelectLevel = { selectedLevel = it },
                        programs = eduViewModel.studyPrograms,
                        tutorState = tutorState,
                        onAskTutor = eduViewModel::askAiTutor,
                        onLaunchProgram = { title, url ->
                            onDismiss()
                            onRunInstantUrl(title, url)
                        }
                    )
                    1 -> NasaLaunchesTab(
                        launchesState = launchesState,
                        apodState = apodState,
                        onRunPortal = { title, url ->
                            onDismiss()
                            onRunInstantUrl(title, url)
                        }
                    )
                    2 -> ScientistsAndGuideTab(
                        featured = eduViewModel.featuredScientists,
                        scientistState = scientistState,
                        tutorState = tutorState,
                        onLoadScientist = eduViewModel::loadScientist,
                        onAskAiGuide = { topic ->
                            eduViewModel.askAiTutor("Space & Science", "Scientists & Launches", topic)
                        },
                        onOpenWiki = { title, url ->
                            onDismiss()
                            onRunInstantUrl(title, url)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ClassesAndTutorTab(
    selectedLevel: String,
    onSelectLevel: (String) -> Unit,
    programs: List<com.example.education.EducationProgramItem>,
    tutorState: UiState<String>?,
    onAskTutor: (String, String, String) -> Unit,
    onLaunchProgram: (String, String) -> Unit
) {
    var gradeLevel by remember { mutableStateOf("School (6–12)") }
    var subject by remember { mutableStateOf("Science & Math") }
    var question by remember { mutableStateOf("") }

    val levels = listOf("All", "Kids (KG–5)", "School (1–12)", "School (6–12)", "College & Tech", "NASA & Space")
    val filtered = remember(programs, selectedLevel) {
        if (selectedLevel == "All") programs else programs.filter { it.level == selectedLevel }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Free AI Study Tutor (Kids to College)",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Kids (KG–5)", "School (6–12)", "College / B.Tech", "UPSC / Science").forEach { lvl ->
                            FilterChip(selected = gradeLevel == lvl, onClick = { gradeLevel = lvl }, label = { Text(lvl) })
                        }
                    }
                    OutlinedTextField(
                        value = question,
                        onValueChange = { question = it },
                        label = { Text("Ask any study, math, coding, or NASA doubt...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        enabled = question.isNotBlank() && tutorState !is UiState.Loading,
                        onClick = { onAskTutor(gradeLevel, subject, question) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (tutorState is UiState.Loading) "Solving..." else "Get Free Explanation")
                    }
                    when (tutorState) {
                        is UiState.Loading -> CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        is UiState.Error -> Text(tutorState.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        is UiState.Success -> {
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = tutorState.data,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                        null -> {}
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                levels.forEach { lvl ->
                    FilterChip(selected = selectedLevel == lvl, onClick = { onSelectLevel(lvl) }, label = { Text(lvl) })
                }
            }
        }

        items(filtered, key = { it.id }) { item ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.level, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Text(item.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        Text(item.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onLaunchProgram(item.title, item.url) },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Rounded.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Open")
                    }
                }
            }
        }
    }
}

@Composable
private fun NasaLaunchesTab(
    launchesState: UiState<List<com.example.education.SpaceLaunchItem>>,
    apodState: UiState<com.example.education.NasaApodItem>,
    onRunPortal: (String, String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = { onRunPortal("NASA Eyes 3D", "https://eyes.nasa.gov") }) {
                    Text("NASA Eyes 3D")
                }
                Button(onClick = { onRunPortal("NASA+ Official", "https://plus.nasa.gov") }) {
                    Text("NASA+ Live")
                }
                Button(onClick = { onRunPortal("ISRO Missions", "https://www.isro.gov.in") }) {
                    Text("ISRO Portal")
                }
            }
        }

        item {
            when (apodState) {
                is UiState.Success -> {
                    val apod = apodState.data
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("NASA Daily Briefing • ${apod.date}", style = MaterialTheme.typography.labelMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(apod.title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = apod.explanation.take(260) + if (apod.explanation.length > 260) "..." else "",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                is UiState.Error -> {
                    Text(apodState.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
                is UiState.Loading -> {}
            }
        }

        item {
            Text("Live Upcoming & Recent Rocket Launches", style = MaterialTheme.typography.titleMedium)
        }

        when (launchesState) {
            is UiState.Loading -> {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
            is UiState.Error -> {
                item {
                    Text(launchesState.message, color = MaterialTheme.colorScheme.error)
                }
            }
            is UiState.Success -> {
                items(launchesState.data, key = { it.id }) { launch ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (launch.isUpcoming) "UPCOMING • ${launch.statusName}" else "COMPLETED • ${launch.statusName}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(launch.netDate, style = MaterialTheme.typography.labelSmall)
                            }
                            Text(launch.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            Text(
                                text = "${launch.provider} • ${launch.orbit} • ${launch.padLocation}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = launch.missionDescription,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScientistsAndGuideTab(
    featured: List<String>,
    scientistState: UiState<com.example.education.ScientistSummary>,
    tutorState: UiState<String>?,
    onLoadScientist: (String) -> Unit,
    onAskAiGuide: (String) -> Unit,
    onOpenWiki: (String, String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search any scientist or NASA mission...") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    enabled = searchQuery.isNotBlank(),
                    onClick = { onLoadScientist(searchQuery) }
                ) {
                    Text("Find")
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                featured.forEach { name ->
                    FilterChip(
                        selected = false,
                        onClick = { onLoadScientist(name) },
                        label = { Text(name) }
                    )
                }
            }
        }

        item {
            when (scientistState) {
                is UiState.Loading -> CircularProgressIndicator()
                is UiState.Error -> Text(scientistState.message, color = MaterialTheme.colorScheme.error)
                is UiState.Success -> {
                    val info = scientistState.data
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(info.title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                            Text(info.description, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            Text(info.extract, style = MaterialTheme.typography.bodyMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { onOpenWiki(info.title, info.pageUrl) }) {
                                    Text("Read Full Bio")
                                }
                                Button(
                                    onClick = {
                                        onAskAiGuide("Explain ${info.title}'s major launches, discoveries, experiments, and what succeeded or failed in simple terms for students.")
                                    }
                                ) {
                                    Text("AI Key Facts")
                                }
                            }
                        }
                    }
                }
            }
        }

        if (tutorState != null) {
            item {
                when (tutorState) {
                    is UiState.Loading -> Text("Loading scientist breakdown...")
                    is UiState.Error -> Text(tutorState.message, color = MaterialTheme.colorScheme.error)
                    is UiState.Success -> {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = tutorState.data,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

fun instantAppFromUrl(title: String, url: String): StoreApp = StoreApp(
    id = "instant_${title.hashCode()}",
    creatorId = "open_catalog",
    creatorName = "Tube Instant",
    title = title,
    tagline = "Instant No-Download Web Portal",
    description = url,
    category = AppCategory.EDUCATION.label,
    downloadUrl = url,
    versionName = "Live",
    visibility = "public"
)
