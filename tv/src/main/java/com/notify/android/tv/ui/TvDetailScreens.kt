package com.notify.android.tv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.notify.android.tv.ui.components.TvCard
import com.notify.android.tv.ui.components.TvSectionTitle
import com.notify.android.tv.ui.components.TvTrackList
import com.notify.core.model.DiscoverAlbum
import com.notify.core.ui.albumViewModel
import com.notify.core.ui.artistViewModel
import com.notify.core.ui.imageUrl
import com.notify.core.ui.navigation.navId
import com.notify.core.ui.player.PlayerViewModel

@Composable
fun TvArtistScreen(artistId: String, playerVm: PlayerViewModel, nav: TvNavState) {
    val vm = artistViewModel(artistId)
    val data by vm.data.collectAsState()
    val discover by vm.discover.collectAsState()

    val isDiscover = vm.isDiscover
    val artist = data?.artist
    val discArtist = discover?.artist
    val tracks = (data?.popularTracks?.ifEmpty { data?.tracks }) ?: discover?.popularTracks?.ifEmpty { discover?.tracks }.orEmpty()

    data class ArtistAlbum(val navId: String?, val title: String, val subtitle: String, val image: String?)
    val albums = buildList {
        data?.albums?.forEach { a ->
            add(ArtistAlbum(a.navId().ifEmpty { null }, a.title, listOfNotNull(a.artist?.name, a.year?.toString()).joinToString(" · "), a.image))
        }
        discover?.albums?.forEach { a ->
            add(ArtistAlbum(if (a.href != null || a.mbid != null) a.navId().ifEmpty { null } else null, a.title, listOfNotNull(a.artist?.name, a.year?.toString()).joinToString(" · "), a.image))
        }
    }

    val name = artist?.name ?: discArtist?.name ?: ""
    val hero = imageUrl(artist?.image ?: discArtist?.image)

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Row(
                modifier = Modifier.padding(start = 40.dp, top = 36.dp, end = 40.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                TvHeroArt(hero, name, rounded = true, size = 160.dp)
                Column(Modifier.padding(start = 24.dp)) {
                    Text(name, color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Bold)
                    val meta = mutableListOf<String>()
                    artist?.trackCount?.let { meta.add("$it songs") }
                    artist?.albumCount?.let { meta.add("$it albums") }
                    artist?.genres?.take(3)?.let { meta.addAll(it) }
                    if (discArtist != null) meta.add("Spotify")
                    Text(
                        meta.joinToString(" · "),
                        color = Color(0xFFB3B3B3),
                        fontSize = 16.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        item {
            Row(Modifier.padding(start = 40.dp, top = 20.dp)) {
                com.notify.android.tv.ui.components.TvPlayButton(
                    onClick = { playerVm.playQueue(tracks, 0) }
                )
                Spacer(Modifier.width(16.dp))
                if (!isDiscover && artist != null) {
                    com.notify.android.tv.ui.components.TvHeartButton(
                        liked = artist.liked == true,
                        onClick = { vm.toggleLike() }
                    )
                }
            }
        }

        if (tracks.isNotEmpty()) {
            item { TvSectionTitle("Popular") }
            item {
                TvTrackList(
                    tracks = tracks.take(10),
                    playerVm = playerVm
                )
            }
        }

        if (albums.isNotEmpty()) {
            item { TvSectionTitle("Discography") }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 40.dp)) {
                    items(albums) { album ->
                        TvCard(
                            imageUrl = imageUrl(album.image),
                            title = album.title,
                            subtitle = album.subtitle,
                            onClick = { album.navId?.let { nav.navigate(TvScreen.Album(it)) } },
                            width = 220.dp
                        )
                    }
                }
            }
        }

        artist?.bio?.let { bio ->
            item { TvSectionTitle("About $name") }
            item {
                Text(
                    bio,
                    color = Color(0xFFB3B3B3),
                    fontSize = 15.sp,
                    modifier = Modifier.padding(horizontal = 40.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun TvAlbumScreen(albumId: String, playerVm: PlayerViewModel, nav: TvNavState) {
    val vm = albumViewModel(albumId)
    val data by vm.data.collectAsState()
    val discover by vm.discover.collectAsState()

    val isDiscover = vm.isDiscover
    val album = data?.album
    val discAlbum = discover?.album
    val tracks = data?.tracks.orEmpty().ifEmpty { discover?.tracks.orEmpty() }

    val title = album?.title ?: discAlbum?.title ?: ""
    val artistName = album?.artist?.name ?: discAlbum?.artist?.name
    val hero = imageUrl(album?.image ?: discAlbum?.image)

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Row(
                modifier = Modifier.padding(start = 40.dp, top = 36.dp, end = 40.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                TvHeroArt(hero, title, rounded = false, size = 160.dp)
                Column(Modifier.padding(start = 24.dp)) {
                    Text("ALBUM", color = Color(0xFFB3B3B3), fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                    Text(title, color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Bold)
                    if (artistName != null) {
                        Text(artistName, color = Color.White, fontSize = 18.sp, modifier = Modifier.padding(top = 6.dp))
                    }
                    val meta = mutableListOf<String>()
                    (album?.year ?: discAlbum?.year)?.let { meta.add(it.toString()) }
                    if (tracks.isNotEmpty()) meta.add("${tracks.size} songs")
                    Text(meta.joinToString(" · "), color = Color(0xFFB3B3B3), fontSize = 15.sp, modifier = Modifier.padding(top = 6.dp))
                }
            }
        }

        item {
            Row(Modifier.padding(start = 40.dp, top = 20.dp)) {
                com.notify.android.tv.ui.components.TvPlayButton(
                    onClick = { playerVm.playQueue(tracks, 0) }
                )
                Spacer(Modifier.width(16.dp))
                if (!isDiscover && album != null) {
                    com.notify.android.tv.ui.components.TvInputButton(text = "Save", onClick = { vm.toggleLike() })
                }
            }
        }

        item {
            TvTrackList(
                tracks = tracks,
                playerVm = playerVm
            )
        }
    }
}

@Composable
fun TvHeroArt(url: String?, title: String, rounded: Boolean, size: androidx.compose.ui.unit.Dp) {
    val shape = if (rounded) CircleShape else RoundedCornerShape(10.dp)
    Box(
        modifier = Modifier
            .size(size)
            .clip(shape)
            .background(Color(0xFF2A2A2A)),
        contentAlignment = Alignment.Center
    ) {
        if (url != null) {
            AsyncImage(model = url, contentDescription = title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Text(title.firstOrNull()?.uppercase() ?: "♪", color = Color(0xFF7A7A7A), fontSize = 40.sp, fontWeight = FontWeight.Bold)
        }
    }
}
