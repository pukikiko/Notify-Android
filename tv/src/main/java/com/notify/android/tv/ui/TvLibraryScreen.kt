package com.notify.android.tv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notify.android.tv.ui.components.TvCard
import com.notify.android.tv.ui.components.TvSectionTitle
import com.notify.android.tv.ui.components.TvTrackList
import com.notify.core.ui.imageUrl
import com.notify.core.ui.libraryViewModel
import com.notify.core.ui.player.PlayerViewModel
import com.notify.core.ui.playlistsViewModel

/** Spotify TV library: unified panel of "Liked Songs" + playlists, mirroring the web app. */
@Composable
fun TvLibraryScreen(playerVm: PlayerViewModel, nav: TvNavState) {
    val libVm = libraryViewModel()
    val tracks by libVm.tracks.collectAsState()
    val playlistsVm = playlistsViewModel()
    val playlists by playlistsVm.playlists.collectAsState()

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Text(
                "Your Library",
                color = Color.White,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 40.dp, top = 28.dp)
            )
        }

        item {
            LazyRow(contentPadding = PaddingValues(horizontal = 40.dp)) {
                items(listOf(Unit)) {
                    TvLikedSongsCard(
                        count = tracks?.size ?: 0,
                        width = 212.dp,
                        onClick = { nav.navigate(TvScreen.LikedSongs) }
                    )
                }
            }
        }
        if (!playlists.isNullOrEmpty()) {
            item { TvSectionTitle("Your playlists") }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 40.dp)) {
                    items(playlists.orEmpty()) { p ->
                        TvCard(
                            imageUrl = p.image?.let { imageUrl(it) },
                            title = p.name,
                            subtitle = "Playlist • ${p.trackCount ?: 0} songs",
                            onClick = { nav.navigate(TvScreen.Playlist(p.id)) },
                            width = 212.dp
                        )
                    }
                }
            }
        }
        if (playlists.isNullOrEmpty()) {
            item { EmptyTv("No playlists yet. Create one to get started.") }
        }
    }
}

/** Liked Songs detail: flat list of your liked tracks. */
@Composable
fun TvLikedSongsScreen(playerVm: PlayerViewModel) {
    val vm = libraryViewModel()
    val tracks by vm.tracks.collectAsState()

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Text(
                "Liked Songs",
                color = Color.White,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 40.dp, top = 28.dp)
            )
            Text(
                "${tracks?.size ?: 0} songs",
                color = Color(0xFFB3B3B3),
                fontSize = 16.sp,
                modifier = Modifier.padding(start = 40.dp, top = 4.dp)
            )
        }

        if (tracks.isNullOrEmpty()) {
            item { EmptyTv("No liked tracks yet. Tap the heart on any track.") }
        } else {
            item {
                TvTrackList(
                    tracks = tracks.orEmpty(),
                    playerVm = playerVm
                )
            }
        }
    }
}

@Composable
private fun TvLikedSongsCard(count: Int, width: Dp = 212.dp, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .width(width)
            .background(
                if (focused) Color.White.copy(alpha = 0.18f) else Color.Transparent,
                RoundedCornerShape(12.dp)
            )
            .border(
                width = if (focused) 4.dp else 0.dp,
                color = if (focused) Color.White else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .onFocusChanged { focused = it.hasFocus }
            .padding(10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF450AF5), Color(0xFFC4EFD9)))),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Favorite, null, tint = Color.White, modifier = Modifier.size(48.dp))
        }
        Text(
            "Liked Songs",
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 12.dp)
        )
        Text(
            "Playlist • $count songs",
            color = Color(0xFFB3B3B3),
            fontSize = 13.sp
        )
    }
}

@Composable
private fun EmptyTv(text: String) {
    Text(
        text,
        color = Color(0xFFB3B3B3),
        fontSize = 16.sp,
        modifier = Modifier.padding(start = 40.dp, top = 24.dp)
    )
}
