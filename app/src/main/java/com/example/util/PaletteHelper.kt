package com.example.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.media.MediaMetadataRetriever
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlin.math.abs

object PaletteHelper {

    /**
     * Tries to extract embedded album art from the MP3 file path.
     */
    fun extractEmbeddedArt(filePath: String): Bitmap? {
        var retriever: MediaMetadataRetriever? = null
        return try {
            retriever = MediaMetadataRetriever()
            retriever.setDataSource(filePath)
            val artBytes = retriever.embeddedPicture
            if (artBytes != null) {
                android.graphics.BitmapFactory.decodeByteArray(artBytes, 0, artBytes.size)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        } finally {
            try {
                retriever?.release()
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    /**
     * Extracts a balanced pair of dominant and vibrant/muted contrast colors from a Bitmap.
     * Uses the official Android Palette API with programmatic saturation/lightness boosting.
     */
    fun extractPalette(bitmap: Bitmap): Pair<Color, Color> {
        return try {
            val palette = androidx.palette.graphics.Palette.from(bitmap).generate()

            val primaryRGB = palette.vibrantSwatch?.rgb 
                ?: palette.darkVibrantSwatch?.rgb
                ?: palette.lightVibrantSwatch?.rgb
                ?: palette.dominantSwatch?.rgb
                ?: palette.mutedSwatch?.rgb
                ?: 0xFFBD83FF.toInt()

            val secondaryRGB = palette.dominantSwatch?.rgb 
                ?: palette.mutedSwatch?.rgb
                ?: palette.darkMutedSwatch?.rgb
                ?: palette.lightMutedSwatch?.rgb
                ?: palette.vibrantSwatch?.rgb
                ?: 0xFF00ADB5.toInt()

            // Programmatic saturation and lightness adjustment logic to make colors glow/neon
            fun adjustColor(colorInt: Int): Color {
                val hsv = FloatArray(3)
                android.graphics.Color.colorToHSV(colorInt, hsv)
                // Boost saturation to ensure color is vivid (at least 0.70, or +20% boost)
                hsv[1] = (hsv[1] * 1.25f).coerceIn(0.70f, 1.0f)
                // Boost brightness to ensure color glows (at least 0.65, or +15% boost)
                hsv[2] = (hsv[2] * 1.15f).coerceIn(0.65f, 0.95f)
                return Color(android.graphics.Color.HSVToColor(hsv))
            }

            var primaryColor = adjustColor(primaryRGB)
            var secondaryColor = adjustColor(secondaryRGB)

            // If primary and secondary colors are too similar, rotate hue of secondary to create a gorgeous gradient
            val hsvP = FloatArray(3)
            val hsvS = FloatArray(3)
            android.graphics.Color.colorToHSV(primaryRGB, hsvP)
            android.graphics.Color.colorToHSV(secondaryRGB, hsvS)
            
            val hueDiff = abs(hsvP[0] - hsvS[0])
            val isTooSimilar = hueDiff < 30f || (primaryRGB == secondaryRGB)
            if (isTooSimilar) {
                // Rotate the hue of secondary color by 45 degrees to find a lovely contrast color
                val newHue = (hsvP[0] + 45f) % 360f
                val boostedColorInt = android.graphics.Color.HSVToColor(floatArrayOf(newHue, 0.75f, 0.85f))
                secondaryColor = Color(boostedColorInt)
            }

            Pair(primaryColor, secondaryColor)
        } catch (e: Exception) {
            Pair(Color(0xFFBD83FF), Color(0xFF00ADB5))
        }
    }

    /**
     * Fast & lightweight downsampling color picker to extract the primary dominant color from a Bitmap.
     */
    fun extractDominantColor(bitmap: Bitmap): Color {
        return extractPalette(bitmap).first
    }

    /**
     * Fallback high-fidelity color picker based on title and artist hash.
     */
    fun getDeterministicColor(title: String, artist: String): Color {
        val hash = abs((title + artist).hashCode())
        val colors = listOf(
            Color(0xFF00ADB5), // Dynamic Neon Cyan
            Color(0xFFBD83FF), // Neon Violet/Purple
            Color(0xFFFF5722), // Deep Flame Orange
            Color(0xFFF43F5E), // Vivid Rose/Pink
            Color(0xFF10B981), // Synth Emerald Green
            Color(0xFF3B82F6), // Azure Blue
            Color(0xFFFBBF24), // Vibrant Gold
            Color(0xFF8B5CF6)  // Cosmic Indigo
        )
        return colors[hash % colors.size]
    }

    /**
     * Generates a majestic 128x128 abstract gradient pattern bitmap representing the song's vibe!
     * Acts as the virtual album art so we always have beautifully rich art for our custom synth tracks.
     */
    fun generateProceduralArt(title: String, artist: String): Bitmap {
        val bitmap = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()

        // Get key colors
        val primaryColor = getDeterministicColor(title, artist)
        val secondaryColor = getDeterministicColor(artist, title)

        // Draw deep base fill
        canvas.drawColor(0xFF05050A.toInt())

        // Draw a glowing sweeping color circle in the center
        paint.isAntiAlias = true
        val radius = 90f
        val centerX = 64f
        val centerY = 64f

        val radialGradient = RadialGradient(
            centerX,
            centerY,
            radius,
            intArrayOf(primaryColor.toArgb(), secondaryColor.toArgb(), 0x00000000),
            floatArrayOf(0.0f, 0.45f, 1.0f),
            Shader.TileMode.CLAMP
        )
        paint.shader = radialGradient
        canvas.drawRect(0f, 0f, 128f, 128f, paint)

        // Draw subtle accent bubbles or ring to enrich visual detail
        paint.shader = null
        paint.color = primaryColor.copy(alpha = 0.25f).toArgb()
        canvas.drawCircle(40f, 40f, 30f, paint)

        paint.color = secondaryColor.copy(alpha = 0.15f).toArgb()
        canvas.drawCircle(88f, 88f, 35f, paint)

        return bitmap
    }
}
