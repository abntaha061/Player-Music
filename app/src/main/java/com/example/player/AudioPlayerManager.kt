package com.example.player

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import com.example.data.SongEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

enum class PlaybackState {
    IDLE, PLAYING, PAUSED, STOPPED
}

enum class RepeatMode {
    OFF, ALL, ONE
}

class AudioPlayerManager(private val context: Context) {
    private val TAG = "AudioPlayerManager"

    private var mediaPlayer: MediaPlayer? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    private var positionTrackerJob: Job? = null

    // Track state flows
    private val _currentSong = MutableStateFlow<SongEntity?>(null)
    val currentSong: StateFlow<SongEntity?> = _currentSong.asStateFlow()

    private val _playbackState = MutableStateFlow(PlaybackState.IDLE)
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.ALL)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _isShuffleEnabled = MutableStateFlow(false)
    val isShuffleEnabled: StateFlow<Boolean> = _isShuffleEnabled.asStateFlow()

    private var playlist = listOf<SongEntity>()
    private var currentIndex = -1

    // Listener for when track finishes to trigger auto-advance
    var onTrackFinishedListener: (() -> Unit)? = null

    init {
        initializeMediaPlayer()
    }

    private fun initializeMediaPlayer() {
        mediaPlayer = MediaPlayer().apply {
            setOnCompletionListener {
                Log.d(TAG, "Media playback completed. Triggering handlePlaybackCompletion().")
                handlePlaybackCompletion()
            }
            setOnErrorListener { mp, what, extra ->
                Log.e(TAG, "MediaPlayer error occurred: what=$what, extra=$extra")
                _playbackState.value = PlaybackState.IDLE
                false
            }
        }
    }

    fun setPlaylist(songs: List<SongEntity>, currentSong: SongEntity) {
        this.playlist = songs
        this.currentIndex = songs.indexOfFirst { it.filePath == currentSong.filePath }
        Log.d(TAG, "Playlist set with ${songs.size} items. Active index=$currentIndex")
    }

    fun play(song: SongEntity) {
        try {
            stopPositionTracker()
            mediaPlayer?.reset() ?: initializeMediaPlayer()

            val file = File(song.filePath)
            if (!file.exists()) {
                Log.e(TAG, "File does not exist: ${song.filePath}")
                return
            }

            mediaPlayer?.setDataSource(song.filePath)
            mediaPlayer?.prepare()
            mediaPlayer?.start()

            _currentSong.value = song
            _playbackState.value = PlaybackState.PLAYING
            
            // Sync active index if song is in current list
            val index = playlist.indexOfFirst { it.filePath == song.filePath }
            if (index != -1) {
                currentIndex = index
            }

            startPositionTracker()
            Log.d(TAG, "Now playing: ${song.title}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed playing song: ${song.title}", e)
            _playbackState.value = PlaybackState.IDLE
        }
    }

    fun pause() {
        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.pause()
            _playbackState.value = PlaybackState.PAUSED
            stopPositionTracker()
        }
    }

    fun resume() {
        if (_playbackState.value == PlaybackState.PAUSED) {
            mediaPlayer?.start()
            _playbackState.value = PlaybackState.PLAYING
            startPositionTracker()
        } else if (_currentSong.value != null) {
            play(_currentSong.value!!)
        }
    }

    fun stop() {
        mediaPlayer?.stop()
        _playbackState.value = PlaybackState.STOPPED
        stopPositionTracker()
        _currentPosition.value = 0L
    }

    fun seekTo(positionMs: Long) {
        mediaPlayer?.let {
            it.seekTo(positionMs.toInt())
            _currentPosition.value = positionMs
        }
    }

    fun togglePlayPause() {
        when (_playbackState.value) {
            PlaybackState.PLAYING -> pause()
            PlaybackState.PAUSED -> resume()
            else -> {
                if (_currentSong.value != null) {
                    play(_currentSong.value!!)
                } else if (playlist.isNotEmpty()) {
                    play(playlist.first())
                }
            }
        }
    }

    fun next() {
        if (playlist.isEmpty()) return

        if (_isShuffleEnabled.value) {
            val randomIndex = (playlist.indices).random()
            currentIndex = randomIndex
        } else {
            currentIndex = (currentIndex + 1) % playlist.size
        }

        val nextSong = playlist[currentIndex]
        play(nextSong)
    }

    fun previous() {
        if (playlist.isEmpty()) return

        if (_currentPosition.value > 5000L) {
            // Seek to start of current song if more than 5s played
            seekTo(0L)
            return
        }

        if (_isShuffleEnabled.value) {
            val randomIndex = (playlist.indices).random()
            currentIndex = randomIndex
        } else {
            currentIndex = if (currentIndex - 1 < 0) {
                playlist.size - 1
            } else {
                currentIndex - 1
            }
        }

        val prevSong = playlist[currentIndex]
        play(prevSong)
    }

    fun toggleRepeatMode() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        Log.d(TAG, "Repeat mode changed to: ${_repeatMode.value}")
    }

    fun toggleShuffle() {
        _isShuffleEnabled.value = !_isShuffleEnabled.value
        Log.d(TAG, "Shuffle mode changed to: ${_isShuffleEnabled.value}")
    }

    private fun handlePlaybackCompletion() {
        onTrackFinishedListener?.invoke()
        
        when (_repeatMode.value) {
            RepeatMode.ONE -> {
                // Play same track again
                _currentSong.value?.let { play(it) }
            }
            RepeatMode.ALL -> {
                next()
            }
            RepeatMode.OFF -> {
                if (playlist.isNotEmpty() && currentIndex == playlist.size - 1) {
                    // Last song in list, stop playback
                    stop()
                } else {
                    next()
                }
            }
        }
    }

    private fun startPositionTracker() {
        stopPositionTracker()
        positionTrackerJob = coroutineScope.launch {
            while (true) {
                mediaPlayer?.let {
                    if (it.isPlaying) {
                        _currentPosition.value = it.currentPosition.toLong()
                    }
                }
                delay(200)
            }
        }
    }

    private fun stopPositionTracker() {
        positionTrackerJob?.cancel()
        positionTrackerJob = null
    }

    fun release() {
        stopPositionTracker()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
