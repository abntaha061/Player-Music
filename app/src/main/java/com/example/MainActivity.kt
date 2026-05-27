package com.example

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MusicPlayerViewModel
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.util.LocalMusicScanner
import com.example.util.LyricLine
import com.example.util.Song

class MainActivity : ComponentActivity() {

    private val musicViewModel: MusicPlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            PlayerMusicTheme {
                val songs by musicViewModel.songs.collectAsState()
                val currentSong by musicViewModel.currentSong.collectAsState()
                val isPlaying by musicViewModel.isPlaying.collectAsState()
                val currentTimeSec by musicViewModel.currentTimeSec.collectAsState()
                val volume by musicViewModel.volume.collectAsState()
                val trackDominantColor by musicViewModel.trackDominantColor.collectAsState()
                val trackVibrantColor by musicViewModel.trackVibrantColor.collectAsState()

                var isPlayerExpanded by remember { mutableStateOf(false) }
                var mainScreenTab by remember { mutableIntStateOf(0) }

                val context = LocalContext.current
                var hasSelectPermission by remember {
                    mutableStateOf(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            context.checkSelfPermission(android.Manifest.permission.READ_MEDIA_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED
                        } else {
                            context.checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED
                        }
                    )
                }

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    hasSelectPermission = isGranted
                    musicViewModel.deployAndScan()
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SpaceBlack)
                ) {
                    // 1. Core Aurora Fluid Background - 100% Native brush interpolation (High performance, zero GPU cost)
                    AuroraBackground(
                        animatedDominant = trackDominantColor,
                        animatedVibrant = trackVibrantColor,
                        modifier = Modifier.fillMaxSize()
                    )

                    // 2. Safe Drawing Container
                    Scaffold(
                        containerColor = Color.Transparent,
                        contentWindowInsets = WindowInsets.safeDrawing,
                        modifier = Modifier.fillMaxSize()
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                            ) {
                                // 1. Flat Header Tab Selectors (Matching 100% video tabs layout at the top)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "لك من أجلك / For You",
                                        fontSize = if (mainScreenTab == 0) 20.sp else 16.sp,
                                        fontWeight = if (mainScreenTab == 0) FontWeight.ExtraBold else FontWeight.Bold,
                                        color = if (mainScreenTab == 0) TextWhite else TextGrey,
                                        modifier = Modifier
                                            .clickable { mainScreenTab = 0 }
                                            .padding(end = 24.dp)
                                            .testTag("for_you_tab")
                                    )
                                    Text(
                                        text = "مكتبة الموسيقى / My Library",
                                        fontSize = if (mainScreenTab == 1) 20.sp else 16.sp,
                                        fontWeight = if (mainScreenTab == 1) FontWeight.ExtraBold else FontWeight.Bold,
                                        color = if (mainScreenTab == 1) TextWhite else TextGrey,
                                        modifier = Modifier
                                            .clickable { mainScreenTab = 1 }
                                            .testTag("my_library_tab")
                                    )

                                    Spacer(modifier = Modifier.weight(1f))

                                    // Refresh/Scan Trigger in library
                                    if (mainScreenTab == 1) {
                                        IconButton(
                                            onClick = { musicViewModel.deployAndScan() },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Refresh,
                                                contentDescription = "Rescan",
                                                tint = TextWhite.copy(alpha = 0.8f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }

                                if (mainScreenTab == 0) {
                                    // ==========================================
                                    // SCREEN 0: For You (لك من أجلك)
                                    // ==========================================
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth()
                                            .verticalScroll(rememberScrollState())
                                            .padding(horizontal = 16.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        // A. 2x2 Grid of cards
                                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                                Box(modifier = Modifier.weight(1f)) {
                                                    GridItemCard(
                                                        title = "الأغاني",
                                                        subtitle = "${songs.size} أغنية",
                                                        icon = Icons.Rounded.Favorite,
                                                        iconBgColor = Color(0xFFFF5252).copy(alpha = 0.2f),
                                                        iconTint = Color(0xFFFF5252)
                                                    )
                                                }
                                                Box(modifier = Modifier.weight(1f)) {
                                                    GridItemCard(
                                                        title = "الأكثر استماعاً",
                                                        subtitle = "تحليل محلي",
                                                        icon = Icons.Rounded.GraphicEq,
                                                        iconBgColor = Color(0xFF00ADB5).copy(alpha = 0.2f),
                                                        iconTint = Color(0xFF00ADB5)
                                                    )
                                                }
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                                Box(modifier = Modifier.weight(1f)) {
                                                    GridItemCard(
                                                        title = "ملفات الـ LRC",
                                                        subtitle = "كلمات متزامنة",
                                                        icon = Icons.Rounded.Description,
                                                        iconBgColor = Color(0xFF4CAF50).copy(alpha = 0.2f),
                                                        iconTint = Color(0xFF4CAF50)
                                                    )
                                                }
                                                Box(modifier = Modifier.weight(1f)) {
                                                    GridItemCard(
                                                        title = "المشغلة حديثاً",
                                                        subtitle = "تاريخ الاستماع",
                                                        icon = Icons.Rounded.History,
                                                        iconBgColor = Color(0xFFBD83FF).copy(alpha = 0.2f),
                                                        iconTint = Color(0xFFBD83FF)
                                                    )
                                                }
                                            }
                                        }

                                        // B. Artists For You
                                        Column {
                                            Text(
                                                text = "فنانين من أجلك / Artists For You",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextGrey,
                                                letterSpacing = 1.sp,
                                                modifier = Modifier.padding(bottom = 10.dp)
                                            )
                                            LazyRow(
                                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                                            ) {
                                                item {
                                                    ArtistItem(
                                                        letter = "أ",
                                                        name = "ألوان النور",
                                                        gradient = Brush.linearGradient(listOf(Color(0xFF00ADB5), Color(0xFFBD83FF)))
                                                    )
                                                }
                                                item {
                                                    ArtistItem(
                                                        letter = "ل",
                                                        name = "لحن الهدوء الممزوج",
                                                        gradient = Brush.linearGradient(listOf(Color(0xFFBD83FF), Color(0xFFFF5252)))
                                                    )
                                                }
                                                item {
                                                    ArtistItem(
                                                        letter = "ن",
                                                        name = "نبضات الكوكب",
                                                        gradient = Brush.linearGradient(listOf(Color(0xFFFF5252), Color(0xFFFFB300)))
                                                    )
                                                }
                                            }
                                        }

                                        // C. Discover Local (اكتشف الموسيقى في جهازك)
                                        Column(modifier = Modifier.padding(bottom = 100.dp)) {
                                            Text(
                                                text = "اكتشف الموسيقى في جهازك / Discover Local",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextGrey,
                                                letterSpacing = 1.sp,
                                                modifier = Modifier.padding(bottom = 10.dp)
                                            )

                                            if (songs.isEmpty()) {
                                                GlassCard(
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Column(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(vertical = 24.dp, horizontal = 16.dp),
                                                        horizontalAlignment = Alignment.CenterHorizontally
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.FolderOpen,
                                                            contentDescription = null,
                                                            tint = TextGrey,
                                                            modifier = Modifier.size(48.dp)
                                                        )
                                                        Spacer(modifier = Modifier.height(10.dp))
                                                        Text(
                                                            text = "لم يتم الكشف عن أي ملفات .mp3 بعد",
                                                            fontSize = 14.sp,
                                                            color = TextGrey,
                                                            textAlign = TextAlign.Center
                                                        )
                                                        Spacer(modifier = Modifier.height(16.dp))
                                                        Button(
                                                            onClick = { musicViewModel.deployAndScan() },
                                                            colors = ButtonDefaults.buttonColors(containerColor = trackVibrantColor),
                                                            shape = RoundedCornerShape(12.dp)
                                                        ) {
                                                            Text("توليد ملفات وبطاقات تجريبية", color = Color.White)
                                                        }
                                                    }
                                                }
                                            } else {
                                                // List of songs under Discover Local
                                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    songs.forEach { song ->
                                                        val isCurrent = song.dataPath == currentSong?.dataPath
                                                        DiscoverLocalItem(
                                                            song = song,
                                                            isCurrent = isCurrent,
                                                            onClick = {
                                                                musicViewModel.selectSong(song)
                                                                isPlayerExpanded = true
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    // ==========================================
                                    // SCREEN 1: My Library (المكتبة الموسيقية)
                                    // ==========================================
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth()
                                    ) {
                                        if (songs.isEmpty()) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(24.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.LibraryMusic,
                                                    contentDescription = null,
                                                    tint = TextGrey,
                                                    modifier = Modifier.size(64.dp)
                                                )
                                                Spacer(modifier = Modifier.height(16.dp))
                                                Text(
                                                    text = "مكتبتك خاوية حتى الآن",
                                                    fontSize = 18.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextWhite,
                                                    textAlign = TextAlign.Center
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    text = "تأكد من وجود ملفات موسيقى بمجلد /Music/ بالنظام، أو اضغط على الزر بالأسفل لتوليد أغاني تجريبية متكاملة.",
                                                    fontSize = 13.sp,
                                                    color = TextGrey,
                                                    textAlign = TextAlign.Center
                                                )
                                                Spacer(modifier = Modifier.height(24.dp))
                                                Button(
                                                    onClick = { musicViewModel.deployAndScan() },
                                                    colors = ButtonDefaults.buttonColors(containerColor = trackVibrantColor),
                                                    shape = RoundedCornerShape(12.dp)
                                                ) {
                                                    Text("توليد ملفات وبطاقات تجريبية", color = Color.White)
                                                }
                                            }
                                        } else {
                                            LazyColumn(
                                                modifier = Modifier.fillMaxSize(),
                                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp)
                                            ) {
                                                itemsIndexed(songs) { index, song ->
                                                    val isCurrent = song.dataPath == currentSong?.dataPath
                                                    LibrarySongItem(
                                                        index = index,
                                                        song = song,
                                                        isCurrent = isCurrent,
                                                        isPlaying = isPlaying,
                                                        onPlayToggle = {
                                                            if (isCurrent) {
                                                                musicViewModel.togglePlayback()
                                                            } else {
                                                                musicViewModel.selectSong(song)
                                                            }
                                                        },
                                                        onClick = {
                                                            musicViewModel.selectSong(song)
                                                            isPlayerExpanded = true
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Minimalist Floating Bottom Player Bar (Glass Panel)
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp, start = 8.dp, end = 8.dp)
                            ) {
                                GlassFloatingBar(
                                    song = currentSong,
                                    isPlaying = isPlaying,
                                    currentTimeSec = currentTimeSec,
                                    onPlayPause = { musicViewModel.togglePlayback() },
                                    onExpand = { isPlayerExpanded = true }
                                )
                            }
                        }
                    }

                    // 3. Immersive Full-screen Now Playing Overlay
                    AnimatedVisibility(
                        visible = isPlayerExpanded && currentSong != null,
                        enter = slideInVertically(
                            initialOffsetY = { h: Int -> h },
                            animationSpec = tween(450)
                        ) + fadeIn(animationSpec = tween(450)),
                        exit = slideOutVertically(
                            targetOffsetY = { h: Int -> h },
                            animationSpec = tween(400)
                        ) + fadeOut(animationSpec = tween(400)),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        currentSong?.let { song ->
                            ExpandedPlayerScreen(
                                song = song,
                                isPlaying = isPlaying,
                                currentTimeSec = currentTimeSec,
                                volume = volume,
                                trackDominantColor = trackDominantColor,
                                trackVibrantColor = trackVibrantColor,
                                onCollapse = { isPlayerExpanded = false },
                                onPrev = { musicViewModel.previousSong() },
                                onNext = { musicViewModel.nextSong() },
                                onPlayPause = { musicViewModel.togglePlayback() },
                                onVolumeChange = { musicViewModel.updateVolume(it) },
                                onSeek = { musicViewModel.seekTo(it) },
                                lyrics = song.getLyrics(),
                                onLyricLineClick = { line: LyricLine -> musicViewModel.seekTo((line.timeMs / 1000).toInt()) }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun formatTime(seconds: Int): String {
        val m = seconds / 60
        val s = seconds % 60
        return String.format("%02d:%02d", m, s)
    }
}

@Composable
fun SongArtworkIndicator(
    song: Song,
    isCurrent: Boolean,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    var bitmap by remember(song.dataPath) { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(song.dataPath) {
        bitmap = LocalMusicScanner.extractEmbeddedAlbumArt(song.dataPath)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isCurrent) {
                    Brush.linearGradient(listOf(song.dominantColor, song.vibrantColor))
                } else {
                    Brush.linearGradient(listOf(GlassWhite, Color.Transparent))
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            if (isCurrent && isPlaying) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.GraphicEq,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        } else {
            Icon(
                imageVector = if (isCurrent && isPlaying) Icons.Rounded.GraphicEq else Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = if (isCurrent) Color.White else song.vibrantColor,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
fun GlassFloatingBar(
    song: Song?,
    isPlaying: Boolean,
    currentTimeSec: Int,
    onPlayPause: () -> Unit,
    onExpand: () -> Unit
) {
    if (song == null) return
    val progress = currentTimeSec.toFloat() / song.durationSeconds.toFloat()
    val barColor = song.vibrantColor

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF0D0D14).copy(alpha = 0.85f))
            .border(
                1.dp,
                Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.02f))
                ),
                RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onExpand)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        // Linear bottom progress bar indicator
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(2.dp)
                .background(barColor)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Rotating disk mini version
            CrispAlbumArt(
                song = song,
                isPlaying = isPlaying,
                modifier = Modifier
                    .size(42.dp)
                    .testTag("mini_album_art")
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    fontSize = 12.sp,
                    color = TextGrey,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Simple Playback Toggle
            IconButton(
                onClick = onPlayPause,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("mini_play_button")
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.PauseCircleFilled else Icons.Rounded.PlayCircleFilled,
                    contentDescription = "play_toggle",
                    tint = TextWhite,
                    modifier = Modifier.size(34.dp)
                )
            }
        }
    }
}

@Composable
fun ExpandedPlayerScreen(
    song: Song,
    isPlaying: Boolean,
    currentTimeSec: Int,
    volume: Float,
    trackDominantColor: Color,
    trackVibrantColor: Color,
    onCollapse: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onPlayPause: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onSeek: (Int) -> Unit,
    lyrics: List<LyricLine>,
    onLyricLineClick: (LyricLine) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceBlack)
    ) {
        // Redraw Aurora in expanded state for beautiful immersive transition
        AuroraBackground(
            animatedDominant = trackDominantColor,
            animatedVibrant = trackVibrantColor,
            modifier = Modifier.fillMaxSize()
        )

        // Column for layout
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
        ) {
            // Header Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onCollapse,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(GlassWhite)
                        .testTag("player_collapse_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "collapse",
                        tint = TextWhite
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "NOW PLAYING",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextGrey,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = song.album,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 200.dp)
                    )
                }

                IconButton(
                    onClick = { /* harmless click feedback */ },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(GlassWhite)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MoreHoriz,
                        contentDescription = "more",
                        tint = TextWhite
                    )
                }
            }

            // Beautiful Tab/Layout split for Disk vs Lyrics
            var selectedTab by remember { mutableIntStateOf(0) } // 0 = Player, 1 = Lyrics

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (selectedTab == 0) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Spacer(modifier = Modifier.height(10.dp))

                        // Large spinning CD disk with spring resizing on Play/Pause
                        CrispAlbumArt(
                            song = song,
                            isPlaying = isPlaying,
                            modifier = Modifier
                                .fillMaxWidth(0.72f)
                                .testTag("expanded_album_art")
                        )

                        Spacer(modifier = Modifier.height(30.dp))

                        Text(
                            text = song.title,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = TextWhite,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = song.artist,
                            fontSize = 16.sp,
                            color = trackVibrantColor,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(20.dp))
                    }
                } else {
                    // Synced Lyrics View
                    SyncedLyricsView(
                        lyrics = lyrics,
                        currentTimeMs = currentTimeSec.toLong() * 1000L,
                        onLineClick = onLyricLineClick,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Visual view selector toggle: Player controls / Lyrics
            Row(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clip(CircleShape)
                    .background(GlassWhite)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (selectedTab == 0) TextWhite.copy(alpha = 0.2f) else Color.Transparent)
                        .clickable { selectedTab = 0 }
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text("Player", color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (selectedTab == 1) TextWhite.copy(alpha = 0.2f) else Color.Transparent)
                        .clickable { selectedTab = 1 }
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text("Lyrics", color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Progress seekbar
            val progress = currentTimeSec.toFloat() / song.durationSeconds.toFloat()
            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = progress.coerceIn(0f, 1f),
                    onValueChange = { onSeek((it * song.durationSeconds).toInt()) },
                    colors = SliderDefaults.colors(
                        activeTrackColor = trackVibrantColor,
                        inactiveTrackColor = Color.White.copy(alpha = 0.15f),
                        thumbColor = TextWhite
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("player_progress_slider")
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(currentTimeSec),
                        fontSize = 12.sp,
                        color = TextGrey
                    )
                    Text(
                        text = formatTime(song.durationSeconds),
                        fontSize = 12.sp,
                        color = TextGrey
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Playback buttons Panel
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Prev button
                IconButton(
                    onClick = onPrev,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(GlassWhite)
                        .testTag("prev_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipPrevious,
                        contentDescription = "prev",
                        tint = TextWhite,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Play toggle floating pill
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(trackDominantColor, trackVibrantColor)
                            )
                        )
                        .clickable(onClick = onPlayPause)
                        .testTag("play_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = "play_pause",
                        tint = Color.White,
                        modifier = Modifier.size(38.dp)
                    )
                }

                // Next button
                IconButton(
                    onClick = onNext,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(GlassWhite)
                        .testTag("next_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = "next",
                        tint = TextWhite,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Volume Controller
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.VolumeDown,
                    contentDescription = null,
                    tint = TextGrey,
                    modifier = Modifier.size(20.dp)
                )

                Slider(
                    value = volume,
                    onValueChange = onVolumeChange,
                    colors = SliderDefaults.colors(
                        activeTrackColor = TextWhite.copy(alpha = 0.5f),
                        inactiveTrackColor = Color.White.copy(alpha = 0.1f),
                        thumbColor = TextWhite
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                        .testTag("player_volume_slider")
                )

                Icon(
                    imageVector = Icons.Rounded.VolumeUp,
                    contentDescription = null,
                    tint = TextGrey,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private fun formatTime(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format("%02d:%02d", m, s)
}

// =========================================================================
// ADDITIONAL CUSTOM COMPOSABLES FOR FOR YOU AND MY LIBRARY DESIGNS
// =========================================================================

@Composable
fun GridItemCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBgColor: Color,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = TextGrey
                )
            }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun ArtistItem(
    letter: String,
    name: String,
    gradient: Brush,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.width(90.dp)
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(gradient),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = letter,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = name,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = TextWhite,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "1 مسار • 0 تشغيل",
            fontSize = 10.sp,
            color = TextGrey,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun DiscoverLocalItem(
    song: Song,
    isCurrent: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        focused = isCurrent
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Circle Folder Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(song.vibrantColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Folder,
                    contentDescription = null,
                    tint = song.vibrantColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    fontSize = 11.sp,
                    color = TextGrey,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // File Size / Music tag
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(GlassWhite)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                val size = when(song.title) {
                    "أمل بكرة / Aml Bokra" -> "10MB"
                    "عتمة / 3atma" -> "6MB"
                    else -> "8MB"
                }
                Text(
                    text = "$size",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextGrey
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = TextGrey,
                    modifier = Modifier.size(10.dp)
                )
            }
        }
    }
}

@Composable
fun LibrarySongItem(
    index: Int,
    song: Song,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onPlayToggle: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isLiked by remember { mutableStateOf(false) }
    
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        focused = isCurrent
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Play Button on left
            IconButton(
                onClick = onPlayToggle,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(song.vibrantColor.copy(alpha = 0.15f))
            ) {
                Icon(
                    imageVector = if (isCurrent && isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = song.vibrantColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Heart Icon Button
            IconButton(
                onClick = { isLiked = !isLiked },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = "Like",
                    tint = if (isLiked) Color.Red else TextGrey,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${song.artist} / ${song.album}",
                    fontSize = 11.sp,
                    color = TextGrey,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Circle Album Artwork Image on the right
            SongArtworkCircleIndicator(
                song = song,
                isCurrent = isCurrent,
                isPlaying = isPlaying,
                modifier = Modifier.size(36.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Song Index e.g. "01"
            Text(
                text = String.format("%02d", index + 1),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextGrey,
                modifier = Modifier.width(20.dp),
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
fun SongArtworkCircleIndicator(
    song: Song,
    isCurrent: Boolean,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    var bitmap by remember(song.dataPath) { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(song.dataPath) {
        bitmap = LocalMusicScanner.extractEmbeddedAlbumArt(song.dataPath)
    }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(
                Brush.linearGradient(listOf(song.dominantColor, song.vibrantColor))
            ),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            val letter = if (song.artist.isNotEmpty()) song.artist.take(1) else "S"
            Text(
                text = letter,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
        }
    }
}
