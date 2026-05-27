package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.SongEntity
import com.example.data.SongRepository
import com.example.player.AudioPlayerManager
import com.example.player.PlaybackState
import com.example.player.RepeatMode
import com.example.util.LyricLine
import com.example.util.MusicUtil
import com.example.util.PaletteHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MusicPlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "MusicPlayerViewModel"

    private val database = AppDatabase.getDatabase(application)
    private val repository = SongRepository(application, database.songDao())
    val playerManager = AudioPlayerManager(application)

    // Data lists from repository
    val allSongs: StateFlow<List<SongEntity>> = repository.allSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteSongs: StateFlow<List<SongEntity>> = repository.favoriteSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyPlayed: StateFlow<List<SongEntity>> = repository.recentlyPlayed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mostPlayed: StateFlow<List<SongEntity>> = repository.mostPlayed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val topArtists: StateFlow<List<ArtistStats>> = allSongs
        .map { songs ->
            songs.groupBy { it.artist }
                .map { (artistName, artistSongs) ->
                    val totalPlayCount = artistSongs.sumOf { it.playCount }
                    ArtistStats(
                        name = artistName,
                        playCount = totalPlayCount,
                        songCount = artistSongs.size,
                        color = PaletteHelper.getDeterministicColor(artistName, "artist")
                    )
                }
                .sortedWith(compareByDescending<ArtistStats> { it.playCount }.thenByDescending { it.songCount })
                .take(6)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Player state exposed from manager
    val currentSong: StateFlow<SongEntity?> = playerManager.currentSong
    val playbackState: StateFlow<PlaybackState> = playerManager.playbackState
    val currentPosition: StateFlow<Long> = playerManager.currentPosition
    val repeatMode: StateFlow<RepeatMode> = playerManager.repeatMode
    val isShuffleEnabled: StateFlow<Boolean> = playerManager.isShuffleEnabled

    // Dynamic palette colors for active player tinting!
    private val _trackDominantColor = MutableStateFlow<Color>(Color(0xFFBD83FF)) // Default Purple Accent
    val trackDominantColor: StateFlow<Color> = _trackDominantColor.asStateFlow()

    private val _trackVibrantColor = MutableStateFlow<Color>(Color(0xFF00ADB5)) // Default Cyan/Teal
    val trackVibrantColor: StateFlow<Color> = _trackVibrantColor.asStateFlow()

    private val _trackAmbientCover = MutableStateFlow<Bitmap?>(null)
    val trackAmbientCover: StateFlow<Bitmap?> = _trackAmbientCover.asStateFlow()

    // Parsing lyrics state
    private val _parsedLyrics = MutableStateFlow<List<LyricLine>>(emptyList())
    val parsedLyrics: StateFlow<List<LyricLine>> = _parsedLyrics.asStateFlow()

    // Active lyric line calculation
    val currentLyricIndex: StateFlow<Int> = combine(currentPosition, _parsedLyrics) { position, lyrics ->
        var activeIndex = -1
        for (i in lyrics.indices) {
            if (position >= lyrics[i].timestampMs) {
                activeIndex = i
            } else {
                break
            }
        }
        activeIndex
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), -1)

    // UI and scanning state
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanResultMessage = MutableStateFlow<String?>(null)
    val scanResultMessage: StateFlow<String?> = _scanResultMessage.asStateFlow()

    init {
        // Wire up playlist completion increments
        playerManager.onTrackFinishedListener = {
            // Handled via smart listening threshold tracker
        }

        // Set up initial lyrics parsing hook & dynamic background palette analyzer
        viewModelScope.launch {
            currentSong.collect { song ->
                if (song != null) {
                    // Update lyrics
                    if (song.hasLyrics) {
                        val lyricContent = song.lyricsText
                        val parsed = MusicUtil.parseLrc(lyricContent)
                        _parsedLyrics.value = parsed
                        Log.d(TAG, "Parsed lyrics for ${song.title}: count=${parsed.size}")
                    } else {
                        _parsedLyrics.value = emptyList()
                    }

                    // Parallel non-blocking extraction of palette & heavy blur background bitmap
                    launch(Dispatchers.Default) {
                        try {
                            val realEmbeddedArt = PaletteHelper.extractEmbeddedArt(song.filePath)
                            if (realEmbeddedArt != null) {
                                val (domColor, vibColor) = PaletteHelper.extractPalette(realEmbeddedArt)
                                _trackDominantColor.value = domColor
                                _trackVibrantColor.value = vibColor
                                
                                // Downscale for real-time background blur rendering
                                val scaledArt = Bitmap.createScaledBitmap(realEmbeddedArt, 64, 64, true)
                                _trackAmbientCover.value = scaledArt
                            } else {
                                // Fallback to gorgeous procedural abstract artwork
                                val proceduralArt = PaletteHelper.generateProceduralArt(song.title, song.artist)
                                val (domColor, vibColor) = PaletteHelper.extractPalette(proceduralArt)
                                _trackDominantColor.value = domColor
                                _trackVibrantColor.value = vibColor
                                
                                _trackAmbientCover.value = proceduralArt
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error processing song palette assets: ${e.message}")
                            _trackDominantColor.value = Color(0xFFBD83FF)
                            _trackVibrantColor.value = Color(0xFF00ADB5)
                            _trackAmbientCover.value = null
                        }
                    }
                } else {
                    _parsedLyrics.value = emptyList()
                    _trackDominantColor.value = Color(0xFFBD83FF)
                    _trackVibrantColor.value = Color(0xFF00ADB5)
                    _trackAmbientCover.value = null
                }
            }
        }

        // Smart Listening Threshold Tracker Logic
        var lastCountedFilePath: String? = null
        var hasCountedCurrentPlay = false

        viewModelScope.launch {
            currentSong.collect { song ->
                if (song != null) {
                    if (song.filePath != lastCountedFilePath) {
                        hasCountedCurrentPlay = false
                        lastCountedFilePath = song.filePath
                    }
                } else {
                    hasCountedCurrentPlay = false
                    lastCountedFilePath = null
                }
            }
        }

        viewModelScope.launch {
            currentPosition.collect { position ->
                val song = currentSong.value
                val isPlaying = playbackState.value == PlaybackState.PLAYING
                if (song != null && isPlaying && !hasCountedCurrentPlay) {
                    val threshold = minOf(30000L, (song.durationMs * 0.20).toLong())
                    if (position >= threshold && threshold > 0) {
                        hasCountedCurrentPlay = true
                        viewModelScope.launch(Dispatchers.IO) {
                            repository.incrementPlayCount(song.filePath)
                            Log.d(TAG, "Threshold met for ${song.title}: $position ms >= $threshold ms. play_count incremented on Dispatchers.IO.")
                        }
                    }
                }
            }
        }

        // Scan storage on startup to fetch real local device files
        viewModelScope.launch {
            try {
                repository.performSyncAndCleanup()
                val count = repository.scanLocalMusic()
                Log.d(TAG, "Startup scan complete. Found $count local music files.")
            } catch (e: Exception) {
                Log.e(TAG, "Startup sync/scan failed: ${e.message}", e)
            }
        }
    }

    fun playSong(song: SongEntity, currentList: List<SongEntity>) {
        viewModelScope.launch {
            playerManager.setPlaylist(currentList, song)
            playerManager.play(song)
        }
    }

    fun togglePlayPause() {
        playerManager.togglePlayPause()
    }

    fun next() {
        playerManager.next()
    }

    fun previous() {
        playerManager.previous()
    }

    fun seekTo(positionMs: Long) {
        playerManager.seekTo(positionMs)
    }

    fun toggleFavorite(song: SongEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val updatedFavoriteState = !song.isFavorite
            repository.toggleFavorite(song.filePath, updatedFavoriteState)
            // If the song is the one currently playing, update its local entity to refresh UI
            if (currentSong.value?.filePath == song.filePath) {
                val current = currentSong.value
                if (current != null) {
                    playerManager.play(current.copy(isFavorite = updatedFavoriteState))
                }
            }
        }
    }

    fun toggleRepeatMode() {
        playerManager.toggleRepeatMode()
    }

    fun toggleShuffle() {
        playerManager.toggleShuffle()
    }

    fun scanFiles() {
        viewModelScope.launch {
            _isScanning.value = true
            _scanResultMessage.value = "جاري مسح الذاكرة بحثاً عن ملفات الموسيقى..."
            try {
                val scanned = repository.scanLocalMusic()
                _scanResultMessage.value = "تم المسح بنجاح! تم العثور على $scanned ملف جديد."
            } catch (e: Exception) {
                _scanResultMessage.value = "فشل المسح: ${e.message}"
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun seedDemoTracks() {
        viewModelScope.launch {
            _isScanning.value = true
            _scanResultMessage.value = "جاري توليد المسارات التجريبية وتأثيرات الضباب..."
            try {
                val count = repository.loadDemoMusic()
                repository.scanLocalMusic()
                _scanResultMessage.value = "تم توليد وتزامن $count مسارات موسيقية عربية كاملة!"
            } catch (e: Exception) {
                _scanResultMessage.value = "فشلت العملية: ${e.message}"
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun dismissScanMessage() {
        _scanResultMessage.value = null
    }

    private suspend fun withContextIO(block: suspend () -> Unit) {
        kotlinx.coroutines.withContext(Dispatchers.IO) { block() }
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.release()
    }
}

data class ArtistStats(
    val name: String,
    val playCount: Int,
    val songCount: Int,
    val color: Color
)
