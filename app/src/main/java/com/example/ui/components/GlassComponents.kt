package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
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

    // Smooth color transitions with tween details to prevent abrupt jumps or UI flashes (1000ms transition)
    val animatedDominant by animateColorAsState(
        targetValue = targetDominantColor,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "AnimDominantColor"
    )
    val animatedVibrant by animateColorAsState(
        targetValue = targetVibrantColor,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
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
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        animatedDominant.copy(alpha = 0.9f),
                        animatedVibrant.copy(alpha = 0.9f)
                    )
                )
            )
    ) {
        // 1. Correctly cropped, blurred, non-distorted album artwork background (Glassmorphism blur is preserved)
        ambientCover?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(50.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
            )
        }

        // 2. Dynamic glowing bubbles for visual depth
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
                                animatedDominant.copy(alpha = 0.22f),
                                animatedDominant.copy(alpha = 0.07f),
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
                                animatedVibrant.copy(alpha = 0.25f),
                                animatedVibrant.copy(alpha = 0.06f),
                                Color.Transparent
                            ),
                            center = Offset(width * 0.8f, height * 0.75f),
                            radius = width * pulseScale2 * 2.5f
                        )
                    )
                }
        )

        // 3. Perfect transparent dark overlay scrim (28% opacity) above base gradient to ensure maximum contrast and clear text readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SpaceBlack.copy(alpha = 0.28f))
        )

        content()
    }
}
