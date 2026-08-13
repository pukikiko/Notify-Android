package com.notify.android.tv.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notify.android.tv.theme.NotifyPurple
import com.notify.android.tv.ui.components.TvInputButton
import com.notify.android.tv.ui.components.TvTextField
import com.notify.android.tv.ui.components.TvTrackList
import com.notify.core.ui.player.PlayerViewModel
import com.notify.core.ui.playlistViewModel

@Composable
fun TvPlaylistScreen(playlistId: String, playerVm: PlayerViewModel, nav: TvNavState) {
    val vm = playlistViewModel(playlistId)
    val data by vm.data.collectAsState()
    val searchResults by vm.searchResults.collectAsState()

    var showAdd by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }

    val playlist = data?.playlist
    val tracks = data?.tracks.orEmpty()

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Text(
                playlist?.name ?: "…",
                color = Color.White,
                fontSize = 38.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 40.dp, top = 36.dp)
            )
            Text(
                "Playlist · ${tracks.size} songs",
                color = Color(0xFFB3B3B3),
                fontSize = 16.sp,
                modifier = Modifier.padding(start = 40.dp, top = 6.dp)
            )
            Row(Modifier.padding(start = 40.dp, top = 16.dp)) {
                TvInputButton(if (showAdd) "Close" else "Add tracks", onClick = { showAdd = !showAdd })
                Spacer(Modifier.width(12.dp))
                TvInputButton("Delete", onClick = { vm.deletePlaylist { nav.back() } })
                Spacer(Modifier.width(12.dp))
                if (tracks.isNotEmpty()) {
                    TvInputButton("Play all", primary = true, onClick = { playerVm.playQueue(tracks, 0) })
                }
            }
        }

        if (showAdd) {
            item {
                Column(Modifier.padding(start = 40.dp, top = 16.dp, end = 40.dp)) {
                    TvTextField(value = query, onValueChange = { query = it; vm.searchLibrary(it) }, label = "Add songs from your library…")
                    Spacer(Modifier.height(8.dp))
                    if (searchResults.isNotEmpty()) {
                        Column {
                            searchResults.take(8).forEach { t ->
                                val exists = tracks.any { it.id == t.id }
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(t.title, color = Color.White, fontSize = 15.sp)
                                        Text(
                                            listOfNotNull(t.displayArtist, t.displayAlbum).joinToString(" · "),
                                            color = Color(0xFFB3B3B3),
                                            fontSize = 13.sp
                                        )
                                    }
                                    if (exists) {
                                        Text("Added", color = NotifyPurple, fontSize = 13.sp)
                                    } else {
                                        TvInputButton("Add", onClick = { vm.addTrack(t.id) })
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            if (tracks.isEmpty()) {
                Text(
                    "Empty playlist. Add some tracks.",
                    color = Color(0xFFB3B3B3),
                    fontSize = 16.sp,
                    modifier = Modifier.padding(40.dp)
                )
            } else {
                TvTrackList(
                    tracks = tracks,
                    playerVm = playerVm
                )
            }
        }
    }
}
