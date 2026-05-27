package com.example

import android.app.Activity
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.ui.platform.LocalContext
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.player.PlaybackState
import com.example.ui.MusicPlayerViewModel
import com.example.ui.components.AmbientGlassBackground
import com.example.ui.components.glassmorphic
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonAccent
import com.example.ui.theme.TextMuted
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.BlurredEdgeTreatment

enum class NavPage {
    HOME, LIBRARY, SEARCH, SETTINGS
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val musicViewModel: MusicPlayerViewModel = viewModel()
                // Outer Ambient Glowing Atmosphere
                AmbientGlassBackground(viewModel = musicViewModel) {
                    MainAppScaffold(musicViewModel = musicViewModel)
                }
            }
        }
    }
}

@Composable
fun MainAppScaffold(musicViewModel: MusicPlayerViewModel = viewModel()) {
    
    // Page state control
    var currentPage by remember { mutableStateOf(NavPage.HOME) }
    var selectedArtistName by remember { mutableStateOf<String?>(null) }
    
    // Fullscreen Now Playing overlay panel control state
    var isPlayerExpanded by remember { mutableStateOf(false) }

    val currentSong by musicViewModel.currentSong.collectAsState()
    val playbackState by musicViewModel.playbackState.collectAsState()
    val activeColor by musicViewModel.trackDominantColor.collectAsState()
    val ambientCover by musicViewModel.trackAmbientCover.collectAsState()

    val context = LocalContext.current
    var lastBackPressTime by remember { mutableStateOf(0L) }

    // System Back Press Handler inside Jetpack Compose:
    BackHandler(enabled = true) {
        if (isPlayerExpanded) {
            isPlayerExpanded = false
        } else if (selectedArtistName != null) {
            selectedArtistName = null
        } else if (currentPage != NavPage.HOME) {
            currentPage = NavPage.HOME
        } else {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastBackPressTime < 2000) {
                (context as? Activity)?.finish()
            } else {
                lastBackPressTime = currentTime
                Toast.makeText(context, "اضغط مرة أخرى للخروج", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            modifier = Modifier.fillMaxSize(),
            topBar = {}, // Let the screen content flow beautiful edge-to-edge
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Transparent)
                ) {
                    // FLOATING COMPACT MINI AUDIO CONTROLLER
                    // Positioned ABOVE the bottom navigation bar with a subtle safe spacing
                    if (currentSong != null) {
                        AnimatedVisibility(
                            visible = !isPlayerExpanded,
                            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 14.dp, end = 14.dp, top = 2.dp, bottom = 0.dp)
                        ) {
                            MiniAudioController(
                                title = currentSong!!.title,
                                artist = currentSong!!.artist,
                                isPlaying = playbackState == PlaybackState.PLAYING,
                                activeColor = activeColor,
                                albumArt = ambientCover,
                                onRowClick = { isPlayerExpanded = true },
                                onPlayPauseClick = { musicViewModel.togglePlayPause() },
                                onNextClick = { musicViewModel.next() }
                            )
                        }
                    }

                    // STANDARD STABLE BOTTOM NAVIGATION BAR
                    BottomGlassNavigationBar(
                        activePage = currentPage,
                        activeColor = activeColor,
                        onPageSelected = { 
                            currentPage = it
                            selectedArtistName = null // Close artist details when jumping tabs
                        }
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .statusBarsPadding() // Protect status bar and camera notch safety area
            ) {
                // Crossfade between active screen pages
                Crossfade(targetState = currentPage, label = "ScreenTransition") { page ->
                    when (page) {
                        NavPage.HOME -> {
                            if (selectedArtistName != null) {
                                ArtistScreen(
                                    artistName = selectedArtistName!!,
                                    viewModel = musicViewModel,
                                    onBack = { selectedArtistName = null }
                                )
                            } else {
                                HomeScreen(
                                    viewModel = musicViewModel,
                                    onNavigateToLibrary = { currentPage = NavPage.LIBRARY },
                                    onArtistClick = { selectedArtistName = it }
                                )
                            }
                        }
                        NavPage.LIBRARY -> LibraryScreen(viewModel = musicViewModel)
                        NavPage.SEARCH -> SearchScreen(viewModel = musicViewModel)
                        NavPage.SETTINGS -> SettingsScreen(viewModel = musicViewModel)
                    }
                }
            }
        }

        // FULL SCREEN GLIDING NOW PLAYING CONTROLLER WITH 100% OPAQUE BLOCKED UNDERLAY
        AnimatedVisibility(
            visible = isPlayerExpanded,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(durationMillis = 400)
            ) + fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(durationMillis = 350)
            ) + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF040408)) // 100% Opaque Solid base to block home screen contents
            ) {
                NowPlayingScreen(
                    viewModel = musicViewModel,
                    onCollapse = { isPlayerExpanded = false }
                )
            }
        }
    }
}

/**
 * Bottom glass styled tab bar for standard navigation tabs
 */
@Composable
fun BottomGlassNavigationBar(
    activePage: NavPage,
    activeColor: Color,
    onPageSelected: (NavPage) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding() // Automatically offsets above device gesture handle / 3-button system bars
            .padding(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 10.dp)
            .height(56.dp)
            .glassmorphic(cornerRadius = 14.dp, alpha = 0.12f, borderColorAlpha = 0.22f),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavigationTabItem(
                label = "الرئيسية",
                englishLabel = "Home",
                icon = Icons.Filled.Home,
                isActive = activePage == NavPage.HOME,
                activeColor = activeColor,
                modifier = Modifier.testTag("nav_tab_home")
            ) { onPageSelected(NavPage.HOME) }

            NavigationTabItem(
                label = "المكتبة",
                englishLabel = "Library",
                icon = Icons.Filled.LibraryMusic,
                isActive = activePage == NavPage.LIBRARY,
                activeColor = activeColor,
                modifier = Modifier.testTag("nav_tab_library")
            ) { onPageSelected(NavPage.LIBRARY) }

            NavigationTabItem(
                label = "البحث",
                englishLabel = "Search",
                icon = Icons.Filled.Search,
                isActive = activePage == NavPage.SEARCH,
                activeColor = activeColor,
                modifier = Modifier.testTag("nav_tab_search")
            ) { onPageSelected(NavPage.SEARCH) }

            NavigationTabItem(
                label = "الضبط",
                englishLabel = "Settings",
                icon = Icons.Filled.Settings,
                isActive = activePage == NavPage.SETTINGS,
                activeColor = activeColor,
                modifier = Modifier.testTag("nav_tab_settings")
            ) { onPageSelected(NavPage.SETTINGS) }
        }
    }
}

@Composable
fun RowScope.NavigationTabItem(
    label: String,
    englishLabel: String,
    icon: ImageVector,
    isActive: Boolean,
    activeColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val inactiveColor = Color.White.copy(alpha = 0.55f)

    Column(
        modifier = modifier
            .weight(1f)
            .clickable(onClick = onClick)
            .fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isActive) activeColor else inactiveColor,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            color = if (isActive) activeColor else inactiveColor,
            fontSize = 9.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(top = 1.dp)
        )
    }
}

/**
 * Floating bar appearing at base of browser view mimicking an offline Mini Player
 */
@Composable
fun MiniAudioController(
    title: String,
    artist: String,
    isPlaying: Boolean,
    activeColor: Color,
    albumArt: Bitmap?,
    onRowClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .glassmorphic(cornerRadius = 14.dp, alpha = 0.2f, borderColorAlpha = 0.3f)
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(onClick = onRowClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Rotating disk album icon lookalike
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                if (albumArt != null) {
                    Image(
                        bitmap = albumArt.asImageBitmap(),
                        contentDescription = "Mini Player Album Art",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp))
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.MusicNote,
                        contentDescription = null,
                        tint = activeColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = artist,
                    color = TextMuted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Play Pause control button inside Mini Player
            IconButton(
                onClick = onPlayPauseClick,
                modifier = Modifier.testTag("mini_player_play_pause")
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = "Mini Play Pause",
                    tint = Color.White
                )
            }

            // Skip track next button inside Mini Player
            IconButton(
                onClick = onNextClick,
                modifier = Modifier.testTag("mini_player_next")
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = "Mini Skip Next",
                    tint = Color.White
                )
            }
        }
    }
}
