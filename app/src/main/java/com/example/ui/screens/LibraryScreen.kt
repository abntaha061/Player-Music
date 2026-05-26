package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.SongEntity
import com.example.ui.MusicPlayerViewModel
import com.example.ui.components.AlbumArtView
import com.example.ui.components.glassmorphic
import com.example.ui.theme.NeonAccent
import com.example.ui.theme.TextMuted

@Composable
fun LibraryScreen(viewModel: MusicPlayerViewModel) {
    val allSongs by viewModel.allSongs.collectAsState()
    var selectedSongForInfo by remember { mutableStateOf<SongEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "مكتبة الموسيقى / My Library",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${allSongs.size} ملفات صوتية محلية تم كشفها",
                    color = TextMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            IconButton(
                onClick = { viewModel.scanFiles() },
                modifier = Modifier
                    .testTag("library_scan_button")
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.08f))
            ) {
                Icon(
                    imageVector = Icons.Filled.Loop,
                    contentDescription = "Scan Storage",
                    tint = NeonAccent
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (allSongs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .glassmorphic(cornerRadius = 16.dp)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.LibraryMusic,
                        contentDescription = "Empty",
                        tint = TextMuted,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "مكتبتك خاوية حتى الآن",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "تأكد من وجود ملفات موسيقى بمجلد /Music/ بالنظام، أو اضغط على الزر بالأسفل لتوليد أغاني تجريبية متكاملة.",
                        color = TextMuted,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.seedDemoTracks() },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonAccent),
                        modifier = Modifier.testTag("seed_tracks_button_library")
                    ) {
                        Text("توليد ملفات وبطاقات تجريبية", color = Color.Black)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(allSongs) { index, song ->
                    LibrarySongRow(
                        song = song,
                        index = index + 1,
                        onPlay = { viewModel.playSong(song, allSongs) },
                        onFavoriteToggle = { viewModel.toggleFavorite(song) },
                        onInfoClick = { selectedSongForInfo = song }
                    )
                }
            }
        }

    }

    // Song Info and Cryptographic Hash integrity Dialog
    selectedSongForInfo?.let { song ->
        IntegrityVerificationDialog(song = song) {
            selectedSongForInfo = null
        }
    }
}

@Composable
fun LibrarySongRow(
    song: SongEntity,
    index: Int,
    onPlay: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onInfoClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassmorphic(cornerRadius = 12.dp, alpha = 0.05f)
            .clickable(onClick = onPlay)
            .padding(12.dp)
            .testTag("library_song_row_${song.title.hashCode()}"),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Index number
            Text(
                text = index.toString().padStart(2, '0'),
                color = TextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(24.dp)
            )

            AlbumArtView(
                filePath = song.filePath,
                title = song.title,
                artist = song.artist,
                accentColor = NeonAccent,
                modifier = Modifier.size(40.dp)
            )

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
                    imageVector = Icons.Filled.Mic,
                    contentDescription = "Synced Lyrics Included",
                    tint = NeonAccent,
                    modifier = Modifier
                        .size(16.dp)
                        .padding(end = 4.dp)
                )
            }

            IconButton(onClick = onFavoriteToggle) {
                Icon(
                    imageVector = if (song.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = "Favorite Toggle",
                    tint = if (song.isFavorite) Color(0xFFFF5722) else Color.White.copy(alpha = 0.6f)
                )
            }

            IconButton(onClick = onInfoClick) {
                Icon(
                    imageVector = Icons.Filled.Security,
                    contentDescription = "Integrity Hash Check",
                    tint = NeonAccent.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun IntegrityVerificationDialog(song: SongEntity, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassmorphic(cornerRadius = 20.dp, alpha = 0.2f)
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.VerifiedUser,
                        contentDescription = "Integrity Verified",
                        tint = NeonAccent,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "التحقق من سلامة البيانات\nData Integrity Verification",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                HorizontalDivider(
                    color = Color.White.copy(alpha = 0.15f),
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                // Song Title Details
                Text(
                    text = song.title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "الفنان: ${song.artist}",
                    color = TextMuted,
                    fontSize = 11.sp,
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Metadata details
                Text(
                    text = "خصائص وموقع الملف / File Properties:",
                    color = NeonAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                PropertyRow(label = "مسار الملف (Path)", value = song.filePath)
                PropertyRow(label = "الحجم (Size)", value = "${String.format("%.2f", song.fileSize.toDouble() / 1024 / 1024)} MB (${song.fileSize} Bytes)")
                PropertyRow(label = "عدد مرات التشغيل", value = "${song.playCount} مرات")
                PropertyRow(label = "الكلمات المتزامنة", value = if (song.hasLyrics) "متوفرة ونشطة (.lrc)" else "غير متوفرة")

                Spacer(modifier = Modifier.height(16.dp))

                // Cryptographic Checksums
                Text(
                    text = "بصمات التوقيع والتحقق الهيكلي / Cryptographic Checksums:",
                    color = NeonAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                
                ChecksumField(label = "MD5", hash = song.checksumMd5)
                ChecksumField(label = "SHA-1", hash = song.checksumSha1)
                ChecksumField(label = "SHA-256", hash = song.checksumSha256)

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonAccent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("dialog_dismiss_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "إغلاق / Close",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun PropertyRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = label, color = TextMuted, fontSize = 10.sp)
        Text(
            text = value,
            color = Color.White,
            fontSize = 11.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun ChecksumField(label: String, hash: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = NeonAccent.copy(alpha = 0.5f),
                modifier = Modifier.size(10.dp)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .padding(6.dp)
        ) {
            Text(
                text = if (hash.isEmpty()) "جاري المعالجة..." else hash,
                color = Color.White,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
