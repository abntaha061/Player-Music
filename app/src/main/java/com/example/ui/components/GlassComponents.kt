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
 * Animated ambient backing glow simulating lights breathing in a center-focused radial gradient.
 * Uses a gorgeous 15s breathing loop (pulsing radius & subtle center shift) for visual depth.
 */
@Composable
fun AnimatedBreathingRadialGradient(
    animatedDominant: Color,
    animatedVibrant: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "BreathingBackground")

    // Pulsing radius scale: pulses from 0.60f to 0.90f of the screen size every 15 seconds
    val radiusPulse by infiniteTransition.animateFloat(
        initialValue = 0.60f,
        targetValue = 0.90f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "RadiusPulse"
    )

    // Breathing offset shifts around center of screen (subtle ±30 to 45 pixels loop)
    val centerShiftX by infiniteTransition.animateFloat(
        initialValue = -35f,
        targetValue = 35f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "CenterShiftX"
    )

    val centerShiftY by infiniteTransition.animateFloat(
        initialValue = -45f,
        targetValue = 45f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "CenterShiftY"
    )

    Canvas(
        modifier = modifier
            .fillMaxSize()
    ) {
        val width = size.width
        val height = size.height
        val center = Offset(width / 2f, height / 2f)

        // 1. Dominant color radial gradient centered closely around the middle of the screen
        val dominantRadius = maxOf(width, height) * radiusPulse * 0.70f
        val dominantCenter = Offset(
            center.x + centerShiftX,
            center.y + centerShiftY
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    animatedDominant.copy(alpha = 0.35f),
                    animatedDominant.copy(alpha = 0.12f),
                    Color.Transparent
                ),
                center = dominantCenter,
                radius = dominantRadius
            ),
            center = dominantCenter,
            radius = dominantRadius
        )

        // 2. Vibrant color radial gradient, slightly larger & pulsing in reverse phase, near the middle
        val vibrantRadius = maxOf(width, height) * (1.50f - radiusPulse) * 0.65f
        val vibrantCenter = Offset(
            center.x - centerShiftX,
            center.y - centerShiftY
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    animatedVibrant.copy(alpha = 0.40f),
                    animatedVibrant.copy(alpha = 0.08f),
                    Color.Transparent
                ),
                center = vibrantCenter,
                radius = vibrantRadius
            ),
            center = vibrantCenter,
            radius = vibrantRadius
        )
    }
}

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceBlack)
    ) {
        // 1. Correctly cropped, blurred, non-distorted album artwork background (Glassmorphism blur is preserved)
        ambientCover?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
            )
        }

        // 2. Animated Breathing Radial Gradient exactly centered
        AnimatedBreathingRadialGradient(
            animatedDominant = animatedDominant,
            animatedVibrant = animatedVibrant,
            modifier = Modifier.fillMaxSize()
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
