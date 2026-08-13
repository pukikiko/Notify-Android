package com.notify.android.mobile.ui.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.notify.android.mobile.ui.components.Artwork
import com.notify.android.mobile.ui.components.DownloadCollectionButton
import com.notify.android.mobile.ui.components.TrackRow
import com.notify.android.mobile.ui.home.EmptyState
import com.notify.core.ui.player.PlayerViewModel
import com.notify.core.ui.viewmodels.imageUrl
import com.notify.core.ui.playlistViewModel
import com.notify.core.ui.viewmodels.sessionManager

@Composable
fun PlaylistScreen(
    playlistId: String,
    playerVm: PlayerViewModel,
    onBack: () -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenAlbum: (String) -> Unit
) {
    val vm = playlistViewModel(playlistId)
    val data by vm.data.collectAsState()
    val searchResults by vm.searchResults.collectAsState()
    val isPlaying by playerVm.playing.collectAsState()

    var showAdd by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var editingName by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }

    val playlist = data?.playlist
    val tracks = data?.tracks.orEmpty()

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            listOf(
                                hslColor(playlist?.name ?: "playlist"),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                }
                Spacer(Modifier.weight(1f))
            }
        }

        item {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Artwork(
                    url = null,
                    contentDescription = playlist?.name,
                    modifier = Modifier.size(96.dp)
                )
                Column(Modifier.padding(start = 16.dp).weight(1f)) {
                    Text("PLAYLIST", style = MaterialTheme.typography.labelSmall, color = Color(0xFFB3B3B3))
                    if (editingName) {
                        Row {
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { vm.rename(name); editingName = false }) { Text("Save") }
                        }
                    } else {
                        Text(
                            playlist?.name ?: "…",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White,
                            modifier = Modifier.clickable { name = playlist?.name ?: ""; editingName = true }
                        )
                    }
                    Text(
                        "You · ${tracks.size} songs, ${((playlist?.duration ?: 0L) / 60)} min",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFB3B3B3)
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isPlayingCurrent = tracks.any { playerVm.isCurrent(it) }
                PlayBigButton(
                    isPlaying = isPlayingCurrent && isPlaying,
                    onClick = {
                        if (isPlayingCurrent) playerVm.toggle() else playerVm.playQueue(tracks, 0)
                    }
                )
                Spacer(Modifier.width(16.dp))
                OutlinedButton(onClick = { showAdd = !showAdd }, shape = RoundedCornerShape(50)) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Text(" Add tracks")
                }
                Spacer(Modifier.width(8.dp))
                DownloadCollectionButton(
                    tracks = tracks,
                    collection = com.notify.core.data.OfflineCollection("playlist", playlistId, playlist?.name ?: "Playlist")
                )
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = { vm.deletePlaylist { onBack() } }, shape = RoundedCornerShape(50)) {
                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                    Text(" Delete")
                }
            }
        }

        if (showAdd) {
            item {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it; vm.searchLibrary(it) },
                        placeholder = { Text("Add songs from your library…") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (searchResults.isNotEmpty()) {
                        searchResults.take(12).forEach { t ->
                            val exists = tracks.any { it.id == t.id }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(t.title, style = MaterialTheme.typography.bodyMedium, color = Color.White, maxLines = 1)
                                    Text(
                                        listOfNotNull(t.displayArtist, t.displayAlbum).joinToString(" · "),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFB3B3B3),
                                        maxLines = 1
                                    )
                                }
                                if (exists) {
                                    Text("Added", color = Color(0xFF8F5CFF), style = MaterialTheme.typography.bodySmall)
                                } else {
                                    Button(
                                        onClick = { vm.addTrack(t.id) },
                                        shape = RoundedCornerShape(50),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8F5CFF), contentColor = Color.Black)
                                    ) { Text("Add") }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (tracks.isEmpty()) {
            item { EmptyState("Empty playlist. Add some tracks.") }
        } else {
            items(tracks.size) { index ->
                val track = tracks[index]
                TrackRow(
                    track = track,
                    index = index + 1,
                    artUrl = imageUrl(track.displayArt),
                    isCurrent = playerVm.isCurrent(track),
                    isPlaying = isPlaying,
                    onPlay = { playerVm.playQueue(tracks, index) },
                    onOpenArtist = { track.artist?.id?.let(onOpenArtist) },
                    onOpenAlbum = { track.album?.id?.let(onOpenAlbum) },
                    showArtist = true,
                    trailing = {
                        IconButton(onClick = { vm.removeTrack(track.id) }) {
                            Icon(Icons.Default.Delete, "Remove", tint = Color(0xFFB3B3B3), modifier = Modifier.size(18.dp))
                        }
                    }
                )
            }
        }
    }
}

@Composable
internal fun PlayBigButton(isPlaying: Boolean, onClick: () -> Unit) {
    androidx.compose.material3.FilledIconButton(
        onClick = onClick,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = Color(0xFF8F5CFF),
            contentColor = Color.Black
        ),
        modifier = Modifier.size(52.dp)
    ) {
        Icon(
            if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = if (isPlaying) "Pause" else "Play",
            modifier = Modifier.size(28.dp)
        )
    }
}

private fun hslColor(name: String): Color {
    var h = 0
    for (c in name) h = (h * 31 + c.code) % 360
    return androidx.compose.ui.graphics.Color.hsl(h.toFloat(), 0.45f, 0.22f)
}
