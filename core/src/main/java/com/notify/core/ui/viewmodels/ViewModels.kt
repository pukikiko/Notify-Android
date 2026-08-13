package com.notify.core.ui.viewmodels

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.notify.core.data.ActiveDownload
import com.notify.core.data.ApiException
import com.notify.core.data.Instance
import com.notify.core.data.OfflineCollection
import com.notify.core.data.OfflineTrack
import com.notify.core.di.AppContainer
import com.notify.core.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Shared base exposing the app container to screen ViewModels. */
abstract class NotifyViewModel(container: AppContainer) :
    AndroidViewModel(container.appContext as Application) {
    protected val container = container
    protected val api get() = container.api
    protected val session get() = container.session
    protected val offlineStore get() = container.offlineStore
}

@Composable
fun sessionManager(): com.notify.core.data.SessionManager =
    com.notify.core.ui.appContainer().session

@Composable
fun imageUrl(path: String?): String? =
    com.notify.core.ui.appContainer().session.imageUrl(path)

class HomeViewModel(container: AppContainer) : NotifyViewModel(container) {
    private val _data = MutableStateFlow<HomeData?>(null)
    val data: StateFlow<HomeData?> = _data.asStateFlow()

    private val _downloads = MutableStateFlow<List<Track>>(emptyList())
    val downloads: StateFlow<List<Track>> = _downloads.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        refresh()
        viewModelScope.launch {
            while (true) {
                delay(4000)
                refreshDownloads()
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            runCatching { api.home() }
                .onSuccess { _data.value = it }
                .onFailure { e -> _error.value = e.message }
        }
    }

    private fun refreshDownloads() {
        viewModelScope.launch {
            runCatching { api.downloads() }
                .onSuccess { _downloads.value = it.downloads }
        }
    }
}

class SearchViewModel(container: AppContainer, initialQuery: String = "") : NotifyViewModel(container) {
    private val _query = MutableStateFlow(initialQuery)
    val query: StateFlow<String> = _query.asStateFlow()

    private val _lib = MutableStateFlow<SearchResponse?>(null)
    val lib: StateFlow<SearchResponse?> = _lib.asStateFlow()

    private val _disc = MutableStateFlow<DiscoverResult?>(null)
    val disc: StateFlow<DiscoverResult?> = _disc.asStateFlow()

    private val _searching = MutableStateFlow(false)
    val searching: StateFlow<Boolean> = _searching.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun setQuery(q: String) {
        _query.value = q
        search(q)
    }

    private var searchJob: kotlinx.coroutines.Job? = null

    init {
        if (initialQuery.isNotBlank()) search(initialQuery)
    }

    fun search(q: String) {
        searchJob?.cancel()
        if (q.isBlank()) {
            _lib.value = null
            _disc.value = null
            _searching.value = false
            return
        }
        searchJob = viewModelScope.launch {
            delay(300)
            _searching.value = true
            runCatching { api.librarySearch(q) }
                .onSuccess { _lib.value = it }
                .onFailure { e -> _error.value = "lib: ${e.message}" }
            runCatching { api.discoverSearch(q) }
                .onSuccess { _disc.value = it }
                .onFailure { e -> _error.value = "disc: ${e.message}" }
            _searching.value = false
        }
    }

    suspend fun playNow(payload: DiscoverPlayRequest): List<Track> {
        _busy.value = true
        return try {
            val res = api.discoverPlay(payload)
            if (res.tracks.isEmpty()) throw Exception("Nothing playable found")
            res.tracks
        } finally {
            _busy.value = false
        }
    }
}

class LibraryViewModel(container: AppContainer) : NotifyViewModel(container) {
    private val _tracks = MutableStateFlow<List<Track>?>(null)
    val tracks: StateFlow<List<Track>?> = _tracks.asStateFlow()
    private val _albums = MutableStateFlow<List<Album>?>(null)
    val albums: StateFlow<List<Album>?> = _albums.asStateFlow()
    private val _artists = MutableStateFlow<List<Artist>?>(null)
    val artists: StateFlow<List<Artist>?> = _artists.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _loading.value = true
            runCatching {
                val t = api.likedTracks()
                val al = api.likedAlbums()
                val ar = api.likedArtists()
                _tracks.value = t.tracks
                _albums.value = al.albums
                _artists.value = ar.artists
            }
            _loading.value = false
        }
    }
}

class PlaylistsViewModel(container: AppContainer) : NotifyViewModel(container) {
    private val _playlists = MutableStateFlow<List<Playlist>?>(null)
    val playlists: StateFlow<List<Playlist>?> = _playlists.asStateFlow()

    private val _creating = MutableStateFlow(false)
    val creating: StateFlow<Boolean> = _creating.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            runCatching { api.playlists() }
                .onSuccess { _playlists.value = it.playlists }
                .onFailure { e -> _error.value = e.message }
        }
    }

    fun toggleCreate() { _creating.value = !_creating.value }

    fun create(name: String, onCreated: (String) -> Unit) {
        if (name.isBlank()) return
        viewModelScope.launch {
            runCatching { api.createPlaylist(name) }
                .onSuccess { _creating.value = false; refresh(); onCreated(it.playlist.id) }
                .onFailure { e -> _error.value = e.message }
        }
    }
}

class PlaylistViewModel(
    container: AppContainer,
    private val playlistId: String
) : NotifyViewModel(container) {
    private val _data = MutableStateFlow<PlaylistResponse?>(null)
    val data: StateFlow<PlaylistResponse?> = _data.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Track>>(emptyList())
    val searchResults: StateFlow<List<Track>> = _searchResults.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            runCatching { api.playlist(playlistId) }
                .onSuccess { _data.value = it }
                .onFailure { e -> _error.value = e.message }
        }
    }

    fun searchLibrary(q: String) {
        if (q.isBlank()) { _searchResults.value = emptyList(); return }
        viewModelScope.launch {
            runCatching { api.librarySearch(q) }
                .onSuccess { _searchResults.value = it.tracks }
        }
    }

    fun addTrack(trackId: String) {
        viewModelScope.launch {
            runCatching { api.addTracksToPlaylist(playlistId, listOf(trackId)) }
                .onSuccess {
                    _toast.value = "Added to playlist"
                    refresh()
                }
        }
    }

    fun removeTrack(trackId: String) {
        viewModelScope.launch {
            runCatching { api.removeTrackFromPlaylist(playlistId, trackId) }
                .onSuccess { refresh() }
        }
    }

    fun deletePlaylist(onDeleted: () -> Unit) {
        viewModelScope.launch {
            runCatching { api.deletePlaylist(playlistId) }
                .onSuccess { onDeleted() }
        }
    }

    fun rename(name: String) {
        viewModelScope.launch {
            runCatching { api.renamePlaylist(playlistId, name) }
                .onSuccess { refresh() }
        }
    }
}

class ArtistViewModel(
    container: AppContainer,
    private val artistId: String
) : NotifyViewModel(container) {
    private val _data = MutableStateFlow<ArtistDetailResponse?>(null)
    val data: StateFlow<ArtistDetailResponse?> = _data.asStateFlow()

    private val _discover = MutableStateFlow<DiscoverArtistDetail?>(null)
    val discover: StateFlow<DiscoverArtistDetail?> = _discover.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    val isDiscover = artistId.toLongOrNull() == null

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            if (isDiscover) {
                val key = if (artistId.startsWith("sp-")) artistId.substring(3) else artistId
                runCatching { api.discoverArtist(key) }
                    .onSuccess { _discover.value = it }
                    .onFailure { e -> _error.value = e.message }
            } else {
                runCatching { api.artistDetail(artistId) }
                    .onSuccess { _data.value = it }
                    .onFailure { e -> _error.value = e.message }
            }
        }
    }

    fun toggleLike() {
        if (isDiscover) {
            _toast.value = "Save it by playing something first"
            return
        }
        val current = _data.value ?: return
        viewModelScope.launch {
            runCatching { api.likeArtist(artistId) }
                .onSuccess {
                    _data.value = current.copy(artist = current.artist.copy(liked = it.liked))
                    _toast.value = if (it.liked) "Added to Liked Artists" else "Removed from Liked Artists"
                }
        }
    }

    suspend fun playAlbumDiscover(a: DiscoverAlbum): List<Track> {
        return api.discoverPlay(
            DiscoverPlayRequest(
                kind = "album",
                artist = a.artist?.name,
                album = a.title,
                releaseMbid = a.mbid,
                image = a.image
            )
        ).tracks
    }
}

class AlbumViewModel(
    container: AppContainer,
    private val albumId: String
) : NotifyViewModel(container) {
    private val _data = MutableStateFlow<AlbumDetailResponse?>(null)
    val data: StateFlow<AlbumDetailResponse?> = _data.asStateFlow()

    private val _discover = MutableStateFlow<DiscoverAlbumDetail?>(null)
    val discover: StateFlow<DiscoverAlbumDetail?> = _discover.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    val isDiscover = albumId.toLongOrNull() == null

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            if (isDiscover) {
                val key = if (albumId.startsWith("sp-")) albumId.substring(3) else albumId
                runCatching { api.discoverAlbum(key) }
                    .onSuccess { _discover.value = it }
                    .onFailure { e -> _error.value = e.message }
            } else {
                runCatching { api.albumDetail(albumId) }
                    .onSuccess { _data.value = it }
                    .onFailure { e -> _error.value = e.message }
            }
        }
    }

    fun toggleLike() {
        if (isDiscover) {
            _toast.value = "Save it by playing something first"
            return
        }
        val current = _data.value ?: return
        viewModelScope.launch {
            runCatching { api.likeAlbum(albumId) }
                .onSuccess {
                    _data.value = current.copy(album = current.album.copy(liked = it.liked))
                    _toast.value = if (it.liked) "Added to Liked Albums" else "Removed from Liked Albums"
                }
        }
    }
}

class SettingsViewModel(container: AppContainer) : NotifyViewModel(container) {
    private val _status = MutableStateFlow<Status?>(null)
    val status: StateFlow<Status?> = _status.asStateFlow()

    private val _counts = MutableStateFlow<UserCounts?>(null)
    val counts: StateFlow<UserCounts?> = _counts.asStateFlow()

    private val _instances = MutableStateFlow<List<Instance>>(emptyList())
    val instances: StateFlow<List<Instance>> = _instances.asStateFlow()

    private val _activeId = MutableStateFlow<String?>(null)
    val activeId: StateFlow<String?> = _activeId.asStateFlow()

    private val _offlineCount = MutableStateFlow(0)
    val offlineCount: StateFlow<Int> = _offlineCount.asStateFlow()

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    val currentFormat: String get() = session.preferredFormat
    val currentUser: String? get() = session.username
    val currentBaseUrl: String get() = session.baseUrl

    init {
        refreshStatus()
        viewModelScope.launch {
            session.instances.collect {
                _instances.value = it
                _activeId.value = session.currentInstance?.id
            }
        }
        viewModelScope.launch {
            offlineStore.offlineTracks.collect { tracks ->
                _offlineCount.value = tracks.count { it.instanceId == session.currentInstance?.id }
            }
        }
    }

    fun refreshStatus() {
        viewModelScope.launch {
            runCatching { api.status() }.onSuccess { _status.value = it }
            runCatching { api.userCounts() }.onSuccess { _counts.value = it }
        }
    }

    fun saveFormat(format: String) {
        viewModelScope.launch {
            runCatching { api.updateSettings(preferredFormat = format) }
                .onSuccess {
                    session.update { it.copy(preferredFormat = format) }
                    _saved.value = true
                    kotlinx.coroutines.delay(1800)
                    _saved.value = false
                }
        }
    }

    fun logout() {
        viewModelScope.launch {
            runCatching { api.logout() }
            session.logout()
        }
    }

    fun selectInstance(id: String) {
        viewModelScope.launch { session.setActive(id) }
    }

    fun removeInstance(id: String) {
        viewModelScope.launch {
            val removedActive = session.currentInstance?.id == id
            container.instancesRepository.remove(id)
            if (removedActive) session.setActive(session.currentInstance?.id ?: "")
        }
    }
}

class OfflineViewModel(container: AppContainer) : NotifyViewModel(container) {
    private val _tracks = MutableStateFlow<List<OfflineTrack>>(emptyList())
    val tracks: StateFlow<List<OfflineTrack>> = _tracks.asStateFlow()

    private val _downloads = MutableStateFlow<Map<String, ActiveDownload>>(emptyMap())
    val downloads: StateFlow<Map<String, ActiveDownload>> = _downloads.asStateFlow()

    init {
        viewModelScope.launch {
            offlineStore.offlineTracks.collect {
                _tracks.value = it.filter { ot -> ot.instanceId == session.currentInstance?.id }
            }
            container.offlineDownloads.downloads.collect { _downloads.value = it }
        }
    }

    fun downloadCollection(tracks: List<Track>, collection: OfflineCollection) {
        viewModelScope.launch {
            val resolved = mutableListOf<Track>()
            for (t in tracks) {
                if (t.isResolved) {
                    resolved.add(t)
                } else {
                    runCatching {
                        api.discoverPlay(
                            DiscoverPlayRequest(
                                kind = "track",
                                artist = t.artist?.name,
                                album = t.album?.title,
                                title = t.title,
                                mbid = t.mbid,
                                image = t.image ?: t.artUrl,
                                duration = t.duration
                            )
                        )
                    }.onSuccess { r -> r.tracks.firstOrNull()?.let(resolved::add) }
                }
            }
            container.offlineDownloads.downloadCollection(resolved, collection)
        }
    }

    fun removeCollection(collection: OfflineCollection) {
        viewModelScope.launch { container.offlineDownloads.removeCollection(collection) }
    }

    fun cancel(trackId: String) {
        container.offlineDownloads.cancelDownload(trackId)
    }

    fun remove(trackId: String) {
        viewModelScope.launch {
            val instanceId = session.currentInstance?.id ?: return@launch
            container.offlineDownloads.removeDownload(instanceId, trackId)
        }
    }

    fun removeAll() {
        viewModelScope.launch {
            val instanceId = session.currentInstance?.id ?: return@launch
            offlineStore.removeAllForInstance(instanceId)
            _downloads.value = emptyMap()
        }
    }
}
