package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.LyricLine
import com.example.ui.Song
import com.example.ui.theme.*

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    focused: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val backgroundColor = if (focused) GlassWhiteFocused else GlassWhite
    val borderBrush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.15f),
            Color.White.copy(alpha = 0.03f)
        )
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(backgroundColor)
            .border(1.dp, borderBrush, RoundedCornerShape(24.dp))
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = rememberRipple(),
                        onClick = onClick
                    )
                } else Modifier
            )
            .padding(16.dp),
        content = content
    )
}


@Composable
fun CrispAlbumArt(
    song: Song,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "DiskRotate")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(22000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "DiskAngle"
    )

    val scale by animateFloatAsState(
        targetValue = if (isPlaying) 1.0f else 0.92f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "ScaleArt"
    )

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(12.dp)
            .rotate(if (isPlaying) rotationAngle else 0f)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        song.dominantColor.copy(alpha = 0.9f),
                        song.vibrantColor.copy(alpha = 0.6f),
                        Color(0xFF0F0F1A)
                    )
                )
            )
            .border(
                6.dp,
                Brush.linearGradient(
                    listOf(Color.White.copy(alpha = 0.12f), Color.Black.copy(alpha = 0.8f))
                ),
                CircleShape
            )
            .padding(16.dp)
            .border(2.dp, Color.White.copy(alpha = 0.08f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // Disk grooves
        Canvas(modifier = Modifier.fillMaxSize()) {
            val r = size.minDimension / 2f
            for (i in 1..8) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.015f * i),
                    radius = r * (i / 10f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                )
            }
        }

        // Center visual core
        Box(
            modifier = Modifier
                .fillMaxSize(0.38f)
                .clip(CircleShape)
                .background(Color(0xFF04040A))
                .border(3.dp, Color.White.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = null,
                tint = song.vibrantColor,
                modifier = Modifier.fillMaxSize(0.5f)
            )
        }
    }
}

@Composable
fun GlassIconButton(
    imageVector: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Int = 24,
    enabled: Boolean = true,
    tint: Color = TextWhite
) {
    Box(
        modifier = modifier
            .size(54.dp)
            .clip(CircleShape)
            .background(GlassWhite)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = if (enabled) tint else tint.copy(alpha = 0.3f),
            modifier = Modifier.size(iconSize.dp)
        )
    }
}

@Composable
fun SyncedLyricsView(
    lyrics: List<LyricLine>,
    currentTimeMs: Long,
    onLineClick: (LyricLine) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val activeLineIndex = remember(lyrics, currentTimeMs) {
        var activeIdx = 0
        for (i in lyrics.indices.reversed()) {
            if (currentTimeMs >= lyrics[i].timeMs) {
                activeIdx = i
                break
            }
        }
        activeIdx
    }

    // Scroll to the active lyric line automatically
    LaunchedEffect(activeLineIndex) {
        if (lyrics.isNotEmpty() && activeLineIndex in lyrics.indices) {
            listState.animateScrollToItem(
                index = (activeLineIndex - 2).coerceAtLeast(0),
                scrollOffset = 0
            )
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        itemsIndexed(lyrics) { index, line ->
            val isActive = index == activeLineIndex
            val alpha by animateFloatAsState(
                targetValue = if (isActive) 1f else 0.45f,
                animationSpec = tween(400),
                label = "LyricAlpha"
            )
            val scale by animateFloatAsState(
                targetValue = if (isActive) 1.08f else 0.95f,
                animationSpec = spring(stiffness = Spring.StiffnessLow),
                label = "LyricScale"
            )
            val textColor by animateColorAsState(
                targetValue = if (isActive) TextWhite else TextGrey,
                animationSpec = tween(400),
                label = "LyricColor"
            )

            Text(
                text = line.text,
                color = textColor,
                fontSize = if (isActive) 20.sp else 16.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onLineClick(line) }
                    .padding(vertical = 12.dp, horizontal = 24.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    }
            )
        }
    }
}


