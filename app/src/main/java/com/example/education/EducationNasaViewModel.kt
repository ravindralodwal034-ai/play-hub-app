package com.example.education

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ui.UiState
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

private const val TAG = "EducationNasaVM"

data class EducationProgramItem(
    val id: String,
    val title: String,
    val level: String,
    val subtitle: String,
    val url: String
)

class EducationNasaViewModel(
    private val repository: EducationNasaRepository = EducationNasaRepository()
) : ViewModel() {

    private val aiModel by lazy {
        Firebase.ai.generativeModel("gemini-3.8-flash")
    }

    val studyPrograms: List<EducationProgramItem> = listOf(
        EducationProgramItem(
            id = "edu_diksha",
            title = "DIKSHA NCERT School Classes",
            level = "School (1–12)",
            subtitle = "Official Indian Govt school textbooks, video lessons & quizzes",
            url = "https://diksha.gov.in"
        ),
        EducationProgramItem(
            id = "edu_khan_kids",
            title = "Khan Academy Kids & School",
            level = "Kids (KG–5)",
            subtitle = "Interactive math, reading, science & puzzles completely free",
            url = "https://www.khanacademy.org"
        ),
        EducationProgramItem(
            id = "edu_scratch",
            title = "MIT Scratch Coding Lab",
            level = "Kids (KG–5)",
            subtitle = "Build animations, games & logic blocks directly in-app",
            url = "https://scratch.mit.edu"
        ),
        EducationProgramItem(
            id = "edu_phet",
            title = "PhET Interactive Science Labs",
            level = "School (6–12)",
            subtitle = "Live Physics, Chemistry & Math virtual experiments by UC Boulder",
            url = "https://phet.colorado.edu"
        ),
        EducationProgramItem(
            id = "edu_nptel",
            title = "NPTEL IIT College Classes",
            level = "College & Tech",
            subtitle = "Free IIT/IISc engineering, science & computer degree lectures",
            url = "https://nptel.ac.in"
        ),
        EducationProgramItem(
            id = "edu_swayam",
            title = "SWAYAM University Portal",
            level = "College & Tech",
            subtitle = "Free Indian university courses from Class 9 to Post-Graduation",
            url = "https://swayam.gov.in"
        ),
        EducationProgramItem(
            id = "edu_mit_ocw",
            title = "MIT OpenCourseWare",
            level = "College & Tech",
            subtitle = "Full MIT undergraduate & graduate science and engineering courses",
            url = "https://ocw.mit.edu"
        ),
        EducationProgramItem(
            id = "edu_freecodecamp",
            title = "freeCodeCamp Coding Academy",
            level = "College & Tech",
            subtitle = "Free hands-on web, Python, data science & certification classes",
            url = "https://www.freecodecamp.org"
        ),
        EducationProgramItem(
            id = "nasa_eyes",
            title = "NASA Eyes 3D Solar System",
            level = "NASA & Space",
            subtitle = "Real-time 3D explorer of planets, satellites & active NASA spacecraft",
            url = "https://eyes.nasa.gov"
        ),
        EducationProgramItem(
            id = "nasa_plus",
            title = "NASA+ Official Space Hub",
            level = "NASA & Space",
            subtitle = "Official NASA mission launches, rocket documentaries & live feeds",
            url = "https://plus.nasa.gov"
        ),
        EducationProgramItem(
            id = "isro_official",
            title = "ISRO Space Missions Portal",
            level = "NASA & Space",
            subtitle = "Chandrayaan, Aditya-L1, Gaganyaan & official Indian rocket updates",
            url = "https://www.isro.gov.in"
        )
    )

    val featuredScientists: List<String> = listOf(
        "Kalpana Chawla",
        "A. P. J. Abdul Kalam",
        "Katherine Johnson",
        "Vikram Sarabhai",
        "Sunita Williams",
        "Neil Armstrong",
        "C. V. Raman",
        "Albert Einstein",
        "Marie Curie",
        "Srinivasa Ramanujan"
    )

    private val _launchesState = MutableStateFlow<UiState<List<SpaceLaunchItem>>>(UiState.Loading)
    val launchesState: StateFlow<UiState<List<SpaceLaunchItem>>> = _launchesState.asStateFlow()

    private val _apodState = MutableStateFlow<UiState<NasaApodItem>>(UiState.Loading)
    val apodState: StateFlow<UiState<NasaApodItem>> = _apodState.asStateFlow()

    private val _scientistState = MutableStateFlow<UiState<ScientistSummary>>(UiState.Loading)
    val scientistState: StateFlow<UiState<ScientistSummary>> = _scientistState.asStateFlow()

    private val _tutorAnswerState = MutableStateFlow<UiState<String>?>(null)
    val tutorAnswerState: StateFlow<UiState<String>?> = _tutorAnswerState.asStateFlow()

    init {
        refreshSpaceData()
        loadScientist("Kalpana Chawla")
    }

    fun refreshSpaceData() {
        viewModelScope.launch {
            _launchesState.value = UiState.Loading
            repository.fetchLaunches()
                .onSuccess { _launchesState.value = UiState.Success(it) }
                .onFailure { e ->
                    Log.e(TAG, "Failed to load launches", e)
                    _launchesState.value = UiState.Error(e.localizedMessage ?: "Unable to fetch live launches.")
                }
        }
        viewModelScope.launch {
            _apodState.value = UiState.Loading
            repository.fetchNasaApod()
                .onSuccess { _apodState.value = UiState.Success(it) }
                .onFailure { e ->
                    Log.e(TAG, "Failed to load NASA APOD", e)
                    _apodState.value = UiState.Error(e.localizedMessage ?: "Unable to load NASA daily briefing.")
                }
        }
    }

    fun loadScientist(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            _scientistState.value = UiState.Loading
            repository.fetchScientistInfo(name)
                .onSuccess { _scientistState.value = UiState.Success(it) }
                .onFailure { e ->
                    Log.e(TAG, "Failed to load scientist summary", e)
                    _scientistState.value = UiState.Error(e.localizedMessage ?: "Scientist profile not found.")
                }
        }
    }

    fun askAiTutor(level: String, subject: String, question: String) {
        val cleanQ = question.trim()
        if (cleanQ.isBlank()) return
        viewModelScope.launch {
            _tutorAnswerState.value = UiState.Loading
            try {
                val prompt = """
                    You are an encouraging, crystal-clear teacher & NASA space science mentor helping a student at level: $level (Subject: $subject).
                    Answer their question clearly with real facts, key formulas or dates if relevant, what happened/succeeded, and a quick summary tip for exams or learning:
                    "$cleanQ"
                """.trimIndent()

                val response = withTimeout(60_000L) {
                    aiModel.generateContent(prompt)
                }
                val text = response.text
                if (!text.isNullOrBlank()) {
                    _tutorAnswerState.value = UiState.Success(text)
                } else {
                    _tutorAnswerState.value = UiState.Error("AI tutor returned an empty response.")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI Tutor timed out", e)
                _tutorAnswerState.value = UiState.Error("Request timed out after 60s. Please try again.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI Tutor failed", e)
                _tutorAnswerState.value = UiState.Error(e.localizedMessage ?: "Unable to reach AI Tutor.")
            }
        }
    }
}
