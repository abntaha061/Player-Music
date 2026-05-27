package com.example.ui

import android.app.Application
import android.media.MediaPlayer
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.util.LocalMusicScanner
import com.example.util.LyricLine
import com.example.util.Song
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class MusicPlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentTimeSec = MutableStateFlow(0)
    val currentTimeSec: StateFlow<Int> = _currentTimeSec.asStateFlow()

    private val _volume = MutableStateFlow(0.8f) // Standard crisp default volume (80%)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private val _trackDominantColor = MutableStateFlow(Color(0xFFBD83FF))
    val trackDominantColor: StateFlow<Color> = _trackDominantColor.asStateFlow()

    private val _trackVibrantColor = MutableStateFlow(Color(0xFF00ADB5))
    val trackVibrantColor: StateFlow<Color> = _trackVibrantColor.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null

    init {
        // 1. Initial Deployment & Scan
        deployAndScan()
    }

    fun deployAndScan() {
        viewModelScope.launch {
            val context = getApplication<Application>()
            // Scan directories and media store
            val scannedSongs = LocalMusicScanner.scanDeviceMusic(context)
            _songs.value = scannedSongs

            if (scannedSongs.isNotEmpty() && _currentSong.value == null) {
                selectSong(scannedSongs[0], autoPlay = false)
            }
        }
    }

    fun selectSong(song: Song, autoPlay: Boolean = true) {
        viewModelScope.launch {
            // Stop and release any existing media player
            releasePlayer()

            _currentSong.value = song
            _trackDominantColor.value = song.dominantColor
            _trackVibrantColor.value = song.vibrantColor
            _currentTimeSec.value = 0

            try {
                val player = MediaPlayer().apply {
                    setDataSource(song.dataPath)
                    setVolume(_volume.value, _volume.value)
                    prepare()
                }

                player.setOnCompletionListener {
                    nextSong()
                }

                mediaPlayer = player

                if (autoPlay) {
                    player.start()
                    _isPlaying.value = true
                    startProgressTracker()
                } else {
                    _isPlaying.value = false
                    stopProgressTracker()
                }
            } catch (e: Exception) {
                // Handle open failure gracefully (e.g. file missing)
                _isPlaying.value = false
                stopProgressTracker()
            }
        }
    }

    fun togglePlayback() {
        val player = mediaPlayer ?: return
        if (player.isPlaying) {
            player.pause()
            _isPlaying.value = false
            stopProgressTracker()
        } else {
            try {
                player.start()
                _isPlaying.value = true
                startProgressTracker()
            } catch (e: Exception) {
                // Handle playback start error
            }
        }
    }

    fun seekTo(seconds: Int) {
        val player = mediaPlayer ?: return
        try {
            val ms = seconds * 1000
            player.seekTo(ms)
            _currentTimeSec.value = seconds.coerceIn(0, _currentSong.value?.durationSeconds ?: 0)
        } catch (e: Exception) {
            // Safe seek
        }
    }

    fun updateVolume(newVolume: Float) {
        val clamped = newVolume.coerceIn(0f, 1f)
        _volume.value = clamped
        mediaPlayer?.let { player ->
            try {
                player.setVolume(clamped, clamped)
            } catch (e: Exception) {
                // Safe volume
            }
        }
    }

    fun nextSong() {
        val currentList = _songs.value
        if (currentList.isEmpty()) return
        val current = _currentSong.value ?: return
        val index = currentList.indexOfFirst { it.dataPath == current.dataPath }
        val nextIndex = if (index == -1 || index == currentList.size - 1) 0 else index + 1
        selectSong(currentList[nextIndex], autoPlay = _isPlaying.value)
    }

    fun previousSong() {
        val currentList = _songs.value
        if (currentList.isEmpty()) return
        val current = _currentSong.value ?: return
        val index = currentList.indexOfFirst { it.dataPath == current.dataPath }
        val prevIndex = if (index <= 0) currentList.size - 1 else index - 1
        selectSong(currentList[prevIndex], autoPlay = _isPlaying.value)
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (true) {
                val player = mediaPlayer
                if (player != null && _isPlaying.value) {
                    try {
                        val posMs = player.currentPosition
                        _currentTimeSec.value = posMs / 1000
                    } catch (e: Exception) {
                        // Safe progress fetch
                    }
                }
                delay(400)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun releasePlayer() {
        mediaPlayer?.let { player ->
            try {
                player.stop()
                player.release()
            } catch (e: Exception) {
                // Safe close
            }
        }
        mediaPlayer = null
    }

    override fun onCleared() {
        stopProgressTracker()
        releasePlayer()
        super.onCleared()
    }
}
