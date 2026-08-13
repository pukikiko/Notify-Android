package com.notify.android.mobile.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notify.android.mobile.ui.components.Artwork
import com.notify.android.mobile.ui.home.EmptyState
import com.notify.core.ui.libraryViewModel
import com.notify.core.ui.playlistsViewModel
import com.notify.core.ui.viewmodels.imageUrl

@Composable
fun LibraryScreen(
    onOpenLikedSongs: () -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onOpenSettings: () -> Unit
) {
    val libVm = libraryViewModel()
    val likedTracks by libVm.tracks.collectAsState()
    val playlistsVm = playlistsViewModel()
    val playlists by playlistsVm.playlists.collectAsState()

    var searching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var creating by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }

    val playlistsFiltered = (playlists.orEmpty()).filter { p ->
        query.isBlank() || p.name.contains(query, true)
    }

    Column(Modifier.fillMaxSize()) {
        // Header: avatar + "Your Library" + search/create/settings
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFF2A2A2A))
                    .clickable { onOpenSettings() },
                contentAlignment = Alignment.Center
            ) {
                Text("N", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Text(
                "Your Library",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
            )
            IconButton(onClick = { searching = !searching; if (!searching) query = "" }) {
                Icon(
                    if (searching) Icons.Default.Close else Icons.Default.Search,
                    if (searching) "Close search" else "Search Your Library",
                    tint = Color.White
                )
            }
            IconButton(onClick = { creating = !creating; if (creating) newName = "" }) {
                Icon(Icons.Outlined.Add, "Create playlist", tint = Color.White)
            }
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Default.Settings, "Settings", tint = Color.White)
            }
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
                                    Text("Search your library", color = Color(0xFFB3B3B3), style = MaterialTheme.typography.bodyMedium)
                                }
                                inner()
                            }
                        )
                    }
                }
            }
        }

        // Create playlist inline
        if (creating) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material3.OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Playlist name") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        playlistsVm.create(newName) { id ->
                            creating = false
                            onOpenPlaylist(id)
                        }
                    },
                    enabled = newName.isNotBlank(),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8F5CFF), contentColor = Color.Black)
                ) {
                    Text("Create")
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            if (query.isBlank() || "Liked Songs".contains(query, true)) {
                item {
                    LikedSongsRow(count = likedTracks?.size ?: 0, onClick = onOpenLikedSongs)
                }
            }

            when {
                playlists == null && query.isBlank() -> item {
                    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF2A2A2A))
                    }
                }
                playlistsFiltered.isEmpty() -> item {
                    EmptyState(
                        if (query.isNotBlank()) "No playlists match “$query”."
                        else "No playlists yet. Create one to get started."
                    )
                }
                else -> items(playlistsFiltered) { playlist ->
                    PlaylistRow(
                        imageUrl = playlist.image?.let { imageUrl(it) },
                        name = playlist.name,
                        count = playlist.trackCount ?: 0,
                        onClick = { onOpenPlaylist(playlist.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun LikedSongsRow(count: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF450AF5), Color(0xFFC4EFD9)))),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Favorite, null, tint = Color.White, modifier = Modifier.size(24.dp))
        }
        Column(
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f)
        ) {
            Text(
                "Liked Songs",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "Playlist · $count songs",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFB3B3B3),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PlaylistRow(
    imageUrl: String?,
    name: String,
    count: Int,
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
            contentDescription = name,
            modifier = Modifier.size(56.dp),
            shape = RoundedCornerShape(4.dp)
        )
        Column(
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f)
        ) {
            Text(
                name,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "Playlist · $count songs",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFB3B3B3),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}


