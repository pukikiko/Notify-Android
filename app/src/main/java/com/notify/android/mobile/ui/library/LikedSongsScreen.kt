package com.notify.android.mobile.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
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
import com.notify.android.mobile.ui.home.EmptyState
import com.notify.core.ui.player.PlayerViewModel
import com.notify.core.ui.viewmodels.imageUrl
import com.notify.core.ui.libraryViewModel
import com.notify.core.model.Track

@Composable
fun LikedSongsScreen(
    playerVm: PlayerViewModel,
    onBack: () -> Unit
) {
    val vm = libraryViewModel()
    val tracks by vm.tracks.collectAsState()

    var sort by remember { mutableStateOf("Recents") }
    var gridMode by remember { mutableStateOf(false) }
    var searching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize()) {
        // Header: back + "Liked Songs" + search
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
            }
            Column(Modifier.weight(1f)) {
                Text(
                    "Liked Songs",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${tracks?.size ?: 0} songs",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFB3B3B3)
                )
            }
            IconButton(onClick = { searching = !searching; if (!searching) query = "" }) {
                Icon(
                    if (searching) Icons.Default.Close else Icons.Default.Search,
                    if (searching) "Close search" else "Search Liked Songs",
                    tint = Color.White
                )
            }
            DownloadCollectionButton(
                tracks = tracks.orEmpty(),
                collection = com.notify.core.data.OfflineCollection("liked", "liked", "Liked Songs")
            )
        }

        // In-library search field
        if (searching) {
            Row(Modifier.padding(horizontal = 16.dp)) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(50),
                    color = Color(0xFF2A2A2A)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Search, null, tint = Color(0xFFB3B3B3), modifier = Modifier.size(18.dp))
                        androidx.compose.foundation.text.BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 8.dp),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                            decorationBox = { inner ->
                                if (query.isEmpty()) {
                                    Text("Search your liked songs", color = Color(0xFFB3B3B3), style = MaterialTheme.typography.bodyMedium)
                                }
                                inner()
                            }
                        )
                    }
                }
            }
        }

        // Sort row: sort order + grid/list toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SortPill(label = sort) { sort = it }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { gridMode = !gridMode }) {
                Icon(
                    if (gridMode) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                    if (gridMode) "Show list view" else "Show grid view",
                    tint = Color.White
                )
            }
        }

        val list = tracks
        if (list == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF2A2A2A))
            }
        } else {
            val filtered = filterTracks(list, query).let { sortTracks(it, sort) }
            if (filtered.isEmpty()) {
                EmptyState(if (query.isNotBlank()) "No songs match “$query”." else "No liked tracks yet. Tap the heart on any track.")
            } else if (gridMode) {
                TrackGrid(filtered, playerVm)
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(filtered) { track ->
                        LibraryRow(
                            imageUrl = imageUrl(track.displayArt),
                            title = track.title,
                            subtitle = track.displayArtist,
                            isCurrent = playerVm.isCurrent(track),
                            onClick = { playerVm.playQueue(filtered, filtered.indexOf(track)) }
                        )
                    }
                }
            }
        }
    }
}

private fun filterTracks(list: List<Track>, q: String): List<Track> =
    if (q.isBlank()) list else list.filter {
        it.title.contains(q, true) || it.displayArtist.contains(q, true)
    }

private fun sortTracks(list: List<Track>, sort: String): List<Track> =
    when (sort) {
        "Title" -> list.sortedBy { it.title.lowercase() }
        "Recently added" -> list.sortedByDescending { it.createdAt }
        else -> list
    }

@Composable
private fun TrackGrid(list: List<Track>, playerVm: PlayerViewModel) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 170.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        items(list) { track ->
            Surface(
                onClick = { playerVm.playQueue(list, list.indexOf(track)) },
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF181818),
                modifier = Modifier.padding(4.dp)
            ) {
                Column(Modifier.padding(8.dp)) {
                    Artwork(
                        url = imageUrl(track.displayArt),
                        contentDescription = track.title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    Text(
                        track.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Text(
                        track.displayArtist,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFB3B3B3),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryRow(
    imageUrl: String?,
    title: String,
    subtitle: String,
    isCurrent: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Artwork(
            url = imageUrl,
            contentDescription = title,
            modifier = Modifier.size(56.dp),
            shape = RoundedCornerShape(4.dp)
        )
        Column(
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isCurrent) Color(0xFF8F5CFF) else Color.White,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFB3B3B3),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SortPill(label: String, onSelect: (String) -> Unit) {
    val options = listOf("Recents", "Recently added", "Title")
    var expanded by remember { mutableStateOf(false) }
    Surface(
        onClick = { expanded = true },
        shape = RoundedCornerShape(50),
        color = Color(0xFF2A2A2A),
        modifier = Modifier.padding(vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = Color.White, fontWeight = FontWeight.SemiBold)
            Icon(Icons.Default.ArrowDropDown, null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        options.forEach { opt ->
            DropdownMenuItem(
                text = { Text(opt, color = if (opt == label) Color.White else Color(0xFFB3B3B3)) },
                onClick = {
                    expanded = false
                    onSelect(opt)
                }
            )
        }
    }
}
