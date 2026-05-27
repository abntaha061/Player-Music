package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.SpaceBlack

@Composable
fun AuroraBackground(
    animatedDominant: Color,
    animatedVibrant: Color,
    modifier: Modifier = Modifier
) {
    val dominantColor = if (animatedDominant != Color.Unspecified) animatedDominant else Color(0xFFBD83FF)
    val vibrantColor = if (animatedVibrant != Color.Unspecified) animatedVibrant else Color(0xFF00ADB5)

    // Derived colors using remember to prevent re-instantiation on every frame
    val thirdColor = remember(dominantColor, vibrantColor) {
        Color(
            red = (dominantColor.red * 0.4f + vibrantColor.red * 0.6f).coerceIn(0f, 1f),
            green = (dominantColor.green * 0.8f + vibrantColor.green * 0.2f).coerceIn(0f, 1f),
            blue = (dominantColor.blue * 0.2f + vibrantColor.blue * 0.8f).coerceIn(0f, 1f)
        )
    }

    val fourthColor = remember(dominantColor, vibrantColor) {
        Color(
            red = (dominantColor.red * 0.5f + vibrantColor.red * 0.5f).coerceIn(0f, 1f),
            green = (dominantColor.green * 0.3f + vibrantColor.green * 0.7f).coerceIn(0f, 1f),
            blue = (dominantColor.blue * 0.8f + vibrantColor.blue * 0.2f).coerceIn(0f, 1f)
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "AuroraMeshGradient")

    // Blob 1 (Dominant/Base) - slow breathing coordinates
    val floatX1 by infiniteTransition.animateFloat(
        initialValue = 0.20f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(16000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Blob1_X"
    )
    val floatY1 by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(18000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Blob1_Y"
    )
    val radiusScale1 by infiniteTransition.animateFloat(
        initialValue = 0.60f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Blob1_Radius"
    )

    // Blob 2 (Vibrant) - orbiting animation
    val floatX2 by infiniteTransition.animateFloat(
        initialValue = 0.80f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(21000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Blob2_X"
    )
    val floatY2 by infiniteTransition.animateFloat(
        initialValue = 0.70f,
        targetValue = 0.40f,
        animationSpec = infiniteRepeatable(
            animation = tween(17000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Blob2_Y"
    )
    val radiusScale2 by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 0.90f,
        animationSpec = infiniteRepeatable(
            animation = tween(19000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Blob2_Radius"
    )

    // Blob 3 (Third Color - Hybrid Accent)
    val floatX3 by infiniteTransition.animateFloat(
        initialValue = 0.75f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(23000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Blob3_X"
    )
    val floatY3 by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.50f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Blob3_Y"
    )
    val radiusScale3 by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 0.80f,
        animationSpec = infiniteRepeatable(
            animation = tween(17000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Blob3_Radius"
    )

    // Blob 4 (Fourth Color - Moody Accent)
    val floatX4 by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.60f,
        animationSpec = infiniteRepeatable(
            animation = tween(19000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Blob4_X"
    )
    val floatY4 by infiniteTransition.animateFloat(
        initialValue = 0.80f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(24000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Blob4_Y"
    )
    val radiusScale4 by infiniteTransition.animateFloat(
        initialValue = 0.50f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(22000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Blob4_Radius"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SpaceBlack)
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val width = size.width
            val height = size.height
            val maxDim = maxOf(width, height)

            // Draw Blob 1 (Dominant Background Glow)
            val center1 = Offset(width * floatX1, height * floatY1)
            val r1 = maxDim * radiusScale1 * 0.65f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        dominantColor.copy(alpha = 0.38f),
                        dominantColor.copy(alpha = 0.22f),
                        dominantColor.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = center1,
                    radius = r1
                ),
                center = center1,
                radius = r1,
                blendMode = BlendMode.Screen
            )

            // Draw Blob 2 (Vibrant Accent Glow)
            val center2 = Offset(width * floatX2, height * floatY2)
            val r2 = maxDim * radiusScale2 * 0.60f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        vibrantColor.copy(alpha = 0.42f),
                        vibrantColor.copy(alpha = 0.24f),
                        vibrantColor.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = center2,
                    radius = r2
                ),
                center = center2,
                radius = r2,
                blendMode = BlendMode.Screen
            )

            // Draw Blob 3 (Third Color - Transitioning glow field)
            val center3 = Offset(width * floatX3, height * floatY3)
            val r3 = maxDim * radiusScale3 * 0.55f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        thirdColor.copy(alpha = 0.35f),
                        thirdColor.copy(alpha = 0.18f),
                        thirdColor.copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    center = center3,
                    radius = r3
                ),
                center = center3,
                radius = r3,
                blendMode = BlendMode.Screen
            )

            // Draw Blob 4 (Fourth Color - Balanced contrast glow)
            val center4 = Offset(width * floatX4, height * floatY4)
            val r4 = maxDim * radiusScale4 * 0.52f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        fourthColor.copy(alpha = 0.30f),
                        fourthColor.copy(alpha = 0.15f),
                        fourthColor.copy(alpha = 0.04f),
                        Color.Transparent
                    ),
                    center = center4,
                    radius = r4
                ),
                center = center4,
                radius = r4,
                blendMode = BlendMode.Screen
            )
        }

        // Glass Scrim layer: Perfect dark contrast filter of 28% to guarantee ultra-sharp readable white text
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SpaceBlack.copy(alpha = 0.28f))
        )
    }
}

@Composable
fun AmbientGlassBackground(
    viewModel: com.example.ui.MusicPlayerViewModel,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val trackDominantColor by viewModel.trackDominantColor.collectAsState()
    val trackVibrantColor by viewModel.trackVibrantColor.collectAsState()

    AmbientGlassBackground(
        dominantColor = trackDominantColor,
        vibrantColor = trackVibrantColor,
        modifier = modifier,
        content = content
    )
}

@Composable
fun AmbientGlassBackground(
    dominantColor: Color,
    vibrantColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        AuroraBackground(
            animatedDominant = dominantColor,
            animatedVibrant = vibrantColor,
            modifier = Modifier.fillMaxSize()
        )
        content()
    }
}

