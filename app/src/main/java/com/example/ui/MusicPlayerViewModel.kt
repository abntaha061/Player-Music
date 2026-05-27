package com.example.ui

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.sin

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationSeconds: Int,
    val dominantColor: Color,
    val vibrantColor: Color,
    val chords: DoubleArray,
    val lyrics: List<LyricLine>
)

data class LyricLine(
    val timeMs: Long,
    val text: String
)

class MusicPlayerViewModel : ViewModel() {

    private val presetSongs = listOf(
        Song(
            id = "1",
            title = "Celestial Aurora",
            artist = "Luminance",
            album = "Solar Flares",
            durationSeconds = 180,
            dominantColor = Color(0xFFBD83FF),
            vibrantColor = Color(0xFF00ADB5),
            chords = doubleArrayOf(261.63, 329.63, 392.00, 493.88), // Cmaj7 (C4, E4, G4, B4)
            lyrics = listOf(
                LyricLine(0, "• Ambient Synthesized Prelude •"),
                LyricLine(4000, "Floating high above the clouds..."),
                LyricLine(10000, "Where the celestial starlight shines"),
                LyricLine(16000, "In a peaceful space of thoughts"),
                LyricLine(22000, "Look at the colors bending),"),
                LyricLine(28000, "Woven into a glowing aurora..."),
                LyricLine(35000, "Guiding us safely home."),
                LyricLine(42000, "No burden too heavy to carry"),
                LyricLine(48000, "In this quiet cosmic breeze."),
                LyricLine(55000, "• Smooth Shimmer Transition •"),
                LyricLine(66000, "The universe expands around us"),
                LyricLine(74000, "Quietly singing its ancient melody"),
                LyricLine(81000, "Every frequency in deep harmony..."),
                LyricLine(92000, "A canvas of electric fields"),
                LyricLine(100000, "Floating endlessly in the deep night"),
                LyricLine(112000, "• Gentle Fade Out •")
            )
        ),
        Song(
            id = "2",
            title = "Midnight Mirage",
            artist = "Vortex Field",
            album = "Event Horizon",
            durationSeconds = 210,
            dominantColor = Color(0xFF1E3C72),
            vibrantColor = Color(0xFFFF5858),
            chords = doubleArrayOf(293.66, 349.23, 440.00, 523.25), // Dm7 (D4, F4, A4, C5)
            lyrics = listOf(
                LyricLine(0, "• Deep Echo Introduction •"),
                LyricLine(5000, "Into the endless night..."),
                LyricLine(12000, "We chase a midnight mirage"),
                LyricLine(18000, "Glow lines cutting the sky"),
                LyricLine(25000, "Liquid colors overlapping slowly"),
                LyricLine(32000, "A soft pulse under our feet"),
                LyricLine(40000, "Resting in a sea of glowing glass"),
                LyricLine(48000, "• Synthesizer Solitude •"),
                LyricLine(60000, "No sound can reach us here"),
                LyricLine(68000, "Safe in the electric wave"),
                LyricLine(75000, "Where stars slowly dim out"),
                LyricLine(82000, "Only the aurora remains.")
            )
        ),
        Song(
            id = "3",
            title = "Deep Space Solitude",
            artist = "Cosmo",
            album = "Vast Void",
            durationSeconds = 240,
            dominantColor = Color(0xFF0F2027),
            vibrantColor = Color(0xFFF09819),
            chords = doubleArrayOf(329.63, 392.00, 493.88, 587.33), // Em7 (E4, G4, B4, D5)
            lyrics = listOf(
                LyricLine(0, "• Hollow Cosmos Introduction •"),
                LyricLine(6000, "Vastness is our companion..."),
                LyricLine(14000, "A silent glow in the dark"),
                LyricLine(22000, "A golden thread through deep space"),
                LyricLine(30000, "Connecting the constellations"),
                LyricLine(38000, "Deep warmth in a freezing void"),
                LyricLine(46000, "Tuning in to the cosmic core"),
                LyricLine(54000, "• Harmonic Echo Bridge •"),
                LyricLine(66000, "We are but travelers of light"),
                LyricLine(74000, "Seeking the warm cosmic shore")
            )
        ),
        Song(
            id = "4",
            title = "Stardust Symphony",
            artist = "Aetherium",
            album = "Nebula Dream",
            durationSeconds = 195,
            dominantColor = Color(0xFF7F00FF),
            vibrantColor = Color(0xFFE100FF),
            chords = doubleArrayOf(349.23, 392.00, 523.25, 587.33), // Fmaj7 (F4, G4, C5, D5 Hybrid)
            lyrics = listOf(
                LyricLine(0, "• Shimmering Dust Opening •"),
                LyricLine(4000, "Nebula dust swirling around..."),
                LyricLine(10000, "In a symphony of purple and pink"),
                LyricLine(16000, "We dance without gravity"),
                LyricLine(22000, "A vibrant neon dreams field"),
                LyricLine(29000, "A place made of digital dust"),
                LyricLine(36000, "• Radiant Filter Sweep •"),
                LyricLine(46000, "Close your eyes, breathe in"),
                LyricLine(54000, "Let the stars compose this night"),
                LyricLine(62000, "A liquid sky in constant motion")
            )
        )
    )

    val songsList: List<Song> = presetSongs

    private val _currentSong = MutableStateFlow(presetSongs[0])
    val currentSong: StateFlow<Song> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentTimeSec = MutableStateFlow(0)
    val currentTimeSec: StateFlow<Int> = _currentTimeSec.asStateFlow()

    private val _volume = MutableStateFlow(0.6f) // Soothing default volume (60%)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private val _trackDominantColor = MutableStateFlow(presetSongs[0].dominantColor)
    val trackDominantColor: StateFlow<Color> = _trackDominantColor.asStateFlow()

    private val _trackVibrantColor = MutableStateFlow(presetSongs[0].vibrantColor)
    val trackVibrantColor: StateFlow<Color> = _trackVibrantColor.asStateFlow()

    private var synthJob: Job? = null
    private var progressJob: Job? = null

    init {
        // Automatically start synthesized playback loop if playing is enabled
        observePlaybackState()
    }

    fun selectSong(song: Song) {
        _currentSong.value = song
        _trackDominantColor.value = song.dominantColor
        _trackVibrantColor.value = song.vibrantColor
        _currentTimeSec.value = 0
        if (_isPlaying.value) {
            restartSynthesizer()
        }
    }

    fun togglePlayback() {
        _isPlaying.value = !_isPlaying.value
    }

    fun seekTo(seconds: Int) {
        _currentTimeSec.value = seconds.coerceIn(0, _currentSong.value.durationSeconds)
    }

    fun updateVolume(newVolume: Float) {
        _volume.value = newVolume.coerceIn(0f, 1f)
    }

    fun nextSong() {
        val currentIndex = presetSongs.indexOf(_currentSong.value)
        val nextIndex = (currentIndex + 1) % presetSongs.size
        selectSong(presetSongs[nextIndex])
    }

    fun previousSong() {
        val currentIndex = presetSongs.indexOf(_currentSong.value)
        val prevIndex = if (currentIndex - 1 < 0) presetSongs.size - 1 else currentIndex - 1
        selectSong(presetSongs[prevIndex])
    }

    private fun observePlaybackState() {
        viewModelScope.launch {
            _isPlaying.collect { playing ->
                if (playing) {
                    startSynthesizer()
                    startProgressTracker()
                } else {
                    stopSynthesizer()
                    stopProgressTracker()
                }
            }
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val nextSec = _currentTimeSec.value + 1
                if (nextSec >= _currentSong.value.durationSeconds) {
                    nextSong()
                } else {
                    _currentTimeSec.value = nextSec
                }
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun restartSynthesizer() {
        stopSynthesizer()
        startSynthesizer()
    }

    private fun startSynthesizer() {
        synthJob?.cancel()
        synthJob = viewModelScope.launch(Dispatchers.Default) {
            val sampleRate = 44100
            val minBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            
            val audioTrack = try {
                AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBufferSize.coerceAtLeast(2048),
                    AudioTrack.MODE_STREAM
                )
            } catch (e: Exception) {
                null
            }

            if (audioTrack == null) return@launch

            try {
                audioTrack.play()
            } catch (e: Exception) {
                audioTrack.release()
                return@launch
            }

            val bufferSize = 1024
            val buffer = ShortArray(bufferSize)
            val activeChords = _currentSong.value.chords
            val phase = DoubleArray(activeChords.size)
            
            var smoothVolume = 0f

            try {
                while (true) {
                    // Smoothly fade volume in/out or ramp up volume to prevent audio clicking on play/pause
                    val targetVol = _volume.value
                    if (smoothVolume < targetVol) {
                        smoothVolume = (smoothVolume + 0.05f).coerceAtMost(targetVol)
                    } else if (smoothVolume > targetVol) {
                        smoothVolume = (smoothVolume - 0.05f).coerceAtLeast(targetVol)
                    }

                    for (i in 0 until bufferSize) {
                        var sample = 0.0
                        
                        // Ultra-smooth low frequency ambient synthesizer algorithm with detuned chords
                        for (idx in activeChords.indices) {
                            val detunedFreq = activeChords[idx] + if (idx % 2 == 0) 0.15 else -0.15
                            phase[idx] += 2.0 * Math.PI * detunedFreq / sampleRate
                            if (phase[idx] > 2.0 * Math.PI) phase[idx] -= 2.0 * Math.PI
                            
                            // Combine pure sine waves
                            sample += sin(phase[idx])
                        }
                        
                        // Add sub-bass fundamental to enrich the ambient chord cushion
                        var bassPhase = phase[0] * 0.5
                        if (bassPhase > 2.0 * Math.PI) bassPhase -= 2.0 * Math.PI
                        sample += 0.8 * sin(bassPhase)

                        // Normalize and filter high frequencies for a warm low-pass filtered feeling
                        val scaledSample = (sample / (activeChords.size + 1.0) * 32767.0 * 0.18 * smoothVolume)
                        buffer[i] = scaledSample.toInt().toShort()
                    }
                    audioTrack.write(buffer, 0, bufferSize)
                    delay(1)
                }
            } catch (e: Exception) {
                // Handle cancellation safely
            } finally {
                try {
                    audioTrack.stop()
                    audioTrack.release()
                } catch (e: Exception) {
                    // Ignore release errors
                }
            }
        }
    }

    private fun stopSynthesizer() {
        synthJob?.cancel()
        synthJob = null
    }

    // Helper to retrieve active lyric line based on current track playback progress
    fun getActiveLyricIndex(currentTimeMs: Long): Int {
        val lyricsList = _currentSong.value.lyrics
        for (i in lyricsList.indices.reversed()) {
            if (currentTimeMs >= lyricsList[i].timeMs) {
                return i
            }
        }
        return 0
    }

    override fun onCleared() {
        stopSynthesizer()
        stopProgressTracker()
        super.onCleared()
    }
}
