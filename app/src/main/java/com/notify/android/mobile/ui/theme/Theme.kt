package com.notify.android.mobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val NotifyPurple = Color(0xFF8F5CFF)
val NotifyPurpleBright = Color(0xFFA67BFF)
val SpotifyBlack = Color(0xFF121212)
val SpotifyBlack2 = Color(0xFF000000)
val SpotifySurface = Color(0xFF181818)
val SpotifySurfaceHigh = Color(0xFF282828)
val SpotifyText = Color(0xFFFFFFFF)
val SpotifyTextSub = Color(0xFFB3B3B3)

private val DarkColors = darkColorScheme(
    primary = NotifyPurple,
    onPrimary = SpotifyBlack,
    secondary = NotifyPurpleBright,
    background = SpotifyBlack,
    onBackground = SpotifyText,
    surface = SpotifySurface,
    onSurface = SpotifyText,
    surfaceVariant = SpotifySurfaceHigh,
    onSurfaceVariant = SpotifyTextSub,
    error = Color(0xFFE91429)
)

@Composable
fun NotifyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = Typography,
        content = content
    )
}
