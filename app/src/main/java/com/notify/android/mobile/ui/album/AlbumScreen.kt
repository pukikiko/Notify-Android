package com.notify.android.mobile.ui.album

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
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
import com.notify.android.mobile.ui.playlist.PlayBigButton
import com.notify.core.ui.player.PlayerViewModel
import com.notify.core.ui.navigation.navId
import com.notify.core.ui.albumViewModel
import com.notify.core.ui.viewmodels.imageUrl
import com.notify.core.model.Track

@Composable
fun AlbumScreen(
    albumId: String,
    playerVm: PlayerViewModel,
    onBack: () -> Unit,
    onOpenArtist: (String) -> Unit
) {
    val vm = albumViewModel(albumId)
    val data by vm.data.collectAsState()
    val discover by vm.discover.collectAsState()
    val error by vm.error.collectAsState()
    val isPlaying by playerVm.playing.collectAsState()

    val isDiscover = vm.isDiscover

    val album = data?.album
    val discAlbum = discover?.album
    val tracks: List<Track> = data?.tracks.orEmpty().ifEmpty { discover?.tracks.orEmpty() }

    val title = album?.title ?: discAlbum?.title ?: ""
    val artistName = album?.artist?.name ?: discAlbum?.artist?.name
    val year = album?.year ?: discAlbum?.year
    val heroImage = imageUrl(album?.image ?: discAlbum?.image)

    val totalMin = tracks.sumOf { (it.duration ?: 0.0).toLong() } / 60
    val metaParts = mutableListOf<String>()
    if (year != null) metaParts.add(year.toString())
    if (tracks.isNotEmpty()) metaParts.add("${tracks.size} songs" + if (totalMin > 0) ", $totalMin min" else "")

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Box(Modifier.fillMaxWidth()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .background(heroGradient("${title}${artistName ?: ""}"))
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
                    contentDescription = title,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                        .size(140.dp)
                )
            }
        }

        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Text("ALBUM", style = MaterialTheme.typography.labelSmall, color = Color(0xFFB3B3B3))
                Text(
                    title,
                    style = MaterialTheme.typography.displaySmall,
                    color = Color.White,
                    modifier = Modifier.padding(top = 4.dp)
                )
                if (artistName != null) {
                    Text(
                        artistName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .clickable {
                            val artist = album?.artist ?: discAlbum?.artist
                            val target = artist?.id?.takeIf { it.isNotBlank() && it.toLongOrNull() != null }
                                ?: artist?.mbid?.let { "sp-$it" }
                            if (target != null) onOpenArtist(target)
                        }
                    )
                }
                if (metaParts.isNotEmpty()) {
                    Text(
                        metaParts.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFB3B3B3),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    val isPlayingAlbum = tracks.any { playerVm.isCurrent(it) }
                    PlayBigButton(
                        isPlaying = isPlayingAlbum && isPlaying,
                        onClick = {
                            if (isPlayingAlbum) playerVm.toggle()
                            else playerVm.playQueue(tracks, 0)
                        }
                    )
                    Spacer(Modifier.width(16.dp))
                    val liked = album?.liked == true
                    IconButton(
                        onClick = { vm.toggleLike() },
                        colors = IconButtonDefaults.iconButtonColors(contentColor = if (liked) Color(0xFF8F5CFF) else Color.White)
                    ) {
                        Icon(
                            if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            if (liked) "Saved" else "Save to library",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    DownloadCollectionButton(
                        tracks = tracks,
                        collection = com.notify.core.data.OfflineCollection("album", albumId, title)
                    )
                }
            }
        }

        if (tracks.isNotEmpty()) {
            item {
                Column(Modifier.padding(top = 16.dp)) {
                    tracks.forEachIndexed { index, track ->
                        TrackRow(
                            track = track,
                            index = track.trackNo ?: index + 1,
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
            androidx.compose.ui.graphics.Color.hsl(hashHue(name), 0.45f, 0.22f),
            androidx.compose.ui.graphics.Color.hsl(hashHue(name), 0.3f, 0.14f),
            Color.Transparent
        )
    )

private fun hashHue(s: String): Float {
    var h = 0
    for (c in s) h = (h * 31 + c.code) % 360
    return h.toFloat()
}
