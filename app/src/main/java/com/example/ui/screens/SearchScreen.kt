package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SongEntity
import com.example.ui.MusicPlayerViewModel
import com.example.ui.components.glassmorphic
import com.example.ui.theme.NeonAccent
import com.example.ui.theme.TextMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(viewModel: MusicPlayerViewModel) {
    val allSongs by viewModel.allSongs.collectAsState()
    var searchUrlText by remember { mutableStateOf("") }

    val filteredSongs = remember(searchUrlText, allSongs) {
        if (searchUrlText.trim().isEmpty()) {
            allSongs
        } else {
            allSongs.filter {
                it.title.contains(searchUrlText, ignoreCase = true) ||
                it.artist.contains(searchUrlText, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "البحث الموسيقي / Sound Search",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "اكتشف المسارات وربط ملفات LRC",
            color = TextMuted,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 2.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Glass Search Input Field
        TextField(
            value = searchUrlText,
            onValueChange = { searchUrlText = it },
            placeholder = { Text("ابحث باسم الأغنية أو الفنان...", color = TextMuted, fontSize = 14.sp) },
            leadingIcon = { Icon(imageVector = Icons.Filled.Search, contentDescription = null, tint = NeonAccent) },
            trailingIcon = {
                if (searchUrlText.isNotEmpty()) {
                    IconButton(onClick = { searchUrlText = "" }) {
                        Icon(imageVector = Icons.Filled.Clear, contentDescription = "Clear search", tint = Color.White)
                    }
                }
            },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White.copy(alpha = 0.08f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.04f),
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = NeonAccent,
                unfocusedIndicatorColor = Color.White.copy(alpha = 0.15f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .glassmorphic(cornerRadius = 12.dp)
                .testTag("search_input_field")
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (filteredSongs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .glassmorphic(cornerRadius = 16.dp)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "لا توجد نتائج مطابقة لمصطلح البحث",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "جرب البحث عن 'عين' أو 'عتمة' أو 'أمل' للكشف عن الأغاني التجريبية.",
                        color = TextMuted,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredSongs) { song ->
                    CompactSongRow(song = song) {
                        viewModel.playSong(song, filteredSongs)
                    }
                }
            }
        }

    }
}
