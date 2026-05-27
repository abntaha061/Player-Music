package com.example.util

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.palette.graphics.Palette

object PaletteHelper {
    /**
     * Extracts dominant and vibrant colors from [bitmap] using Palette API.
     * Falls back to default colors if bitmap is null or extraction fails.
     */
    fun extractColors(
        bitmap: Bitmap?,
        defaultDominant: Color,
        defaultVibrant: Color
    ): Pair<Color, Color> {
        if (bitmap == null) return Pair(defaultDominant, defaultVibrant)
        return try {
            val palette = Palette.from(bitmap).generate()
            val domColor = palette.getDominantColor(defaultDominant.toArgb())
            val vibColor = palette.getVibrantColor(defaultVibrant.toArgb())
            Pair(Color(domColor), Color(vibColor))
        } catch (e: Exception) {
            Pair(defaultDominant, defaultVibrant)
        }
    }
}
