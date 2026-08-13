package com.notify.android.mobile.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notify.android.mobile.ui.components.MediaCard
import com.notify.android.mobile.ui.components.TrackRow
import com.notify.android.mobile.ui.home.EmptyState
import com.notify.core.ui.player.PlayerViewModel
import com.notify.core.ui.navigation.navId
import com.notify.core.ui.viewmodels.imageUrl
import com.notify.core.ui.searchViewModel
import com.notify.core.model.DiscoverAlbum
import com.notify.core.model.DiscoverPlaylist
import com.notify.core.model.DiscoverPlayRequest
import com.notify.core.model.Track
import kotlinx.coroutines.launch

private val BROWSE = listOf(
    "Synthwave" to "#27856a", "Indie Folk" to "#8d67ab", "Electronic" to "#ba5d07",
    "Latin Pop" to "#e13300", "Blues Rock" to "#7358ff", "Ambient" to "#608108",
    "Alternative" to "#1e3264", "Chamber Pop" to "#0d73ec", "Punk" to "#e8115b",
    "Americana" to "#148a08", "House" to "#503750", "Indie Rock" to "#bc5900",
    "Dream Pop" to "#503750", "Post-Rock" to "#e91429", "Shoegaze" to "#477d95",
    "Soul" to "#dc148c"
)

@Composable
fun SearchScreen(
    playerVm: PlayerViewModel,
    initialQuery: String,
    onOpenArtist: (String) -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenPlaylist: (String) -> Unit
) {
    val vm = searchViewModel(initialQuery)
    val query by vm.query.collectAsState()
    val lib by vm.lib.collectAsState()
    val disc by vm.disc.collectAsState()
    val searching by vm.searching.collectAsState()
    val busy by vm.busy.collectAsState()
    val searchError by vm.error.collectAsState()
    val isPlaying by playerVm.playing.collectAsState()

    var text by remember { mutableStateOf(initialQuery) }
    val scope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF2A2A2A)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("N", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Text(
                    "Search",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }

        item {
            SearchBar(
                text = text,
                onTextChange = { newText ->
                    text = newText
                    vm.setQuery(newText)
                }
            )
        }

        if (query.isBlank()) {
            item {
                Text(
                    "Browse all",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 12.dp)
                )
            }
            item {
                val rows = BROWSE.chunked(2)
                rows.forEach { rowItems ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        rowItems.forEach { (title, colorHex) ->
                            BrowseTile(
                                title = title,
                                color = Color(android.graphics.Color.parseColor(colorHex)),
                                onClick = { vm.setQuery(title.lowercase()) },
                                modifier = Modifier.weight(1f).padding(4.dp)
                            )
                        }
                        if (rowItems.size == 1) {
                            Spacer(Modifier.weight(1f).padding(4.dp))
                        }
                    }
                }
            }
        } else {
            if (busy) {
                item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = Color(0xFF2A2A2A)) }
            }

            searchError?.let {
                item {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            val discState = disc
            val libState = lib
            val artists = discState?.artists.orEmpty()
            val albums = discState?.albums.orEmpty()
            val playlists = discState?.playlists.orEmpty()
            val tracks = if (discState?.popularTracks?.isNotEmpty() == true) discState.popularTracks else discState?.tracks.orEmpty()
            val libTracks = libState?.tracks.orEmpty()

            val norm = { s: String? -> (s ?: "").lowercase().replace(Regex("[^a-z0-9]+"), "") }
            val topAlbum = albums.firstOrNull { norm(it.title) == norm(query) }
            val topResult: Any? = discState?.artist ?: topAlbum ?: tracks.firstOrNull() ?: artists.firstOrNull() ?: albums.firstOrNull()

            val hasResults = artists.isNotEmpty() || albums.isNotEmpty() || playlists.isNotEmpty() || tracks.isNotEmpty() || libTracks.isNotEmpty()


            if (searching && !hasResults) {
                item { SearchSkeleton() }
            }

            if (topResult != null && hasResults) {
                item {
                    TopResultCard(
                        result = topResult,
                        tracks = tracks,
                        playerVm = playerVm,
                        onOpenArtist = onOpenArtist,
                        onOpenAlbum = onOpenAlbum,
                        onPlayTracks = { playerVm.playQueue(tracks, tracks.indexOf(topResult as Track)) },
                        onPlayAlbum = {
                            scope.launch {
                                val payload: DiscoverPlayRequest? = when (val r = topResult) {
                                    is DiscoverAlbum -> DiscoverPlayRequest(
                                        kind = "album",
                                        artist = r.artist?.name,
                                        album = r.title,
                                        releaseMbid = r.mbid,
                                        image = r.image
                                    )
                                    is com.notify.core.model.Album -> DiscoverPlayRequest(
                                        kind = "album",
                                        artist = r.artist?.name,
                                        album = r.title,
                                        releaseMbid = r.mbid,
                                        image = r.image
                                    )
                                    else -> null
                                }
                                if (payload != null) {
                                    val res = vm.playNow(payload)
                                    if (res.isNotEmpty()) playerVm.playQueue(res, 0)
                                }
                            }
                        }
                    )
                }
            }

            if (artists.isNotEmpty()) {
                item { SectionTitle("Artists") }
                item {
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp)) {
                        items(artists) { artist ->
                            MediaCard(
                                imageUrl = imageUrl(artist.image),
                                title = artist.name,
                                subtitle = artist.genres.take(2).joinToString(" · "),
                                onClick = { onOpenArtist(artist.navId()) },
                                rounded = true,
                                modifier = Modifier.width(164.dp)
                            )
                        }
                    }
                }
            }

            if (albums.isNotEmpty()) {
                item { SectionTitle("Albums") }
                item {
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp)) {
                        items(albums) { album ->
                            MediaCard(
                                imageUrl = imageUrl(album.image),
                                title = album.title,
                                subtitle = listOfNotNull(album.artist?.name, album.year?.toString()).joinToString(" · "),
                                onClick = { onOpenAlbum(album.navId()) },
                                modifier = Modifier.width(164.dp)
                            )
                        }
                    }
                }
            }

            if (playlists.isNotEmpty()) {
                item { SectionTitle("Playlists") }
                item {
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp)) {
                        items(playlists) { playlist ->
                            MediaCard(
                                imageUrl = imageUrl(playlist.image),
                                title = playlist.name,
                                subtitle = "${playlist.owner ?: "Spotify"} · ${playlist.trackCount ?: 0} songs",
                                onClick = { onOpenPlaylist(playlist.id) },
                                modifier = Modifier.width(164.dp)
                            )
                        }
                    }
                }
            }

            if (tracks.isNotEmpty()) {
                item { SectionTitle("Songs") }
                item {
                    Column {
                        tracks.forEachIndexed { index, track ->
                            TrackRow(
                                track = track,
                                index = index + 1,
                                artUrl = imageUrl(track.displayArt),
                                isCurrent = playerVm.isCurrent(track),
                                isPlaying = isPlaying,
                                onPlay = { playerVm.playQueue(tracks, index) },
                                showAlbum = true
                            )
                        }
                    }
                }
            }

            if (libTracks.isNotEmpty()) {
                item { SectionTitle("In your library") }
                item {
                    Column {
                        libTracks.forEachIndexed { index, track ->
                            TrackRow(
                                track = track,
                                index = index + 1,
                                artUrl = imageUrl(track.displayArt),
                                isCurrent = playerVm.isCurrent(track),
                                isPlaying = isPlaying,
                                onPlay = { playerVm.playQueue(libTracks, index) },
                                showAlbum = true
                            )
                        }
                    }
                }
            }

            if (!searching && !hasResults && !busy) {
                item {
                    EmptyState(
                        text = "No results found for “$query”. Try a different spelling."
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchBar(text: String, onTextChange: (String) -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(50),
        color = Color(0xFF2A2A2A)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Search, null, tint = Color(0xFFB3B3B3))
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                decorationBox = { inner ->
                    if (text.isEmpty()) {
                        Text("What do you want to play?", color = Color(0xFFB3B3B3), style = MaterialTheme.typography.bodyLarge)
                    }
                    inner()
                }
            )
        }
    }
}

@Composable
private fun BrowseTile(title: String, color: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(100.dp)
            .background(color, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        contentAlignment = Alignment.TopStart
    ) {
        Text(
            title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun TopResultCard(
    result: Any,
    tracks: List<Track>,
    playerVm: PlayerViewModel,
    onOpenArtist: (String) -> Unit,
    onOpenAlbum: (String) -> Unit,
    onPlayTracks: () -> Unit,
    onPlayAlbum: () -> Unit
) {
    val art: String?
    val name: String
    val label: String
    val sub: String

    when (result) {
        is com.notify.core.model.DiscoverArtist -> {
            art = result.image; name = result.name; label = "Artist"
            sub = result.genres.take(2).joinToString(" · ")
        }
        is DiscoverAlbum -> {
            art = result.image; name = result.title; label = "Album"
            sub = result.artist?.name ?: "Album"
        }
        is Track -> {
            art = result.displayArt; name = result.title; label = "Song"
            sub = result.displayArtist
        }
        is com.notify.core.model.Artist -> {
            art = result.image; name = result.name; label = "Artist"
            sub = result.genres.take(2).joinToString(" · ")
        }
        is com.notify.core.model.Album -> {
            art = result.image; name = result.title; label = "Album"
            sub = result.artist?.name ?: "Album"
        }
        else -> { art = null; name = "…"; label = ""; sub = "" }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable {
                when (result) {
                    is com.notify.core.model.DiscoverArtist -> onOpenArtist(result.navId().ifEmpty { return@clickable })
                    is com.notify.core.model.Artist -> onOpenArtist(result.navId())
                    is DiscoverAlbum -> onOpenAlbum(result.navId().ifEmpty { return@clickable })
                    is com.notify.core.model.Album -> onOpenAlbum(result.navId())
                    is Track -> onPlayTracks()
                }
            },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            com.notify.android.mobile.ui.components.Artwork(
                url = imageUrl(art),
                contentDescription = name,
                modifier = Modifier.size(72.dp)
            )
            Column(Modifier.padding(start = 16.dp).weight(1f)) {
                Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = Color(0xFFB3B3B3), letterSpacing = 1.sp)
                Text(
                    name,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(sub, style = MaterialTheme.typography.bodySmall, color = Color(0xFFB3B3B3), maxLines = 1)
            }
        }
    }
}

@Composable
private fun SearchSkeleton() {
    Column {
        repeat(4) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(48.dp).background(Color(0xFF232323), RoundedCornerShape(6.dp))
                )
                Column(Modifier.padding(start = 12.dp)) {
                    Box(Modifier.width(140.dp).height(12.dp).background(Color(0xFF232323), RoundedCornerShape(3.dp)))
                    Spacer(Modifier.height(6.dp))
                    Box(Modifier.width(90.dp).height(12.dp).background(Color(0xFF232323), RoundedCornerShape(3.dp)))
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleLarge,
        color = Color.White,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 10.dp)
    )
}

