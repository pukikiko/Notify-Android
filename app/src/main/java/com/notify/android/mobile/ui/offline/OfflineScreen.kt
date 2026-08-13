package com.notify.android.mobile.ui.offline

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
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
import com.notify.android.mobile.ui.home.EmptyState
import com.notify.core.ui.player.PlayerViewModel
import com.notify.core.ui.viewmodels.imageUrl
import com.notify.core.ui.offlineViewModel
import com.notify.core.data.DownloadState
import com.notify.core.data.OfflineTrack
import com.notify.core.model.Track

@Composable
fun OfflineScreen(
    playerVm: PlayerViewModel,
    onBack: () -> Unit
) {
    val vm = offlineViewModel()
    val tracks by vm.tracks.collectAsState()
    val downloads by vm.downloads.collectAsState()

    val groups = remember(tracks) {
        tracks.groupBy { it.collection }
            .toList()
            .sortedByDescending { (_, list) -> list.maxOfOrNull { it.downloadedAt } ?: 0L }
    }

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                }
                Text(
                    "Offline",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (tracks.isNotEmpty()) {
                    TextButton(onClick = { vm.removeAll() }) {
                        Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                        Text(" Delete all")
                    }
                }
            }
        }

        item {
            Text(
                "Music you saved offline plays from your device without a connection.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFB3B3B3),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        if (downloads.isNotEmpty()) {
            item {
                Text(
                    "Downloading",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                )
            }
            downloads.values.forEach { dl ->
                item(key = dl.trackId) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Download, null, tint = Color(0xFF8F5CFF))
                        Column(Modifier.padding(start = 12.dp).weight(1f)) {
                            Text(
                                "Track ${dl.trackId}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            LinearProgressIndicator(
                                progress = { dl.progress.coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                color = Color(0xFF8F5CFF),
                                trackColor = Color(0xFF2A2A2A)
                            )
                        }
                        IconButton(onClick = { vm.cancel(dl.trackId) }) {
                            Icon(Icons.Default.Delete, "Cancel", tint = Color(0xFFB3B3B3))
                        }
                    }
                }
            }
        }

        if (tracks.isEmpty() && downloads.isEmpty()) {
            item {
                EmptyState("No offline music yet. Open Liked Songs, a playlist or an album and tap Download to save it for offline.")
            }
        } else {
            groups.forEach { (collection, list) ->
                item(key = "group-${collection?.key ?: "songs"}") {
                    CollectionHeader(
                        name = collection?.name ?: "Other songs",
                        count = list.size,
                        onRemove = if (collection != null) {
                            { vm.removeCollection(collection) }
                        } else {
                            null
                        }
                    )
                }
                list.forEach { offline ->
                    item(key = offline.trackId) {
                        val trackIndex = list.indexOf(offline)
                        OfflineRow(
                            offline = offline,
                            isCurrent = playerVm.isCurrent(Track(id = offline.trackId, title = offline.title)),
                            isPlaying = playerVm.playing.collectAsState().value,
                            onPlay = { playerVm.playQueue(offlineToTracks(list), trackIndex) },
                            onRemove = { vm.remove(offline.trackId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CollectionHeader(name: String, count: Int, onRemove: (() -> Unit)?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 16.dp, end = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            name,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        Text(
            "$count songs",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFB3B3B3),
            modifier = Modifier.padding(end = 8.dp)
        )
        if (onRemove != null) {
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, "Remove download", tint = Color(0xFFB3B3B3), modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun OfflineRow(
    offline: OfflineTrack,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlay)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Artwork(
            url = imageUrl(offline.artUrl),
            contentDescription = offline.title,
            modifier = Modifier.size(44.dp),
            shape = RoundedCornerShape(4.dp)
        )
        Column(Modifier.padding(start = 12.dp).weight(1f)) {
            Text(
                offline.title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isCurrent) Color(0xFF8F5CFF) else Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                listOfNotNull(offline.artist, offline.album).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFB3B3B3),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            if (isCurrent && isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            null,
            tint = Color(0xFFB3B3B3),
            modifier = Modifier.size(18.dp)
        )
        IconButton(onClick = onRemove) {
            Icon(Icons.Default.Delete, "Remove download", tint = Color(0xFFB3B3B3), modifier = Modifier.size(18.dp))
        }
    }
}

private fun offlineToTracks(tracks: List<OfflineTrack>): List<Track> =
    tracks.map {
        Track(
            id = it.trackId,
            title = it.title,
            artist = it.artist?.let { a -> com.notify.core.model.Artist(id = "", name = a) },
            album = it.album?.let { al -> com.notify.core.model.Album(id = "", title = al) },
            artUrl = it.artUrl,
            duration = it.duration
        )
    }
