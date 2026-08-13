package com.notify.android.tv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.notify.android.tv.ui.components.TvCard
import com.notify.android.tv.ui.components.TvSectionTitle
import com.notify.android.tv.ui.components.TvTrackList
import com.notify.core.model.Album
import com.notify.core.model.DiscoverAlbum
import com.notify.core.model.DiscoverArtist
import com.notify.core.model.DiscoverPlayRequest
import com.notify.core.model.Track
import com.notify.core.ui.imageUrl
import com.notify.core.ui.navigation.navId
import com.notify.core.ui.player.PlayerViewModel
import com.notify.core.ui.searchViewModel
import kotlinx.coroutines.launch

@Composable
fun TvSearchScreen(playerVm: PlayerViewModel, nav: TvNavState) {
    val vm = searchViewModel()
    val query by vm.query.collectAsState()
    val disc by vm.disc.collectAsState()
    val lib by vm.lib.collectAsState()
    val searching by vm.searching.collectAsState()
    val scope = rememberCoroutineScope()

    var text by remember { mutableStateOf("") }

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Row(
                modifier = Modifier
                    .padding(start = 40.dp, top = 36.dp, end = 40.dp)
                    .fillMaxWidth()
                    .background(Color(0xFF2A2A2A), RoundedCornerShape(50))
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Search, null, tint = Color(0xFFB3B3B3))
                BasicTextField(
                    value = text,
                    onValueChange = { newText ->
                        text = newText
                        vm.setQuery(newText)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White, fontSize = 18.sp),
                    decorationBox = { inner ->
                        if (text.isEmpty()) {
                            Text("What do you want to play?", color = Color(0xFFB3B3B3), fontSize = 18.sp)
                        }
                        inner()
                    }
                )
            }
        }

        if (query.isBlank()) {
            item {
                TvSectionTitle("Browse all")
            }
            item {
                val browse = listOf(
                    "Synthwave" to "#27856a", "Indie Folk" to "#8d67ab", "Electronic" to "#ba5d07",
                    "Latin Pop" to "#e13300", "Blues Rock" to "#7358ff", "Ambient" to "#608108",
                    "Alternative" to "#1e3264", "Chamber Pop" to "#0d73ec", "Punk" to "#e8115b",
                    "Americana" to "#148a08", "House" to "#503750", "Indie Rock" to "#bc5900",
                    "Shoegaze" to "#477d95", "Soul" to "#dc148c"
                )
                Row(
                    modifier = Modifier.padding(horizontal = 40.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    browse.chunked(4).forEach { group ->
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            group.forEach { (title, hex) ->
                                BrowseTile(title, Color(android.graphics.Color.parseColor(hex))) {
                                    text = title
                                    vm.setQuery(title.lowercase())
                                }
                            }
                        }
                    }
                }
            }
        } else {
            val discState = disc
            val libState = lib
            val artists = discState?.artists.orEmpty()
            val albums = discState?.albums.orEmpty()
            val playlists = discState?.playlists.orEmpty()
            val tracks = if (discState?.popularTracks?.isNotEmpty() == true) discState.popularTracks else discState?.tracks.orEmpty()
            val libTracks = libState?.tracks.orEmpty()

            if (searching && artists.isEmpty() && albums.isEmpty() && tracks.isEmpty()) {
                item {
                    Text(
                        "Searching…",
                        color = Color(0xFFB3B3B3),
                        fontSize = 16.sp,
                        modifier = Modifier.padding(40.dp)
                    )
                }
            }

            val norm = { s: String? -> (s ?: "").lowercase().replace(Regex("[^a-z0-9]+"), "") }
            val topAlbum = albums.firstOrNull { norm(it.title) == norm(query) }
            val topResult: Any? = discState?.artist ?: topAlbum ?: tracks.firstOrNull() ?: artists.firstOrNull() ?: albums.firstOrNull()

            if (topResult != null) {
                item {
                    TvTopResultCard(
                        result = topResult,
                        tracks = tracks,
                        onOpenArtist = { nav.navigate(TvScreen.Artist(it)) },
                        onOpenAlbum = { nav.navigate(TvScreen.Album(it)) },
                        onPlayQueue = { list, idx -> playerVm.playQueue(list, idx) },
                        onPlayDiscover = { payload ->
                            scope.launch {
                                runCatching { vm.playNow(payload) }
                                    .onSuccess { if (it.isNotEmpty()) playerVm.playQueue(it, 0) }
                            }
                        }
                    )
                }
            }

            if (artists.isNotEmpty()) {
                item { TvSectionTitle("Artists") }
                item {
                    LazyRow(contentPadding = PaddingValues(horizontal = 40.dp)) {
                        items(artists) { artist ->
                            TvCard(
                                imageUrl = imageUrl(artist.image),
                                title = artist.name,
                                subtitle = artist.genres.take(2).joinToString(" · "),
                                onClick = { nav.navigate(TvScreen.Artist(artist.navId())) },
                                rounded = true,
                                width = 200.dp
                            )
                        }
                    }
                }
            }

            if (albums.isNotEmpty()) {
                item { TvSectionTitle("Albums") }
                item {
                    LazyRow(contentPadding = PaddingValues(horizontal = 40.dp)) {
                        items(albums) { album ->
                            TvCard(
                                imageUrl = imageUrl(album.image),
                                title = album.title,
                                subtitle = listOfNotNull(album.artist?.name, album.year?.toString()).joinToString(" · "),
                                onClick = { nav.navigate(TvScreen.Album(album.navId())) },
                                width = 220.dp
                            )
                        }
                    }
                }
            }

            if (playlists.isNotEmpty()) {
                item { TvSectionTitle("Playlists") }
                item {
                    LazyRow(contentPadding = PaddingValues(horizontal = 40.dp)) {
                        items(playlists) { playlist ->
                            TvCard(
                                imageUrl = imageUrl(playlist.image),
                                title = playlist.name,
                                subtitle = "${playlist.owner ?: "Spotify"} · ${playlist.trackCount ?: 0} songs",
                                onClick = { nav.navigate(TvScreen.Playlist("sp-${playlist.id}")) },
                                width = 220.dp
                            )
                        }
                    }
                }
            }

            if (tracks.isNotEmpty()) {
                item { TvSectionTitle("Songs") }
                item {
                    TvTrackList(
                        tracks = tracks,
                        playerVm = playerVm
                    )
                }
            }

            if (libTracks.isNotEmpty()) {
                item { TvSectionTitle("In your library") }
                item {
                    TvTrackList(
                        tracks = libTracks,
                        playerVm = playerVm
                    )
                }
            }

            if (!searching && artists.isEmpty() && albums.isEmpty() && playlists.isEmpty() && tracks.isEmpty() && libTracks.isEmpty()) {
                item {
                    Text(
                        "No results found for “$query”.",
                        color = Color(0xFFB3B3B3),
                        fontSize = 16.sp,
                        modifier = Modifier.padding(40.dp)
                    )
                }
            }
        }
    }
}

/** Big top-result card, mirroring the web/mobile search. Clicking plays the
 *  matched song or opens the matched artist/album (playing it if it's a
 *  Spotify-only result with no library route). */
@Composable
private fun TvTopResultCard(
    result: Any,
    tracks: List<Track>,
    onOpenArtist: (String) -> Unit,
    onOpenAlbum: (String) -> Unit,
    onPlayQueue: (List<Track>, Int) -> Unit,
    onPlayDiscover: (DiscoverPlayRequest) -> Unit
) {
    var focused by remember { mutableStateOf(false) }

    val art: String?
    val name: String
    val label: String
    val sub: String
    var onClick: () -> Unit = {}

    when (result) {
        is DiscoverArtist -> {
            art = result.image; name = result.name; label = "Artist"
            sub = result.genres.take(2).joinToString(" · ")
            onClick = {
                val id = result.navId()
                if (id.isNotEmpty()) onOpenArtist(id)
                else onPlayDiscover(DiscoverPlayRequest(kind = "artist", artist = result.name))
            }
        }
        is Album -> {
            art = result.image; name = result.title; label = "Album"
            sub = result.artist?.name ?: "Album"
            onClick = { onOpenAlbum(result.navId()) }
        }
        is DiscoverAlbum -> {
            art = result.image; name = result.title; label = "Album"
            sub = result.artist?.name ?: "Album"
            onClick = {
                val id = result.navId()
                if (id.isNotEmpty()) onOpenAlbum(id)
                else onPlayDiscover(
                    DiscoverPlayRequest(
                        kind = "album",
                        artist = result.artist?.name,
                        album = result.title,
                        releaseMbid = result.mbid,
                        image = result.image
                    )
                )
            }
        }
        is Track -> {
            art = result.displayArt; name = result.title; label = "Song"
            sub = result.displayArtist
            onClick = { onPlayQueue(tracks, tracks.indexOf(result).coerceAtLeast(0)) }
        }
        else -> {
            art = null; name = "…"; label = ""; sub = ""
        }
    }

    Row(
        modifier = Modifier
            .padding(start = 40.dp, top = 24.dp, end = 40.dp)
            .fillMaxWidth()
            .background(
                if (focused) Color.White.copy(alpha = 0.25f) else Color(0xFF181818),
                RoundedCornerShape(12.dp)
            )
            .border(
                width = if (focused) 4.dp else 0.dp,
                color = if (focused) Color.White else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .onFocusChanged { focused = it.hasFocus }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(112.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF2A2A2A)),
            contentAlignment = Alignment.Center
        ) {
            if (!art.isNullOrBlank()) {
                AsyncImage(
                    model = imageUrl(art),
                    contentDescription = name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(Icons.Filled.MusicNote, null, tint = Color(0xFF7A7A7A), modifier = Modifier.size(36.dp))
            }
        }
        Column(Modifier.padding(start = 24.dp)) {
            Text(
                label.uppercase(),
                color = Color(0xFFB3B3B3),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Text(
                name,
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (sub.isNotEmpty()) {
                Text(sub, color = Color(0xFFB3B3B3), fontSize = 15.sp, maxLines = 1)
            }
        }
    }
}

@Composable
private fun BrowseTile(title: String, color: Color, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .size(width = 220.dp, height = 110.dp)
            .border(
                width = if (focused) 4.dp else 0.dp,
                color = if (focused) Color.White else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .background(color, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .onFocusChanged { focused = it.hasFocus }
            .padding(14.dp),
        contentAlignment = Alignment.TopStart
    ) {
        Text(title, color = Color.White, fontSize = 17.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
    }
}
