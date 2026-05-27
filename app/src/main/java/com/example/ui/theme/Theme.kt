package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val SpaceBlack = Color(0xFF040408)
val GlassWhite = Color(0x1AFFFFFF)
val GlassWhiteFocused = Color(0x2BFFFFFF)
val TextWhite = Color(0xFFFFFFFF)
val TextGrey = Color(0xFF9092A3)

val PurpleGlow = Color(0xFFBD83FF)
val CyanGlow = Color(0xFF00ADB5)

private val DarkColorScheme = darkColorScheme(
    primary = PurpleGlow,
    secondary = CyanGlow,
    background = SpaceBlack,
    surface = Color(0xFF0D0D14),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = TextWhite,
    onSurface = TextWhite
)

@Composable
fun PlayerMusicTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
