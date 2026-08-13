package com.notify.android.mobile.ui.artist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.notify.android.mobile.ui.components.Artwork
import com.notify.android.mobile.ui.components.MediaCard
import com.notify.android.mobile.ui.components.TrackRow
import com.notify.android.mobile.ui.home.EmptyState
import com.notify.android.mobile.ui.playlist.PlayBigButton
import com.notify.core.ui.player.PlayerViewModel
import com.notify.core.ui.navigation.navId
import com.notify.core.ui.artistViewModel
import com.notify.core.ui.viewmodels.imageUrl
import com.notify.core.model.Artist
import com.notify.core.model.DiscoverAlbum
import com.notify.core.model.Track
import kotlinx.coroutines.launch

@Composable
fun ArtistScreen(
    artistId: String,
    playerVm: PlayerViewModel,
    onBack: () -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenSearch: (String) -> Unit
) {
    val vm = artistViewModel(artistId)
    val dataState by vm.data.collectAsState()
    val discoverState by vm.discover.collectAsState()
    val error by vm.error.collectAsState()
    val toast by vm.toast.collectAsState()
    val isPlaying by playerVm.playing.collectAsState()
    val scope = rememberCoroutineScope()

    val data = dataState
    val discover = discoverState
    val isDiscover = vm.isDiscover

    val artist: Artist? = data?.artist
    val discArtist = discover?.artist
    val tracks: List<Track> = (data?.popularTracks?.ifEmpty { data?.tracks }) ?: discover?.popularTracks?.ifEmpty { discover?.tracks }.orEmpty()

    // Unify discography from both library and Spotify detail responses.
    data class ArtistAlbum(
        val navId: String?,
        val title: String,
        val subtitle: String,
        val image: String?,
        val playable: Boolean,
        val album: DiscoverAlbum?
    )
    val albums = buildList {
        data?.albums?.forEach { a ->
            add(
                ArtistAlbum(
                    navId = a.navId().ifEmpty { null },
                    title = a.title,
                    subtitle = listOfNotNull(a.artist?.name, a.year?.toString()).joinToString(" · ") + if (a.albumType == "album") " · Album" else "",
                    image = a.image,
                    playable = false,
                    album = null
                )
            )
        }
        discover?.albums?.forEach { a ->
            add(
                ArtistAlbum(
                    navId = if (a.href != null || a.mbid != null) a.navId().ifEmpty { null } else null,
                    title = a.title,
                    subtitle = listOfNotNull(a.artist?.name, a.year?.toString()).joinToString(" · "),
                    image = a.image,
                    playable = a.href == null && a.mbid == null,
                    album = a
                )
            )
        }
    }

    val name = artist?.name ?: discArtist?.name ?: ""
    val heroImage = imageUrl(artist?.image ?: discArtist?.image)
    val metaParts = mutableListOf<String>()
    val libArtist = data?.artist
    if (libArtist?.trackCount != null) metaParts.add("${libArtist.trackCount} songs")
    if (libArtist?.albumCount != null) metaParts.add("${libArtist.albumCount} albums")
    if (discArtist != null) metaParts.add("Spotify")
    artist?.genres?.take(3)?.let { metaParts.addAll(it) }

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Box(Modifier.fillMaxWidth()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .background(heroGradient(name))
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                }
                Artwork(
                    url = heroImage,
                    contentDescription = name,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                        .size(140.dp),
                    shape = if (!isDiscover) RoundedCornerShape(12.dp) else androidx.compose.foundation.shape.CircleShape
                )
            }
        }

        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Text(name, style = MaterialTheme.typography.displaySmall, color = Color.White)
                if (metaParts.isNotEmpty()) {
                    Text(
                        metaParts.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFB3B3B3),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    val isPlayingArtist = tracks.any { playerVm.isCurrent(it) }
                    PlayBigButton(
                        isPlaying = isPlayingArtist && isPlaying,
                        onClick = {
                            if (isPlayingArtist) playerVm.toggle()
                            else playerVm.playQueue(tracks, 0)
                        }
                    )
                    Spacer(Modifier.width(16.dp))
                    val liked = artist?.liked == true
                    IconButton(
                        onClick = {
                            if (isDiscover) {
                                // toast shown via VM
                            } else {
                                vm.toggleLike()
                            }
                        },
                        colors = IconButtonDefaults.iconButtonColors(contentColor = if (liked) Color(0xFF8F5CFF) else Color.White)
                    ) {
                        Icon(
                            if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            if (liked) "Following" else "Follow artist",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }

        if (tracks.isNotEmpty()) {
            item {
                Text(
                    "Popular",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
                )
            }
            item {
                Column {
                    tracks.take(10).forEachIndexed { index, track ->
                        TrackRow(
                            track = track,
                            index = index + 1,
                            artUrl = imageUrl(track.displayArt),
                            isCurrent = playerVm.isCurrent(track),
                            isPlaying = isPlaying,
                            onPlay = { playerVm.playQueue(tracks, index) },
                            showArtist = true
                        )
                    }
                }
            }
        }

        if (albums.isNotEmpty()) {
            item {
                Text(
                    "Discography",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 10.dp)
                )
            }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp)) {
                    items(albums) { album ->
                        MediaCard(
                            imageUrl = imageUrl(album.image),
                            title = album.title,
                            subtitle = album.subtitle,
                            onClick = {
                                val nav = album.navId
                                if (nav != null) {
                                    onOpenAlbum(nav)
                                } else {
                                    scope.launch {
                                        album.album?.let { discAlbum ->
                                            runCatching { vm.playAlbumDiscover(discAlbum) }
                                                .onSuccess { if (it.isNotEmpty()) playerVm.playQueue(it, 0) }
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.width(150.dp)
                        )
                    }
                }
            }
        }

        if (libArtist?.similar?.isNotEmpty() == true) {
            item {
                Text(
                    "Fans also like",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 10.dp)
                )
            }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp)) {
                    items(libArtist.similar) { similarName ->
                        Surface(
                            onClick = { onOpenSearch(similarName) },
                            shape = RoundedCornerShape(50),
                            color = Color(0xFF2A2A2A),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                similarName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        artist?.bio?.let { bio ->
            item {
                Text(
                    "About $name",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
                )
                Text(
                    bio,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFB3B3B3),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        if (data == null && discover == null && error == null) {
            item {
                Box(Modifier.fillMaxWidth().padding(60.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF2A2A2A))
                }
            }
        }
        if (error != null) {
            item { EmptyState(error ?: "Not found") }
        }
    }
}

private fun heroGradient(name: String) =
    androidx.compose.ui.graphics.Brush.verticalGradient(
        listOf(
            androidx.compose.ui.graphics.Color.hsl(hashHue(name), 0.45f, 0.18f),
            androidx.compose.ui.graphics.Color.hsl(hashHue(name), 0.3f, 0.12f),
            Color.Transparent
        )
    )

private fun hashHue(s: String): Float {
    var h = 0
    for (c in s) h = (h * 31 + c.code) % 360
    return h.toFloat()
}
