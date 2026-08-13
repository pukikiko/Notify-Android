package com.notify.android.tv.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.structuralEqualityPolicy

/** Simple screen stack for the TV app. */
sealed class TvScreen {
    object Home : TvScreen()
    object Search : TvScreen()
    object Library : TvScreen()
    object LikedSongs : TvScreen()
    data class Playlist(val id: String) : TvScreen()
    data class Artist(val id: String) : TvScreen()
    data class Album(val id: String) : TvScreen()
    object NowPlaying : TvScreen()
    object Settings : TvScreen()
}

class TvNavState {
    var stack by mutableStateOf(listOf<TvScreen>(TvScreen.Home), structuralEqualityPolicy())
        private set

    val current: TvScreen get() = stack.last()

    fun navigate(screen: TvScreen) {
        if (stack.last() == screen) return
        if (screen is TvScreen.Playlist || screen is TvScreen.Artist ||
            screen is TvScreen.Album || screen is TvScreen.LikedSongs ||
            screen is TvScreen.NowPlaying
        ) {
            stack = stack + screen
        } else {
            // Top-level destinations replace the stack
            stack = listOf(screen)
        }
    }

    fun back() {
        if (stack.size > 1) stack = stack.dropLast(1)
    }
}
