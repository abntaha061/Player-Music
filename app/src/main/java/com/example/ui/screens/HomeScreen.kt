package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SongEntity
import com.example.ui.MusicPlayerViewModel
import com.example.ui.components.glassmorphic
import com.example.ui.theme.NeonAccent
import com.example.ui.theme.TextMuted

@Composable
fun HomeScreen(
    viewModel: MusicPlayerViewModel,
    onNavigateToLibrary: () -> Unit,
    onArtistClick: (String) -> Unit
) {
    val allSongs by viewModel.allSongs.collectAsState()
    val favorites by viewModel.favoriteSongs.collectAsState()
    val rawRecent by viewModel.recentlyPlayed.collectAsState()
    val rawMostPlay by viewModel.mostPlayed.collectAsState()
    val topArtists by viewModel.topArtists.collectAsState()

    // Ensure we filter out only valid files or use all scanned
    val recentSongs = rawRecent
    val mostPlayedSongs = rawMostPlay

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Quick summary card (Scan trigger or status)
        AnimatedScanStatusCard(viewModel)

        Spacer(modifier = Modifier.height(16.dp))

        // 1. For You Section (2x3 Quick grid)
        Text(
            text = "لك من أجلك / For You",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.weight(1f)) {
                QuickGridItem(
                    title = "الأغاني المفضلة",
                    subtitle = "${favorites.size} أغنية",
                    icon = Icons.Filled.Favorite,
                    tint = Color(0xFFFF5722),
                    modifier = Modifier.testTag("shortcut_favorites")
                ) {
                    if (favorites.isNotEmpty()) {
                        viewModel.playSong(favorites.first(), favorites)
                    } else {
                        onNavigateToLibrary()
                    }
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Box(modifier = Modifier.weight(1f)) {
                QuickGridItem(
                    title = "الأكثر استماعاً",
                    subtitle = "تحليل محلي",
                    icon = Icons.Filled.BarChart,
                    tint = Color(0xFF00E5FF),
                    modifier = Modifier.testTag("shortcut_most_played")
                ) {
                    if (mostPlayedSongs.isNotEmpty()) {
                        viewModel.playSong(mostPlayedSongs.first(), mostPlayedSongs)
                    } else {
                        onNavigateToLibrary()
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.weight(1f)) {
                QuickGridItem(
                    title = "المشغلة حديثاً",
                    subtitle = "تاريخ الاستماع",
                    icon = Icons.Filled.History,
                    tint = Color(0xFFBD83FF),
                    modifier = Modifier.testTag("shortcut_recently")
                ) {
                    if (recentSongs.isNotEmpty()) {
                        viewModel.playSong(recentSongs.first(), recentSongs)
                    } else {
                        onNavigateToLibrary()
                    }
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Box(modifier = Modifier.weight(1f)) {
                QuickGridItem(
                    title = "ملفات الـ LRC",
                    subtitle = "كلمات متزامنة",
                    icon = Icons.Filled.LibraryMusic,
                    tint = Color(0xFF8BC34A),
                    modifier = Modifier.testTag("shortcut_lrc")
                ) {
                    val lrcSongs = allSongs.filter { it.hasLyrics }
                    if (lrcSongs.isNotEmpty()) {
                        viewModel.playSong(lrcSongs.first(), lrcSongs)
                    } else {
                        onNavigateToLibrary()
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 2. Artists For You (Horizontal circular view)
        Text(
            text = "فنانين من أجلك / Artists For You",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Gather dynamic top artists from smart stats tracker
        val artistsToDisplay = if (topArtists.isNotEmpty()) {
            topArtists
        } else {
            // fallback mock if library is not yet populated
            listOf(
                com.example.ui.ArtistStats("ألوان النور", playCount = 0, songCount = 1, color = Color(0xFF00ADB5)),
                com.example.ui.ArtistStats("لحن الهدوء الممزوج", playCount = 0, songCount = 1, color = Color(0xFFBD83FF)),
                com.example.ui.ArtistStats("نبضات الكوكب", playCount = 0, songCount = 1, color = Color(0xFFFF5722))
            )
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(artistsToDisplay) { artist ->
                Column(
                    modifier = Modifier
                        .width(100.dp)
                        .clickable { onArtistClick(artist.name) }
                        .testTag("artist_item_${artist.name.hashCode()}"),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Circle background with initial character
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(artist.color, artist.color.copy(alpha = 0.4f))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = artist.name.take(1),
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = artist.name,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${artist.songCount} مسار • ${artist.playCount} تشغيل",
                        color = TextMuted,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 3. Recently Played list
        if (recentSongs.isNotEmpty()) {
            Text(
                text = "تم تشغيلها مؤخراً / Recently Played",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(recentSongs) { song ->
                    AlbumArtVerticalItem(song) {
                        viewModel.playSong(song, recentSongs)
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // 4. All tracks Preview list
        Text(
            text = "اكتشف الموسيقى في جهازك / Discover Local",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (allSongs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassmorphic(cornerRadius = 12.dp)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.FolderOpen,
                        contentDescription = "No tracks",
                        tint = TextMuted,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "لم يتم الكشف عن أي ملفات .mp3 بعد",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.seedDemoTracks() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00ADB5)),
                        modifier = Modifier.testTag("seed_tracks_button")
                    ) {
                        Text("توليد ملفات وبطاقات تجريبية")
                    }
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                allSongs.take(5).forEach { song ->
                    CompactSongRow(song) {
                        viewModel.playSong(song, allSongs)
                    }
                }
            }
        }

    }
}

@Composable
fun AnimatedScanStatusCard(viewModel: MusicPlayerViewModel) {
    val isScanning by viewModel.isScanning.collectAsState()
    val scanResultMessage by viewModel.scanResultMessage.collectAsState()

    if (scanResultMessage != null) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .glassmorphic(cornerRadius = 16.dp, alpha = 0.12f),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isScanning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = NeonAccent,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = "Scan status",
                            tint = NeonAccent
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = scanResultMessage ?: "",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { viewModel.dismissScanMessage() }) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close indicator",
                            tint = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuickGridItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .glassmorphic(cornerRadius = 14.dp, alpha = 0.08f)
            .clickable(onClick = onClick)
            .padding(12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(tint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = subtitle,
                    color = TextMuted,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun AlbumArtVerticalItem(
    song: SongEntity,
    onClick: () -> Unit
) {
    val hash = song.title.hashCode()
    val itemThemeColor = when (hash % 3) {
        0 -> Color(0xFF00ADB5)
        1 -> Color(0xFFBD83FF)
        else -> Color(0xFFFF5722)
    }

    Column(
        modifier = Modifier
            .width(130.dp)
            .clickable(onClick = onClick)
            .testTag("album_card_${song.title.hashCode()}")
    ) {
        // Pseudo Album Cover with Glass Gradient looks majestic!
        Box(
            modifier = Modifier
                .size(130.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.radialGradient(
                        colors = listOf(itemThemeColor.copy(alpha = 0.6f), Color.Transparent),
                        radius = 280f
                    )
                )
                .glassmorphic(cornerRadius = 16.dp, alpha = 0.15f),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = null,
                tint = itemThemeColor,
                modifier = Modifier.size(54.dp)
            )

            // Dynamic lyric badge
            if (song.hasLyrics) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("LRC", color = NeonAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = song.title,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = song.artist,
            color = TextMuted,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun CompactSongRow(
    song: SongEntity,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .glassmorphic(cornerRadius = 12.dp, alpha = 0.05f)
            .clickable(onClick = onClick)
            .padding(12.dp)
            .testTag("compact_song_${song.title.hashCode()}"),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    color = TextMuted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (song.hasLyrics) {
                Icon(
                    imageVector = Icons.Filled.TextFormat,
                    contentDescription = "Lyrics available",
                    tint = NeonAccent.copy(alpha = 0.8f),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Text(
                text = "${song.fileSize / 1024 / 1024}MB",
                color = TextMuted,
                fontSize = 11.sp
            )
        }
    }
}

data class ArtistMock(
    val name: String,
    val genre: String,
    val color: Color
)
