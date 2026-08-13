package com.notify.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.notify.core.data.SessionManager
import com.notify.core.di.AppContainer
import com.notify.core.di.NotifyApplication
import com.notify.core.ui.auth.AuthViewModel
import com.notify.core.ui.auth.RootViewModel
import com.notify.core.ui.player.PlayerViewModel
import com.notify.core.ui.viewmodels.*
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY

@Composable
fun appContainer(): AppContainer =
    (LocalContext.current.applicationContext as NotifyApplication).container

@Composable
fun sessionManager(): SessionManager = appContainer().session

@Composable
fun imageUrl(path: String?): String? = appContainer().session.imageUrl(path)

inline fun <reified VM : ViewModel> notifyFactory(
    crossinline create: (AppContainer) -> VM
): ViewModelProvider.Factory = viewModelFactory {
    initializer {
        val app = (this[APPLICATION_KEY] as NotifyApplication)
        create(app.container)
    }
}

@Composable
fun rootViewModel() = viewModel<RootViewModel>(factory = notifyFactory { RootViewModel(it) })
@Composable
fun authViewModel() = viewModel<AuthViewModel>(factory = notifyFactory { AuthViewModel(it) })
@Composable
fun playerViewModel() = viewModel<PlayerViewModel>(factory = notifyFactory { PlayerViewModel(it) })
@Composable
fun homeViewModel() = viewModel<HomeViewModel>(factory = notifyFactory { HomeViewModel(it) })
@Composable
fun searchViewModel(initialQuery: String = "") =
    viewModel<SearchViewModel>(
        key = "search-$initialQuery",
        factory = notifyFactory { SearchViewModel(it, initialQuery) }
    )
@Composable
fun libraryViewModel() = viewModel<LibraryViewModel>(factory = notifyFactory { LibraryViewModel(it) })
@Composable
fun playlistsViewModel() = viewModel<PlaylistsViewModel>(factory = notifyFactory { PlaylistsViewModel(it) })

@Composable
fun playlistViewModel(playlistId: String) =
    viewModel<PlaylistViewModel>(
        key = "playlist-$playlistId",
        factory = notifyFactory { PlaylistViewModel(it, playlistId) }
    )

@Composable
fun artistViewModel(artistId: String) =
    viewModel<ArtistViewModel>(
        key = "artist-$artistId",
        factory = notifyFactory { ArtistViewModel(it, artistId) }
    )

@Composable
fun albumViewModel(albumId: String) =
    viewModel<AlbumViewModel>(
        key = "album-$albumId",
        factory = notifyFactory { AlbumViewModel(it, albumId) }
    )

@Composable
fun settingsViewModel() = viewModel<SettingsViewModel>(factory = notifyFactory { SettingsViewModel(it) })
@Composable
fun offlineViewModel() = viewModel<OfflineViewModel>(factory = notifyFactory { OfflineViewModel(it) })
