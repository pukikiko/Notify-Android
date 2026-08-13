package com.notify.core.ui.player

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.notify.core.data.SessionManager
import com.notify.core.di.AppContainer
import com.notify.core.model.Track
import com.notify.core.player.PlayerEngine
import com.notify.core.player.PlayerService
import com.notify.core.player.RepeatMode
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PlayerViewModel(private val container: AppContainer) : AndroidViewModel(container.appContext as Application) {

    private val session: SessionManager = container.session

    init {
        // Bind the shared player to the media session service so playback keeps
        // running (with a notification / lock screen card) in the background.
        // Media3 promotes the service to foreground itself once playback starts.
        val app = getApplication<Application>()
        try {
            app.startService(Intent(app, PlayerService::class.java))
        } catch (_: Exception) {
            // Service already running or start blocked — the engine is shared.
        }
    }

    private val engine: PlayerEngine = container.playerEngine

    val queue: StateFlow<List<Track>> = engine.queue
    val index: StateFlow<Int> = engine.index
    val current: StateFlow<Track?> = engine.current
    val playing: StateFlow<Boolean> = engine.playing
    val preparing: StateFlow<Boolean> = engine.preparing
    val buffering: StateFlow<Boolean> = engine.buffering
    val shuffle: StateFlow<Boolean> = engine.shuffle
    val repeat: StateFlow<RepeatMode> = engine.repeat
    val volume: StateFlow<Float> = engine.volume
    val position: StateFlow<Long> = engine.position
    val duration: StateFlow<Long> = engine.duration

    fun playQueue(tracks: List<Track>, startIndex: Int = 0) = engine.playQueue(tracks, startIndex)
    fun toggle() = engine.toggle()
    fun next() = engine.advanceForward()
    fun previous() = engine.advanceBackward()
    fun seekTo(ms: Long) = engine.seekTo(ms)
    fun setVolume(v: Float) = engine.setVolume(v)
    fun toggleShuffle() = engine.toggleShuffle()
    fun cycleRepeat() = engine.cycleRepeat()
    fun loadIndex(idx: Int) = engine.loadIndex(idx)

    fun isPlayingTrack(track: Track): Boolean {
        val c = current.value ?: return false
        return c.id == track.id
    }

    /** True if this track is the one currently loaded in the player. */
    fun isCurrent(track: Track): Boolean {
        val c = current.value ?: return false
        return c.id == track.id
    }

    fun toggleLikeCurrent() {
        val c = current.value ?: return
        if (c.id.toLongOrNull() == null) return
        viewModelScope.launch {
            try {
                val res = container.api.likeTrack(c.id)
                engine.replaceCurrent(c.copy(liked = res.liked))
            } catch (e: Exception) {
                // transient — ignore
            }
        }
    }

    fun currentArtUrl(): String? {
        val c = current.value ?: return null
        return session.imageUrl(c.displayArt)
    }

    fun currentArtistId(): String? {
        val c = current.value ?: return null
        return c.artist?.id?.takeIf { it.toLongOrNull() != null }
    }

    fun currentAlbumId(): String? {
        val c = current.value ?: return null
        return c.album?.id?.takeIf { it.toLongOrNull() != null }
    }

    override fun onCleared() {
        // The shared engine is owned by PlayerService; the UI must not release
        // it here or background playback would stop when the activity dies.
        super.onCleared()
    }
}
