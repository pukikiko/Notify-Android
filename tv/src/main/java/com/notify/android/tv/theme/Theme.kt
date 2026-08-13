package com.notify.android.tv.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val NotifyPurple = Color(0xFF8F5CFF)
val SpotifyBlack = Color(0xFF121212)
val SpotifySurface = Color(0xFF181818)
val SpotifySurfaceHigh = Color(0xFF282828)
val SpotifyText = Color(0xFFFFFFFF)
val SpotifyTextSub = Color(0xFFB3B3B3)

private val TvColors = darkColorScheme(
    primary = NotifyPurple,
    onPrimary = SpotifyBlack,
    background = SpotifyBlack,
    onBackground = SpotifyText,
    surface = SpotifySurface,
    onSurface = SpotifyText,
    surfaceVariant = SpotifySurfaceHigh,
    onSurfaceVariant = SpotifyTextSub
)

@Composable
fun NotifyTvTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TvColors,
        content = content
    )
}
