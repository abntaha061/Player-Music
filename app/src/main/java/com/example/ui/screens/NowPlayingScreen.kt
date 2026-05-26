package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.activity.compose.BackHandler
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SongEntity
import com.example.player.PlaybackState
import com.example.player.RepeatMode
import com.example.ui.MusicPlayerViewModel
import com.example.ui.components.AlbumArtView
import com.example.ui.components.glassmorphic
import com.example.ui.theme.NeonAccent
import com.example.ui.theme.TextMuted
import com.example.util.LyricLine
import kotlinx.coroutines.launch

@Composable
fun NowPlayingScreen(
    viewModel: MusicPlayerViewModel,
    onCollapse: () -> Unit
) {
    val currentSong by viewModel.currentSong.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val currentPosition by viewModel.currentPosition.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val isShuffleEnabled by viewModel.isShuffleEnabled.collectAsState()

    val parsedLyrics by viewModel.parsedLyrics.collectAsState()
    val currentLyricIndex by viewModel.currentLyricIndex.collectAsState()

    val coroutineScope = rememberCoroutineScope()

    // Screen configuration status: Fullscreen lyrics vs Standard dashboard
    var isLyricsFullscreen by remember { mutableStateOf(false) }
    var isTranslationActive by remember { mutableStateOf(false) }

    // Immersive back press gesture handler: if lyrics are fullscreen, collapse back to disk first!
    BackHandler(enabled = isLyricsFullscreen) {
        isLyricsFullscreen = false
    }

    // If no song is loaded, show a friendly empty player state
    if (currentSong == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "قريباً... اختر أحد المسارات الصوتية للتشغيل\nChoose a song to start listening...",
                color = Color.White,
                fontSize = 15.sp,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    val song = currentSong!!

    // Infinite rotating animations for the disk cover art when playing!
    val infiniteTransition = rememberInfiniteTransition(label = "DiskRotation")
    val diskRotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "DiskAngle"
    )
    val activeDiskRotation = if (playbackState == PlaybackState.PLAYING) diskRotationAngle else 0f

    // Dominant color theme matching from the animated palette state of the view model!
    val accentColor by viewModel.trackDominantColor.collectAsState()
    val vibrantAccentColor by viewModel.trackVibrantColor.collectAsState()

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            if (!isLyricsFullscreen) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onCollapse,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .testTag("player_collapse_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ExpandMore,
                            contentDescription = "Collapse Player",
                            tint = Color.White
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "شاشة المشغل النشط / Now Playing",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "المسار: ${song.albumName}",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }

                    IconButton(
                        onClick = { /* Checksum dialog can open here too */ },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Security,
                            contentDescription = "File integrity info",
                            tint = accentColor
                        )
                    }
                }
            } else {
                // High Immersive minimal fullscreen bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { isLyricsFullscreen = false },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Exit Fullscreen",
                            tint = Color.White
                        )
                    }

                    // Floating text header
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = song.title,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = song.artist,
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }

                    // Translations toggle
                    IconButton(
                        onClick = { isTranslationActive = !isTranslationActive },
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isTranslationActive) accentColor else Color.White.copy(alpha = 0.08f))
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Translate,
                            contentDescription = "Translate Lyrics",
                            tint = if (isTranslationActive) Color.Black else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        if (isLyricsFullscreen) {
            // IMMERSIVE FULL-SCREEN LYRICS MODE
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Standardise centering scroll synced lyrics container
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    SyncedLyricsScrollContainer(
                        parsedLyrics = parsedLyrics,
                        currentLyricIndex = currentLyricIndex,
                        isTranslationActive = isTranslationActive,
                        onLineClick = { timestamp ->
                            viewModel.seekTo(timestamp)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // COMPACT TRANSPARENT MINI PLAYER (at the bottom under fullscreen lyrics)
                // Contains ONLY: slider (progress/seekbar), play/pause button, current/total time
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .glassmorphic(cornerRadius = 18.dp, alpha = 0.08f)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val duration = song.durationMs
                    val posFloat = currentPosition.toFloat().coerceIn(0f, duration.toFloat())

                    // Seek Slider bar
                    Slider(
                        value = posFloat,
                        onValueChange = { viewModel.seekTo(it.toLong()) },
                        valueRange = 0f..duration.toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = accentColor,
                            activeTrackColor = accentColor,
                            inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatTime(currentPosition),
                            color = TextMuted,
                            fontSize = 11.sp
                        )

                        // Centered Play/Pause pill/circle button
                        IconButton(
                            onClick = { viewModel.togglePlayPause() },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        ) {
                            Icon(
                                imageVector = if (playbackState == PlaybackState.PLAYING) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = "Play/Pause Toggle",
                                tint = Color.Black,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Text(
                            text = formatTime(duration),
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        } else {
            // STANDARD DASHBOARD DISC MODE
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 1. Cover Disk Art + glow ring
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Backing rotating glass ring
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .rotate(-activeDiskRotation)
                            .border(
                                width = 3.dp,
                                brush = Brush.sweepGradient(
                                    colors = listOf(accentColor, Color.Transparent, accentColor.copy(alpha = 0.3f), accentColor)
                                ),
                                shape = CircleShape
                            )
                    )

                    // Disk shape Art with dynamic physical Album artwork loading!
                    Box(
                        modifier = Modifier
                            .fillMaxSize(0.9f)
                            .clip(CircleShape)
                            .rotate(activeDiskRotation)
                            .border(6.dp, Color.White.copy(alpha = 0.08f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        AlbumArtView(
                            filePath = song.filePath,
                            title = song.title,
                            artist = song.artist,
                            accentColor = accentColor,
                            modifier = Modifier.fillMaxSize(),
                            isCircular = true
                        )

                        // Outer elegant physical vinyl-disk looking center core
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.45f))
                                .border(1.dp, accentColor.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Radio,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        // Playback wave indicators overlaying disk
                        if (playbackState == PlaybackState.PLAYING) {
                            MiniAudioVisualizerBars(accentColor)
                        }
                    }
                }

                // 2. Metadata (Song Info)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = song.title,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = song.artist,
                        color = TextMuted,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // 3. Compact Lyrics Preview (only shown if NOT in fullscreen lyrics)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                        .padding(vertical = 4.dp)
                        .glassmorphic(cornerRadius = 14.dp, alpha = 0.08f)
                        .clickable { isLyricsFullscreen = true }
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "الكلمات المتزامنة / Lyrics Preview",
                                color = accentColor.copy(alpha = 0.82f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = Icons.Filled.Fullscreen,
                                contentDescription = "Expand Lyrics",
                                tint = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        val activeLineText = if (parsedLyrics.isEmpty()) {
                            "لا توجد كلمات متاحة لهذه الأغنية"
                        } else if (currentLyricIndex in parsedLyrics.indices) {
                            parsedLyrics[currentLyricIndex].text
                        } else {
                            "•••"
                        }

                        Text(
                            text = activeLineText,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.animateContentSize()
                        )

                        val nextLineText = if (currentLyricIndex + 1 in parsedLyrics.indices) {
                            parsedLyrics[currentLyricIndex + 1].text
                        } else {
                            ""
                        }

                        if (nextLineText.isNotEmpty()) {
                            Text(
                                text = nextLineText,
                                color = Color.White.copy(alpha = 0.45f),
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // 4. Timeline Slider block
                Column(modifier = Modifier.fillMaxWidth()) {
                    val duration = song.durationMs
                    val posFloat = currentPosition.toFloat().coerceIn(0f, duration.toFloat())

                    Slider(
                        value = posFloat,
                        onValueChange = { viewModel.seekTo(it.toLong()) },
                        valueRange = 0f..duration.toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = accentColor,
                            activeTrackColor = accentColor,
                            inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.testTag("playback_slider")
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatTime(currentPosition),
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                        Text(
                            text = formatTime(duration),
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                // 5. Control Deck
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.toggleShuffle() }) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Filled.Shuffle,
                                contentDescription = "Shuffle",
                                tint = if (isShuffleEnabled) NeonAccent else Color.White.copy(alpha = 0.6f)
                            )
                            if (isShuffleEnabled) {
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .clip(CircleShape)
                                        .background(NeonAccent)
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = { viewModel.previous() },
                        modifier = Modifier.testTag("player_prev")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SkipPrevious,
                            contentDescription = "Previous Track",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(accentColor.copy(alpha = 0.4f), Color.Transparent)
                                    )
                                )
                        )

                        IconButton(
                            onClick = { viewModel.togglePlayPause() },
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .testTag("player_play_pause")
                        ) {
                            Icon(
                                imageVector = if (playbackState == PlaybackState.PLAYING) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = "Play Pause Toggle",
                                tint = Color.Black,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.next() },
                        modifier = Modifier.testTag("player_next")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SkipNext,
                            contentDescription = "Next Track",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    IconButton(onClick = { viewModel.toggleRepeatMode() }) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = when (repeatMode) {
                                    RepeatMode.OFF -> Icons.Filled.Repeat
                                    RepeatMode.ALL -> Icons.Filled.Repeat
                                    RepeatMode.ONE -> Icons.Filled.RepeatOne
                                },
                                contentDescription = "Repeat",
                                tint = if (repeatMode != RepeatMode.OFF) accentColor else Color.White.copy(alpha = 0.6f)
                            )
                            if (repeatMode != RepeatMode.OFF) {
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .clip(CircleShape)
                                        .background(accentColor)
                                )
                            }
                        }
                    }
                }

                // 6. Favorite toggle heart button
                IconButton(
                    onClick = { viewModel.toggleFavorite(song) },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.06f))
                ) {
                    Icon(
                        imageVector = if (song.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Favorite Song",
                        tint = if (song.isFavorite) Color(0xFFFF5722) else Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun SyncedLyricsScrollContainer(
    parsedLyrics: List<LyricLine>,
    currentLyricIndex: Int,
    isTranslationActive: Boolean,
    onLineClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Smooth scrolls the currently active lyric line into the center of the viewport whenever index shifts!
    LaunchedEffect(currentLyricIndex) {
        if (currentLyricIndex in parsedLyrics.indices) {
            coroutineScope.launch {
                listState.animateScrollToItem(
                    index = if (currentLyricIndex > 2) currentLyricIndex - 2 else 0,
                    scrollOffset = 0
                )
            }
        }
    }

    Box(
        modifier = modifier
            .glassmorphic(cornerRadius = 16.dp, alpha = 0.06f)
            .padding(12.dp)
    ) {
        if (parsedLyrics.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "لا توجد كلمات متزامنة متاحة لهذه الأغنية\nNo interactive lyrics available for this song",
                    color = TextMuted,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 140.dp), // Spacious vertical padding for pure centering visuals
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                itemsIndexed(parsedLyrics) { index, line ->
                    val isActive = index == currentLyricIndex
                    LyricLineRow(
                        line = line,
                        isActive = isActive,
                        isTranslationActive = isTranslationActive,
                        onClick = { onLineClick(line.timestampMs) }
                    )
                }
            }
        }
    }
}

@Composable
fun LyricLineRow(
    line: LyricLine,
    isActive: Boolean,
    isTranslationActive: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.15f else 0.95f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "ScaleLyric"
    )

    val color by animateColorAsState(
        targetValue = if (isActive) Color.White else Color.White.copy(alpha = 0.35f),
        label = "ColorLyric"
    )

    val fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Normal

    // Determine lyrics translation display text
    val displayText = if (isTranslationActive) {
        generateLrcTranslation(line.text)
    } else {
        line.text
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = displayText,
            color = color,
            fontSize = 18.sp,
            fontWeight = fontWeight,
            textAlign = TextAlign.Center,
            modifier = Modifier.scale(scale)
        )
    }
}

@Composable
fun BoxScope.MiniAudioVisualizerBars(color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "Visualizer")
    
    // Animate 3 visual bars pulsing differently
    val heightScale1 by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "Bar1"
    )

    val heightScale2 by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "Bar2"
    )

    val heightScale3 by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(520, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "Bar3"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth(0.4f)
            .height(24.dp)
            .align(Alignment.BottomCenter)
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(heightScale1)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(heightScale2)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(heightScale3)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
    }
}

private fun formatTime(ms: Long): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return String.format("%02d:%02d", min, sec)
}

/**
 * Generate instantaneous poetic translations for demo items in real time
 */
private fun generateLrcTranslation(arabicLyricText: String): String {
    return when {
        arabicLyricText.contains("عين سحرية", ignoreCase = true) -> "Magic eye, observing the future and light..."
        arabicLyricText.contains("نرى الأفق", ignoreCase = true) -> "We see the distant horizon waking with dawn..."
        arabicLyricText.contains("نسير معاً", ignoreCase = true) -> "We walk together while paths tell details of patience..."
        arabicLyricText.contains("بشغف وسلام", ignoreCase = true) -> "Watching tomorrow with passion, peace and silence..."
        arabicLyricText.contains("عتمة", ignoreCase = true) -> "Darkness - a blend of complete celestial silence..."
        arabicLyricText.contains("في هدأة الليل", ignoreCase = true) -> "In the quiet of the night, we sail without sails..."
        arabicLyricText.contains("أمل بكرة", ignoreCase = true) -> "Tomorrow's hope, bridges of planet heartbeats..."
        arabicLyricText.contains("نزرع الأمنيات", ignoreCase = true) -> "We plant wishes in the palm of the near future..."
        arabicLyricText.contains("غداً تضحك العيون", ignoreCase = true) -> "Tomorrow eyes will laugh and a bright sun will rise..."
        else -> "Translating: \"$arabicLyricText\"..."
    }
}
