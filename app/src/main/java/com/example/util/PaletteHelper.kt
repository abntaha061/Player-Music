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
     * Fast & lightweight downsampling color picker to extract the primary dominant color from a Bitmap.
     */
    fun extractDominantColor(bitmap: Bitmap): Color {
        return try {
            val width = bitmap.width
            val height = bitmap.height
            // Use standard sample points across the image to calculate average dominant tone
            var rSum = 0L
            var gSum = 0L
            var bSum = 0L
            var count = 0

            // Sample highly to grab true dominant color
            val stepX = (width / 8).coerceAtLeast(1)
            val stepY = (height / 8).coerceAtLeast(1)

            for (x in 0 until width step stepX) {
                for (y in 0 until height step stepY) {
                    val pixel = bitmap.getPixel(x, y)
                    val alpha = (pixel shr 24) and 0xFF
                    if (alpha > 180) { // Keep highly non-transparent colors
                        rSum += (pixel shr 16) and 0xFF
                        gSum += (pixel shr 8) and 0xFF
                        bSum += pixel and 0xFF
                        count++
                    }
                }
            }

            if (count > 0) {
                Color(
                    red = (rSum / count).toInt(),
                    green = (gSum / count).toInt(),
                    blue = (bSum / count).toInt()
                )
            } else {
                Color(0xFFBD83FF) // Violet fallback
            }
        } catch (e: Exception) {
            Color(0xFFBD83FF) // Default gorgeous Purple fallback
        }
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
