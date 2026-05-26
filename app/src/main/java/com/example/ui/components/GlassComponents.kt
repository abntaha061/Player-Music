package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.BlurredEdgeTreatment
import com.example.ui.MusicPlayerViewModel
import com.example.ui.theme.GlassBg
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.SpaceBlack

/**
 * Custom Modifier for simulating a beautiful Glassmorphic translucent panel
 */
fun Modifier.glassmorphic(
    cornerRadius: Dp = 16.dp,
    borderWidth: Dp = 1.dp,
    alpha: Float = 0.08f,
    borderColorAlpha: Float = 0.15f
): Modifier = this
    .clip(RoundedCornerShape(cornerRadius))
    .background(Color.White.copy(alpha = alpha))
    .border(
        width = borderWidth,
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = borderColorAlpha + 0.05f),
                Color.White.copy(alpha = borderColorAlpha / 2)
            )
        ),
        shape = RoundedCornerShape(cornerRadius)
    )

/**
 * Animated ambient backing glow simulating lights breathing and bleeding behind the glass.
 * Uses dynamic track-derived palette extraction to update the background atmosphere on track changes.
 */
@Composable
fun AmbientGlassBackground(
    viewModel: MusicPlayerViewModel,
    content: @Composable () -> Unit
) {
    val targetDominantColor by viewModel.trackDominantColor.collectAsState()
    val targetVibrantColor by viewModel.trackVibrantColor.collectAsState()
    val ambientCover by viewModel.trackAmbientCover.collectAsState()

    // Smooth color transitions with tween details to prevent abrupt jumps or UI flashes
    val animatedDominant by animateColorAsState(
        targetValue = targetDominantColor,
        animationSpec = tween(1400, easing = LinearEasing),
        label = "AnimDominantColor"
    )
    val animatedVibrant by animateColorAsState(
        targetValue = targetVibrantColor,
        animationSpec = tween(1400, easing = LinearEasing),
        label = "AnimVibrantColor"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "AmbientGlow")

    val pulseScale1 by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Pulse1"
    )

    val pulseScale2 by infiniteTransition.animateFloat(
        initialValue = 0.12f,
        targetValue = 0.24f,
        animationSpec = infiniteRepeatable(
            animation = tween(11000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Pulse2"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceBlack)
    ) {
        // 1. Correctly cropped, blurred, non-distorted album artwork background
        ambientCover?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(60.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
            )
        }

        // 2. Dynamic glowing bubbles and heavy dark overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val width = size.width
                    val height = size.height

                    // Glowing dynamic dominant light bubble
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                animatedDominant.copy(alpha = 0.35f),
                                animatedDominant.copy(alpha = 0.12f),
                                Color.Transparent
                            ),
                            center = Offset(width * 0.25f, height * 0.3f),
                            radius = width * pulseScale1 * 2f
                        )
                    )

                    // Glowing dynamic vibrant sibling light bubble
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                animatedVibrant.copy(alpha = 0.38f),
                                animatedVibrant.copy(alpha = 0.08f),
                                Color.Transparent
                            ),
                            center = Offset(width * 0.8f, height * 0.75f),
                            radius = width * pulseScale2 * 2.5f
                        )
                    )

                    // Subtle center deep accent
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                animatedDominant.copy(alpha = 0.18f),
                                animatedVibrant.copy(alpha = 0.05f),
                                Color.Transparent
                            ),
                            center = Offset(width * 0.5f, height * 0.5f),
                            radius = width * 0.35f
                        )
                    )

                    // Heavy dark overlay to ensure maximum contrast and clear readability
                    drawRect(
                        color = SpaceBlack.copy(alpha = 0.72f)
                    )
                }
        )

        content()
    }
}
