package com.notify.android.tv.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.notify.android.tv.ui.components.TvArtistShowcase
import com.notify.android.tv.ui.components.TvCard
import com.notify.android.tv.ui.components.TvSectionTitle
import com.notify.android.tv.ui.components.TvTrackList
import com.notify.core.ui.imageUrl
import com.notify.core.ui.navigation.navId
import com.notify.core.ui.homeViewModel
import com.notify.core.ui.player.PlayerViewModel

@Composable
fun TvHomeScreen(playerVm: PlayerViewModel, nav: TvNavState) {
    val vm = homeViewModel()
    val data by vm.data.collectAsState()

    val d = data

    LazyColumn(Modifier.fillMaxSize()) {
        // Full-bleed featured-artist hero (webapp ArtistShowcase) fills the top
        if (d != null && d.popularArtists.isNotEmpty()) {
            item {
                TvArtistShowcase(
                    artists = d.popularArtists,
                    onOpenArtist = { nav.navigate(TvScreen.Artist(it)) }
                )
            }
        }

        if (d != null) {
            if (d.popularAlbums.isNotEmpty()) {
                item { TvSectionTitle("Popular albums and singles") }
                item {
                    LazyRow(contentPadding = PaddingValues(horizontal = 40.dp)) {
                        items(d.popularAlbums) { album ->
                            TvCard(
                                imageUrl = imageUrl(album.image),
                                title = album.title,
                                subtitle = listOfNotNull("Album", album.artist?.name).joinToString(" • "),
                                onClick = { nav.navigate(TvScreen.Album(album.navId())) },
                                width = 212.dp
                            )
                        }
                    }
                }
            }

            if (d.recentAlbums.isNotEmpty()) {
                item { TvSectionTitle("Recently added albums") }
                item {
                    LazyRow(contentPadding = PaddingValues(horizontal = 40.dp)) {
                        items(d.recentAlbums) { album ->
                            TvCard(
                                imageUrl = imageUrl(album.image),
                                title = album.title,
                                subtitle = listOfNotNull("Album", album.artist?.name).joinToString(" • "),
                                onClick = { nav.navigate(TvScreen.Album(album.navId())) },
                                width = 212.dp
                            )
                        }
                    }
                }
            }

            if (d.popularTracks.isNotEmpty()) {
                item { TvSectionTitle("Popular songs") }
                item {
                    TvTrackList(
                        tracks = d.popularTracks,
                        playerVm = playerVm
                    )
                }
            }
        }
    }
}
