package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SongEntity
import com.example.player.PlaybackState
import com.example.ui.MusicPlayerViewModel
import com.example.ui.components.glassmorphic
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

    val parsedLyrics by viewModel.parsedLyrics.collectAsState()
    val currentLyricIndex by viewModel.currentLyricIndex.collectAsState()

    val ambientCover by viewModel.trackAmbientCover.collectAsState()
    val targetDominantColor by viewModel.trackDominantColor.collectAsState()
    val targetVibrantColor by viewModel.trackVibrantColor.collectAsState()

    // Smooth color transitions with 1000ms crossfade
    val animatedDominant by animateColorAsState(
        targetValue = targetDominantColor,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "AnimDominantNP"
    )
    val animatedVibrant by animateColorAsState(
        targetValue = targetVibrantColor,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "AnimVibrantNP"
    )

    // If no song is loaded, show a friendly empty player state
    if (currentSong == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "اختر أحد المسارات الصوتية للتشغيل\nChoose a song to start listening...",
                color = Color.White,
                fontSize = 15.sp,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    val song = currentSong!!

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Base Layer: Dynamic vertical/diagonal palette gradient background based on current album colors
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            animatedDominant.copy(alpha = 0.9f),
                            animatedVibrant.copy(alpha = 0.9f)
                        )
                    )
                )
        )

        // 2. Mid Layer: Glassmorphism Blur of the current artwork (preserved as-is)
        if (ambientCover != null) {
            Image(
                bitmap = ambientCover!!.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(50.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
            )
        }

        // 3. Top Scrim Layer: Dark Scrim (50% opacity) above gradient to safeguard high-contrast text visibility
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF040408).copy(alpha = 0.50f))
        )

        // Main content block
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                // 2. Top Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Chevron Down icon for collapse actions
                    IconButton(
                        onClick = onCollapse,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .testTag("player_collapse_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = "Collapse Player",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Center: Track Title & Artist Subtext
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = song.title,
                            color = Color.White,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = song.artist,
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Right: Three vertical dots (More Options)
                    IconButton(
                        onClick = { /* More choices trigger */ },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "More Options",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            },
            bottomBar = {
                // 4. Bottom Controls Deck
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val duration = song.durationMs
                    val posFloat = currentPosition.toFloat().coerceIn(0f, duration.toFloat())

                    // Minimalist line-styled progress seekbar
                    Slider(
                        value = posFloat,
                        onValueChange = { viewModel.seekTo(it.toLong()) },
                        valueRange = 0f..duration.toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color.White.copy(alpha = 0.2f),
                            activeTickColor = Color.Transparent,
                            inactiveTickColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .testTag("playback_slider")
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Current progress elapsed
                        Text(
                            text = formatTime(currentPosition),
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )

                        // Track total duration
                        Text(
                            text = formatTime(duration),
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // symmetrical playback layout centering exactly 3 primary control buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Previous track
                        IconButton(
                            onClick = { viewModel.previous() },
                            modifier = Modifier
                                .padding(end = 28.dp)
                                .size(48.dp)
                                .testTag("player_prev")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SkipPrevious,
                                contentDescription = "Previous Track",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        // Play / Pause Circle (Center - largest)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(76.dp)
                        ) {
                            IconButton(
                                onClick = { viewModel.togglePlayPause() },
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .testTag("player_play_pause")
                            ) {
                                Icon(
                                    imageVector = if (playbackState == PlaybackState.PLAYING) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = Color.Black,
                                    modifier = Modifier.size(38.dp)
                                )
                            }
                        }

                        // Next track
                        IconButton(
                            onClick = { viewModel.next() },
                            modifier = Modifier
                                .padding(start = 28.dp)
                                .size(48.dp)
                                .testTag("player_next")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SkipNext,
                                contentDescription = "Next Track",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            // 3. Middle Area: Synced Lyrics Scrolling View
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                SyncedLyricsScrollContainer(
                    parsedLyrics = parsedLyrics,
                    currentLyricIndex = currentLyricIndex,
                    onLineClick = { timestamp ->
                        viewModel.seekTo(timestamp)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun SyncedLyricsScrollContainer(
    parsedLyrics: List<LyricLine>,
    currentLyricIndex: Int,
    onLineClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    // Automatically centers the active lyric row smoothly into viewport
    LaunchedEffect(currentLyricIndex) {
        if (currentLyricIndex in parsedLyrics.indices) {
            coroutineScope.launch {
                val offsetPx = with(density) { -180.dp.roundToPx() }
                listState.animateScrollToItem(
                    index = currentLyricIndex,
                    scrollOffset = offsetPx
                )
            }
        }
    }

    Box(
        modifier = modifier
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
                contentPadding = PaddingValues(vertical = 180.dp), // Large space top & bottom so items can scroll to middle
                verticalArrangement = Arrangement.spacedBy(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                itemsIndexed(parsedLyrics) { index, line ->
                    val isActive = index == currentLyricIndex
                    LyricLineRow(
                        line = line,
                        isActive = isActive,
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
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.12f else 0.95f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "ScaleLyric"
    )

    val color by animateColorAsState(
        targetValue = if (isActive) Color.White else Color.White.copy(alpha = 0.55f),
        label = "ColorLyric"
    )

    val fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
    val fontSize = if (isActive) 19.sp else 16.sp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = line.text,
            color = color,
            fontSize = fontSize,
            fontWeight = fontWeight,
            textAlign = TextAlign.Center,
            modifier = Modifier.scale(scale)
        )
    }
}

private fun formatTime(ms: Long): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return String.format("%02d:%02d", min, sec)
}
