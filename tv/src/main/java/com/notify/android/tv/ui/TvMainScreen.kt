package com.notify.android.tv.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notify.android.tv.theme.NotifyPurple
import com.notify.android.tv.theme.SpotifyBlack
import com.notify.android.tv.ui.components.ArtworkPlaceholder
import com.notify.core.ui.imageUrl
import com.notify.core.ui.player.PlayerViewModel
import coil.compose.AsyncImage

@Composable
fun TvMainScreen(playerVm: PlayerViewModel, onLogout: () -> Unit) {
    val nav = remember { TvNavState() }
    var showNowPlaying by remember { mutableStateOf(false) }
    BackHandler(enabled = showNowPlaying || nav.current !is TvScreen.Home || nav.stack.size > 1) {
        if (showNowPlaying) {
            showNowPlaying = false
        } else if (nav.stack.size > 1) {
            nav.back()
        } else {
            nav.navigate(TvScreen.Home)
        }
    }

    // When the now playing screen is up, compose ONLY it. Keeping the underlying
    // screens in the composition would leave their elements focusable behind the
    // overlay, so D-pad focus could wander onto hidden items and appear to get lost.
    if (showNowPlaying || nav.current is TvScreen.NowPlaying) {
        TvNowPlayingScreen(playerVm = playerVm)
    } else {
        Column(Modifier.fillMaxSize().background(SpotifyBlack)) {
            // Spotify TV top navigation bar
            TvTopBar(
                current = nav.current,
                playerVm = playerVm,
                onNavigate = { nav.navigate(it) },
                onNowPlaying = { showNowPlaying = true }
            )

            // Content
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (val s = nav.current) {
                    is TvScreen.Home -> TvHomeScreen(playerVm = playerVm, nav = nav)
                    is TvScreen.Search -> TvSearchScreen(playerVm = playerVm, nav = nav)
                    is TvScreen.Library -> TvLibraryScreen(playerVm = playerVm, nav = nav)
                    is TvScreen.LikedSongs -> TvLikedSongsScreen(playerVm = playerVm)
                    is TvScreen.Playlist -> TvPlaylistScreen(playlistId = s.id, playerVm = playerVm, nav = nav)
                    is TvScreen.Artist -> TvArtistScreen(artistId = s.id, playerVm = playerVm, nav = nav)
                    is TvScreen.Album -> TvAlbumScreen(albumId = s.id, playerVm = playerVm, nav = nav)
                    is TvScreen.Settings -> TvSettingsScreen(nav = nav, onLogout = onLogout)
                    is TvScreen.NowPlaying -> {}
                }
            }
        }
    }
}

@Composable
private fun TvTopBar(
    current: TvScreen,
    playerVm: PlayerViewModel,
    onNavigate: (TvScreen) -> Unit,
    onNowPlaying: () -> Unit
) {
    val currentTrack by playerVm.current.collectAsState()
    val homeFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        homeFocusRequester.requestFocus()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .background(Color(0xFF000000))
            .padding(horizontal = 40.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TopNavItem(
            "Home",
            Icons.Filled.Home,
            current is TvScreen.Home,
            focusRequester = homeFocusRequester
        ) { onNavigate(TvScreen.Home) }
        TopNavItem("Search", Icons.Filled.Search, current is TvScreen.Search) { onNavigate(TvScreen.Search) }
        TopNavItem("Your Library", Icons.Filled.LibraryMusic, current is TvScreen.Library) { onNavigate(TvScreen.Library) }

        Spacer(Modifier.weight(1f))

        val track = currentTrack
        if (track != null) {
            var nowPlayingFocused by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier
                    .border(
                        width = if (nowPlayingFocused) 4.dp else 0.dp,
                        color = if (nowPlayingFocused) Color.White else Color.Transparent,
                        shape = RoundedCornerShape(50)
                    )
                    .background(
                        if (nowPlayingFocused) Color.White.copy(alpha = 0.25f) else Color.Transparent,
                        RoundedCornerShape(50)
                    )
                    .clickable(onClick = onNowPlaying)
                    .onFocusChanged { nowPlayingFocused = it.hasFocus }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (track.displayArt.isNullOrBlank()) {
                    ArtworkPlaceholder(track.title, Modifier.size(40.dp))
                } else {
                    AsyncImage(
                        model = imageUrl(track.displayArt),
                        contentDescription = null,
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFF2A2A2A), RoundedCornerShape(4.dp)),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                }
                Column(Modifier.padding(start = 12.dp)) {
                    Text("NOW PLAYING", color = NotifyPurple, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Text(track.title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                }
            }
        }

        TopNavIcon(Icons.Filled.Settings, "Settings", selected = current is TvScreen.Settings) {
            onNavigate(TvScreen.Settings)
        }
    }
}

@Composable
private fun TopNavItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .background(
                if (selected) Color(0xFF3A3A3A)
                else if (focused) Color.White.copy(alpha = 0.25f)
                else Color.Transparent,
                RoundedCornerShape(50)
            )
            .border(
                width = if (focused) 4.dp else 0.dp,
                color = if (focused) Color.White else Color.Transparent,
                shape = RoundedCornerShape(50)
            )
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .clickable(onClick = onClick)
            .onFocusChanged { focused = it.hasFocus }
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (selected || focused) Color.White else Color(0xFFB3B3B3),
            modifier = Modifier.size(22.dp)
        )
        Text(
            label,
            color = if (selected || focused) Color.White else Color(0xFFB3B3B3),
            fontSize = 17.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(start = 10.dp)
        )
    }
}

@Composable
private fun TopNavIcon(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .padding(start = 8.dp)
            .size(56.dp)
            .background(
                if (selected) Color(0xFF3A3A3A)
                else if (focused) Color.White.copy(alpha = 0.25f)
                else Color.Transparent,
                RoundedCornerShape(50)
            )
            .border(
                width = if (focused) 4.dp else 0.dp,
                color = if (focused) Color.White else Color.Transparent,
                shape = RoundedCornerShape(50)
            )
            .clickable(onClick = onClick)
            .onFocusChanged { focused = it.hasFocus },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (selected || focused) Color.White else Color(0xFFB3B3B3),
            modifier = Modifier.size(24.dp)
        )
    }
}
