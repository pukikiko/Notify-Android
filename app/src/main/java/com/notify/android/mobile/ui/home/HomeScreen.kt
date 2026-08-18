package com.notify.android.mobile.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.notify.android.mobile.ui.components.MediaCard
import com.notify.android.mobile.ui.components.TrackRow
import com.notify.core.ui.player.PlayerViewModel
import com.notify.core.ui.navigation.navId
import com.notify.core.ui.homeViewModel
import com.notify.core.ui.viewmodels.imageUrl
import com.notify.core.model.Album
import com.notify.core.model.Artist
import com.notify.core.model.Track
import java.util.Calendar

@Composable
fun HomeScreen(
    playerVm: PlayerViewModel,
    onOpenArtist: (String) -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onOpenSearch: (String) -> Unit,
    onOpenLibrary: () -> Unit
) {
    val vm = homeViewModel()
    val data by vm.data.collectAsState()
    val downloads by vm.downloads.collectAsState()
    val homeError by vm.error.collectAsState()
    val isPlaying by playerVm.playing.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Spotify home header: avatar left
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Spotify-style circular avatar (opens profile/settings)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF2A2A2A))
                        .clickable { onOpenLibrary() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "N",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Spacer(Modifier.weight(1f))
            }
        }

        if (data == null && homeError != null) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Couldn't load your home feed",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        homeError ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFB3B3B3),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Button(
                        onClick = { vm.refresh() },
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8F5CFF), contentColor = Color.Black),
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        Text("Retry")
                    }
                }
            }
        }

        if (downloads.isNotEmpty()) {
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp)) {
                    items(downloads) { d ->
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xFF2A2A2A),
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Download, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Text(
                                    d.title,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White,
                                    modifier = Modifier.padding(start = 4.dp),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        val d = data
        if (d != null) {
            // Spotify's green "Made For You" style grid of recently played
            val gridItems = buildList<Any> {
                d.liked.take(4).forEach { add(it) }
                d.recentAlbums.take(2).forEach { add(it) }
                d.popularTracks.take(2).forEach { add(it) }
            }.distinctBy { if (it is Track) it.id else if (it is Album) it.id else "" }.take(4)

            if (gridItems.isNotEmpty()) {
                item {
                    Text(
                        "Jump back in",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 10.dp)
                    )
                }
                item {
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        gridItems.chunked(2).forEach { rowItems ->
                            Row(Modifier.fillMaxWidth()) {
                                rowItems.forEach { item ->
                                    HomeGridTile(item, Modifier.weight(1f), onOpenArtist, onOpenAlbum) { t ->
                                        playerVm.playQueue(
                                            if (t is Track) d.liked.takeIf { it.any { x -> x.id == t.id } } ?: d.popularTracks else emptyList(),
                                            (if (t is Track) (d.liked.takeIf { it.any { x -> x.id == t.id } } ?: d.popularTracks).indexOfFirst { it.id == t.id } else -1).coerceAtLeast(0)
                                        )
                                    }
                                    if (rowItems.size == 1) Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            if (d.popularArtists.isNotEmpty()) {
                item { SectionTitle("Popular artists") }
                item {
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp)) {
                        items(d.popularArtists) { artist ->
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

            if (d.popularAlbums.isNotEmpty()) {
                item { SectionTitle("Popular albums and singles") }
                item {
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp)) {
                        items(d.popularAlbums) { album ->
                            MediaCard(
                                imageUrl = imageUrl(album.image),
                                title = album.title,
                                subtitle = album.artist?.name,
                                onClick = { onOpenAlbum(album.navId()) },
                                modifier = Modifier.width(164.dp)
                            )
                        }
                    }
                }
            }

            if (d.recentAlbums.isNotEmpty()) {
                item { SectionTitle("Recently added albums") }
                item {
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp)) {
                        items(d.recentAlbums) { album ->
                            MediaCard(
                                imageUrl = imageUrl(album.image),
                                title = album.title,
                                subtitle = album.artist?.name,
                                onClick = { onOpenAlbum(album.navId()) },
                                modifier = Modifier.width(164.dp)
                            )
                        }
                    }
                }
            }

            if (d.popularTracks.isNotEmpty()) {
                item { SectionTitle("Popular songs") }
                item {
                    TrackList(
                        tracks = d.popularTracks,
                        playerVm = playerVm,
                        isPlaying = isPlaying,
                        onOpenArtist = onOpenArtist,
                        onOpenAlbum = onOpenAlbum
                    )
                }
            }

            if (d.liked.isNotEmpty()) {
                item { SectionTitle("Liked songs") }
                item {
                    TrackList(
                        tracks = d.liked,
                        playerVm = playerVm,
                        isPlaying = isPlaying,
                        onOpenArtist = onOpenArtist,
                        onOpenAlbum = onOpenAlbum
                    )
                }
            }

            if (d.popularTracks.isEmpty() && d.popularAlbums.isEmpty() && d.popularArtists.isEmpty() && d.recentAlbums.isEmpty() && d.liked.isEmpty()) {
                item {
                    EmptyState(
                        text = "Nothing cached yet. Go to Search, type an artist or album, and hit play — Notify downloads it automatically.",
                        actionText = "Search",
                        onAction = { onOpenSearch("") }
                    )
                }
            }
        }
    }
}

/** Spotify's pill search bar shown on the home tab. */
@Composable
fun HomeSearchBar(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Search, null, tint = Color(0xFF121212), modifier = Modifier.size(20.dp))
        Text(
            "What do you want to play?",
            color = Color(0xFF121212),
            fontSize = 16.sp,
            modifier = Modifier.padding(start = 10.dp)
        )
    }
}

/** Spotify's 2-column "recently played" tile with green-tinted background. */
@Composable
private fun HomeGridTile(
    item: Any,
    modifier: Modifier = Modifier,
    onOpenArtist: (String) -> Unit,
    onOpenAlbum: (String) -> Unit,
    onPlay: (Any) -> Unit
) {
    val title: String
    val sub: String
    val art: String?
    var onClick: () -> Unit = { onPlay(item) }
    when (item) {
        is Track -> {
            title = item.title; sub = item.displayArtist; art = item.displayArt
            onClick = { onPlay(item) }
        }
        is Album -> {
            title = item.title; sub = item.artist?.name ?: "Album"; art = item.image
            onClick = { onOpenAlbum(item.navId()) }
        }
        is Artist -> {
            title = item.name; sub = "Artist"; art = item.image
            onClick = { onOpenArtist(item.navId()) }
        }
        else -> { title = ""; sub = ""; art = null }
    }

    Row(
        modifier = modifier
            .padding(4.dp)
            .height(56.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF2E1E4F), Color(0xFF8F5CFF).copy(alpha = 0.25f), Color(0xFF121212))))
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
            Box(
                modifier = Modifier.size(56.dp).background(Color(0xFF333333)),
            contentAlignment = Alignment.Center
        ) {
            if (art != null) {
                AsyncImage(
                    model = imageUrl(art),
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = Color(0xFF121212),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Column(Modifier.padding(horizontal = 10.dp)) {
            Text(
                title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                sub,
                color = Color(0xFFB3B3B3),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
internal fun TrackList(
    tracks: List<Track>,
    playerVm: PlayerViewModel,
    isPlaying: Boolean,
    onOpenArtist: (String) -> Unit,
    onOpenAlbum: (String) -> Unit
) {
    Column {
        tracks.forEachIndexed { index, track ->
            TrackRow(
                track = track,
                index = index + 1,
                artUrl = imageUrl(track.displayArt),
                isCurrent = playerVm.isCurrent(track),
                isPlaying = isPlaying,
                onPlay = { playerVm.playQueue(tracks, index) },
                onOpenArtist = { track.artist?.id?.let(onOpenArtist) },
                onOpenAlbum = { track.album?.id?.let(onOpenAlbum) }
            )
        }
    }
}

@Composable
internal fun SectionTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        modifier = modifier.padding(start = 16.dp, top = 20.dp, bottom = 10.dp)
    )
}

@Composable
internal fun EmptyState(text: String, actionText: String? = null, onAction: (() -> Unit)? = null) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFFB3B3B3),
            textAlign = TextAlign.Center
        )
        if (actionText != null && onAction != null) {
            Button(
                onClick = onAction,
                modifier = Modifier.padding(top = 16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A2A), contentColor = Color.White),
                shape = RoundedCornerShape(50)
            ) {
                Text(actionText)
            }
        }
    }
}
