package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.PaletteHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * An adaptive Compose component that loads embedded album art from local audio files 
 * asynchronously and provides a gorgeous glassmorphic fallback placeholder if none exists.
 */
@Composable
fun AlbumArtView(
    filePath: String,
    title: String,
    artist: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    isCircular: Boolean = false
) {
    var bitmap by remember(filePath) { mutableStateOf<Bitmap?>(null) }
    var isLoaded by remember(filePath) { mutableStateOf(false) }

    LaunchedEffect(filePath) {
        withContext(Dispatchers.IO) {
            val art = PaletteHelper.extractEmbeddedArt(filePath)
            bitmap = art
            isLoaded = true
        }
    }

    val shape = if (isCircular) CircleShape else RoundedCornerShape(12.dp)

    Box(
        modifier = modifier
            .clip(shape)
            .border(
                width = 1.2.dp, 
                color = Color.White.copy(alpha = 0.08f), 
                shape = shape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = "Album Art for $title",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            // Glassmorphic / procedural gorgeous fallback placeholder
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.10f),
                                Color.White.copy(alpha = 0.02f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.MusicNote,
                        contentDescription = "No Artwork",
                        tint = accentColor.copy(alpha = 0.65f),
                        modifier = Modifier.size(
                            if (isCircular) 32.dp else 24.dp
                        )
                    )
                    
                    if (!isCircular) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (title.isNotEmpty()) title.take(1).uppercase() else "M",
                            color = Color.White.copy(alpha = 0.35f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
