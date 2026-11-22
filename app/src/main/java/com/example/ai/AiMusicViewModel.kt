package com.example.ai

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

private const val TAG = "AiMusicViewModel"

data class GeneratedSongPlan(
    val title: String,
    val language: String,
    val genre: String,
    val bpm: Int,
    val keyScale: String,
    val instruments: String,
    val chords: List<String>,
    val lyrics: String
)

sealed interface AiMusicUiState {
    data object Idle : AiMusicUiState
    data object Loading : AiMusicUiState
    data class Success(val song: GeneratedSongPlan) : AiMusicUiState
    data class Error(val message: String) : AiMusicUiState
}

class AiMusicViewModel : ViewModel() {

    private val songSchema = Schema.obj(
        properties = mapOf(
            "title" to Schema.string(description = "Original song title"),
            "bpm" to Schema.integer(description = "Recommended tempo in BPM between 65 and 160"),
            "keyScale" to Schema.string(description = "Musical scale or raga, e.g. C Major or Raga Mand"),
            "instruments" to Schema.string(description = "Recommended instruments list"),
            "chords" to Schema.array(
                items = Schema.string(),
                description = "4 chord progression symbols such as C, G, Am, F"
            ),
            "lyrics" to Schema.string(description = "Complete song lyrics with Verse 1, Chorus, Verse 2, and Outro")
        )
    )

    private val config = generationConfig {
        responseMimeType = "application/json"
        responseSchema = songSchema
    }

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = config
        )
    }

    private val _uiState = MutableStateFlow<AiMusicUiState>(AiMusicUiState.Idle)
    val uiState: StateFlow<AiMusicUiState> = _uiState.asStateFlow()

    private val _isPlayingBeat = MutableStateFlow(false)
    val isPlayingBeat: StateFlow<Boolean> = _isPlayingBeat.asStateFlow()

    private var synthJob: Job? = null

    fun composeSong(language: String, genre: String, topic: String) {
        stopBeatPreview()
        viewModelScope.launch {
            _uiState.value = AiMusicUiState.Loading
            try {
                val cleanTopic = topic.trim().ifBlank { "Celebration, culture, and rhythm" }
                val prompt = """
                    Compose an original, authentic $genre song in the $language language about: "$cleanTopic".
                    Write authentic, singable lyrics with [Verse 1], [Chorus], [Verse 2], and [Outro] sections in $language (if Marwadi / Rajasthani, use genuine Marwadi phrases and folk imagery).
                    Provide an accurate BPM, musical scale/raga, instrument arrangement, and 4-chord progression.
                """.trimIndent()

                val response = withTimeout(60_000L) {
                    model.generateContent(prompt)
                }
                val rawJson = response.text ?: throw IllegalStateException("AI returned an empty response.")
                val json = JSONObject(rawJson)
                val chordsArray = json.optJSONArray("chords")
                val chordsList = if (chordsArray != null) {
                    (0 until chordsArray.length()).mapNotNull { idx ->
                        chordsArray.optString(idx).takeIf { it.isNotBlank() }
                    }
                } else {
                    emptyList()
                }.ifEmpty { listOf("C", "G", "Am", "F") }

                val song = GeneratedSongPlan(
                    title = json.optString("title", "$language $genre Track"),
                    language = language,
                    genre = genre,
                    bpm = json.optInt("bpm", 104).coerceIn(60, 170),
                    keyScale = json.optString("keyScale", "C Major / Folk"),
                    instruments = json.optString("instruments", "Dholak, Harmonium, Synth Pad"),
                    chords = chordsList,
                    lyrics = json.optString("lyrics", "")
                )

                if (song.lyrics.isBlank()) {
                    _uiState.value = AiMusicUiState.Error("Song lyrics were empty. Please try again.")
                } else {
                    _uiState.value = AiMusicUiState.Success(song)
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI Music generation timed out", e)
                _uiState.value = AiMusicUiState.Error("AI composition timed out after 60s. Please try again.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI Music generation failed", e)
                _uiState.value = AiMusicUiState.Error(
                    e.localizedMessage ?: "Failed to compose AI music. Check connection."
                )
            }
        }
    }

    fun toggleBeatPreview(song: GeneratedSongPlan) {
        if (_isPlayingBeat.value) {
            stopBeatPreview()
            return
        }
        _isPlayingBeat.value = true
        synthJob = viewModelScope.launch(Dispatchers.Default) {
            val sampleRate = 22050
            val beatSamples = ((60.0 / song.bpm.coerceIn(60, 170)) * sampleRate).toInt().coerceAtLeast(4000)
            val bufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(beatSamples * 2)

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            try {
                track.play()
                val pcm = ShortArray(beatSamples)
                var step = 0
                while (isActive) {
                    val chordSymbol = song.chords[step % song.chords.size]
                    val rootFreq = chordRootFrequency(chordSymbol)
                    val thirdFreq = rootFreq * if (chordSymbol.contains("m", ignoreCase = false)) 1.1892 else 1.2599
                    val fifthFreq = rootFreq * 1.4983

                    for (i in 0 until beatSamples) {
                        val t = i.toDouble() / sampleRate
                        val chordEnv = exp(-3.5 * t)
                        val kickEnv = if (step % 2 == 0) exp(-22.0 * t) else 0.0
                        val kickFreq = 58.0 + 90.0 * exp(-35.0 * t)
                        val chordWave = (sin(2.0 * PI * rootFreq * t) +
                            0.7 * sin(2.0 * PI * thirdFreq * t) +
                            0.6 * sin(2.0 * PI * fifthFreq * t)) / 2.3
                        val kickWave = sin(2.0 * PI * kickFreq * t) * kickEnv
                        val sample = (chordWave * chordEnv * 0.48 + kickWave * 0.45).coerceIn(-0.98, 0.98)
                        pcm[i] = (sample * Short.MAX_VALUE).toInt().toShort()
                    }
                    track.write(pcm, 0, pcm.size)
                    step++
                }
            } catch (e: Exception) {
                Log.w(TAG, "Audio synth stopped: ${e.message}")
            } finally {
                try {
                    track.stop()
                    track.release()
                } catch (_: Exception) {
                }
                _isPlayingBeat.value = false
            }
        }
    }

    fun stopBeatPreview() {
        synthJob?.cancel()
        synthJob = null
        _isPlayingBeat.value = false
    }

    override fun onCleared() {
        stopBeatPreview()
        super.onCleared()
    }

    private fun chordRootFrequency(symbol: String): Double {
        val clean = symbol.trim().uppercase()
        return when {
            clean.startsWith("C#") || clean.startsWith("DB") -> 277.18
            clean.startsWith("C") -> 261.63
            clean.startsWith("D#") || clean.startsWith("EB") -> 311.13
            clean.startsWith("D") -> 293.66
            clean.startsWith("E") -> 329.63
            clean.startsWith("F#") || clean.startsWith("GB") -> 369.99
            clean.startsWith("F") -> 349.23
            clean.startsWith("G#") || clean.startsWith("AB") -> 415.30
            clean.startsWith("G") -> 392.00
            clean.startsWith("A#") || clean.startsWith("BB") -> 466.16
            clean.startsWith("A") -> 220.00
            clean.startsWith("B") -> 246.94
            else -> 261.63
        }
    }
}
